
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.io.PrintStream;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;

public class MainAgent extends Agent {

    private GUI gui;
    private AID[] playerAgents;
    private GameParametersStruct parameters = new GameParametersStruct();
   

    
    

    public Results getResult_Results(String action1, String action2){
        System.out.println(action1 + " , " + action2);
        
        if (action1.equals("H")){
            if (action2.equals("H")){
                return new Results(-1,-1);
            }else if(action2.equals("D")){
                return new Results(10,0);
            }

        }else if(action1.equals("D")){
            if (action2.equals("H")){
                return new Results(0,10);

            }else if(action2.equals("D")){
                return new Results(5,5);
            }
        }
        return new Results(0,0);
    }


    
    @Override
    protected void setup() {
        gui = new GUI(this);
        System.setOut(new PrintStream(gui.getLoggingOutputStream()));

        updatePlayers();
        gui.logLine("Agent " + getAID().getName() + " is ready.");
    }

    public int updatePlayers() {
        gui.logLine("Updating player list");
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        template.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                gui.logLine("Found " + result.length + " players");
            }
            playerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
                
            }
        } catch (FIPAException fe) {
            gui.logLine(fe.getMessage());
        }
        //Provisional
        String[] playerNames = new String[playerAgents.length];
        

        gui.model.setColumnCount(0);
        gui.model.setRowCount(0);
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName();
            String nombre = playerAgents[i].getName().split("@")[0];

            gui.model.addColumn(nombre);
            if (gui.model.getRowCount() == 0)
            {
                gui.model.addRow(playerNames);
            }
            gui.model.setValueAt(nombre, 0, i);
            
        }

        gui.setPlayersUI(playerNames);
        this.parameters.N = playerNames.length;
        gui.actualizarParametros();
        return 0;
    }

    public int newGame() {
        addBehaviour(new GameManager());
        return 0;
    }

    /**
     * In this behavior this agent manages the course of a match during all the
     * rounds.
     */
    private class GameManager extends SimpleBehaviour {

        @Override
        public void action() {
            //Assign the IDs
            ArrayList<PlayerInformation> players = new ArrayList<>();
            int lastId = 0;
            for (AID a : playerAgents) {
                players.add(new PlayerInformation(a, lastId++));
            }

            // Enviamos la informacion de su id a cada agente
            for (PlayerInformation player : players) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N);
                msg.addReceiver(player.aid);
                send(msg);
            }

            // Organize the matches
            Object[] resultados_tabla = new Object[players.size()];

            for (int i = 0; i < players.size(); i++) {
                resultados_tabla[i] = 0; // O el valor inicial adecuado
            }

            for (int i = 0; i < players.size(); i++) {
                for (int j = i + 1; j < players.size(); j++) { //too lazy to think, let's see if it works or it breaks
                    Results resultados = playGame(players.get(i), players.get(j));
                    resultados_tabla[i] = (int)resultados_tabla[i] + resultados.player1;
                    resultados_tabla[j] = (int)resultados_tabla[j] + resultados.player2;
                }
            }
            System.out.println("Hola \n\n");
            gui.model.addRow(resultados_tabla);
        }

        private Results playGame(PlayerInformation player1, PlayerInformation player2) {
            int player1_value = 0;
            int player2_value = 0;
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);

            for (int i = 0; i < parameters.R; i++){
                //Empezamos enviado el mensaje de NewGame a ambos jugadores.
                msg.addReceiver(player1.aid);
                msg.addReceiver(player2.aid);
                msg.setContent("NewGame#" + player1.id + "," + player2.id);
                send(msg);

                String action1, action2;

                //Enviamos el mensaje de accion a uno de los jugadores
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Action");
                msg.addReceiver(player1.aid);
                send(msg);

                // Recivimos el mensaje de accion de uno de los jugadores
                gui.logLine("Main Waiting for movement");
                ACLMessage move1 = blockingReceive();
                gui.logLine("Main Received " + move1.getContent() + " from " + move1.getSender().getName());
                action1 = (move1.getContent().split("#")[1]);

                //Enviamos el mensaje de accion a uno de los jugadores
                msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setContent("Action");
                msg.addReceiver(player2.aid);
                send(msg);

                // Recivimos el mensaje de accion de uno de los jugadores
                gui.logLine("Main Waiting for movement");
                ACLMessage move2 = blockingReceive();
                gui.logLine("Main Received " + move2.getContent() + " from " + move2.getSender().getName());
                action2 = (move2.getContent().split("#")[1]);

                // Enviamos los resultados
                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(player1.aid);
                msg.addReceiver(player2.aid);

                Results resultados = new Results(0,0);
                resultados.setResults(action1, action2);

                msg.setContent("Results#" + player1.id + "," + player2.id + "#" +action1 + "," + action2 + resultados.toString());
                player1_value += resultados.player1;
                player2_value += resultados.player2;

                send(msg);
               
            }

            // Terminamos el juego.
            msg.setContent("GameOver#" + player1.id + "," + player2.id + "#" + player1_value + "," + player2_value);
            send(msg);

            return new Results(player1_value, player2_value);

        }


        @Override
        public boolean done() {
            return true;
        }
    }

    private class Results{
        int player1;
        int player2;
        public Results(int player1,int player2){
            this.player1 = player1;
            this.player2 = player2;
        }

        public String toString(){
            return "#" + player1 + "," + player2;
        }

        public void setResults(String action1, String action2){
            
            if (action1.equals("H")){
                if (action2.equals("H")){
                    player1 = -1;
                    player2 = -1;
                }else if(action2.equals("D")){
                    player1 = 10;
                    player2 = 0;
                }

            }else if(action1.equals("D")){
                if (action2.equals("H")){
                    player1 = 0;
                    player2 = 10;

                }else if(action2.equals("D")){
                    player1 = 5;
                    player2 = 5;
                }
            }
        }

        public Object[] getObject(){
            return new Object[] {player1,player2};
        } 
    }

    public class PlayerInformation {

        AID aid; // El AID es utilizado para identificar y comunicarse con el agente en el sistema multiagente
        int id; 

        public PlayerInformation(AID a, int i) {
            aid = a;
            id = i;
        }

        @Override
        public boolean equals(Object o) {
            return aid.equals(o);
        }
    }

    public class GameParametersStruct {
        
        int N; // Numero total de jugadores
        int R; // Numero de rondas en cada juego de dos jugadores

        public GameParametersStruct() {
            N = 0;
            R = 4;
        }
        public String getParametros() {
            return String.format("<html>Parameters:<br> Nº Players: %d <br>Rounds:%d </html>", N, R);
        }
    }

    public String getParametros(){
        return this.parameters.getParametros();
    }

    public void updatePrametres(){
        if (playerAgents != null){
            parameters.N = playerAgents.length;
        }
        gui.actualizarParametros();
    }


    public void setNumberRounds(String numero) {
        System.out.println(("Numero de rounds:" + numero));
        this.parameters.R = Integer.parseInt(numero);
    }

    public void deletePlayer(int indice){
        // Crear un nuevo arreglo sin el objeto a eliminar
        AID aidToRemove = playerAgents[indice];

        AID[] newPlayerAgents = new AID[playerAgents.length - 1];
        int newIndex = 0;
        for (AID aid : playerAgents) {
            if (!aid.equals(aidToRemove)) {
                newPlayerAgents[newIndex] = aid;
                
                newIndex++;
            }
        }
        


        // Actualizar la lista original con el nuevo arreglo
        playerAgents = newPlayerAgents;

        String[] playerNames = new String[playerAgents.length];
        gui.model.setColumnCount(0);
        gui.model.setRowCount(0);
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName();

            String nombre = playerAgents[i].getName().split("@")[0];
            gui.model.addColumn(nombre);
            if (gui.model.getRowCount() == 0)
            {
                gui.model.addRow(playerNames);
            }
            gui.model.setValueAt(nombre, 0, i);
        }
        gui.setPlayersUI(playerNames);
        System.out.println("Player " + aidToRemove.getName() + " delete.");
        this.updatePrametres();
    }
}
