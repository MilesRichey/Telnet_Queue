package co.raring.telnetqueue.controllers;

import co.raring.telnetqueue.TQMain;
import co.raring.telnetqueue.tool.LogViewAppender;
import co.raring.telnetqueue.tool.Telnet;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.function.UnaryOperator;

public class TabController implements Initializable {
    // Debug Options
    //private static final boolean QUERY = true; // TRUE=Query PuTTY Registry,FALSE=Use predefined utility configurations
    private static final int WAIT_MULTIPLIER = 60000; // Multiplier for Queue Wait, 60000 = 1 minute
    private static final long COMMAND_TIMEOUT = (1000 * 60) * 120; // Command timeout in MS, 1000*60*120=2 hours

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set Top Logo to Zenner
        zennerLogo.setImage(new Image(TQMain.class.getResourceAsStream("zlogo.png")));

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

    @FXML
    protected void onLogView() {
        AnchorPane consAp = null;
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
            e.printStackTrace();
        }

    }

    @FXML
    protected void onClientAction() throws IOException {
        String[] conn = TQMain.SESSIONS.get(clientList.getValue()).split(":");
        Telnet tel = new Telnet(conn[0], Integer.parseInt(conn[1]));
        tel.init();
        ObservableList<String> gws = FXCollections.observableList(tel.readGateways());
        for (Iterator<String> it = gws.iterator(); it.hasNext(); ) {
            String gw = it.next();
            if (gw.endsWith("(OFFLINE)")) {
                TQMain.LOGGER.warn("Removing offline collector from gwList: " + gw + " @ " + clientList.getValue());
                it.remove();
            }
        }
        gws.add("All");
        gwList.setItems(gws);
        tel.close();
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
            String miuContents = readFile(this.miuFile);
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

        TQMain.LOGGER.info(String.format("Starting connection to %s(%s) @ %s\n", clientList.getValue(), TQMain.SESSIONS.get(clientList.getValue()), gwList.getValue()));


        // Initialize Telnet instance
        String[] assoc = TQMain.SESSIONS.get(clientList.getValue()).split(":");
        Telnet tel = new Telnet(assoc[0], Integer.parseInt(assoc[1]));
        tel.init();

        // Set time variable so we can see how long a process takes.
        final double[] time = new double[1];


        Task<Void> sleeper = new Task<>() {
            @Override
            protected Void call() {
                try {
                    // Start process timer
                    time[0] = System.currentTimeMillis();


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
                        // Select the proper gateway, if GW is set to -1 then TQ will iterate through each gateway under a specified utility
                        String currentGw = (gwList.getValue().equals("All")) ? gwList.getItems().get(z) : gwList.getValue();
                        TQMain.LOGGER.info("Selecting Gateway: " + currentGw);
                        tel.sendCommand("c select " + currentGw);

                        // Iterate through each MIU listed in file, unless miuList is 0 then execute command once per gateway
                        for (int j = 0; j < miuMax; j++) {

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
                                if (WAIT_FOR_RESPONSE) {
                                    TQMain.LOGGER.info("Executing command and waiting for response");
                                    if (miuList.isEmpty()) {
                                        if (command.startsWith("rexec")) {
                                            TQMain.LOGGER.debug("Response Received: " + tel.sendMIUCommand(command, command.split(" ")[1], COMMAND_TIMEOUT));
                                        } else
                                            TQMain.LOGGER.debug("Response Received: " + tel.sendMIUCommand(command, "\n", COMMAND_TIMEOUT));
                                    } else tel.sendMIUCommand(command, miuList.get(j), COMMAND_TIMEOUT);
                                } else {
                                    tel.sendCommand(command);
                                    // TODO: Prevent sleep after last command is issued
                                    TQMain.LOGGER.info("Performing sleep for " + queueWait.getValue() * WAIT_MULTIPLIER + "ms");
                                    Thread.sleep((long) queueWait.getValue() * WAIT_MULTIPLIER);
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
            TQMain.LOGGER.info(String.format("Successfully executed commands. Process took %.2f seconds", timeSec));
            TQMain.showMessage(String.format("Successfully executed commands.\nProcess took %.2f seconds", timeSec));
        });
        // TODO: Add Pause, Resume, Stop functionality
        Thread sleeperThread = new Thread(sleeper);
        TQMain.QUEUES.add(sleeperThread);
        sleeperThread.start();
    }

    /**
     * Method to read a files content and show an error message if it's not possible
     *
     * @param file File to read data from
     * @return The contents of @file, or null if an exception is caught
     */
    // Non-FXML Methods
    private String readFile(File file) {
        StringBuilder fileContents = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String buffer;
            while ((buffer = br.readLine()) != null) {
                fileContents.append(buffer);
                fileContents.append("\n");
            }
        } catch (IOException e) {
            TQMain.throwError("Unable to access file '" + chosenFile.getText() + "'", e);
            return null;
        }
        return fileContents.toString();
    }
}