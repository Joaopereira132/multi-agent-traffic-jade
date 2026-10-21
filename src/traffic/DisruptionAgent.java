// Agente responsável por simular acidentes de trânsito com base em fatores como obras e congestionamento, utilizando uma abordagem de regressão logística para aprender padrões e gerar acidentes virtuais.

package traffic;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import java.util.Map;
import java.util.Random;

public class DisruptionAgent extends Agent {
    // Valor base (Probabilidade de acidente quando tudo está calmo)
    private double bias = -2.0;
    
    // Pesos: [0] = Obras, [1] = Fila
    private double[] w = new double[] { 2.5, 1.5 }; 
    
    private Random rnd = new Random();

    @Override
    protected void setup() {
        // Ciclo de decisão a cada 4 segundos
        addBehaviour(new TickerBehaviour(this, 4000) {
            protected void onTick() {
                // Coletar Fatores Reais
                // Verifica se existem barreiras reais no mapa (input binário)
                boolean hasRealBarriers = !Environment.getInstance().getBlockedRoads().isEmpty();
                double constr = hasRealBarriers ? 1.0 : 0.0; 
                
                // Tamanho da fila (aumenta se forçarmos semáforos a vermelho)
                double queue = avgQueue(); 
                
                // Calcular Probabilidade (Regressão Logística)
                // Fórmula: z = Bias + (Obras * PesoObras) + (Fila * PesoFila)
                double z = bias + (constr * w[0]) + (queue * w[1]);
                // Função Sigmóide: Transforma z em probabilidade [0,1]
                double p = 1.0 / (1.0 + Math.exp(-z));
                
                // Gerar Acidente se probabilidade for alta (> 40%)
                if (p > 0.40) { 
                    simulateAccident();
                    String reason = determineReason(constr, queue);
                    String logMsg = String.format("ACIDENTE DETETADO (Prob: %.3f) | Motivo: %s", p, reason);
                    notifyAllAgents(logMsg);
                }
                
                // Aprendizagem (online)
                // Define o "Label Real": Se o trânsito está mau (obras/filas), DEVERIA haver acidente (1)
                int label = (constr > 0.5 || queue > 1.0) ? 1 : 0;
                onlineUpdate(constr, queue, label);
            }
        });
    }
    
    // Identifica o motivo (Obras ou Fila)
    private String determineReason(double c, double q) {
        // Calcula o impacto real de cada fator na fórmula
        double impactoObras = c * w[0];  // Ex: 1.0 * 4.0 = 4.0
        double impactoFila = q * w[1];   // Ex: 0.8 * 3.0 = 2.4
    
        // Quem tiver maior impacto, é o motivo principal
        if (impactoObras > impactoFila && c > 0) {
            return "OBRAS NA VIA (Barreiras Ativas)";
        } else if (impactoFila > impactoObras && q > 0) {
            return "TRÂNSITO INTENSO (Semáforos Vermelhos)";
        } else {
            // Se ambos forem zero (ou muito baixos) mas o Bias disparou o alarme
            return "FALHA MECÂNICA / ALEATÓRIO";
        }
    }

    private double avgQueue() {
        // Calcula média de carros parados em todos os semáforos
        Map<String, Integer> qs = Environment.getInstance().getAllQueuesSnapshot();
        if (qs.isEmpty()) return 0.0;
        double sum = 0;
        for (int q : qs.values()) sum += q;
        return sum / qs.size();
    }

    private void notifyAllAgents(String content) {
        Metrics.getInstance().logDisruptionImpact(content);
    }

    // Atualização Online dos Pesos usando Regressão Logística 
    private void onlineUpdate(double constr, double queue, int label) {
        // Recalcular a previsão atual
        double z = bias + (constr * w[0]) + (queue * w[1]);
        double pred = 1.0 / (1.0 + Math.exp(-z));
        
        double lr = 0.05; // Taxa de aprendizagem
        double erro = label - pred;

        // Atualizar Pesos e Bias
        bias += lr * erro * 1.0;       // Atualiza o Bias
        w[0] += lr * erro * constr;    // Atualiza peso das Obras
        w[1] += lr * erro * queue;     // Atualiza peso da Fila
    }

    private void simulateAccident() {
        // Simula um acidente aumentando artificialmente a fila em um semáforo aleatório
        int interId = rnd.nextInt(6) + 1; 
        String affectedLight = rnd.nextBoolean() ? "TL_NS" + interId : "TL_EW" + interId;
        Environment.getInstance().incQueue(affectedLight);
    }
}