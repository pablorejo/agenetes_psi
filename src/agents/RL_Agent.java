package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;

import java.util.Random;

import agents.tools.LearningTools;

// Class that represents the Random Agent, a player that choose randomly between actions D or H
public class RL_Agent extends Agent {

    private State state;
    private AID mainAgent;
    private ACLMessage msg;
    private int myId;
    private int N; // Numero de jugadores
    
    private LearningTools learningTools = new LearningTools();
    private int numberOfActions = 2;
    // Method used to register in the yellow pages as a player
    protected void setup() {

        // The first state is assigned
        state = State.s0NoConfig;

        // So that the Main Agent can find them as registered with type player and in the agents package
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Player");
        sd.setName("agents");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // The behavior is called to start playing
        addBehaviour(new Play());
        System.out.println("RandomAgent " + getAID().getName() + " is ready.");

    }

    // Method used to deregister the agent from the yellow pages
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.println("Error during deregistration Random: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("RandomPlayer " + getAID().getName() + " terminating.");
    }

    // The different states in which the player could be
    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    // Class used to play the games
    private class Play extends CyclicBehaviour { 

        // To select random action D or H
        Random random = new Random();
        @Override
        public void action() {
            // Waiting to receive a message
            msg = blockingReceive();
            if (msg != null) { 
                // The logic of the agent with all the states in which the player could be
                switch (state) {
                    // If the player is not configured, the first message he receives is his id, the number of rounds and the number of players
                    case s0NoConfig:
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) { 
                            boolean parametersUpdated = false;
                            try {
                                // The message is validated
                                parametersUpdated = validateSetupMessage(msg);
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            if (parametersUpdated) state = State.s1AwaitingGame;

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    // If the player is already configured, he can receive a message with his id, the number of rounds and the number of players, or the new game message with his id and his opponent's id
                    case s1AwaitingGame: 
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            // Game settings updated
                            if (msg.getContent().startsWith("Id#")) { 
                                try {
                                    // The message is validated
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                            // New game
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                try {
                                    // The message is validated
                                    gameStarted = validateNewGame(msg.getContent());
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                                if (gameStarted) state = State.s2Round;
                            }
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    // The rounds have started and the player has to perform action D or H
                    case s2Round:
                        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().startsWith("Action")) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(mainAgent);
                            // Generates a random value between 0 and 1 (inclusive)
                            // Generar un número aleatorio entre 0 y 1
                            int randomNumber = learningTools.iNewAction;
                            
                            
                            // Usar el número aleatorio para elegir entre "D" y "H"
                            String choice = (randomNumber == 0) ? "H" : "D";
                            msg.setContent("Action#"+choice); 
                            send(msg);
                            state = State.s3AwaitingResult;
                        // When the rounds of the game are over, the message with the final results arrives
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver")) {
                            // He returns to the state where he waits for another new game or change in the game parameters
                            state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    // Waiting for the results of the round
                    case s3AwaitingResult:
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            // He returns to the state of choosing what action to perform
                            state = State.s2Round;
                            int reward = obtenerRecompensa(msg.getContent());
                            learningTools.vGetNewActionQLearning(obtenerEstado(msg.getContent()), numberOfActions, reward);
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                
                }
            }
        }

        /**
         * Validates the setup message
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            System.out.println(msg);
            System.out.println("validateSetupMessage");
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("Id")) return false;
            
            myId = Integer.parseInt(contentSplit[1]);
            N = Integer.parseInt(contentSplit[2]);

            String[] parametersSplit = contentSplit[2].split(",");
            if (parametersSplit.length != 2) return false;

            // The AID of the Main Agents is obtained
            mainAgent = msg.getSender();
            System.out.println("validateSetupMessage");     
            return true;
        }

        /**
         * Validates the New Game message
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            String[] contentSplit = msgContent.split("#");
            System.out.println(msgContent);
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("NewGame")) {
                return false;
            } else {
                return true;
            }
        }
    }
     private int obtenerRecompensa( String resulString){
        int resultado = 0;
        String []idsString = resulString.split("#")[1].split(",");
        String []recompensasString = resulString.split("#")[3].split(",");

        if (myId == Integer.parseInt(idsString[0])){
            return Integer.parseInt(recompensasString[0]);
        }else if (myId == Integer.parseInt(idsString[1])){
            return Integer.parseInt(recompensasString[1]);
        }
        return resultado;
    }

    private String obtenerEstado( String resulString){
        String []idsString = resulString.split("#")[1].split(",");
        String []accioneStrings = resulString.split("#")[2].split(",");

        if (myId == Integer.parseInt(idsString[0])){
            return estadoResultante(accioneStrings);
        }else if (myId == Integer.parseInt(idsString[1])){
            String[] newAcionesStrings = new String[2];
            newAcionesStrings[0] = accioneStrings[1];
            newAcionesStrings[1] = accioneStrings[0];
            return estadoResultante(newAcionesStrings);
        }
        return "";
    }

    private String estadoResultante(String[] estados){
        if (estados[0].equals("H")){
            if (estados[1].equals("H")){
                return "state_1";
            }else if(estados[1].equals("D")){
                return "state_2";
            }
        }else if (estados[0].equals("D")){
            if (estados[1].equals("H")){
                return "state_3";
            }else if(estados[1].equals("D")){
                return "state_4";
            }
        }
        return "";
    }

}
