package co.raring.telnetqueue.controllers;

import co.raring.telnetqueue.TQMain;
import co.raring.telnetqueue.tool.FileHelper;
import co.raring.telnetqueue.tool.LogViewAppender;
import co.raring.telnetqueue.tool.QConfiguration;
import co.raring.telnetqueue.tool.Telnet;
import com.sun.jna.platform.unix.X11;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

public class TabController implements Initializable {
    // Debug Options
    //private static final boolean QUERY = true; // TRUE=Query PuTTY Registry,FALSE=Use predefined utility configurations
    private static final int WAIT_MULTIPLIER = 60000; // Multiplier for Queue Wait, 60000 = 1 minute
    private static final long COMMAND_TIMEOUT = (1000 * 60) * 120; // Command timeout in MS, 1000*60*120=2 hours
    //private static final long COMMAND_TIMEOUT = 5000; // 5 Second debug time

    @FXML
    private AnchorPane ap;
    @FXML
    private Spinner<Integer> queueWait;
    @FXML
    private TextField chosenFile;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextArea commandList;
    @FXML
    private ChoiceBox<String> clientList;
    @FXML
    private ChoiceBox<String> gwList;
    @FXML
    private Label progressLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private ImageView zennerLogo;
    @FXML
    private ToggleGroup waitMethod;

    private File miuFile;
    private List<String> miuList;
    private QConfiguration savedConfiguration;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set Top Logo to Zenner
        zennerLogo.setImage(new Image(TQMain.class.getResourceAsStream("zlogo.png")));

        savedConfiguration = new QConfiguration();

        clientList.setItems(FXCollections.observableList(new ArrayList<>(TQMain.SESSIONS.keySet())));
        clientList.getSelectionModel().select(0);

        NumberFormat format = NumberFormat.getIntegerInstance();
        UnaryOperator<TextFormatter.Change> filter = c -> {
            if (c.isContentChange()) {
                ParsePosition parsePosition = new ParsePosition(0);
                // NumberFormat evaluates the beginning of the text
                format.parse(c.getControlNewText(), parsePosition);
                if (parsePosition.getIndex() == 0 ||
                        parsePosition.getIndex() < c.getControlNewText().length()) {
                    // reject parsing the complete text failed
                    return null;
                }
            }
            return c;
        };
        TextFormatter<Integer> queueFormatter = new TextFormatter<>(
                new IntegerStringConverter(), 30, filter);
        SpinnerValueFactory<Integer> valueFac = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 360, 30);
        //valueFac.setValue(30);
        queueWait.setValueFactory(valueFac);
        queueWait.getEditor().setTextFormatter(queueFormatter);

    }

    // TODO: Complete this configuration saving/loading procedure. Also, implement buttons for these features
    protected void onLoad() {
        //blah blah QConfiguration loading
    }

    protected void onSave() {

    }

    @FXML
    protected void onTestRunClick() throws URISyntaxException {
        String cwd = new File(TQMain.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent();
        // Check if user wants to proceed with testing all PuTTY sessions. Continue if yes, cancel if no
        if (!TQMain.showConfirmation("Are you sure you want to test the connection for all PuTTY sessions? This could take a while")) {
            return;
        }
        StringBuilder failedConns = new StringBuilder("\"Utility Name\",\"Connection String\"\n");
        AtomicInteger failCount = new AtomicInteger();
        AtomicInteger index = new AtomicInteger();
        final int sesSize = TQMain.SESSIONS.size();
        // Create and run thread to test each connection so we don't hang the process
        Task<Boolean> checkUtil = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                TQMain.SESSIONS.forEach((k, v) -> { //k=Utility Name, v=IP:Port
                    // Increment progress bar index
                    float fir = System.currentTimeMillis();
                    index.getAndIncrement();

                    String[] conn = v.split(":");
                    Telnet tel = new Telnet(conn[0], Integer.parseInt(conn[1]));
                    if (tel.init(2000)) { // 2000ms Timeout Period
                        // Connection successful (Exclude working utilities)
                        TQMain.LOGGER.debug("1," + k + "," + v + "\n");
                        try {
                            tel.close();
                        } catch (IOException ignored) {}
                    } else {
                        // Connection failed
                        // "Utility Name","Connection String"
                        failedConns.append("\"").append(k).append("\",\"").append(v).append("\"\n");
                        failCount.getAndIncrement();
                        TQMain.LOGGER.debug("0," + k + "," + v + "\n");
                    }
                    // Update progress bar
                    float progress = (float) index.get() / sesSize;
                    int timeRemaining = (int )Math.abs((sesSize*.15)-failCount.get()) * 2; // Assume 15% of utilities are non working, each of which has a 2 second timeout
                    final String finTime = timeRemaining > 60 ? (timeRemaining/60) + " minutes " + (timeRemaining % 60) + " seconds" : timeRemaining + " seconds";
                    progressBar.setProgress(progress);
                    // Update progress text and time remaining
                    Platform.runLater(()->{
                        progressLabel.setText("Progress: " + Math.round(progress * 100) + "%");
                        timeLabel.setText(String.format("Est Time Remaining: %s", finTime));
                    });
                });
                return true;
            }
        };

        checkUtil.setOnSucceeded(e -> {
            // Erase progress bar before showing final messages
            progressBar.setProgress(0);
            Platform.runLater(()->{
                progressLabel.setText("");
                timeLabel.setText("");
            });


            // Write all failed connections out to a file located inside the running directory
            File outputFile = new File(cwd + File.separator + "FailedConnections.csv");
            FileHelper.appendFile(outputFile, failedConns.toString());

            if (TQMain.showConfirmation("Found " + failCount.get() + " failed connections.\nWould you like to view the status file?")) {
                boolean succ = false;
                if(Desktop.isDesktopSupported()) {
                    try {
                        Desktop.getDesktop().edit(outputFile);
                        succ = true;
                    } catch (IOException ignored) {}
                }
                // If unable to open the file successfully, then display unable to open and list the file location
                if(!succ) {
                    TQMain.showMessage("Unable to open status file, located at " + outputFile.getAbsolutePath() + ".");
                }
            } else { // If they don't want to view the status file, print out the location of the file
                TQMain.showMessage("File has been saved to " + outputFile.getAbsolutePath() + ".");
            }
        });

        new Thread(checkUtil).start();
    }

    @FXML
    protected void onLogView() {
        try {
            //consAp = new AnchorPane(FXMLLoader.load(TQMain.class.getResource("tq-tab.fxml")));
            Stage stg = new Stage();
            AnchorPane ap = FXMLLoader.load(TQMain.class.getResource("tq-logview.fxml"));
            stg.setScene(new Scene(ap));
            stg.show();
            /*stg.setOnShown((ev) -> {
                //TQLogViewController.setText();
            });*/
            stg.setOnCloseRequest((ev) -> {
                LogViewAppender.closeArea();
            });
        } catch (IOException e) {
            TQMain.throwError("Error while displaying log view.", e);
        }

    }

    @FXML
    protected void onClientAction() throws IOException {
        try {
            String[] conn = TQMain.SESSIONS.get(clientList.getValue()).split(":");
            Telnet tel = new Telnet(conn[0], Integer.parseInt(conn[1]));
            String fullUtilConnName = clientList.getValue() + " (" + conn[0] + ":" + conn[1] + ")";
            if (!tel.init()) { // TODO: Handle error message better maybe
                throw new ConnectException(fullUtilConnName);
            }

            ObservableList<String> gws = FXCollections.observableList(tel.readGateways());
            for (Iterator<String> it = gws.iterator(); it.hasNext(); ) {
                String gw = it.next();
                if (gw.endsWith("(OFFLINE)") || gw.endsWith("NOT CONNECTED")) {
                    TQMain.LOGGER.warn("Removing offline collector from gwList: " + gw + " @ " + fullUtilConnName);
                    it.remove();
                }
            }
            if (!gws.isEmpty()) {
                gws.add("All");
            } else {
                TQMain.throwError("No gateways found for utility!\nPlease check gateway connections through PuTTY on the following utility: " + fullUtilConnName + "\nIt's possible all gateways are down, or this utility doesn't have a gateway setup yet.", null);
            }
            gwList.setItems(gws);
            tel.close();
        } catch (ConnectException ex) {
            TQMain.throwError("Error while connecting to utility, please verify the connection is still valid.\n" + ex.getMessage(), ex);
        }
    }

    @FXML
    protected void onChooseFileClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Command List");

        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            chosenFile.setText(selectedFile.getAbsolutePath());
            this.miuFile = selectedFile;
        }
    }

    @FXML
    protected void onExecuteClick() {
        // Allow execution without an MIU CSV List
        miuList = new ArrayList<>();
        if (this.miuFile != null) {
            TQMain.LOGGER.debug("Using File: " + chosenFile.getText());
            String miuContents = FileHelper.readFile(this.miuFile);
            // Allow both newline and comma csv delimiter
            if (miuContents != null) {
                if (miuContents.contains(",")) {
                    miuList.addAll(Arrays.asList(miuContents.split(",")));
                } else {
                    miuList.addAll(Arrays.asList(miuContents.split("\n")));
                }
            }
            TQMain.LOGGER.debug("File Contents:\n" + miuList.toString());
        } else if (!TQMain.showConfirmation("Are you sure you want to run without an MIU list")) {
            return;
        }

        // Check Command List
        if (commandList.getText().isEmpty()) {
            TQMain.throwError("No command was entered!", new IllegalArgumentException());
            return;
        } else {
            TQMain.LOGGER.debug("Command List: " + commandList.getText());
        }

        // Queue Wait Logging / Initialization
        boolean WAIT_FOR_RESPONSE = ((RadioButton) waitMethod.getSelectedToggle()).getText().startsWith("Response");
        TQMain.LOGGER.debug("Wait Method: " + ((RadioButton) waitMethod.getSelectedToggle()).getText());
        if (!WAIT_FOR_RESPONSE) {
            TQMain.LOGGER.debug("Queue Wait: " + queueWait.getValue());
        }

        if (gwList.getValue().isEmpty()) {
            TQMain.throwError("Please select a gateway", new IllegalArgumentException());
            return;
        }

        TQMain.LOGGER.info(String.format("Starting connection to %s(%s) @ %s\n", clientList.getValue(), TQMain.SESSIONS.get(clientList.getValue()), gwList.getValue()));


        // Initialize Telnet instance
        String[] assoc = TQMain.SESSIONS.get(clientList.getValue()).split(":");
        Telnet tel = new Telnet(assoc[0], Integer.parseInt(assoc[1]));

        tel.init();

        // Set time variable so we can see how long a process takes.
        final double[] time = new double[1];


        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    // Start process timer
                    time[0] = System.currentTimeMillis();

                    List<String> noResponses = new ArrayList<>();

                    // Select Gateway
                    String[] split = commandList.getText().split("\n");
                    int gwMax = (gwList.getValue().equals("All") ? gwList.getItems().size() - 1/*Exclude GW "-1" */ : 1);
                    int miuMax = (miuList.size() == 0 ? 1 : miuList.size());
                    int commMax = split.length;
                    int totalIterations = commMax * miuMax * gwMax;

                    // Estimated Time remaining in seconds, mainly applies to Timed wait method
                    int timeRemaining = (queueWait.getValue()) * totalIterations;

                    // Iterate through all Gateways
                    for (int z = 0; z < gwMax; z++) {
                        if(!TQMain.ACTIVE) return null;
                        // Select the proper gateway, if GW is set to -1 then TQ will iterate through each gateway under a specified utility
                        String currentGw = (gwList.getValue().equals("All")) ? gwList.getItems().get(z) : gwList.getValue();
                        TQMain.LOGGER.info("Selecting Gateway: " + currentGw);
                        tel.sendCommand("c select " + currentGw);

                        // Iterate through each MIU listed in file, unless miuList is 0 then execute command once per gateway
                        for (int j = 0; j < miuMax; j++) {
                            if(!TQMain.ACTIVE) return null;
                            // Iterate through all commands, and replace $1 with the respective MIU number if applicable
                            for (int i = 0; i < commMax; i++) {
                                String command = split[i];
                                command = command.contains("$1") ? command.replaceAll("\\$1", miuList.get(j)) : command;
                                TQMain.LOGGER.info(String.format(
                                        "Sending Command(GW: %s%s): %s",
                                        currentGw, (miuMax != 1) ? ", MIU: " + miuList.get(j) : "", command
                                ));
                                // Progress Bar Math
                                double progress = ((i + 1D) * (j + 1D) * (z + 1D)) / totalIterations;
                                progressBar.setProgress(progress);
                                timeRemaining = timeRemaining - queueWait.getValue();
                                int finalTimeRemaining = timeRemaining;
                                TQMain.LOGGER.info(String.format("Progress: %.2f", progress * 100) + "%");
                                TQMain.LOGGER.info(String.format("Time Remaining: %d minutes", timeRemaining));
                                Platform.runLater(() -> {
                                    progressLabel.setText("Progress: " + Math.round(progress * 100) + "%");
                                    timeLabel.setText("Time Remaining: " + finalTimeRemaining + " minutes");
                                });
                                String targetMiu = command.split(" ")[1];
                                if (WAIT_FOR_RESPONSE && !targetMiu.equals("-1")) {
                                    TQMain.LOGGER.info("Executing command and waiting for response");
                                    String term = !miuList.isEmpty() ? miuList.get(j) : (command.startsWith("rexec") ? command.split(" ")[1] : "\n");
                                    String resp = tel.sendMIUCommand(command, term, COMMAND_TIMEOUT);
                                    if (resp.equals("Timeout")) {
                                        noResponses.add(term);
                                        TQMain.LOGGER.warn(String.format("Unable to get response from %s after %d minutes", term, COMMAND_TIMEOUT / 1000 / 60));
                                        Platform.runLater(() -> {
                                            TQMain.showMessage(String.format("Unable to get response from %s after %d minutes", term, COMMAND_TIMEOUT / 1000 / 60));
                                        });
                                    } else {
                                        TQMain.LOGGER.debug("Response Received: " + resp);
                                    }
                                } else {
                                    tel.sendCommand(command);
                                    // TODO: Prevent sleep after last command is issued
                                    TQMain.LOGGER.info("Performing sleep for " + queueWait.getValue() * WAIT_MULTIPLIER + "ms");
                                    //sleeper.wait((targetMiu.equals("-1")) ? 0 : (long) queueWait.getValue() * WAIT_MULTIPLIER);
                                    Thread.sleep((targetMiu.equals("-1")) ? 0 : (long) queueWait.getValue() * WAIT_MULTIPLIER);
                                }
                            }
                        }

                        Platform.runLater(() -> {
                            progressLabel.setText("Progress:");
                            timeLabel.setText("");
                        });
                    }
                    TQMain.LOGGER.info("Closing Connection for " + clientList.getValue());
                    tel.close();
                    if (!noResponses.isEmpty()) {
                        TQMain.LOGGER.warn("Timeout Nodes: " + Arrays.toString(noResponses.toArray()));
                        File jarPath = new File(System.getProperty("user.dir") + File.separator + "unresponsiveNodes.csv");
                        TQMain.LOGGER.info("Logged timeout nodes to " + jarPath.getAbsolutePath());
                        StringBuilder append = new StringBuilder();
                        for (int i = 0; i < noResponses.size(); i++) {
                            append.append(noResponses.get(i));
                            if (i != noResponses.size() - 1) {
                                append.append(",");
                            }
                        }
                        FileHelper.appendFile(jarPath, append.toString());
                    }
                } catch (IOException ex) {
                    TQMain.throwError("Error while closing connection", ex);
                } catch (InterruptedException ex) {
                    TQMain.throwError("Error while waiting in between commands", ex);
                }
                return null;
            }
        };


        sleeper.setOnSucceeded(wse -> {
            double timeSec = (System.currentTimeMillis() - time[0]) / 1000F;
            TQMain.showMessage(String.format("Successfully executed commands. Process took %.2f seconds", timeSec));
        });
        // TODO: Add Pause, Resume, Stop functionality
        Thread sleeperThread = new Thread(sleeper);
        TQMain.QUEUES.add(sleeperThread);
        sleeperThread.start();
    }
}