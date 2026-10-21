// Agente Veículo de Emergência, que apresenta um comportamento prioritário

package traffic;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import java.util.*;

public class EmergencyVehicleAgent extends Agent {
    // Variáveis de estado (semelhante ao VehicleAgent)
    private String vid;
    private double x, y;
    private double destX, destY;
    private double startX, startY;
    private String destinationName = "Emergencia";
    private double speed = 5.5; // Velocidade superior para veículos de emergência
    private String direction;
    private Random rnd = new Random();
    private boolean isWaiting = false;
    private boolean hasAskedPermission = false;
    private String lastLogLight = "";

    // Métricas 
    private long tripStartTime;
    private double accumulatedCost = 0.0;
    private boolean finishedCycle = false;
    private boolean forcingExit = false;
    
    private int failureCount = 0;

    // Navegação A*
    private int finalDestinationNode = -1; 
    private Queue<Integer> currentPath = new LinkedList<>(); 
    private int nextNode = -1; 
    private String intendedTurn = null; 
    private int lastNode = -1;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) vid = (String) args[0];
        else vid = getLocalName();

        // Obtém a posição inicial do veículo de emergência a partir do Environment
        double[] initialPos = Environment.getInstance().getVehicles().get(vid);
        if (initialPos != null) { this.x = initialPos[0]; this.y = initialPos[1]; } 
        else { this.x = 50; this.y = 265; }

        this.startX = this.x;
        this.startY = this.y;

        defineInitialDirection();
        Environment.getInstance().updateVehicle(vid, x, y, direction);

        initTrip();

        // TickerBehaviour: Executa o moveStep() a cada 100ms para mover o veículo de emergência
        addBehaviour(new TickerBehaviour(this, 100) {
            protected void onTick() {
                moveStep();
            }
        });
    }

    private void defineInitialDirection() {
        // Define a direção inicial (N, S, E, W) com base no ID do veículo de emergência
        if (vid.equalsIgnoreCase("EM1") || vid.equalsIgnoreCase("EM6")) direction = "E";
        else if (vid.equalsIgnoreCase("EM2") || vid.equalsIgnoreCase("EM3") || vid.equalsIgnoreCase("EM5")) direction = "S";
        else if (vid.equalsIgnoreCase("EM4")) direction = "W";
        else direction = "N";
    }

    private void initTrip() {
        // Reseta variáveis para uma nova viagem
        this.accumulatedCost = getStartSegmentCost();
        this.tripStartTime = System.currentTimeMillis();
        this.finishedCycle = false;
        this.forcingExit = false;
        this.isWaiting = false;
        this.hasAskedPermission = false;
        this.intendedTurn = null;
        this.lastLogLight = "";
        
        this.x = startX;
        this.y = startY;
        defineInitialDirection();
        
        this.lastNode = getApproximateNode(x, y);
        if (this.lastNode == -1) this.lastNode = 1;

        assignDestination();
        calculatePath();

        int tripNum = Metrics.getInstance().getCycle(vid);
        Metrics.getInstance().log(vid + " iniciou Viagem #" + tripNum + " para: " + destinationName);
        
        Environment.getInstance().updateVehicle(vid, x, y, direction);
    }

    // Lógica de movimento prioritário
    private void moveStep() {
        Environment env = Environment.getInstance();
        boolean canMove = true;
        
        // Chegada ao destino 
        double dist = Math.sqrt(Math.pow(destX - x, 2) + Math.pow(destY - y, 2));
        boolean arrived = (dist < 60.0);
        if (destinationName.equals("Casa 2") && y >= 1000 && Math.abs(x - destX) < 40) arrived = true;

        if (!forcingExit && arrived) {
            // Se chegou, mas a estrada está bloqueada, aborta a viagem
            if (isCurrentRoadBlocked(env)) {
                if (!forcingExit) {
                    forcingExit = true;
                    double duration = (System.currentTimeMillis() - tripStartTime) / 1000.0;
                    Metrics.getInstance().notifyArrival(vid, "VIAGEM ABANDONADA", accumulatedCost, duration);
                }
            }
            else if (!finishedCycle) {
                // Se chegar sem bloqueios, regista a chegada e inicia nova viagem
                if (lastNode != finalDestinationNode) {
                    accumulatedCost += getRealEdgeCost(lastNode, finalDestinationNode, env);
                }
                accumulatedCost += getEndSegmentCost();
                
                finishedCycle = true;
                failureCount = 0;
                double duration = (System.currentTimeMillis() - tripStartTime) / 1000.0;
                Metrics.getInstance().notifyArrival(vid, destinationName, accumulatedCost, duration);
                initTrip();
                return;
            }
        }

        // Deteta barreiras no caminho
        if (isApproachingBarrier(env)) {
            Metrics.getInstance().log("--- " + vid + " (EM) detetou barreira! Analisando opções...");
            intendedTurn = null; 
            
            int currentNode = getApproximateNode(x, y);
            int safeStartNode = (currentNode != -1) ? currentNode : (lastNode != -1 ? lastNode : 1);

            calculatePath();

            boolean aStarFoundPath = !currentPath.isEmpty();
            int targetNode = -1;
            
            if (aStarFoundPath) {
                targetNode = currentPath.peek();
                String newDir = getDirectionToNode(safeStartNode, targetNode);
                
                if (!checkBlockageByID(env, newDir)) {
                    if (!newDir.equals(direction)) {
                        // CORREÇÃO: Soma custo do desvio A*
                        accumulatedCost += getRealEdgeCost(safeStartNode, targetNode, env);
                        lastNode = targetNode;
                        intendedTurn = newDir;
                    }
                } else { aStarFoundPath = false; }
            }

            if (!aStarFoundPath) {
                boolean straightFree = !checkBlockageByID(env, direction);
                int nextStraightNode = getNodeInDirection(safeStartNode, direction);
                
                if (straightFree && nextStraightNode != -1) {
                    // CORREÇÃO: Soma custo da sobrevivência direta
                    accumulatedCost += getRealEdgeCost(safeStartNode, nextStraightNode, env);
                    lastNode = nextStraightNode;
                    currentPath.clear();
                    currentPath.add(nextStraightNode);
                    nextNode = nextStraightNode;
                    intendedTurn = null; 
                } 
                else {
                    escapeBlockage(env);
                    if (intendedTurn != null) {
                        // CORREÇÃO: Soma custo da manobra de escape
                        int escapeTarget = getNodeInDirection(safeStartNode, intendedTurn);
                        if (escapeTarget != -1) {
                            accumulatedCost += getRealEdgeCost(safeStartNode, escapeTarget, env);
                            lastNode = escapeTarget;
                        }
                    } else {
                        if (!forcingExit) {
                            forcingExit = true;
                            double duration = (System.currentTimeMillis() - tripStartTime) / 1000.0;
                            Metrics.getInstance().log(vid + " bloqueado sem saída. Abortando.");
                            Metrics.getInstance().notifyArrival(vid, "VIAGEM ABANDONADA", accumulatedCost, duration);
                            initTrip();
                            return;
                        } 
                        canMove = false; 
                    }
                }
            }
        }

        // Lógica de movimento principal
        if (canMove) {
            if (!currentPath.isEmpty() || lastNode != finalDestinationNode) followPathLogic(env);
            else if (!forcingExit) guideToDestination();
        }

        if (intendedTurn != null) checkAndExecuteTurn();

        // Interação com os semáforos 
        String targetLight = getTargetLightAgent();
        if (targetLight != null) {
            if (!hasAskedPermission) {
                if (!targetLight.equals(lastLogLight)) {
                    Enums.LightState status = Environment.getInstance().getLightState(targetLight);
                    Metrics.getInstance().log(vid + " está a pedir autorização para passar no semáforo: " + targetLight + " (Estado atual: " + status + ")");
                    lastLogLight = targetLight; // Marca como logado
                }
                
                if (askTrafficLight(targetLight, "EMERGENCY_PASS")) {
                    hasAskedPermission = true;
                    Metrics.getInstance().log("O semáforo " + targetLight + " deu a passagem prioritária ao veículo " + vid);
                }
            }
        } else { 
            hasAskedPermission = false; 
            lastLogLight = "";
        }

        // Preve colisões com outros veículos
        if (hasObstacleAhead(env, direction, x, y)) canMove = false; 

        // Movimento (Mais Rápido)
        if (canMove) {
            switch (direction) {
                case "E": x += speed; break;
                case "W": x -= speed; break;
                case "S": y += speed; break;
                case "N": y -= speed; break;
            }
            if (isWaiting) isWaiting = false;
        } else {
            if (!isWaiting) isWaiting = true;
        }

        env.updateVehicle(vid, x, y, direction);
        if (x < -200 || x > 2400 || y < -200 || y > 1400) {
            failureCount++;
            Metrics.getInstance().log(vid + " abandonou por ser impossível chegar ao destino");
            initTrip();
        }
    }

    //-----------------------------------------------------//
    //----------------  MÉTODOS AUXILIARES ----------------//
    //-----------------------------------------------------//

    private int getGridID(double cx, double cy) {
        // Converte coordenadas (x,y) em ID de nó da grelha (1 a 6)
        int col = (int) (cx / 700);
        int row = (int) (cy / 500);
        if (col < 0) col = 0; if (col > 2) col = 2;
        if (row < 0) row = 0; if (row > 1) row = 1;
        return (col * 2) + row + 1;
    }

    private int getApproximateNode(double x, double y) {
        return getGridID(x, y);
    }

    private int getNodeInDirection(int currentNode, String dir) {
        switch(currentNode) {
            case 1: if(dir.equals("E")) return 3; if(dir.equals("S")) return 2; break;
            case 2: if(dir.equals("N")) return 1; if(dir.equals("E")) return 4; break;
            case 3: if(dir.equals("W")) return 1; if(dir.equals("E")) return 5; if(dir.equals("S")) return 4; break;
            case 4: if(dir.equals("W")) return 2; if(dir.equals("E")) return 6; if(dir.equals("N")) return 3; break;
            case 5: if(dir.equals("W")) return 3; if(dir.equals("S")) return 6; break;
            case 6: if(dir.equals("W")) return 4; if(dir.equals("N")) return 5; break;
        }
        return -1; 
    }

    private void calculatePath() {
        // Prepara e executa o algoritmo A*
        int startNode = getApproximateNode(x, y);
        if (startNode == -1) startNode = lastNode != -1 ? lastNode : 1;

        // Chama a classe ASstar passando o Environment para detetar bloqueios
        List<Integer> path = AStar.findPath(startNode, finalDestinationNode, Environment.getInstance());
        currentPath = new LinkedList<>(path);

        if (!currentPath.isEmpty() && currentPath.peek() == startNode) currentPath.poll();
        if (!currentPath.isEmpty()) nextNode = currentPath.peek();
        else nextNode = -1; // Sem caminho disponível
    }

    private void followPathLogic(Environment env) {
        // Segue e atualiza o caminho A*
        if (currentPath.isEmpty()) return;
        nextNode = currentPath.peek();
        if (isNearIntersectionCenter()) {
            int currentNode = getApproximateNode(x, y);
            if (currentNode == nextNode) {
                // Chegou a um nó, avança para o próximo
                if (lastNode != currentNode) {
                    accumulatedCost += getRealEdgeCost(lastNode, currentNode, env);
                }
                lastNode = currentNode;
                currentPath.poll();
            }
            if (!currentPath.isEmpty()) {
                String req = getDirectionToNode(currentNode, currentPath.peek());
                if (!req.equals(direction)) intendedTurn = req;
            }
        }
    }

    private void assignDestination() {
        int currentCycle = Metrics.getInstance().getCycle(vid);
        int currentNode = getApproximateNode(x, y);

        // Na primeira viagem, os destinos são fixos para validação, a partir daí são aleatórias
        if (currentCycle == 1 && failureCount == 0) {
            switch (vid.toUpperCase()) {
                case "EM1": setDest(2, 350, 890, "Bombeiros"); break;
                case "EM2": setDest(5, 1750, 100, "Hospital"); break;
                case "EM3": setDest(5, 1750, 100, "Hospital"); break;
                case "EM4": setDest(6, 1950, 750, "Policia"); break;
                case "EM5": setDest(5, 1750, 100, "Hospital"); break;
                case "EM6": setDest(5, 1750, 100, "Hospital"); break;
                case "EM7": setDest(4, 1035, 950, "Casa 2"); break; 
                case "EM8": setDest(6, 1950, 750, "Policia"); break;
                case "EM9": setDest(2, 350, 890, "Bombeiros"); break;
                default: setDest(5, 1750, 100, "Hospital"); break;
            }
        } else {
            int targetNode = -1;
            do {
                int r = rnd.nextInt(6);
                switch(r) {
                    case 0: targetNode=1; setDest(1, 350, 100, "Casa 1"); break;  
                    case 1: targetNode=2; setDest(2, 350, 890, "Bombeiros"); break;  
                    case 2: targetNode=4; setDest(4, 1035, 950, "Casa 2"); break; 
                    case 3: targetNode=5; setDest(5, 1750, 100, "Hospital"); break; 
                    case 4: targetNode=5; setDest(5, 1950, 250, "Casa 3"); break; 
                    case 5: targetNode=6; setDest(6, 1950, 750, "Policia"); break; 
                }
            } while (targetNode == currentNode); // Garante que o destino é diferente do ponto de partida
        }
    }

    private void setDest(int node, double dx, double dy, String name) {
        this.finalDestinationNode = node;
        this.destX = dx;
        this.destY = dy;
        this.destinationName = name;
    }

    private double getStartSegmentCost() {
        // Retorna o custo inicial baseado no ponto de partido (valor fixo)
        switch (vid.toUpperCase()) {
            case "EM1": return 5.0; 
            case "EM2": case "EM3": case "EM5": case "EM7": case "EM8": case "EM9": return 2.0;
            case "EM4": return 4.0;
            case "EM6": return 3.0;
            default: return 0.0;
        }
    }

    private double getEndSegmentCost() {
        // Retorna o custo final baseado no destino (valor fixo)
        switch (destinationName) {
            case "Casa 1": return 2.0; 
            case "Casa 2": return 2.0; 
            case "Casa 3": return 4.0; 
            case "Hospital": return 2.0; 
            case "Policia": return 4.0; 
            case "Bombeiros": return 2.0; 
            default: return 0.0;
        }
    }

    private void checkAndExecuteTurn() {
        int id = getGridID(x, y);
        int col = (id - 1) / 2;
        int row = (id - 1) % 2;
        double cx = col * 700; 
        double cy = row * 500;
        
        double target = -1; boolean checkX = false; 
        
        if (intendedTurn.equals("S")) { target = cx + 335; checkX = true; } 
        else if (intendedTurn.equals("N")) { target = cx + 365; checkX = true; } 
        else if (intendedTurn.equals("E")) { target = cy + 265; checkX = false; } 
        else if (intendedTurn.equals("W")) { target = cy + 235; checkX = false; } 
        
        double tol = Math.max(speed * 2.0, 25.0); 
        boolean turnNow = false;
        if (checkX) { if (Math.abs(x - target) <= tol) { x = target; direction = intendedTurn; intendedTurn = null; } } 
        else { if (Math.abs(y - target) <= tol) { y = target; direction = intendedTurn; intendedTurn = null; } }
    }

    private void guideToDestination() {
        // Navega simples (sem nós) para a fase final do destino
        double dx = destX - x;
        double dy = destY - y;
        if (intendedTurn == null) {
            if (Math.abs(dx) > Math.abs(dy)) {
                if (dx > 20 && !direction.equals("E")) intendedTurn = "E";
                else if (dx < -20 && !direction.equals("W")) intendedTurn = "W";
            } else {
                if (dy > 20 && !direction.equals("S")) intendedTurn = "S";
                else if (dy < -20 && !direction.equals("N")) intendedTurn = "N";
            }
        }
    }

    private boolean isCurrentRoadBlocked(Environment env) {
        int id = getGridID(x, y);
        if (id < 1 || id > 6) return false;
        return env.isRoadBlocked("ROAD_" + id + "_" + direction);
    }

    private double getRealEdgeCost(int from, int to, Environment env) {
        // Mapeia nós para custos reais do Environment
        Map<String, Integer> costs = env.getRoadCosts();
        String key = "";
        if ((from==1 && to==3) || (from==3 && to==1)) key="Seg_Top_Mid";
        else if ((from==3 && to==5) || (from==5 && to==3)) key="Seg_Top_Right";
        else if ((from==2 && to==4) || (from==4 && to==2)) key="Seg_Bot_Mid";
        else if ((from==4 && to==6) || (from==6 && to==4)) key="Seg_Bot_Right";
        else if ((from==1 && to==2) || (from==2 && to==1)) key="Seg_Vert_Left";
        else if ((from==3 && to==4) || (from==4 && to==3)) key="Seg_Vert_Mid";
        else if ((from==5 && to==6) || (from==6 && to==5)) key="Seg_Vert_Right";
        return costs.getOrDefault(key, 0) * 1.0;
    }

    private void escapeBlockage(Environment env) {
        // Tenta encontrar uma direção livre para escapar de bloqueios
        int currentNode = getApproximateNode(x, y);
        String leftDir = getLeftDir(direction);
        String rightDir = getRightDir(direction);
        boolean leftOpen = !checkBlockageByID(env, leftDir);
        boolean rightOpen = !checkBlockageByID(env, rightDir);
        if (leftOpen) intendedTurn = leftDir;
        else if (rightOpen) intendedTurn = rightDir;
    }
    
    private double getCostForDir(int node, String dir, Environment env) {
        for (int neighbor : AStar.getNeighbors(node)) {
            if (AStar.getDir(node, neighbor).equals(dir)) return getRealEdgeCost(node, neighbor, env);
        }
        return 999.0; 
    }
    
    private String getLeftDir(String d) { if(d.equals("N"))return "W"; if(d.equals("W"))return "S"; if(d.equals("S"))return "E"; return "N"; }
    private String getRightDir(String d) { if(d.equals("N"))return "E"; if(d.equals("E"))return "S"; if(d.equals("S"))return "W"; return "N"; }

    private boolean checkBlockageByID(Environment env, String checkDir) {
        // Verifica se a estrada à frente ou adjacente está bloqueada
        double lookAheadDist = 60; double fx = x, fy = y;
        switch (direction) {
            case "E": fx += lookAheadDist; break; case "W": fx -= lookAheadDist; break;
            case "S": fy += lookAheadDist; break; case "N": fy -= lookAheadDist; break;
        }
        int id = getGridID(fx, fy);
        if (id < 1 || id > 6) return false;
        if (env.isRoadBlocked("ROAD_" + id + "_" + checkDir)) return true;
        return false;
    }
    
    private String getOppositeDir(String d) {
        if (d.equals("N")) return "S"; if (d.equals("S")) return "N";
        if (d.equals("E")) return "W"; if (d.equals("W")) return "E";
        return d;
    }

    private boolean isApproachingBarrier(Environment env) {
        // Deteta se está a aproximar de uma barreira na direção atual antes de lá chegar
        int id = getGridID(x, y);
        if (id < 1 || id > 6) return false;

        int col = (id - 1) / 2; int row = (id - 1) % 2;
        double ox = col * 700; double oy = row * 500; double stopDist = 200.0;

        boolean inZone = false;
        switch (direction) {
            case "E": if (x < ox + 410 && x + stopDist >= ox + 410) inZone=true; break;
            case "W": if (x > ox + 270 && x - stopDist <= ox + 270) inZone=true; break;
            case "S": if (y < oy + 310 && y + stopDist >= oy + 310) inZone=true; break;
            case "N": if (y > oy + 170 && y - stopDist <= oy + 170) inZone=true; break;
        }
        return inZone && checkBlockageByID(env, (intendedTurn != null) ? intendedTurn : direction);
    }

    private boolean askTrafficLight(String agentName, String content) {
        // Envia um pedido FIPA-Request ao semáforo e espera pela resposta
        ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
        request.addReceiver(new AID(agentName, AID.ISLOCALNAME));
        request.setContent(content);
        send(request);
        ACLMessage reply = blockingReceive(200);
        return (reply != null && reply.getPerformative() == ACLMessage.AGREE);
    }

    private boolean hasObstacleAhead(Environment env, String direction, double x, double y) {
        // Sensor de proximidade para evitar colisões com outros veículos 
        Map<String, double[]> vehicles = env.getVehicles(); double safeDistance = 35.0; 
        for (Map.Entry<String, double[]> entry : vehicles.entrySet()) {
            if (entry.getKey().equals(vid)) continue;
            double ox = entry.getValue()[0]; double oy = entry.getValue()[1];
            // Verifica se há um veículo na direção atual dentro da distância segura
            switch (direction) {
                case "E": if (Math.abs(oy - y) < 15 && ox > x && (ox - x) < safeDistance) return true; break;
                case "W": if (Math.abs(oy - y) < 15 && ox < x && (x - ox) < safeDistance) return true; break;
                case "S": if (Math.abs(ox - x) < 15 && oy > y && (oy - y) < safeDistance) return true; break;
                case "N": if (Math.abs(ox - x) < 15 && oy < y && (y - oy) < safeDistance) return true; break;
            }
        }
        return false;
    }

    private String getTargetLightAgent() {
        // Verifica se está prestes a alcançar um semáforo e retorna o seu ID
        int id = getGridID(x, y);
        if(id<1||id>6) return null;
        int col = (id-1)/2; int row = (id-1)%2;
        double ox = col*700; double oy = row*500;
        boolean stop = false;
        switch(direction) {
            case "E": if(x>=265+ox && x<285+ox) stop=true; return stop?"trafficLight-EW"+id:null;
            case "W": if(x<=435+ox && x>415+ox) stop=true; return stop?"trafficLight-EW"+id:null;
            case "S": if(y>=165+oy && y<185+oy) stop=true; return stop?"trafficLight-NS"+id:null;
            case "N": if(y<=335+oy && y>315+oy) stop=true; return stop?"trafficLight-NS"+id:null;
        }
        return null;
    }

    private String getDirectionToNode(int from, int to) {
        int fx=(from-1)/2, fy=(from-1)%2; int tx=(to-1)/2, ty=(to-1)%2;
        if (tx > fx) return "E"; if (tx < fx) return "W";
        if (ty > fy) return "S"; if (ty < fy) return "N";
        return direction; 
    }
    
    private boolean isNearIntersectionCenter() {
        int id = getGridID(x, y);
        int col = (id-1)/2; int row = (id-1)%2;
        double ox = col*700; double oy = row*500;
        if (direction.equals("E") || direction.equals("W")) return (x > ox + 250 && x < ox + 450);
        return (y > oy + 150 && y < oy + 350);
    }

    // Classe Estática para o algoritmo A*, calcula o caminho de menor custo
    private static class AStar {
        static class Node implements Comparable<Node> {
            int id; double g, f; Node parent;
            Node(int id, double g, double f, Node parent) { this.id=id; this.g=g; this.f=f; this.parent=parent; }
            public int compareTo(Node o) { return Double.compare(this.f, o.f); }
        }
        public static List<Integer> findPath(int start, int goal, Environment env) {
            PriorityQueue<Node> openSet = new PriorityQueue<>();
            Map<Integer, Double> gScore = new HashMap<>();
            // Inicializa A*
            openSet.add(new Node(start, 0, heuristic(start, goal), null));
            gScore.put(start, 0.0);
            while (!openSet.isEmpty()) {
                Node current = openSet.poll();
                if (current.id == goal) return reconstructPath(current);
                for (int neighbor : getNeighbors(current.id)) {
                    // Custo dinâmico baseado no Environment, se houver bloqueio o custo é 9999 logo ignora esse caminho
                    double moveCost = getCost(current.id, neighbor, env);
                    if (moveCost >= 9999) continue;
                    double tentativeG = gScore.getOrDefault(current.id, Double.MAX_VALUE) + moveCost;
                    if (tentativeG < gScore.getOrDefault(neighbor, Double.MAX_VALUE)) {
                        gScore.put(neighbor, tentativeG);
                        openSet.add(new Node(neighbor, tentativeG, tentativeG + heuristic(neighbor, goal), current));
                    }
                }
            }
            return new ArrayList<>(); 
        }
        private static List<Integer> reconstructPath(Node end) {
            List<Integer> path = new LinkedList<>(); Node curr = end;
            while (curr != null) { path.add(0, curr.id); curr = curr.parent; }
            return path;
        }
        private static double heuristic(int a, int b) { return Math.abs((a-1)/2 - (b-1)/2) + Math.abs((a-1)%2 - (b-1)%2); }
        public static int[] getNeighbors(int id) {
            if (id==1) return new int[]{2,3}; if (id==2) return new int[]{1,4}; if (id==3) return new int[]{1,4,5};
            if (id==4) return new int[]{2,3,6}; if (id==5) return new int[]{3,6}; if (id==6) return new int[]{4,5};
            return new int[]{};
        }
        private static double getCost(int from, int to, Environment env) {
            // Retorna o custo real do Environment da estrada
            Map<String, Integer> costs = env.getRoadCosts();
            String key = "";
            if ((from==1 && to==3) || (from==3 && to==1)) key="Seg_Top_Mid";
            if ((from==3 && to==5) || (from==5 && to==3)) key="Seg_Top_Right";
            if ((from==2 && to==4) || (from==4 && to==2)) key="Seg_Bot_Mid";
            if ((from==4 && to==6) || (from==6 && to==4)) key="Seg_Bot_Right";
            if ((from==1 && to==2) || (from==2 && to==1)) key="Seg_Vert_Left";
            if ((from==3 && to==4) || (from==4 && to==3)) key="Seg_Vert_Mid";
            if ((from==5 && to==6) || (from==6 && to==5)) key="Seg_Vert_Right";
            String dir = getDir(from, to);
            if (env.isRoadBlocked("ROAD_" + from + "_" + dir)) return 99999;
            String revDir = getOppositeDir(dir);
            if (env.isRoadBlocked("ROAD_" + to + "_" + revDir)) return 99999;
            return costs.getOrDefault(key, 1) * 1.0;
        }
        private static String getOppositeDir(String d) {
            if (d.equals("N")) return "S"; if (d.equals("S")) return "N";
            if (d.equals("E")) return "W"; if (d.equals("W")) return "E";
            return d;
        }
        public static String getDir(int from, int to) {
            if (from==1 && to==3) return "E"; if (from==3 && to==1) return "W";
            if (from==1 && to==2) return "S"; if (from==2 && to==1) return "N";
            if (from==3 && to==5) return "E"; if (from==5 && to==3) return "W";
            if (from==3 && to==4) return "S"; if (from==4 && to==3) return "N";
            if (from==2 && to==4) return "E"; if (from==4 && to==2) return "W";
            if (from==5 && to==6) return "S"; if (from==6 && to==5) return "N";
            if (from==4 && to==6) return "E"; if (from==6 && to==4) return "W";
            return "N";
        }
    }
}