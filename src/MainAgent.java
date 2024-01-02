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
import java.util.Objects;
import java.util.concurrent.Semaphore;

// Class that represents the Main Agent, that manage all the agents and games
public class MainAgent extends Agent {

    private GUI gui;
    private AID[] playerAgents;
    private String[] playerNames;
    private GameParametersStruct parameters= new GameParametersStruct(); 
    ArrayList<PlayerInformation> players = new ArrayList<>();
    int numGamesPlayed=0;
    // The stop variable is set to true when the game is in the stop mode and false in other cases
    volatile boolean stop;
    // The boolean variable to determine if there is a tournament in progress
    volatile boolean currentGame = false;
    // The semaphores to control the stop and continue modes without active standby 
    private Semaphore semaphore = new Semaphore(1);
    // The semaphore to control the access to the stop variable
    private Semaphore stopSem = new Semaphore(1);
    // The semaphore to control the access to the currentGame variable
    private Semaphore currentGameSem = new Semaphore(1);
    private int totalGames;

    @Override
    // Method to initialize the Main Agent
    protected void setup() {
        gui = new GUI(this);
        System.setOut(new PrintStream(gui.getLoggingOutputStream()));

        // The registered agents are updated
        updatePlayers();
        gui.logLine("Agent " + getAID().getName() + " is ready.");
    }

    // Method used to update the players who are registered in the service
    public int updatePlayers() { 
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        // To find the agents that are registered with type player and in the agents package
        sd.setType("Player"); 
        sd.setName("agents"); 
        template.addServices(sd);
        try {
            // Registered agents are sought
            DFAgentDescription[] result = DFService.search(this, template);
            parameters.setN(result.length);
            gui.labelNumPlayers(result.length);
            if (result.length > 0) {
                // The total number of games that agents have to play is calculated ([N * (N-1)] / 2). N is the number of players
                totalGames = (result.length * (result.length - 1))/2;
                gui.logLine("Updating player list. Found " + result.length + " players");
            }else{
                totalGames = 0;
            }

            playerAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i) {
                playerAgents[i] = result[i].getName();
            }

            int lastId = 0;
            for (AID a : playerAgents) {
                boolean agentExists = false;
                // Check if the agent is already in the players list
                for (PlayerInformation player : players) {
                    if (player.aid.equals(a)) {
                        agentExists = true;
                        break;
                    }
                }
                // If the agent is not in the list, it is added to the list
                if (!agentExists) {
                    // IDs are assigned
                    players.add(new PlayerInformation(a, a.getName(), a.getLocalName(), lastId++, 0, 0, 0, 0, 0));
                }
            }      

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        playerNames = new String[playerAgents.length];
        for (int i = 0; i < playerAgents.length; i++) {
            playerNames[i] = playerAgents[i].getName();
        }
        // Player names are sent to the GUI so that they can be shown in the interface
        gui.setPlayersUI(playerNames);
        return 0;
    }

    // Method used to reset each player's scores and statistics
    public int resetPlayers(){
        for(PlayerInformation player : players){
            player.setReset();
        } 
        // The number of played games is reset too
        numGamesPlayed = 0;
        gui.labelNumGamesPlayed(numGamesPlayed,0);
        return 0;
    }

    // Method used from the interface to obtain the list of all players' names  
    public String[] listPlayersNames(){
        return playerNames;
    }
    
    // Method used to remove the chosen agent in the graphical interface
    public int removePlayer(String namePlayerRemoved){
        gui.logLine(namePlayerRemoved+ " is removed from the game");
        try{
            PlayerInformation playerToRemove = null;
            for (PlayerInformation player : players) {
                if(player.nameAgent.equals(namePlayerRemoved)){
                        playerToRemove = player;
                        break;
                }
            }
            if(playerToRemove!=null){
                players.remove(playerToRemove);
                // A description of the agent you wish to remove including its AID is created
                DFAgentDescription descripcionAgente = new DFAgentDescription();
                descripcionAgente.setName(playerToRemove.aid);
                // Deregister the agent from the yellow pages
                DFService.deregister(this, descripcionAgente);
                // We update the registered agents in the game because they have changed
                updatePlayers();
            }
        }catch(FIPAException e){
            System.err.println("Error during deregistration: " + e.getMessage());
        }
        return 0;
    }

    // Method used to modify the number of rounds called from the interface
    public int changeRounds(int R){
        parameters.setR(R);
        return 0;
    }

    // Method used to obtain the number of rounds established for the game
    public int getRounds(){
        return parameters.getR();
    }

    // Method used to obtain the number of players in the game
    public int getNumPlayers(){
        return parameters.getN();
    }

    // Method used to start a new game, calling the behavior of the Main Agent
    public int newGame() {
        try{
        // If the stop mode is disabled and there is not a tournament in progress
        currentGameSem.acquire();
        if(!stop && !currentGame){
            // The new game is started
            numGamesPlayed=0;
            currentGame = true;
            addBehaviour(new GameManager());
        }
        currentGameSem.release();
        }catch (InterruptedException e) {
                e.printStackTrace();
        }
        return 0;
    }

    // Method used to stop the game
    public void Stop(){
        try{
            stopSem.acquire();
            // If the stop mode is not active
            if (!stop){
                try {
                    // The stop mode is activated
                    stop=true;
                    gui.logLine("The game is paused");
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            stopSem.release();
        }catch (InterruptedException e) {
                e.printStackTrace();
        }
        return;
    }

    // Method used to resume the game
    public void Continue(){
        try{
            stopSem.acquire();
            // If the stop mode is active
            if (stop){
                // The stop mode is deactivated
                stop=false;
                semaphore.release(); 
            }
            stopSem.release();
        }catch (InterruptedException e) {
                e.printStackTrace();
        }
        return;
    }

    // Method used to obtain all the statistics of a player, knowing his name
    public ArrayList<Object> getPlayerInfo(String namePlayer){
        ArrayList<Object> playerInfo = new ArrayList<>(); 
        for (PlayerInformation player : players) {
            // To choose the correct player
            if(player.nameAgent.equals(namePlayer)){
                playerInfo.add(player.id);
                playerInfo.add(player.localName);
                playerInfo.add(player.getScorePlayer());
                playerInfo.add(player.gamesPlayed);
                playerInfo.add(player.gamesWon);
                playerInfo.add(player.gamesLost);
                playerInfo.add(player.gamesTied);
                break;
            }
        }
        return playerInfo;
    }

    /**
     * In this behavior this agent manages the course of a match during all the
     * rounds.
     */
    private class GameManager extends SimpleBehaviour {

        @Override
        public void action() {
            //Initialize (inform ID)
            for (PlayerInformation player : players) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Id#" + player.id + "#" + parameters.N + "," + parameters.R);
                msg.addReceiver(player.aid);
                send(msg);
            }
            // Organize 2-player matches
            for (int i = 0; i < players.size(); i++) {
                for (int j = i + 1; j < players.size(); j++) { 
                    playGame(players.get(i), players.get(j));
                }
            }
        }

        private void playGame(PlayerInformation player1, PlayerInformation player2) {
            int result1game = 0;
            int result2game = 0;
            try{
                // To store the results of each round
                ArrayList<Result> results = new ArrayList<>();

                // A message is sent to the two players of the game that a new game has started with their ids
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(player1.aid); 
                msg.addReceiver(player2.aid);
                msg.setContent("NewGame#" + player1.id + "#" + player2.id);
                send(msg);
                gui.logLine(msg.getContent());

                String act1, act2;
                int i;
                // To be repeated R times (number of rounds)
                for (i=0;i< parameters.R;i++){
                    // If the stop mode is activated, the game is stopped 
                    // Acquire the semaphore before starting the game
                    semaphore.acquire();
                    // Release the semaphore so that it does not get blocked in the following rounds
                    semaphore.release();

                    // Player 1 is asked to take action
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent("Action");
                    msg.addReceiver(player1.aid);
                    send(msg);
                    // Waiting to receive the player's action
                    ACLMessage move1 = blockingReceive();
                    act1 = move1.getContent().split("#")[1];
                    
                    // Player 2 is asked to take action
                    msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.setContent("Action");
                    msg.addReceiver(player2.aid);
                    send(msg);
                    // Waiting to receive the player's action
                    ACLMessage move2 = blockingReceive();
                    act2 = move2.getContent().split("#")[1]; 
                    
                    int result1ronda=0, result2ronda=0;
                    // The actions of this round are analyzed to determine the points obtained by each player
                    if(act1.equals("H") && act2.equals("H")){
                        result1ronda= -1;
                        result2ronda= -1;
                    }else if(act1.equals("D") && act2.equals("D")){
                        result1ronda= 5;
                        result2ronda= 5;
                    }else if(act1.equals("H") && act2.equals("D")){
                        result1ronda= 10;
                        result2ronda= 0;
                    }else if(act1.equals("D") && act2.equals("H")){                     
                        result1ronda= 0;
                        result2ronda= 10;
                    }

                    // The total scores in the current game of the players are saved
                    result1game = result1game + result1ronda;
                    result2game = result2game + result2ronda;

                    // The message with the results of the current round is sent
                    msg = new ACLMessage(ACLMessage.INFORM);
                    msg.addReceiver(player1.aid);
                    msg.addReceiver(player2.aid);
                    msg.setContent("Results#"+player1.id+","+player2.id+"#"+act1+","+act2+"#"+result1ronda+","+result2ronda);
                    send(msg);

                    // Information about the player with his score is saved
                    String resultPlayer1= player1.localName + ":"+ result1ronda;
                    String resultPlayer2= player2.localName + ":"+ result2ronda;
                    // Add the results of the round to the list
                    results.add(new Result(i,resultPlayer1,resultPlayer2));
                }

                // R rounds have already been played and the message must be sent with the total scores of the game
                msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(player1.aid);
                msg.addReceiver(player2.aid);
                msg.setContent("GameOver#"+player1.id+","+player2.id+"#"+result1game+","+result2game);
                send(msg);
                gui.logLine(msg.getContent());

                // The total scores of the players are saved
                player1.setScorePlayer(player1.getScorePlayer() + result1game);
                player2.setScorePlayer(player2.getScorePlayer() + result2game);

                // Send the results of all rounds to the interface
                for(Result result : results){
                    gui.agregateValuesTable(result.getRound(), result.resultPlayer1, result.resultPlayer2);
                }

                // The number of played games for each player is increased, and depending on the result of the game, it is added one to the number of games won, lost or tied
                player1.incrementGamesPlayed();
                player2.incrementGamesPlayed();
                if(result1game > result2game){
                    gui.addWinner(player1.localName, result1game, result2game);
                    player1.incrementGamesWon();
                    player2.incrementGamesLost();
                }else if(result2game > result1game){
                    gui.addWinner(player2.localName, result1game, result2game);
                    player1.incrementGamesLost();
                    player2.incrementGamesWon();
                }else {
                    gui.addWinner("DRAW", result1game, result2game);
                    player1.incrementGamesTied();
                    player2.incrementGamesTied();
                }

                // The number of played games is updated and sent to the interface
                numGamesPlayed += 1;
                gui.labelNumGamesPlayed(numGamesPlayed, totalGames);
                // Player statistics information is sent to the interface to be displayed
                gui.agregarStatistic(player1.localName, player1.getScorePlayer(), player1.gamesPlayed, player1.gamesWon, player1.gamesLost, player1.gamesTied);
                gui.agregarStatistic(player2.localName, player2.getScorePlayer(), player2.gamesPlayed, player2.gamesWon, player2.gamesLost, player2.gamesTied);
    
                // The tournament ends
                if(numGamesPlayed == totalGames){
                    try{
                        currentGameSem.acquire();
                        // The boolean variable that determines if there is a tournament in progress is set to false
                        currentGame = false;
                        currentGameSem.release();
                    }catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } 

        }

        @Override
        public boolean done() {
            return true;
        }

    }

    // Class with the results of each round to be sent to the interface and displayed in the table
    public class Result {

        int round;
        String resultPlayer1;
        String resultPlayer2;

        // The class constructor
        public Result(int nround, String nresultPlayer1, String nresultPlayer2){
            round = nround;
            resultPlayer1 = nresultPlayer1;
            resultPlayer2 = nresultPlayer2;
        }

        // Method used to get the number of the round
        public int getRound() {
            return round;
        }
    
        // Method used to get the score of the player 1 in that round
        public String getResultPlayer1() {
            return resultPlayer1;
        }
    
        // Method used to get the score of the player 2 in that round
        public String getResultPlayer2() {
            return resultPlayer2;
        }
    
    }

    // Class with all the parameters of each player
    public class PlayerInformation {

        AID aid;
        int id;
        int scorePlayer;
        String nameAgent;
        String localName;
        int gamesPlayed;
        int gamesWon;
        int gamesLost;
        int gamesTied;

        // The class constructor
        public PlayerInformation(AID a, String name, String localNameAgent, int i, int result, int gamesp, int gamesw, int gamesl, int gamest) {
            aid = a;
            nameAgent = name;
            id = i;
            scorePlayer= result;
            localName = localNameAgent;
            gamesPlayed = gamesp;
            gamesWon = gamesw;
            gamesLost = gamesl;
            gamesTied = gamest;

        }

        // Method used to get the score of the player
        public int getScorePlayer(){
            return scorePlayer;
        }

        // Method used to set the score of the player
        public void setScorePlayer(int score){
            scorePlayer = score;
        }

        // Method used to get the number of played games
        public int getGamesPlayed() {
            return gamesPlayed;
        }

        // Method used to increase the number of played games
        public void incrementGamesPlayed() {
            gamesPlayed++;
        }

        // Method used to get the number of won games
        public int getGamesWon() {
            return gamesWon;
        }

        // Method used to increase the number of won games
        public void incrementGamesWon() {
            gamesWon++;
        }

        // Method used to get the number of lost games
        public int getGamesLost() {
            return gamesLost;
        }

        // Method used to increase the number of lost games
        public void incrementGamesLost() {
            gamesLost++;
        }

        // Method used to get the number of tied games
        public int getGamesTied() {
            return gamesTied;
        }

        // Method used to increase the number of tied games
        public void incrementGamesTied() {
            gamesTied++;
        }

        // Method used to obtain the full name of the agent
        public String getName() {
            return nameAgent;
        }

        // Method used to reset the player statistics
        public void setReset() {
            scorePlayer = 0;
            gamesPlayed = 0;
            gamesWon = 0;
            gamesLost = 0;
            gamesTied = 0;
        }

        @Override
        // Equals method to compare two objects of this class
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            PlayerInformation other = (PlayerInformation) obj;
            return Objects.equals(nameAgent, other.nameAgent) && Objects.equals(aid, other.aid);
        }
    }

    // It contains the game parameters: number of rounds (R) and number of players (N)
    public class GameParametersStruct {  

        int N;
        int R;

        // The class constructor with the default number of rounds
        public GameParametersStruct() {
            R=50;
        }

        // Method used to set the number of rounds
        public void setR(int newValue) {
            R = newValue;
        }

        // Method used to get the number of rounds
        public int getR(){
            return R;
        }

        // Method used to set the number of players
        public void setN(int newValue) {
            N = newValue;
        }

        // Method used to get the number of players
        public int getN(){
            return N;
        }
    }
}
