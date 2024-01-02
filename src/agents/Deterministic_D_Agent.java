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

public class Deterministic_D_Agent extends Agent {

    private State state;
    private AID mainAgent;
    private int myId, opponentId;
    private int N; // Numero de jugadores
            
    
    private ACLMessage msg;

    protected void setup() {
        state = State.s0NoConfig;  // Establece el estado inicial del agente
    
        // Registra el agente en las Páginas Amarillas (Yellow Pages) como un jugador
        DFAgentDescription dfd = new DFAgentDescription();  // Crea una descripción del agente
        dfd.setName(getAID());  // Establece el nombre del agente en la descripción
    
        ServiceDescription sd = new ServiceDescription();  // Crea una descripción de servicio
        sd.setType("Player");  // Define el tipo de servicio como "Player"
        sd.setName("Game");  // Establece el nombre del servicio como "Game"
    
        dfd.addServices(sd);  // Agrega la descripción de servicio a la descripción del agente
    
        try {
            DFService.register(this, dfd);  // Registra el agente y sus servicios en las Páginas Amarillas
        } catch (FIPAException fe) {
            fe.printStackTrace();  // En caso de error al registrar, muestra información de la excepción
        }
    
        addBehaviour(new Play());  // Agrega un comportamiento llamado "Play" al agente
        System.out.println("RandomAgent " + getAID().getName() + " is ready.");  // Muestra un mensaje en la consola indicando que el agente está listo
    }
    

    protected void takeDown() {
        //Deregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println("RandomPlayer " + getAID().getName() + " terminating.");
    }

    private enum State {
        s0NoConfig, s1AwaitingGame, s2Round, s3AwaitingResult
    }

    private class Play extends CyclicBehaviour {
        @Override
        public void action() {
            System.out.println(getAID().getName() + ":" + state.name());
            msg = blockingReceive();
            if (msg != null) {
                System.out.println(getAID().getName() + " received " + msg.getContent() + " from " + msg.getSender().getName()); //DELETEME
                //-------- Agent logic
                switch (state) {
                    case s0NoConfig:
                        // Si el mensaje comienza con "Id#" y es de tipo INFORM, procesa la configuración y pasa al estado 1
                        // De lo contrario, muestra un mensaje de error
                        if (msg.getContent().startsWith("Id#") && msg.getPerformative() == ACLMessage.INFORM) {
                            boolean parametersUpdated = false;
                            try {
                                parametersUpdated = validateSetupMessage(msg);
                            } catch (NumberFormatException e) {
                                System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                            }
                            if (parametersUpdated) state = State.s1AwaitingGame;

                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                    case s1AwaitingGame:
                        // Si el mensaje es de tipo INFORM y comienza con "Id#", actualiza la configuración
                        // Si el mensaje comienza con "NewGame#", procesa el inicio del juego y pasa al estado 2
                        // De lo contrario, muestra un mensaje de error
                        if (msg.getPerformative() == ACLMessage.INFORM) {
                            if (msg.getContent().startsWith("Id#")) { //Game settings updated
                                try {
                                    validateSetupMessage(msg);
                                } catch (NumberFormatException e) {
                                    System.out.println(getAID().getName() + ":" + state.name() + " - Bad message");
                                }
                            } else if (msg.getContent().startsWith("NewGame#")) {
                                boolean gameStarted = false;
                                try {
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
                    case s2Round:
                        // Si el mensaje es de tipo REQUEST y contiene el mensaje "Action", responde con la posición y pasa al estado 3
                        // Si el mensaje es de tipo INFORM y comienza con "Changed#", no hace nada
                        // Si el mensaje comienza con "EndGame", vuelve al estado 1
                        // De lo contrario, muestra un mensaje de error
                        if (msg.getPerformative() == ACLMessage.REQUEST && msg.getContent().equals("Action")) {
                            // Enviamos la accion al agente principal
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            
                            
                            msg.addReceiver(mainAgent);
                            msg.setContent("Action#D" );
                            System.out.println(getAID().getName() + " sent " + msg.getContent());
                            send(msg);
                            state = State.s3AwaitingResult;


                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Changed#")) {
                            // Process changed message, in this case nothing
                        } else if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("GameOver")) {
                            state = State.s1AwaitingGame;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message:" + msg.getContent());
                        }
                        break;
                    case s3AwaitingResult:
                        // Si el mensaje es de tipo INFORM y comienza con "Results#", vuelve al estado 2
                        // De lo contrario, muestra un mensaje de error
                        if (msg.getPerformative() == ACLMessage.INFORM && msg.getContent().startsWith("Results#")) {
                            //Process results
                            // No hay nada que procesar puesto que el agente es aleatorio y nos da igual el resultado
                            state = State.s2Round;
                        } else {
                            System.out.println(getAID().getName() + ":" + state.name() + " - Unexpected message");
                        }
                        break;
                }
            }
        }

        /**
         * Validates and extracts the parameters from the setup message
         *
         * @param msg ACLMessage to process
         * @return true on success, false on failure
         */
        private boolean validateSetupMessage(ACLMessage msg) throws NumberFormatException {
            String msgContent = msg.getContent();

            String[] contentSplit = msgContent.split("#");
            if (contentSplit.length != 3) return false;
            if (!contentSplit[0].equals("Id")) return false;
            myId = Integer.parseInt(contentSplit[1]);
            N = Integer.parseInt(contentSplit[2]);


            mainAgent = msg.getSender();
            return true;
        }

        /**
         * Processes the contents of the New Game message
         * @param msgContent Content of the message
         * @return true if the message is valid
         */
        public boolean validateNewGame(String msgContent) {
            int msgId0, msgId1;
            String[] contentSplit = msgContent.split("#");
            System.out.println(msgContent);
            if (contentSplit.length != 2) return false;
            if (!contentSplit[0].equals("NewGame")) return false;
            String[] idSplit = contentSplit[1].split(",");
            if (idSplit.length != 2) return false;
            msgId0 = Integer.parseInt(idSplit[0]);
            msgId1 = Integer.parseInt(idSplit[1]);
            if (myId == msgId0) {
                opponentId = msgId1;
                return true;
            } else if (myId == msgId1) {
                opponentId = msgId0;
                return true;
            }
            return false;
        }
    }
}
