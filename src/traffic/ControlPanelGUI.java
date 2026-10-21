//Interface gráfica de controlo, permitindo forçar semáforos a vermelho e bloquear estradas em tempo real

package traffic;

import javax.swing.*;
import java.awt.*;

public class ControlPanelGUI extends JFrame {

    public ControlPanelGUI() {
        setTitle("Centro de Comando");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();

        // SEMÁFOROS (Forçar Vermelho)
        JPanel lightsPanel = new JPanel(new GridLayout(7, 3, 5, 5));
        lightsPanel.add(new JLabel("ID", SwingConstants.CENTER));
        lightsPanel.add(new JLabel("Vertical (NS)", SwingConstants.CENTER));
        lightsPanel.add(new JLabel("Horizontal (EW)", SwingConstants.CENTER));

        Environment env = Environment.getInstance();

        // Loops para criar botões de controlo para cada semáforo
        for (int i = 1; i <= 6; i++) {
            final int id = i;
            lightsPanel.add(new JLabel("" + i, SwingConstants.CENTER));

            // Botão Norte-Sul
            JToggleButton btnNS = new JToggleButton("Auto");
            btnNS.addActionListener(e -> {
                boolean force = btnNS.isSelected();
                btnNS.setText(force ? "VERMELHO" : "Auto");
                btnNS.setBackground(force ? Color.RED : null);
                // Chama o ambiente para forçar o semáforo a vermelho
                env.setForceRed("TL_NS" + id, force);
            });
            lightsPanel.add(btnNS);

            // Botão Este-Oeste 
            JToggleButton btnEW = new JToggleButton("Auto");
            btnEW.addActionListener(e -> {
                boolean force = btnEW.isSelected();
                btnEW.setText(force ? "VERMELHO" : "Auto");
                btnEW.setBackground(force ? Color.RED : null);
                // Chama o ambiente para forçar o semáforo a vermelho
                env.setForceRed("TL_EW" + id, force);
            });
            lightsPanel.add(btnEW);
        }

        // OBRAS  (Cortar Estradas) ---
        JPanel roadsPanel = new JPanel(new GridLayout(7, 5, 5, 5));
        roadsPanel.add(new JLabel("ID", SwingConstants.CENTER));
        roadsPanel.add(new JLabel("Norte", SwingConstants.CENTER));
        roadsPanel.add(new JLabel("Sul", SwingConstants.CENTER));
        roadsPanel.add(new JLabel("Este", SwingConstants.CENTER));
        roadsPanel.add(new JLabel("Oeste", SwingConstants.CENTER));

        for (int i = 1; i <= 6; i++) {
            final int id = i;
            roadsPanel.add(new JLabel("Cruzamento " + i, SwingConstants.CENTER));

            // Botão Norte
            roadsPanel.add(createRoadBtn(env, "ROAD_" + id + "_N"));
            // Botão Sul
            roadsPanel.add(createRoadBtn(env, "ROAD_" + id + "_S"));
            // Botão Este
            roadsPanel.add(createRoadBtn(env, "ROAD_" + id + "_E"));
            // Botão Oeste
            roadsPanel.add(createRoadBtn(env, "ROAD_" + id + "_W"));
        }

        tabbedPane.addTab("Semáforos", lightsPanel);
        tabbedPane.addTab("Barreiras", roadsPanel);

        add(tabbedPane);
        setLocation(50, 50);
    }

    // Método auxiliar para criar botões de bloqueio de estrada
    private JToggleButton createRoadBtn(Environment env, String roadId) {
        JToggleButton btn = new JToggleButton("X");
        btn.setFont(new Font("Arial", Font.BOLD, 10));
        btn.addActionListener(e -> {
            boolean block = btn.isSelected();
            btn.setBackground(block ? Color.YELLOW : null);
            btn.setText(block ? "BLOQ" : "X");
            env.toggleRoadBlock(roadId, block); // Atualiza o Environment (Afeta o A*)
        });
        return btn;
    }
}