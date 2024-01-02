import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.border.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Timer;
import java.util.List;

// Class that contains the graphical interface of the game
public final class GUI extends JFrame implements ActionListener {

    JLabel leftPanelRoundsLabel;
    JLabel leftPanelExtraInformation;
    JLabel leftPanelNumPlayersLabel;
    JLabel leftPanelNumGamesPlayedLabel;
    JList<String> list;
    private MainAgent mainAgent;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private JTextArea rightPanelLoggingTextArea;
    private GridBagConstraints gc;
    private LoggingOutputStream loggingOutputStream;
    JTable payoffTable;
    JTable statisticTable;
    private HashMap<String, Integer> playerIndexMap = new HashMap<>();
    private boolean verbose = true; 
    private boolean gameEnd = false;
    private Timer TimerOfLogs;
    List<String> bufferOfLogs = new ArrayList<>();

    // The class constructor without parameters 
    public GUI() {
        initUI();
        initializeLogTimer(); 
    }

    // The constructor of the class that takes the Main Agent as a parameter and can call its methods
    public GUI (MainAgent agent) {
        mainAgent = agent;
        initUI();
        loggingOutputStream = new LoggingOutputStream (rightPanelLoggingTextArea);
        // The timer for the logs is started
        initializeLogTimer(); 
    }

    // Method used to add the log to the buffer instead of printing directly. Also, it is only done if the verbose is enabled.
    public void log(String s) {
        // Only if the verbose window is enabled
        if (verbose) {
            Runnable appendLine = () -> {
                bufferOfLogs.add('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
            };
            SwingUtilities.invokeLater(appendLine);
        }
    }

    // Method used to print the accumulated logs and empty the buffer
    private void printAccumulatedLogs() {
        // If the verbose window is enabled and the buffer is not empty
        if(verbose){
            if (!bufferOfLogs.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    List<String> logsCopy = new ArrayList<>(bufferOfLogs); // Create a copy
                    for (String log : logsCopy) {
                        rightPanelLoggingTextArea.append(log);
                    }
                    bufferOfLogs.clear();
                    rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
                });
            }
        }
    }

    // Method used to initialize the timer with which the logs are printed every 5 seconds. It has delay 0 and period 5 seconds
    private void initializeLogTimer() {
        TimerOfLogs = new Timer(true);
        TimerOfLogs.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Calling the method that prints the accumulated logs
                printAccumulatedLogs();
            }
        }, 0, 1);
    }
    
    // Method used to set the boolean verbose variable true or false to print or not the logs
    public void setVerboseEnabledOrNot(boolean verboseState) {
        verbose = verboseState;
    }

    public OutputStream getLoggingOutputStream() {
        return loggingOutputStream;
    }

    public void logLine (String s) {
        log(s + "\n");
    }

    // Method used to display registered agents in the interface
    public void setPlayersUI (String[] players) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : players) {
            listModel.addElement(s);
        }
        list.setModel(listModel);
    }

    // Method used to initialize the interface with all its components
    public void initUI() {
        setTitle("GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(1000, 600));
        setJMenuBar(createMainMenuBar());
        setContentPane(createMainContentPane());
        pack();
        setVisible(true);
    }

    // Method used to create the structure of the panels
    private Container createMainContentPane() {
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weighty = 0.2;

        // It is added the top panel with the buttons and registered agents
        gc.gridx = 0;
        gc.weightx = 1;
        pane.add(createTopPanel(), gc);

        gc.gridx = 1;
        gc.weightx = 1;
        pane.add(createCentralTopSubpanel(), gc);
    
        // It is added the results table panel
        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.weightx = 1;
        gc.gridy = 1;
        gc.weighty = 1.5; 
        pane.add(createResultsTableSubpanel(), gc);
    
        // It is added the statistics table panel
        gc.gridx = 1;
        gc.gridwidth = 1;
        gc.weightx = 1;
        gc.gridy = 1;
        gc.weighty = 1.5; 
        pane.add(createStatisticsTableSubpanel(), gc);
    
        // It is added the logs panel
        gc.gridx = 0;
        gc.gridwidth = 2;
        gc.gridy = 2;
        gc.weightx = 1;
        gc.weighty = 0.5;
        pane.add(createLogsPanel(), gc);
    
        return pane;
    }

    // Method used to create the top panel with the buttons of New Game, Stop, Continue and the Game Parameters information
    private JPanel createTopPanel() {
        leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        leftPanel.setBackground(new Color(115, 198, 182));
        gc = new GridBagConstraints();
    
        int numRondas = mainAgent.getRounds();
        // Label with all the game information ( Number of rounds, number of players and number of played games)
        JLabel gameInfoLabel = new JLabel("Game Parameters");
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.insets = new Insets(0, 15, 0, 0);
        leftPanel.add(gameInfoLabel, gc);
        
        leftPanelRoundsLabel = new JLabel(numRondas+ " rounds");
        leftPanelNumPlayersLabel = new JLabel();
        leftPanelNumGamesPlayedLabel = new JLabel("0/0 played games");
        
        Border buttonBorder = BorderFactory.createCompoundBorder(
                new LineBorder(Color.BLACK), 
                new EmptyBorder(5, 5, 5, 5)    
        );
        
        // Button to start a new game
        JButton leftPanelNewButton = new JButton("New game");
        leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());
        leftPanelNewButton.setBorder(buttonBorder);

        // Button to stop the current game
        JButton leftPanelStopButton = new JButton("Stop");
        leftPanelStopButton.addActionListener(actionEvent -> mainAgent.Stop());
        leftPanelStopButton.setBorder(buttonBorder);

        // Button to continue the current game if the game was stopped
        JButton leftPanelContinueButton = new JButton("Continue");
        leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.Continue());
        leftPanelContinueButton.setBorder(buttonBorder);

        // It is created a JPanel to contain the labels
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK)); 
     
        JPanel labelsPanel = new JPanel(new GridBagLayout());
        labelsPanel.setBackground(new Color(229, 232, 232 ));
        GridBagConstraints labelsGc = new GridBagConstraints();
        labelsGc.insets = new Insets(0, 5, 0, 0);

        // The labels are added to the labelsPanel with some margin
        labelsGc.gridx = 0;
        labelsGc.gridy = 0;
        labelsPanel.add(leftPanelRoundsLabel, labelsGc);
        
        labelsGc.gridy = 1;
        labelsPanel.add(leftPanelNumPlayersLabel, labelsGc);
        
        labelsGc.gridy = 2;
        labelsPanel.add(leftPanelNumGamesPlayedLabel, labelsGc);

        // The labelsPanel are added to the infoPanel
        infoPanel.add(labelsPanel, BorderLayout.CENTER);
    
        // A margin is added to the infoPanel
        gc.insets = new Insets(5, 15, 0, 15);
    
        // The infoPanel is added to the leftPanel
        gc.gridx = 0;
        gc.gridwidth = 1;
        gc.weightx = 0.5;
        gc.gridheight = 1;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.BOTH;
        leftPanel.add(infoPanel, gc);
    
        // The buttons are added to the leftPanel
        gc.gridx = 1;
        gc.gridwidth = 1;
        gc.weightx = 0.5;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridy = 0;
        leftPanel.add(leftPanelNewButton, gc);
        gc.gridy = 1;
        leftPanel.add(leftPanelStopButton, gc);
        gc.gridy = 2;
        leftPanel.add(leftPanelContinueButton, gc);

        return leftPanel;
    }

    // Method used to set the updated number of players
    public void labelNumPlayers(int numPlayers){
        leftPanelNumPlayersLabel.setText(numPlayers+" players");
    }

    // Method used to set the updated number of played games
    public void labelNumGamesPlayed(int numGames, int totalGames){
        leftPanelNumGamesPlayedLabel.setText(numGames+"/"+totalGames+" played games");
        if(numGames==totalGames){
            // The tournament ends
            gameEnd = true;
        }else{
            gameEnd = false;
        }
    }

    // Method used to create the top panel with the registered agents and the buttons to update players and select players information
    private JPanel createCentralTopSubpanel() {
        // The box with the list of registered agents is configured
        JPanel centralTopSubpanel = new JPanel(new GridBagLayout());
        centralTopSubpanel.setBackground(new Color(115, 198, 182));
        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Empty");
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        list.setBackground(new Color(229, 232, 232 ));
        JScrollPane listScrollPane = new JScrollPane(list);

        // Border of the buttons
        Border buttonBorder = BorderFactory.createCompoundBorder(
                new LineBorder(Color.BLACK), 
                new EmptyBorder(5, 5, 5, 5)   
        );

        // Button to select a player and display all his information
        JButton info = new JButton("Selected player info");
        info.setBorder(buttonBorder);
        info.addActionListener(this);
        // Button to update the list of registered players
        JButton updatePlayersButton = new JButton("     Update players    ");
        updatePlayersButton.setBorder(buttonBorder);
        updatePlayersButton.addActionListener(actionEvent -> mainAgent.updatePlayers());

        // Button to clear the logs or the results table
        JButton clearLogsButton = new JButton("        Clear         ");
        clearLogsButton.setBorder(buttonBorder);
        //clearLogsButton.addActionListener(actionEvent -> clearLogs());
        clearLogsButton.addActionListener(actionEvent -> {
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Select an option:",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Clear logs", "Clear table"},
                    "Clear logs");
        
            if (choice == JOptionPane.YES_OPTION) {
                clearLogs();
            } else if (choice == JOptionPane.NO_OPTION) {
                clearTable();
            }
        });

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 1;
        gc.gridy = 0;
        gc.weighty = 2;
        gc.gridheight = 3;
        gc.fill = GridBagConstraints.BOTH;
        gc.insets = new Insets(10, 0, 10, 15);
        centralTopSubpanel.add(listScrollPane, gc);
        
        gc.insets = new Insets(5, 15, 0, 15);
        gc.gridx = 0;
        gc.gridheight = 1;
        gc.gridwidth = 1;
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.NONE;
        centralTopSubpanel.add(info, gc);
        gc.gridy = 1;
        centralTopSubpanel.add(updatePlayersButton, gc);
        gc.gridy = 2;
        gc.insets = new Insets(5, 15, 5, 15);
        centralTopSubpanel.add(clearLogsButton, gc);

        return centralTopSubpanel;
    }
    
    // Method used to create the statistics table panel: Name of the player, total points, played games, won games, lost games and tied games
    private JPanel createStatisticsTableSubpanel() {
        JPanel statisticPanel = new JPanel(new GridBagLayout());
        // The table header
        Object[] nameColumns = {"PLAYER", "TOTAL POINTS", "PLAYED GAMES", "WON GAMES", "LOST GAMES", "TIED GAMES"};
        Object[][] data = {};

        JLabel playerStatisticLabel = new JLabel("Player Statistics");
        DefaultTableModel modelo = new DefaultTableModel(data, nameColumns);
        statisticTable = new JTable(modelo);

        // The table header visibility is set
        statisticTable.getTableHeader().setVisible(true);
        statisticTable.getTableHeader().setBackground(new Color(115, 198, 182));
        
        // A TableRowSorter is added to allow column sorting. The column by which the table is going to be sort is Won Games (column 3)
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelo);
        sorter.setComparator(3, Comparator.comparingInt(o -> (int) o)); 
        statisticTable.setRowSorter(sorter);
        
        // A custom renderer is added for the Player and Won Games column to be able to give them a different color
        statisticTable.getColumnModel().getColumn(0).setCellRenderer(new CustomColorRenderer());
        statisticTable.getColumnModel().getColumn(3).setCellRenderer(new CustomColorRenderer());
        
        JScrollPane playerStatistic = new JScrollPane(statisticTable);
        Border lineBorder = BorderFactory.createLineBorder(new Color(115, 198, 182));
        playerStatistic.setBorder(BorderFactory.createCompoundBorder(lineBorder, BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weighty = 0.4;
        gc.insets = new Insets(0, 15, 0, 0);
        statisticPanel.add(playerStatisticLabel, gc);

        gc.gridy = 1;
        gc.weighty = 8;
        gc.insets = new Insets(0, 0, 0, 0);
        statisticPanel.add(playerStatistic, gc);

        return statisticPanel;
    }

    // Custom renderer to change cell background color
    class CustomColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Background color is changed only for the Player and Won Games columns
            if (column == 0 || column == 3) {
                rendererComponent.setBackground(new Color(115, 198, 182)); 
            }
            
            return rendererComponent;
        }
    }

    // Method used to display player statistics. It is called from the Main Agent with all the information as parameters: Name of the player, total points, played games, won games, lost games and tied games
    public void agregarStatistic(String player, int totalPoints, int gamesPlayed, int gamesWon, int gamesLost, int gamesTied) {
        DefaultTableModel modelo = (DefaultTableModel) statisticTable.getModel();
        Integer rowIndex = playerIndexMap.get(player);

        // If the player is already in the table, the row is updated
        if (rowIndex != null) {
            modelo.setValueAt(totalPoints, rowIndex, 1);
            modelo.setValueAt(gamesPlayed, rowIndex, 2);
            modelo.setValueAt(gamesWon, rowIndex, 3);
            modelo.setValueAt(gamesLost, rowIndex, 4);
            modelo.setValueAt(gamesTied, rowIndex, 5);
        } else {
            // If the player is not in the table, a new row is added
            modelo.addRow(new Object[]{player, totalPoints, gamesPlayed, gamesWon, gamesLost, gamesTied});
            // The player is added to the map
            playerIndexMap.put(player, modelo.getRowCount() - 1);
        }

        // Sort table by Won Games after update
        sortByGamesWon();
    }
    
    // Method used to sort the table according to the Won Games column in descending order
    private void sortByGamesWon() {
        RowSorter<? extends TableModel> rowSorter = statisticTable.getRowSorter();
        if (rowSorter != null) {
            DefaultRowSorter<?, ?> defaultRowSorter = (DefaultRowSorter<?, ?>) rowSorter;
            // The sorting is set by the Won Games column in descending order
            defaultRowSorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(3, SortOrder.DESCENDING)));
            defaultRowSorter.sort();
        }
    }

    // Method used to create the results table panel. The information shown is: Number of the round, name of the player : score of that round (for each player) and the winner of the round
    private JPanel createResultsTableSubpanel() {
        JPanel centralBottomSubpanel = new JPanel(new GridBagLayout());
        // The table header
        Object[] nameColumns = {"ROUND", "PLAYER1: SCORE", "PLAYER2: SCORE", "WINNER"};
        Object[][] data = {};

        JLabel payoffLabel = new JLabel("Player Results");
        DefaultTableModel modelo = new DefaultTableModel(data, nameColumns);
        
        payoffTable = new JTable(modelo);
        // The table header visibility is set
        payoffTable.getTableHeader().setVisible(true);
        payoffTable.getTableHeader().setBackground(new Color(115, 198, 182));
        
        JScrollPane player1ScrollPane = new JScrollPane(payoffTable);
        Border lineBorder = BorderFactory.createLineBorder(new Color(115, 198, 182));
        player1ScrollPane.setBorder(BorderFactory.createCompoundBorder(lineBorder, BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.weighty = 0.4;
        gc.insets = new Insets(0, 15, 0, 0);
        centralBottomSubpanel.add(payoffLabel, gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.weighty = 8;
        gc.insets = new Insets(0, 0, 0, 0);
        centralBottomSubpanel.add(player1ScrollPane, gc);

        return centralBottomSubpanel;
    }

    // Method used to show the information in the table
    public void agregateValuesTable(int numeroronda, String ronda1, String ronda2) {
        // Only if the verbose mode is activated
        if(verbose){
            DefaultTableModel modelo = (DefaultTableModel) payoffTable.getModel();
            modelo.addRow(new Object[]{numeroronda, ronda1, ronda2,"*"});
        }
    }

    // Method used to show the information in the table when the game ends with the total scores of that game for both players and the winner
    public void addWinner(String winner, int score1, int score2){
        DefaultTableModel modelo = (DefaultTableModel) payoffTable.getModel();
        modelo.addRow(new Object[]{"END GAME", score1 , score2 , winner});
        // To put the column Winner in other color
        payoffTable.setDefaultRenderer(Object.class, new CustomRowRenderer());
    }

    // Method used to clean all the logs
    public void clearLogs() {
        SwingUtilities.invokeLater(() -> {
            rightPanelLoggingTextArea.setText("");
        });
    }

    // Method used to clean the results table
    public void clearTable() {
        if(gameEnd){
            DefaultTableModel modeloPayoff = (DefaultTableModel) payoffTable.getModel();
            int rowCount = modeloPayoff.getRowCount();
            for (int i = rowCount - 1; i >= 0; i--) {
                modeloPayoff.removeRow(i);
            }
        }
    }

    // Method used to erase all the data of the tables
    public void eraseDataTable() {
        // First the results table
        DefaultTableModel modeloPayoff = (DefaultTableModel) payoffTable.getModel();
        int rowCount = modeloPayoff.getRowCount();
        for (int i = rowCount - 1; i >= 0; i--) {
            modeloPayoff.removeRow(i);
        }

        // Then the statistics table
        DefaultTableModel modeloStat = (DefaultTableModel) statisticTable.getModel();
        int rowCount2 = modeloStat.getRowCount();
        for (int i = rowCount2 - 1; i >= 0; i--) {
            modeloStat.removeRow(i);
        }

        // The map is cleared
        playerIndexMap.clear();
    }

    // Method used to change the color of the Winner field 
    public class CustomRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // The value of the Winner column is obtained
            Object winnerValue = table.getValueAt(row, table.getColumnModel().getColumnIndex("WINNER"));
            if ("*".equals(winnerValue)) {
                // The color of the field is changed
                c.setBackground(table.getBackground()); 
            } else {
                c.setBackground(new Color(115, 198, 182));
            }

            return c;
        }
    }

    // Method used to remove the chosen agent in the graphical interface
    public void removePlayer() {
        // The list of all players' names is obtained from the Main Agent
        String[] listplayers = mainAgent.listPlayersNames();
        // A dialog message appears to select the player that is going to be removed
        Object select = JOptionPane.showInputDialog(
            this, 
            "Select the player of the tournament that you want to remove",
            "Selection of a player to remove",
            JOptionPane.QUESTION_MESSAGE,
            null,
            listplayers,
            listplayers[0] 
        );
    
        if (select != null) {
            // Ask for confirmation before removing the player
            int removeConf = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove the player?",
                "Confirmation to remove a player",
                JOptionPane.YES_NO_OPTION
            );
    
            if (removeConf == JOptionPane.YES_OPTION) {
                String playerRemoved = select.toString();
                String[] parts = playerRemoved.split("@");
                String playerLocalName = parts[0];
                // The index of the removed player is obtained
                int rowIndexToRemove = getPlayerRowIndex(playerLocalName);
    
                // The row of the table is removed
                if (rowIndexToRemove != -1) {
                    DefaultTableModel model = (DefaultTableModel) statisticTable.getModel();
                    model.removeRow(rowIndexToRemove);
                    // It is removed from the statistics map
                    playerIndexMap.remove(playerRemoved);
                    // Map indexes are adjusted
                    for (Map.Entry<String, Integer> entry : playerIndexMap.entrySet()) {
                        int oldIndex = entry.getValue();
                        // If the eliminated player is before the current player on the table, adjust the index
                        if (oldIndex > rowIndexToRemove) {
                            playerIndexMap.put(entry.getKey(), oldIndex - 1);
                        }
                    }
                }
    
                // Call the Main Agent method to remove the player
                mainAgent.removePlayer(playerRemoved);
                // Call the Main Agent method to get the number of players
                int numPlayers = mainAgent.getNumPlayers();
                // Updates the existing JLabel text
                leftPanelNumPlayersLabel.setText(numPlayers + " players");
            }
        }
    }    

    // Method used to get the index of the row containing the player
    private int getPlayerRowIndex(String playerName) {
        DefaultTableModel model = (DefaultTableModel) statisticTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(playerName)) {
                return i;
            }
        }
        // Return -1 if the player is not found in the table
        return -1; 
    }
    
    // Method used to create the logs panel
    private JPanel createLogsPanel() {
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(new Color(115, 198, 182));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.weighty = 1d;
        c.weightx = 1d;

        rightPanelLoggingTextArea = new JTextArea("");
        rightPanelLoggingTextArea.setEditable(false);
        JScrollPane jScrollPane = new JScrollPane(rightPanelLoggingTextArea);
        rightPanel.add(jScrollPane, c);

        return rightPanel;
    }

    // Method used to create the menu with the different options
    private JMenuBar createMainMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu("File");
        // The exit option to close the graphical interface
        JMenuItem exitFileMenu = new JMenuItem("Exit");
        exitFileMenu.setToolTipText("Exit application");
        exitFileMenu.addActionListener(actionEvent -> System.exit(0));

        // The New Game option to start a new tournament
        JMenuItem newGameFileMenu = new JMenuItem("New Game");
        newGameFileMenu.setToolTipText("Start a new game");
        newGameFileMenu.addActionListener(actionEvent -> mainAgent.newGame());

        menuFile.add(newGameFileMenu);
        menuFile.add(exitFileMenu);
        menuBar.add(menuFile);

        JMenu menuEdit = new JMenu("Edit");
        // The reset players option to reset all the statistics of the players
        JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
        resetPlayerEditMenu.setToolTipText("Reset all player");
        resetPlayerEditMenu.setActionCommand("reset_players");
        resetPlayerEditMenu.addActionListener(this);

        // The remove player option to remove a player
        JMenuItem removePlayerEditMenu = new JMenuItem("Remove player");
        removePlayerEditMenu.setToolTipText("Remove player from the game");
        removePlayerEditMenu.setActionCommand("remove_player");
        removePlayerEditMenu.addActionListener(this);

        menuEdit.add(resetPlayerEditMenu);
        menuEdit.add(removePlayerEditMenu);
        menuBar.add(menuEdit);

        JMenu menuRun = new JMenu("Run");
        // The new option to start a new tournament
        JMenuItem newRunMenu = new JMenuItem("New");
        newRunMenu.setToolTipText("Starts a new series of games");
        newRunMenu.addActionListener(actionEvent -> mainAgent.newGame());

        // The stop option to stop the current game
        JMenuItem stopRunMenu = new JMenuItem("Stop");
        stopRunMenu.setToolTipText("Stops the execution of the current round");
        stopRunMenu.addActionListener(actionEvent -> mainAgent.Stop());

        // The continue option to resume the current game if it was stopped
        JMenuItem continueRunMenu = new JMenuItem("Continue");
        continueRunMenu.setToolTipText("Resume the execution");
        continueRunMenu.addActionListener(actionEvent -> mainAgent.Continue());

        // The number of rounds option to change the number of rounds
        JMenuItem roundNumberRunMenu = new JMenuItem("Number of rounds");
        roundNumberRunMenu.setToolTipText("Change the number of rounds");
        roundNumberRunMenu.setActionCommand("number_rounds");
        roundNumberRunMenu.addActionListener(actionEvent -> {
            String input = JOptionPane.showInputDialog(new Frame("Configure rounds"), "How many rounds?");  
            try{

                int numberOfRounds = Integer.parseInt(input);
                mainAgent.changeRounds(numberOfRounds);
                logLine(numberOfRounds + " rounds");
                int numeroRondas = mainAgent.getRounds();
                // Updates the number of rounds
                leftPanelRoundsLabel.setText("Rounds " + numeroRondas);
            }catch (NumberFormatException e) {
                logLine("Invalid input for the number of rounds");
            }  
        });

        menuRun.add(newRunMenu);
        menuRun.add(stopRunMenu);
        menuRun.add(continueRunMenu);
        menuRun.add(roundNumberRunMenu);
        menuBar.add(menuRun);

        JMenu menuWindow = new JMenu("Window");
        // The verbose option to choose whether or not to display the logs
        JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
        toggleVerboseWindowMenu.addActionListener(actionEvent -> {
            boolean newState = toggleVerboseWindowMenu.getState();
            rightPanel.setVisible(newState);
            setVerboseEnabledOrNot(newState); 
        });
        menuWindow.add(toggleVerboseWindowMenu);
        menuBar.add(menuWindow);

        JMenu menuHelp = new JMenu("Help");
        // The About option to know the information about the author of the program
        JMenuItem aboutHelpMenu = new JMenuItem("About");
        aboutHelpMenu.setToolTipText("Information about the program author");
        aboutHelpMenu.addActionListener(this);
        menuHelp.add(aboutHelpMenu);
        menuBar.add(menuHelp);
        Border menuBarBorder = BorderFactory.createCompoundBorder(
                new LineBorder(new Color(115, 198, 182), 1), 
                new EmptyBorder(5, 5, 5, 5)
        );
        menuBar.setBorder(menuBarBorder);

        return menuBar;
    }

    // Method used to used to set the actions of some buttons and menu items
    @Override
    public void actionPerformed(ActionEvent e) {   
        if (e.getSource() instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            String menuItemText = menuItem.getText();
            String actionCommand = menuItem.getActionCommand();

            if ("About".equals(menuItemText)) {
                // The information about the program author
                logLine("This is the " + menuItemText + " section. The author of this program is Henar Mari\u00F1o Bodel\u00F3n.");
                JLabel messageLabel = new JLabel("The author of this program is Henar Mari\u00F1o Bodel\u00F3n");
                JOptionPane.showMessageDialog(null, messageLabel, "About", JOptionPane.INFORMATION_MESSAGE);

            }else if ("reset_players".equals(actionCommand)) {
                logLine("Menu " + menuItemText);
                int resetConf = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to reset all players?",
                    "Confirmation to reset all players",
                    JOptionPane.YES_NO_OPTION
                );
            
                if (resetConf == JOptionPane.YES_OPTION) {
                    // Call the Main Agent to reset the statistics
                    mainAgent.resetPlayers();
                    // Erase the data of the tables
                    eraseDataTable();
                    clearLogs();
                }
            }else if ("remove_player".equals(actionCommand)) {
                logLine("Menu " + menuItemText);
                // To remove one player
                removePlayer();

            }else{
                logLine("Menu " + menuItemText);
            }
            
        }else if (e.getSource() instanceof JButton) {
            JButton sourceButton = (JButton) e.getSource();
            // To select the information about one selected player
            if (sourceButton.getText().equals("Selected player info")) {
                String selectedPlayer = list.getSelectedValue();
                if (selectedPlayer != null) {
                    ArrayList<Object> infoPlayer = mainAgent.getPlayerInfo(selectedPlayer);
                    String playerInfoString = "Selected Player: " + selectedPlayer + "\n" +
                    "ID in the game : " + infoPlayer.get(0) + "\n" +
                    "Local name : " + infoPlayer.get(1) + "\n" +
                    "Total score : " + infoPlayer.get(2) + "\n" +
                    "Played games : " + infoPlayer.get(3) + "\n" +
                    "Won games : " + infoPlayer.get(4) + "\n" +
                    "Lost games : " + infoPlayer.get(5) + "\n" +
                    "Tied games : " + infoPlayer.get(6);
    
                    // A dialog box with the player's information is displayed
                    JOptionPane.showMessageDialog(null, playerInfoString, "Player Info", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Please select a player", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    // Class used to add the logs to the buffer to be printed with all the accumulated logs
    public class LoggingOutputStream extends OutputStream {

        private JTextArea textArea;
        private StringBuilder currentLine = new StringBuilder();

        public LoggingOutputStream(JTextArea jTextArea) {
            textArea = jTextArea;
        }

        @Override
        public void write(int i) throws IOException {
            char characterToPrint = (char) i;
            // If the verbose is enabled the logs are added to the buffer to be printed
            if (verbose) {
                if (characterToPrint == '\n') {
                    currentLine.append(characterToPrint);
                    bufferOfLogs.add(currentLine.toString());
                    currentLine.setLength(0); 
                } else {
                    currentLine.append(characterToPrint);
                }
            }
        }
    }

}
