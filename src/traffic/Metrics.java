//Responsável por recolher dados durante a execução e gerar o relatório final.

package traffic;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class Metrics {
    private static Metrics instance = new Metrics();

    private Map<String, Long> waitingTimes = new HashMap<>();  // Guarda timestamps de início de espera.
    private Map<String, Integer> vehicleCycles = new HashMap<>(); // Conta viagens por veículo.

    private int vehiclesPassed = 0; // Total de viagens concluídas com sucesso.
    private long totalWaitingTime = 0; // Tempo total acumulado à espera (para média).
     
    // Buffer para guardar o relatório completo em memória antes de salvar
    private StringBuilder logBuffer = new StringBuilder();

    private Metrics() {
        logBuffer.append("=================\n");
        logBuffer.append(" RELATÓRIO DE SIMULAÇÃO\n");
        logBuffer.append("=================\n\n");

        // Regista o hook para salvar quando o programa fechar
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveReportToFile));
    }

    public static Metrics getInstance() {
        return instance;
    }
    
    //Regista uma mensagem no buffer do relatório, substituindo System.out.println para garantir que tudo vai para o ficheiro.
    public synchronized void log(String message) {
        logBuffer.append(message).append("\n"); 
    }
    
    public synchronized int getCycle(String vid) {
        return vehicleCycles.getOrDefault(vid, 1);
    }

    // Chamado quando um veículo chega ao destino, regista os detalhes da viagem (tempo, custo) e prepara o controlador para a próxima viagem.
    public synchronized void notifyArrival(String vid, String destinationName, double totalCost, double durationSeconds) {
        vehiclesPassed++;
        int cycle = vehicleCycles.getOrDefault(vid, 1);
        
        String logEntry = String.format(
            "--------------------------------------------------\n" +
            "--- CHEGADA: %s (Viagem #%d)\n" +
            "    Destino: %s\n" +
            "    Tempo  : %.2f s\n" +
            "    Custo  : %.2f\n" +
            "--------------------------------------------------", 
            vid, cycle, destinationName, durationSeconds, totalCost
        );

        log(logEntry); // Guarda no buffer
        
        vehicleCycles.put(vid, cycle + 1);
    }

    // Escreve o conteúdo do buffer para o ficheiro de relatório, executado no encerramento do programa.
    private void saveReportToFile() {
        // Única mensagem na consola para avisar que o ficheiro foi criado
        System.out.println("\n--- A GERAR RELATORIO ---");
        
        // Adiciona Estatísticas Finais ao buffer
        logBuffer.append("\n==================================================\n");
        logBuffer.append(" ESTATÍSTICAS GERAIS\n");
        logBuffer.append("==================================================\n");
        logBuffer.append("Total Veículos Chegados : ").append(vehiclesPassed).append("\n");
        logBuffer.append("Tempo Médio de Espera   : ").append(String.format("%.2f", getAverageWaitingTime())).append(" s\n");
        logBuffer.append("==================================================\n");

        try (PrintWriter out = new PrintWriter(new FileWriter("dados_do_sistema.txt"))) {
            out.print(logBuffer.toString());
            System.out.println("--- RELATORIO GUARDADO ---");
        } catch (IOException e) {
            System.err.println("ERRO ao guardar relatório: " + e.getMessage());
        }
    }

    // Regista o início do tempo de espera de um veículo (ex. parou num semáforo).
    public synchronized void recordWaitingStart(String vehicleId) {
        waitingTimes.put(vehicleId, System.currentTimeMillis());
    }

    // Regista o fim do tempo de espera de um veículo e atualiza o tempo total de espera.
    public synchronized void recordWaitingEnd(String vehicleId) {
        Long start = waitingTimes.remove(vehicleId);
        if (start != null) {
            long waitTime = (System.currentTimeMillis() - start) / 1000;
            totalWaitingTime += waitTime;
        }
    }
    
    public synchronized int getVehiclesPassed() { return vehiclesPassed; }
    public synchronized double getAverageWaitingTime() { return vehiclesPassed > 0 ? (double) totalWaitingTime / vehiclesPassed : 0.0; }
    
    public synchronized void logDisruptionImpact(String formattedMessage) { 
        log("LOG IMPREVISTO: " + formattedMessage);
    }
}