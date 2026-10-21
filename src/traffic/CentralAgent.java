// Agente Central que controla os semáforos de forma fixa, para garantir segurança (ex: nunca ter Verde em ambas direções)

package traffic;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;

public class CentralAgent extends Agent {

    // Tempos fixos do ciclo (11 segundos no total)
    private final long GREEN_TIME = 9000; // 8 segundos de verde
    private final long YELLOW_TIME = 2000; // 2 segundos de amarelo

    @Override
    protected void setup() {
        System.out.println("--- AGENTE CENTRAL INICIADO ---");
        runTrafficPhase(0); // Começa com a fase 0 (Horizontal Verde)
    }

    // Máquina de estados que gere as fases ( 0 -> 1 -> 2 -> 3 -> 0 ...)
    private void runTrafficPhase(int phase) {
        Enums.LightState nsState = Enums.LightState.RED;
        Enums.LightState ewState = Enums.LightState.RED;
        long duration = 1000;

        // Define as cores baseadas apenas na fase
        switch (phase) {
            case 0: // Horizontal Verde
                nsState = Enums.LightState.RED; ewState = Enums.LightState.GREEN; duration = GREEN_TIME;
                break;
            case 1: // Horizontal Amarelo
                nsState = Enums.LightState.RED; ewState = Enums.LightState.YELLOW; duration = YELLOW_TIME;
                break;
            case 2: // Vertical Verde
                nsState = Enums.LightState.GREEN; ewState = Enums.LightState.RED; duration = GREEN_TIME;
                break;
            case 3: // Vertical Amarelo
                nsState = Enums.LightState.YELLOW; ewState = Enums.LightState.RED; duration = YELLOW_TIME;
                break;
        }

        // Manda as ordens para os semáforos (eles vão obedecer cegamente)
        applyLights(nsState, ewState);

        // Agenda a próxima mudança usando o WakerBehaviour.
        int nextPhase = (phase + 1) % 4;
        addBehaviour(new WakerBehaviour(this, duration) {
            @Override
            protected void onWake() { runTrafficPhase(nextPhase); }
        });
    }

    private void applyLights(Enums.LightState nsState, Enums.LightState ewState) {
        ACLMessage msgNS = new ACLMessage(ACLMessage.PROPOSE); msgNS.setContent(nsState.name());
        ACLMessage msgEW = new ACLMessage(ACLMessage.PROPOSE); msgEW.setContent(ewState.name());

        for (int i = 1; i <= 6; i++) {
            msgNS.addReceiver(new AID("trafficLight-NS" + i, AID.ISLOCALNAME));
            msgEW.addReceiver(new AID("trafficLight-EW" + i, AID.ISLOCALNAME));
            
            // A Central define a cor visualmente. Ninguém mais tem permissão para mudar.
            Environment.getInstance().setLightState("TL_NS" + i, nsState);
            Environment.getInstance().setLightState("TL_EW" + i, ewState);
        }
        send(msgNS);
        send(msgEW);
    }
}