// Classe que define as Enumerações (Tipos de Dados constantes) usadas no sistema. Garante que todos os agentes falam a "mesma língua" em termos de estados e tipos.

package traffic;

public class Enums {

    // Estados possíveis do semáforo utilizado pelo agente TrafficLightAgent para informar o seu estado atual e pelo veiculo para decidir se deve parar ou avançar.
    public enum LightState {
        RED, YELLOW, GREEN  //Vermelho, Amarelo, Verde
    }
    
    // Tipos de pontos de interesse (POI) / destinos no sistema. Utilizados pelo Environment para saber que ícone desenhar na GUI e pelos agentes para definir os seus destinos. 
    public enum POIType {
        HOUSE, HOSPITAL, POLICE, FIRE_STATION
    }
}