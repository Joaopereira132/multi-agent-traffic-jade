//Interface gráfica para o ambiente com desenhos detalhados de locais e veículos.

package traffic;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.util.Map;
import javax.swing.*;

public class EnvironmentGUI extends JPanel {
    private Environment env;
    private JFrame frame;
    private double dynamicScale = 1.0; 

    public EnvironmentGUI(Environment env) {
        this.env = env;
        // Cálculo da escala dinâmica com base na resolução do ecrã do utilizador
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int screenWidth = gd.getDisplayMode().getWidth();
        int screenHeight = gd.getDisplayMode().getHeight();
        int simWidth = 2100; // Tamanho virtual do ambiente
        int simHeight = 1000;

        double scaleX = (double) screenWidth / simWidth;
        double scaleY = (double) screenHeight / simHeight;
        this.dynamicScale = Math.min(scaleX, scaleY) * 0.9; 

        setPreferredSize(new Dimension((int) (simWidth * dynamicScale), (int) (simHeight * dynamicScale)));
        setBackground(new Color(34, 139, 34)); // Fundo verde (relva)

        // Timer para repintar a GUI periodicamente a cada 50 ms
        Timer t = new Timer(50, e -> repaint());
        t.start();
    }

    public void showInFrame() {
        frame = new JFrame("Traffic Environment - Better Visuals");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(this);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
    }

    
    // Método de pintura principal chamado pelo Swing
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.scale(this.dynamicScale, this.dynamicScale);

        // Desenha as camadas por ordem (o que fica por baixo é desenhado primeiro)
        drawRoads(g2d);          // 1. Estradas
        drawCosts(g2d);          // 2. Setas de Custo
        drawLocations(g2d);      // 3. Edificíos
        drawBarriers(g2d);       // 4. Obras
        drawTrafficLights(g2d);  // 5. Semáforos
        drawVehicles(g2d);       // 6. Veículos 

        g2d.dispose();
        drawInfo(g); // 7. Informação sobre o ambiente
    }

    // LÓGICA DE DESENHO DOS LOCAIS
    private void drawLocations(Graphics2D g) {
        Map<String, Environment.PointOfInterest> locs = env.getLocations();
        
        for (Map.Entry<String, Environment.PointOfInterest> entry : locs.entrySet()) {
            String name = entry.getKey();
            Environment.PointOfInterest poi = entry.getValue();
            
            // Desenha o ícone específico consoante o tipo de local
            switch (poi.type) {
                case HOSPITAL: 
                    drawDetailedHospital(g, poi.x, poi.y, 100);
                    break;
                case POLICE:   
                    drawDetailedPolice(g, poi.x, poi.y, 100);
                    break;
                case FIRE_STATION: 
                    drawDetailedFireStation(g, poi.x, poi.y, 100);
                    break;
                case HOUSE:    
                    drawDetailedHouse(g, poi.x, poi.y, 70, name);
                    break;
            }
        }
    }

    //DESENHO DA CASA
    private void drawDetailedHouse(Graphics2D g, int x, int y, int size, String name) {
        // Corpo da Casa
        g.setColor(new Color(245, 222, 179)); // Bege (Wheat)
        g.fillRect(x, y + (size/3), size, size - (size/3));
        g.setColor(Color.BLACK);
        g.drawRect(x, y + (size/3), size, size - (size/3));

        // Telhado (Triângulo)
        Polygon roof = new Polygon();
        roof.addPoint(x, y + (size/3));
        roof.addPoint(x + size, y + (size/3));
        roof.addPoint(x + (size/2), y - 10);
        g.setColor(new Color(139, 69, 19)); 
        g.fillPolygon(roof);
        g.setColor(Color.BLACK);
        g.drawPolygon(roof);

        // Porta
        g.setColor(new Color(101, 67, 33)); // Castanho escuro
        g.fillRect(x + (size/2) - 10, y + size - 30, 20, 30);

        // Janelas 
        int winSize = 15;
        int bodyTopY = y + (size / 3); 
        
        // Janela 1: À esquerda, logo abaixo do telhado
        drawDetailedWindow(g, x + 5, bodyTopY, winSize);
        
        // Janela 2: À direita, logo abaixo do telhado
        drawDetailedWindow(g, x + size - 5 - winSize, bodyTopY, winSize);
        
        // Nome em cima
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(name, x + 5, y - 15);
    }

    // JANELA DETALHADA
    private void drawDetailedWindow(Graphics2D g, int x, int y, int size) {
        // Preenchimento de vidro (Azul Céu)
        g.setColor(new Color(135, 206, 250));
        g.fillRect(x, y, size, size);
        
        // Moldura da janela (Preto)
        g.setColor(Color.BLACK);
        g.drawRect(x, y, size, size);
        
        // Vidraças (Cruz no meio)
        // Linha Vertical
        g.drawLine(x + (size / 2), y, x + (size / 2), y + size);
        // Linha Horizontal
        g.drawLine(x, y + (size / 2), x + size, y + (size / 2));
    }

    //DESENHO DO HOSPITAL
    private void drawDetailedHospital(Graphics2D g, int x, int y, int size) {
        // Corpo do Hospital
        g.setColor(Color.WHITE);
        g.fillRect(x, y, size, size);
        g.setColor(Color.GRAY);
        g.drawRect(x, y, size, size);

        // Telhado
        g.setColor(new Color(200, 200, 200)); // Cinza claro
        g.fillRect(x - 5, y - 10, size + 10, 15);

        // Cruz Vermelha Grande
        g.setColor(new Color(220, 20, 60)); // Crimson
        int thick = 20;
        int len = 60;
        // Horizontal
        g.fillRect(x + (size/2) - (len/2), y + (size/2) - (thick/2), len, thick);
        // Vertical
        g.fillRect(x + (size/2) - (thick/2), y + (size/2) - (len/2), thick, len);

        // Texto
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("HOSPITAL", x + 15, y - 15);
    }

    //DESENHO DA POLÍCIA
    private void drawDetailedPolice(Graphics2D g, int x, int y, int size) {
        // Corpo da Polícia
        g.setColor(new Color(65, 105, 225));
        g.fillRect(x, y, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, size, size);

        // Faixa Branca
        g.setColor(Color.WHITE);
        g.fillRect(x, y + 40, size, 20);
        
        // Texto POLÍCIA
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("POLÍCIA", x + 25, y + 55);

        // Sirene no topo
        g.setColor(Color.RED);
        g.fillOval(x + 20, y - 10, 15, 15);
        g.setColor(Color.BLUE);
        g.fillOval(x + size - 35, y - 10, 15, 15);
        
        // Porta
        g.setColor(Color.DARK_GRAY);
        g.fillRect(x + 35, y + 70, 30, 30);
    }

    //DESENHO DOS BOMBEIROS
    private void drawDetailedFireStation(Graphics2D g, int x, int y, int size) {
        // Corpo (Vermelho Tijolo)
        g.setColor(new Color(178, 34, 34)); // Firebrick
        g.fillRect(x, y, size, size);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, size, size);

        // Portão da Garagem (Cinza com linhas)
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x + 10, y + 40, size - 20, size - 40);
        g.setColor(Color.GRAY);
        for(int i=0; i < size-40; i+=10) {
            g.drawLine(x+10, y+40+i, x+size-10, y+40+i);
        }

        // Placa "BOMBEIROS"
        g.setColor(Color.YELLOW);
        g.fillRect(x + 10, y + 5, size - 20, 20);
        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("BOMBEIROS", x + 10, y + 20);
    }

    //DESENHO DAS SETAS DE CUSTO
    private void drawCosts(Graphics2D g) {
        g.setFont(new Font("Arial", Font.BOLD, 22));
        Map<String, Integer> costs = env.getRoadCosts();
        
        // HORIZONTAIS CENTRAIS
        String dirTop = "DOWN";
        drawSingleCost(g, costs.get("Seg_Top_Left"), 175, 150, dirTop);  
        drawSingleCost(g, costs.get("Seg_Top_Mid"), 875, 150, dirTop);   
        drawSingleCost(g, costs.get("Seg_Top_Right"), 1575, 150, dirTop);
        
        String dirBot = "UP";
        drawSingleCost(g, costs.get("Seg_Bot_Left"), 175, 850, dirBot); 
        drawSingleCost(g, costs.get("Seg_Bot_Mid"), 875, 850, dirBot);  
        drawSingleCost(g, costs.get("Seg_Bot_Right"), 1575, 850, dirBot); 
        
        // VERTICAIS CENTRAIS
        String dirVert = "LEFT";
        drawSingleCost(g, costs.get("Seg_Vert_Left"), 450, 500, dirVert);  
        drawSingleCost(g, costs.get("Seg_Vert_Mid"), 1150, 500, dirVert);  
        drawSingleCost(g, costs.get("Seg_Vert_Right"), 1850, 500, dirVert);

        // EXTREMIDADES
        drawSingleCost(g, costs.get("Seg_Vert_Top_1"), 255, 50, "RIGHT");
        drawSingleCost(g, costs.get("Seg_Vert_Top_2"), 1150, 80, "LEFT");
        drawSingleCost(g, costs.get("Seg_Vert_Top_3"), 1650, 50, "RIGHT");

        // Ajuste Y=980
        drawSingleCost(g, costs.get("Seg_Vert_Bot_1"), 450, 980, "LEFT");
        drawSingleCost(g, costs.get("Seg_Vert_Bot_2"), 1150, 980, "LEFT");
        drawSingleCost(g, costs.get("Seg_Vert_Bot_3"), 1850, 980, "LEFT");

        // Ajuste X=1900
        drawSingleCost(g, costs.get("Seg_Horiz_Right_Top"), 2000, 150, "DOWN"); 
        drawSingleCost(g, costs.get("Seg_Horiz_Right_Bot"), 1900, 850, "UP");   
    }
    
    // DESENHO DO CIRUCLO DO CUSTO E SETA
    private void drawSingleCost(Graphics2D g, Integer cost, int x, int y, String arrowDir) {
        if (cost != null) {
            g.setColor(Color.BLACK);
            g.fillOval(x - 20, y - 20, 40, 40);
            g.setColor(Color.YELLOW);
            g.setStroke(new BasicStroke(2));
            g.drawOval(x - 20, y - 20, 40, 40);
            g.drawString(String.valueOf(cost), x - 6, y + 8);
            
            g.setColor(Color.BLACK); 
            Polygon arrow = new Polygon();
            int arrowSize = 8; 
            int offset = 22;   
            
            switch (arrowDir) {
                case "DOWN": 
                    arrow.addPoint(x - arrowSize, y + offset);
                    arrow.addPoint(x + arrowSize, y + offset);
                    arrow.addPoint(x, y + offset + arrowSize + 2);
                    break;
                case "UP": 
                    arrow.addPoint(x - arrowSize, y - offset);
                    arrow.addPoint(x + arrowSize, y - offset);
                    arrow.addPoint(x, y - offset - arrowSize - 2);
                    break;
                case "LEFT": 
                    arrow.addPoint(x - offset, y - arrowSize);
                    arrow.addPoint(x - offset, y + arrowSize);
                    arrow.addPoint(x - offset - arrowSize - 2, y);
                    break;
                case "RIGHT": 
                    arrow.addPoint(x + offset, y - arrowSize);
                    arrow.addPoint(x + offset, y + arrowSize);
                    arrow.addPoint(x + offset + arrowSize + 2, y);
                    break;
            }
            g.fillPolygon(arrow);
        }
    }
    
    // DESENHO DAS OBRAS (BARREIRAS)
    private void drawBarriers(Graphics2D g) {
        for (String roadId : env.getBlockedRoads()) {
            String[] parts = roadId.split("_");
            if(parts.length < 3) continue; 
            int id = Integer.parseInt(parts[1]);
            String dir = parts[2];

            int col = (id - 1) / 2; int row = (id - 1) % 2; 
            int ox = col * 700; int oy = row * 500;
            int bx = 0, by = 0, bw = 0, bh = 0;

            switch (dir) {
                case "E": bx = ox + 410; by = oy + 250; bw = 20; bh = 60; break;
                case "W": bx = ox + 270; by = oy + 190; bw = 20; bh = 60; break;
                case "S": bx = ox + 290; by = oy + 310; bw = 60; bh = 20; break;
                case "N": bx = ox + 350; by = oy + 170; bw = 60; bh = 20; break;
            }

            g.setColor(Color.BLACK);
            g.fillRect(bx, by, bw, bh);
            g.setColor(Color.YELLOW);
            if (bw > bh) { for(int k=0; k<bw; k+=20) g.fillRect(bx+k, by, 10, bh); } 
            else { for(int k=0; k<bh; k+=20) g.fillRect(bx, by+k, bw, 10); }
            g.setColor(Color.RED);
            g.drawRect(bx, by, bw, bh);
        }
    }

    // DESENHO DAS ESTRADAS
    private void drawRoads(Graphics2D g) {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int offsetX = col * 700;
                int offsetY = row * 500;
                g.setColor(Color.lightGray);
                g.fillRect(offsetX, offsetY + 190, 700, 120);
                g.fillRect(offsetX + 290, offsetY, 120, 500);
                g.setColor(Color.white);
                g.fillRect(offsetX, offsetY + 248, 285, 4);
                g.fillRect(offsetX + 410, offsetY + 248, 290, 4);
                g.fillRect(offsetX + 348, offsetY, 4, 185);
                g.fillRect(offsetX + 348, offsetY + 310, 4, 190);
                for (int i = 0; i < 700; i += 30) {
                    if (i < 285 || i > 410) {
                        g.fillRect(offsetX + i, offsetY + 218, 15, 4);
                        g.fillRect(offsetX + i, offsetY + 278, 15, 4);
                    }
                }
                for (int i = 0; i < 500; i += 30) {
                    if (i < 185 || i > 310) {
                        g.fillRect(offsetX + 318, offsetY + i, 4, 15);
                        g.fillRect(offsetX + 378, offsetY + i, 4, 15);
                    }
                }
            }
        }
    }

    // DESENHO DOS SEMÁFOROS
    private void drawTrafficLights(Graphics2D g) {
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int inter = (col * 2) + row + 1;
                int offsetX = col * 700;
                int offsetY = row * 500;
                Enums.LightState sNS = env.getLightState("TL_NS" + inter);
                Enums.LightState sEW = env.getLightState("TL_EW" + inter);
                drawLight(g, offsetX + 250, offsetY + 100, sNS);
                drawLight(g, offsetX + 430, offsetY + 330, sEW);
            }
        }
    }

    // DESENHO DAS LUZES DO SEMÁFORO
    private void drawLight(Graphics2D g, int x, int y, Enums.LightState s) {
        g.setColor(Color.black);
        g.fillRect(x - 10, y - 10, 40, 85);
        g.setColor(s == Enums.LightState.RED ? Color.red : Color.darkGray);
        g.fillOval(x, y, 20, 20);
        g.setColor(s == Enums.LightState.YELLOW ? Color.yellow : Color.darkGray);
        g.fillOval(x, y + 22, 20, 20);
        g.setColor(s == Enums.LightState.GREEN ? Color.green : Color.darkGray);
        g.fillOval(x, y + 44, 20, 20);
    }

    // DESENHO DOS VEÍCULOS
    private void drawVehicles(Graphics2D g2d) {
        Map<String, double[]> vehicles = env.getVehicles();

        for (Map.Entry<String, double[]> e : vehicles.entrySet()) {
            String vid = e.getKey();
            double[] pos = e.getValue();
            int x = (int) pos[0];
            int y = (int) pos[1];

            String dir = env.getVehicleDirection(vid);

            // Rotação do Veículo conforme a direção
            AffineTransform oldTransform = g2d.getTransform();
            g2d.translate(x, y); // Move para a posição do veículo

            if (dir.equals("S")) { g2d.rotate(Math.toRadians(180)); } 
            else if (dir.equals("E")) { g2d.rotate(Math.toRadians(90)); } 
            else if (dir.equals("W")) { g2d.rotate(Math.toRadians(-90)); }

            // Desenho do corpo 
            if (vid.startsWith("EM")) { g2d.setColor(Color.RED); }  // Emergência = Vermelho
            else { g2d.setColor(Color.BLUE); } // Normal = Azul
            g2d.fillRect(-6, -10, 12, 20);

            // Detalhes do veículo
            if (vid.startsWith("EM")) { g2d.setColor(Color.WHITE); } 
            else { g2d.setColor(new Color(50, 50, 50)); }
            g2d.fillRect(-5, -7, 10, 10);

            // Luzes
            g2d.setColor(Color.CYAN.darker());
            g2d.fillRect(-5, -9, 10, 4);

            // Faróis e Luzes Traseiras
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(-5, -11, 4, 4);
            g2d.fillOval(1, -11, 4, 4);

            // Luzes Traseiras
            g2d.setColor(Color.RED.darker().darker());
            g2d.fillRect(-5, 8, 4, 2);
            g2d.fillRect(1, 8, 4, 2);

            // Restauração da transformação original
            g2d.setTransform(oldTransform);

            // Desenho do ID do veículo
            g2d.setColor(Color.BLACK);
            g2d.drawString(vid, x + 10, y);
        }
    }

    private void drawInfo(Graphics g) {
        g.setColor(Color.black);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString("Veículos: " + env.getVehicles().size(), 10, 20);
        if (env.isAnyLightForcedRed()) {
            g.setColor(Color.RED);
            g.drawString("ALERTA: SEMÁFOROS FORÇADOS", 10, 40);
        }
        if (!env.getBlockedRoads().isEmpty()) {
            g.setColor(Color.RED);
            g.drawString("ALERTA: ESTRADAS CORTADAS", 10, 60);
        }
    }
}