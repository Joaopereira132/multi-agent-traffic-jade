// Agente Semáforo, que recebe ordens da Central de Tráfego
// sobre qual cor deve exibir, e responder a pedidos de veículos

package traffic;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;

public class TrafficLightAgent extends Agent {
    private String tlId;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) tlId = (String) args[0];
        else tlId = getLocalName();

        // OUVIR CENTRAL (PROPOSE)
        // Recebe a ordem da central sobre a cor atual. O agente não decide a cor, apenas aceita o que a central diz.
        // Usa CyclicBehaviour simples para não bloquear o protocolo FIPA REQUEST usado para responder aos veículos.
        MessageTemplate mtPropose = MessageTemplate.MatchPerformative(ACLMessage.PROPOSE);
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive(mtPropose);
                // A Central já atualizou o Environment diretamente, 
                // aqui só recebemos a mensagem para não bloquear o protocolo.
                if (msg == null) block();
            }
        });

        // RESPONDER AOS PEDIDOS (FIPA REQUEST)
        // Usa o protocolo AchieveREResponder para gerir interações Request-Response.
        MessageTemplate mtRequest = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

        addBehaviour(new AchieveREResponder(this, mtRequest) {
            @Override
            protected ACLMessage prepareResponse(ACLMessage request) {
                ACLMessage reply = request.createReply();
                String content = request.getContent();
                
                // Verifica o estado visual atual (definido pela Central)
                Enums.LightState visibleState = Environment.getInstance().getLightState(tlId);

                if (content.equals("EMERGENCY_PASS")) {
                    // Veículo de emergência pode sempre passar.
                    String nomeAmbulancia = request.getSender().getLocalName();
                    Metrics.getInstance().log(">>> SEMÁFORO " + tlId + ": Recebi pedido de " + nomeAmbulancia + " para passar.");
                    
                    reply.setPerformative(ACLMessage.AGREE);
                    
                    Metrics.getInstance().log(tlId + ": Pedido Aceite");
                } 
                else if (content.equals("CAN_I_PASS")) {
                    // Para carros normais, a regra é estrita:
                    // Só diz que sim (AGREE) se a luz estiver VERDE.
                    if (visibleState == Enums.LightState.GREEN) {
                        reply.setPerformative(ACLMessage.AGREE);
                    } else {
                        reply.setPerformative(ACLMessage.REFUSE); // Luz AMARELA ou VERMELHA = NÃO PODE PASSAR
                    }
                } else {
                    reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                }
                return reply;
            }
        });
    }
}