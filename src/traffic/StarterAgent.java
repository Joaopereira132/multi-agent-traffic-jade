//Agente inicializador do sistema, responsável por criar o ambiente e todos os outros agentes.
//Ele fica inativo após o setup uma vez que apenas serve para iniciar o sistema.

package traffic;

import jade.core.Agent;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;

public class StarterAgent extends Agent {
    @Override
    protected void setup() {
        //Inicializa o ambiente 
        Environment env = Environment.getInstance();
        env.initDefault(); //Carrega o ambiente, custos e configurações iniciais.

        //Inicializa a GUI do ambiente (visualização)
        EnvironmentGUI gui = new EnvironmentGUI(env);
        env.setGui(gui); //Associa a GUI ao ambiente para atualizações futuras
        gui.showInFrame();

        //Inicializa o painel de controle (controle manual dos semáforos e barreiras). Usamos invokeLater para garantir a segurança da thread gráfica
        java.awt.EventQueue.invokeLater(() -> {
            ControlPanelGUI controlPanel = new ControlPanelGUI();
            controlPanel.setVisible(true);
        });

        try {
            //Obtém o controlador do JADE para criar agentes
            ContainerController cc = getContainerController();

            //Criação dos semáforos (12 no total, 6 para cada direção NS e EW)
            for (int i = 1; i <= 6; i++) {
                //Semáforos Norte-Sul (Vertical)
                AgentController tlNS = cc.createNewAgent("trafficLight-NS" + i, "traffic.TrafficLightAgent",
                        new Object[] { "TL_NS" + i });
                //Semáforos Este-Oeste (Horizontal)
                AgentController tlEW = cc.createNewAgent("trafficLight-EW" + i, "traffic.TrafficLightAgent",
                        new Object[] { "TL_EW" + i });
                tlNS.start();
                tlEW.start();
            }

            //--- CRIAÇÃO DOS VEÍCULOS NORMAIS ---
            String[] purplePoints = { "P1", "P2", "P3", "P4", "P5", "P6", "P7", "P8", "P9", "P10" };
            for (String p : purplePoints) {
                for (int i = 1; i <= 2; i++) {
                    //Cria 2 veículos para cada ponto de spawn para garantir volume de trânsito
                    cc.createNewAgent("vehicle-" + p + "-" + i, "traffic.VehicleAgent",
                            new Object[] { "V-" + p + "-" + i }).start();
                }
            }

            //--- CRIAÇÃO DOS VEÍCULOS DE EMERGÊNCIA ---
            String[] emIds = { "EM1", "EM2", "EM3", "EM4", "EM5", "EM6", "EM7", "EM8", "EM9" };
            for (String emId : emIds) {
                cc.createNewAgent("emergency-" + emId, "traffic.EmergencyVehicleAgent",
                        new Object[] { emId }).start();
            }

            // --- AGENTES DE GESTÃO E DISRUPÇÃO ---
            // CentralAgent: Coordena as fases dos semáforos
            AgentController central = cc.createNewAgent("central", "traffic.CentralAgent", null);
            // DisruptionAgent: Gera acidentes e aprende com o trânsito (MachineLearning)
            AgentController disruption = cc.createNewAgent("disruption", "traffic.DisruptionAgent", null);

            central.start();
            disruption.start();

            System.out.println("Todos os Agentes foram criados!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}