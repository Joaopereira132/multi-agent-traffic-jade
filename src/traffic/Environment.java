// Representa o estado partilhado do mundo. Armazena a posição de todos os veículos, o estado dos semáforos, as filas de espera e os custos do mapa. Uiliza estruturas 'Concurrent' para garantir thread-safety, pois múltiplos agentes (Threads) leem e escrevem aqui simultaneamente.

package traffic;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Environment {
    private static Environment instance = new Environment();

    // --- DADOS DINÂMICOS (Mudam em tempo real) ---
    // Estado das luzes (RED/GREEN/YELLOW) por ID de semáforo
    private Map<String, Enums.LightState> lightStates = new ConcurrentHashMap<>();
    // Posição (x, y) de cada veículo por ID de veículo;
    private Map<String, double[]> vehicles = new ConcurrentHashMap<>();
    // Tamanho das filas de espera em cada semáforo (usado pelo DisruptionAgent
    private Map<String, Integer> queues = new ConcurrentHashMap<>();
    // Direção atual de cada veículo (N, S, E, W) para desenhar o ícone rodado corretamente
    private Map<String, String> vehicleDirections = new ConcurrentHashMap<>();
    
    // Conjuntos para gestão de eventos manuais (Painel de Controlo)
    private Set<String> forcedRedLights = ConcurrentHashMap.newKeySet(); // Semáforos forçados a vermelho
    private Set<String> blockedRoads = ConcurrentHashMap.newKeySet();    // Estradas com obras (barreiras)

    //--- DADOS ESTÁTICOS ---
    private Map<String, Integer> roadCosts = new HashMap<>(); // Custos das estradas para o A*
    
    // Estrutura para os edifícios (Casas, Hospitais, etc.)
    public static class PointOfInterest {
        public int x, y; 
        public Enums.POIType type;
        public PointOfInterest(int x, int y, Enums.POIType type) { 
            this.x = x; this.y = y; this.type = type; 
        }
    }
    // Mapa dos locais por nome
    private Map<String, PointOfInterest> poiLocations = new HashMap<>();

    private EnvironmentGUI gui; // Referência para o GUI para repintar quando necessário

    private Environment() { }

    public static Environment getInstance() { return instance; }

    // Inicializa o ambiente com valores pré-definidos. Define a posição dos semáforos, locais, custos das estradas
    public void initDefault() {
        // Inicializa os Semáforos ---
        for (int i = 1; i <= 6; i++) {
            lightStates.put("TL_NS" + i, Enums.LightState.RED);
            lightStates.put("TL_EW" + i, Enums.LightState.GREEN); // Inicia com Horizontal a verde
            queues.put("TL_NS" + i, 0);
            queues.put("TL_EW" + i, 0);
        }

        //--- CONFIGURAÇÃO DOS PONTOS DE INTERESSE --- (Coordenadas x,y fixas para desenho e destino)
        poiLocations.put("Bombeiros", new PointOfInterest(185, 890, Enums.POIType.FIRE_STATION));
        poiLocations.put("Policia", new PointOfInterest(1980, 835, Enums.POIType.POLICE));
        poiLocations.put("Hospital", new PointOfInterest(1830, 40, Enums.POIType.HOSPITAL));
        poiLocations.put("Casa 1", new PointOfInterest(435, 30, Enums.POIType.HOUSE));   
        poiLocations.put("Casa 2", new PointOfInterest(885, 900, Enums.POIType.HOUSE));   
        poiLocations.put("Casa 3", new PointOfInterest(2000, 340, Enums.POIType.HOUSE));  

        // --- CUSTOS DOS TROÇOS DE ESTRADA --- Definem o "peso" de passar por cada via para o algoritmo A*
        roadCosts.put("Seg_Top_Left", 5); 
        roadCosts.put("Seg_Top_Mid", 5); 
        roadCosts.put("Seg_Top_Right", 5); 
        
        roadCosts.put("Seg_Bot_Left", 3);
        roadCosts.put("Seg_Bot_Mid", 6); 
        roadCosts.put("Seg_Bot_Right", 3);

        roadCosts.put("Seg_Vert_Left", 2); 
        roadCosts.put("Seg_Vert_Mid", 2); 
        roadCosts.put("Seg_Vert_Right", 2); 

        roadCosts.put("Seg_Vert_Top_1", 2); 
        roadCosts.put("Seg_Vert_Top_2", 2); 
        roadCosts.put("Seg_Vert_Top_3", 2); 

        roadCosts.put("Seg_Vert_Bot_1", 2); 
        roadCosts.put("Seg_Vert_Bot_2", 2); 
        roadCosts.put("Seg_Vert_Bot_3", 2); 
        
        roadCosts.put("Seg_Horiz_Right_Top", 4); 
        roadCosts.put("Seg_Horiz_Right_Bot", 4); 

        // --- CONFIGURAÇÃO INICIAL DOS VEÍCULOS ---
        double stagger = 50.0; // Distancia entre carros para evitar sobreposição inicial
        setupVehicles(stagger);
    }
    
    private void setupVehicles(double stagger) {
        // Define as posições iniciais dos veículos (x, y)
        vehicles.put("V-P1-1", new double[] { 30, 295 });
        vehicles.put("V-P1-2", new double[] { 30 + stagger, 295 });
        vehicles.put("V-P2-1", new double[] { 305, 30 });
        vehicles.put("V-P2-2", new double[] { 305, 30 + stagger });
        vehicles.put("V-P3-1", new double[] { 1005, 30 });
        vehicles.put("V-P3-2", new double[] { 1005, 30 + stagger });
        vehicles.put("V-P4-1", new double[] { 2070, 205 });
        vehicles.put("V-P4-2", new double[] { 2070 - stagger, 205 });
        vehicles.put("V-P5-1", new double[] { 30, 795 });
        vehicles.put("V-P5-2", new double[] { 30 + stagger, 795 });
        vehicles.put("V-P6-1", new double[] { 395, 970 });
        vehicles.put("V-P6-2", new double[] { 395, 970 - stagger });
        vehicles.put("V-P7-1", new double[] { 1095, 970 });
        vehicles.put("V-P7-2", new double[] { 1095, 970 - stagger });
        vehicles.put("V-P8-1", new double[] { 1795, 970 });
        vehicles.put("V-P8-2", new double[] { 1795, 970 - stagger });
        vehicles.put("V-P9-1", new double[] { 2070, 705 });
        vehicles.put("V-P9-2", new double[] { 2070 - stagger, 705 });
        vehicles.put("V-P10-1", new double[] { 1705, 30 });
        vehicles.put("V-P10-2", new double[] { 1705, 30 + stagger });

        vehicles.put("EM1", new double[] { -150, 265 });
        vehicles.put("EM2", new double[] { 335, -150 });
        vehicles.put("EM3", new double[] { 1035, -150 }); 
        vehicles.put("EM4", new double[] { 2250, 235 }); 
        vehicles.put("EM5", new double[] { 1735, -150 }); 
        vehicles.put("EM6", new double[] { -150, 765 }); 
        vehicles.put("EM7", new double[] { 365, 1150 }); 
        vehicles.put("EM8", new double[] { 1065, 1150 }); 
        vehicles.put("EM9", new double[] { 1765, 1150 }); 
    }

    // Atualiza o estado de bloqueio de uma estrada (obras), é chamado quando o utilizador interage com o Control Panel GUI na parte das obras
    public void toggleRoadBlock(String roadId, boolean block) {
        if (block) blockedRoads.add(roadId);
        else blockedRoads.remove(roadId);
        if (gui != null) gui.repaint();
    }

    // Verifica se uma estrada está bloqueada (usado pelos veículos)
    public boolean isRoadBlocked(String roadId) { return blockedRoads.contains(roadId); }
    public Set<String> getBlockedRoads() { return blockedRoads; }

    // Força um semáforo a vermelho, é chamado quando o utilizador interage com o Control Panel GUI na parte dos semáforos, sobrepõe-se às ordens da central 
    public void setForceRed(String lightId, boolean force) {
        if (force) { forcedRedLights.add(lightId); setLightStateInternal(lightId, Enums.LightState.RED); } 
        else forcedRedLights.remove(lightId);
    }
    public boolean isAnyLightForcedRed() { return !forcedRedLights.isEmpty(); }

    // Atualiza o estado de um semáforo, chamado pelo CentralAgent ou TrafficLightAgent, synchronized para evitar conflitos
    public synchronized void setLightState(String id, Enums.LightState state) {
        if (forcedRedLights.contains(id)) return; 
        setLightStateInternal(id, state);
    }
    
    private void setLightStateInternal(String id, Enums.LightState state) {
        lightStates.put(id, state);

        // Lógica para semáforos opostos, se um está verde ou amarelo, o oposto deve estar vermelho
        if (state == Enums.LightState.GREEN || state == Enums.LightState.YELLOW) {
            String oppositeId = getOppositeLightId(id);
            if (oppositeId != null) lightStates.put(oppositeId, Enums.LightState.RED);
        }
        if (gui != null) gui.repaint();
    }

    // Getters para acesso aos dados 
    public Map<String, Integer> getRoadCosts() { return roadCosts; }
    public Map<String, PointOfInterest> getLocations() { return poiLocations; }
    public synchronized Enums.LightState getLightState(String id) { return lightStates.getOrDefault(id, Enums.LightState.RED); }

    // Obtém o ID do semáforo par a ele (ex. NS1 é o par do EW1)
    private String getOppositeLightId(String id) {
        if (id.startsWith("TL_NS")) return id.replace("TL_NS", "TL_EW");
        else if (id.startsWith("TL_EW")) return id.replace("TL_EW", "TL_NS");
        return null;
    }
    
    public Map<String, double[]> getVehicles() { return vehicles; }

    // Atualiza a posição e direção de um veículo, chamado pelo VehicleAgent e EmergencyVehicleAgent. Synchronized para evitar conflitos, é isto que faz com que eles "andem" no GUI
    public synchronized void updateVehicle(String vid, double x, double y, String direction) {
        vehicles.put(vid, new double[] { x, y });
        vehicleDirections.put(vid, direction);
        if (gui != null) gui.repaint();
    }
    public synchronized void removeVehicle(String vid) {
        vehicles.remove(vid); vehicleDirections.remove(vid);
        if (gui != null) gui.repaint();
    }
    public synchronized String getVehicleDirection(String vid) { return vehicleDirections.getOrDefault(vid, "N"); }

    //Métodos para gerir as filas (usados pelo DisruptionAgent para estatísticas)
    public synchronized void incQueue(String tlId) { queues.put(tlId, queues.getOrDefault(tlId, 0) + 1); }
    public synchronized void decQueue(String tlId) { queues.put(tlId, Math.max(0, queues.getOrDefault(tlId, 0) - 1)); }
    public synchronized int getQueueSize(String tlId) { return queues.getOrDefault(tlId, 0); }

    public void setGui(EnvironmentGUI gui) { this.gui = gui; }
    public EnvironmentGUI getGui() { return gui; }

    // Obtém uma cópia do estado atual de todas as filas
    public synchronized Map<String, Integer> getAllQueuesSnapshot() { return new HashMap<>(queues); }
}