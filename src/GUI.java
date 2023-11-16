import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class GUI extends JFrame implements ActionListener {
    JLabel leftPanelRoundsLabel;
    JLabel parametros;
    JList<String> list;
    private MainAgent mainAgent;
    private JPanel rightPanel;
    private JTextArea rightPanelLoggingTextArea;
    private LoggingOutputStream loggingOutputStream;
    DefaultTableModel model = new DefaultTableModel(){
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    JTable payoffTable = new JTable(model);
    public GUI() {
        initUI();
    }

    public GUI (MainAgent agent) {
        mainAgent = agent;
        initUI();
        loggingOutputStream = new LoggingOutputStream (rightPanelLoggingTextArea);
    }

    public void log (String s) {
        Runnable appendLine = () -> {
            rightPanelLoggingTextArea.append('[' + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] - " + s);
            rightPanelLoggingTextArea.setCaretPosition(rightPanelLoggingTextArea.getDocument().getLength());
        };
        SwingUtilities.invokeLater(appendLine);
    }

    public OutputStream getLoggingOutputStream() {
        return loggingOutputStream;
    }

    public void logLine (String s) {
        log(s + "\n");
    }

    public void setPlayersUI (String[] players) {
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String s : players) {
            listModel.addElement(s);
        }
        list.setModel(listModel);
    }

    public void initUI() {
        setTitle("Hawk and dove");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 400));
        setPreferredSize(new Dimension(1000, 600));
        setJMenuBar(createMainMenuBar());
        setContentPane(createMainContentPane());
        pack();
        setVisible(true);

        Image icon = new ImageIcon(getClass().getResource("img/icon.png")).getImage();
        setIconImage(icon);
        
    }

    private Container createMainContentPane() {
        JPanel pane = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridy = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        //LEFT PANEL
        gc.gridx = 0;
        gc.weightx = 1;
        pane.add(createLeftPanel(), gc);

        //CENTRAL PANEL
        gc.gridx = 1;
        gc.weightx = 8;
        pane.add(createCentralPanel(), gc);

        //RIGHT PANEL
        gc.gridx = 2;
        gc.weightx = 8;
        pane.add(createRightPanel(), gc);
        return pane;
    }

    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        leftPanelRoundsLabel = new JLabel("Round 0 /" + mainAgent.getR());

        JButton leftPanelNewButton = new JButton("New");
        leftPanelNewButton.addActionListener(actionEvent -> mainAgent.newGame());
        JButton leftPanelStopButton = new JButton("Stop");
        leftPanelStopButton.addActionListener(actionEvent -> mainAgent.stop());
        JButton leftPanelContinueButton = new JButton("Continue");
        leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.continuar());
        // leftPanelContinueButton.addActionListener(actionEvent -> mainAgent.);

        parametros = new JLabel(mainAgent.getParametros());

        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;
        gc.weightx = 0.5;
        gc.weighty = 0.5;

        gc.gridy = 0;
        leftPanel.add(leftPanelRoundsLabel, gc);
        gc.gridy = 1;
        leftPanel.add(leftPanelNewButton, gc);
        gc.gridy = 2;
        leftPanel.add(leftPanelStopButton, gc);
        gc.gridy = 3;
        leftPanel.add(leftPanelContinueButton, gc);
        gc.gridy = 4;
        gc.weighty = 10;
        leftPanel.add(parametros, gc);

        return leftPanel;
    }


    private JPanel createCentralPanel() {
        JPanel centralPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;

        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.gridx = 0;

        gc.gridy = 0;
        gc.weighty = 1;
        centralPanel.add(createCentralTopSubpanel(), gc);
        gc.gridy = 1;
        gc.weighty = 4;
        centralPanel.add(createCentralBottomSubpanel(), gc);

        return centralPanel;
    }

    private JPanel createCentralTopSubpanel() {
        JPanel centralTopSubpanel = new JPanel(new GridBagLayout());

        DefaultListModel<String> listModel = new DefaultListModel<>();
        listModel.addElement("Empty");
        list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(list);

        JLabel info1 = new JLabel("Selected player info");
        JButton updatePlayersButton = new JButton("Update players");
        updatePlayersButton.addActionListener(actionEvent -> mainAgent.updatePlayers());

        JButton deletePlayerButton = new JButton("Delete player");
        deletePlayerButton.addActionListener(
            new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] indicesSeleccionados = list.getSelectedIndices();

                for (int i = indicesSeleccionados.length - 1; i >= 0; i--) {
                    // list.remove(indicesSeleccionados[i]);
                    mainAgent.deletePlayer(indicesSeleccionados[i]);
                }
            }
        }); 

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 0.5;
        gc.weighty = 0.5;
        gc.anchor = GridBagConstraints.CENTER;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridheight = 1; // Establece la altura de una celda (no 666)
        gc.fill = GridBagConstraints.BOTH;
        centralTopSubpanel.add(listScrollPane, gc);

        gc.gridx = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        centralTopSubpanel.add(info1, gc);

        gc.gridy = 1;
        gc.gridheight = 1; // Establece la altura de una celda (no 666)
        centralTopSubpanel.add(updatePlayersButton, gc);

        gc.gridx = 2; // Cambia el índice de la columna para el botón de eliminar
        centralTopSubpanel.add(deletePlayerButton, gc);

        return centralTopSubpanel;
    }

    private JPanel createCentralBottomSubpanel() {
        JPanel centralBottomSubpanel = new JPanel(new BorderLayout());
    
        JLabel payoffLabel = new JLabel("Player Results");
        payoffTable = new JTable(model);
    
        // payoffTable.setTableHeader(null);
        payoffTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Crear JScrollPane sin barras de desplazamiento horizontales y verticales
        JScrollPane player1ScrollPane = new JScrollPane(payoffTable);
    
        centralBottomSubpanel.add(payoffLabel, BorderLayout.NORTH);
        centralBottomSubpanel.add(player1ScrollPane, BorderLayout.CENTER);
        
        return centralBottomSubpanel;
    }

    private JPanel createRightPanel() {
        rightPanel = new JPanel(new GridBagLayout());
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

    private JMenuBar createMainMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu menuFile = new JMenu("File");
        JMenuItem exitFileMenu = new JMenuItem("Exit");
        exitFileMenu.setToolTipText("Exit application");
        exitFileMenu.addActionListener(this);

        JMenuItem newGameFileMenu = new JMenuItem("New Game");
        newGameFileMenu.setToolTipText("Start a new game");
        newGameFileMenu.addActionListener(this);

        menuFile.add(newGameFileMenu);
        menuFile.add(exitFileMenu);
        menuBar.add(menuFile);

        JMenu menuEdit = new JMenu("Edit");
        JMenuItem resetPlayerEditMenu = new JMenuItem("Reset Players");
        resetPlayerEditMenu.setToolTipText("Resets the statistics of all players.");
        resetPlayerEditMenu.setActionCommand("reset_players");
        resetPlayerEditMenu.addActionListener(this);

        JMenuItem removePlayerEditMenu = new JMenuItem("Remove playe");
        removePlayerEditMenu.setToolTipText("Remove player from the game");
        removePlayerEditMenu.addActionListener(
            new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] indicesSeleccionados = list.getSelectedIndices();

                for (int i = indicesSeleccionados.length - 1; i >= 0; i--) {
                    // list.remove(indicesSeleccionados[i]);
                    mainAgent.deletePlayer(indicesSeleccionados[i]);
                }
            }
        }); 

        

        JMenuItem parametersEditMenu = new JMenuItem("Parameters");
        parametersEditMenu.setToolTipText("Modify the parameters of the game");
        parametersEditMenu.addActionListener(actionEvent -> logLine("Parameters: " + JOptionPane.showInputDialog(new Frame("Configure parameters"), "Enter parameters N,S,R,I,P")));



        menuEdit.add(resetPlayerEditMenu);
        menuEdit.add(removePlayerEditMenu);
        menuEdit.add(parametersEditMenu);
        menuBar.add(menuEdit);

        JMenu menuRun = new JMenu("Run");

        JMenuItem newRunMenu = new JMenuItem("New");
        newRunMenu.setToolTipText("Starts a new series of games");
        newRunMenu.addActionListener(this);

        JMenuItem stopRunMenu = new JMenuItem("Stop");
        stopRunMenu.setToolTipText("Stops the execution of the current round");
        stopRunMenu.addActionListener(this);

        JMenuItem continueRunMenu = new JMenuItem("Continue");
        continueRunMenu.setToolTipText("Resume the execution");
        continueRunMenu.addActionListener(this);

        JMenuItem roundNumberRunMenu = new JMenuItem("Number of rounds");
        roundNumberRunMenu.setToolTipText("Change the number of rounds");
        roundNumberRunMenu.addActionListener(actionEvent -> actualizarRounds());

        menuRun.add(newRunMenu);
        menuRun.add(stopRunMenu);
        menuRun.add(continueRunMenu);
        menuRun.add(roundNumberRunMenu);
        menuBar.add(menuRun);

        JMenu menuWindow = new JMenu("Window");

        JCheckBoxMenuItem toggleVerboseWindowMenu = new JCheckBoxMenuItem("Verbose", true);
        toggleVerboseWindowMenu.addActionListener(actionEvent -> rightPanel.setVisible(toggleVerboseWindowMenu.getState()));

        menuWindow.add(toggleVerboseWindowMenu);
        menuBar.add(menuWindow);

        return menuBar;
    }

    

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            logLine("Button " + button.getText());
        } else if (e.getSource() instanceof JMenuItem) {
            JMenuItem menuItem = (JMenuItem) e.getSource();
            logLine("Menu " + menuItem.getText());
        }
    }

    public class LoggingOutputStream extends OutputStream {
        private JTextArea textArea;

        public LoggingOutputStream(JTextArea jTextArea) {
            textArea = jTextArea;
        }

        @Override
        public void write(int i) throws IOException {
            textArea.append(String.valueOf((char) i));
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
    }

    public void actualizarRounds(){
        mainAgent.setNumberRounds(JOptionPane.showInputDialog(new Frame("Configure rounds"), "How many rounds?"));
        this.actualizarParametros();
    }

    public void actualizarParametros(){
        this.parametros.setText(mainAgent.getParametros());
        this.leftPanelRoundsLabel.setText("Round 0 /" + mainAgent.getR());
    }    
}
