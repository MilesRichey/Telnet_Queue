package co.raring.telnetqueue;

import co.raring.telnetqueue.jna.JNAReg;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class TQController implements Initializable {
    // Debug Options
    private static final boolean QUERY = true; // TRUE=Query PuTTY Registry,FALSE=Use predefined utility configurations
    private static final int WAIT_MULTIPLIER = 60000; // Multiplier for Queue Wait, 60000 = 1 minute

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
    private ChoiceBox<Integer> gwList;
    @FXML
    private Label progressLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private ImageView zennerLogo;

    private Map<String, String> sessions;
    private File miuFile;
    private List<String> miuList;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Set Top Logo to Zenner
        zennerLogo.setImage(new Image(this.getClass().getResourceAsStream("zlogo.png")));

        if (QUERY) {
            sessions = JNAReg.getSessions();
            clientList.setItems(FXCollections.observableList(new ArrayList<>(sessions.keySet())));
        } else {
            // Predefined Utility configuration for testing purposes
            sessions = Map.of("Addison Training", "10.1.9.10:3221", "ZennerTest01", "10.1.9.32:3500");
            clientList.setItems(FXCollections.observableList(List.of("Addison Training", "ZennerTest01")));
        }
        clientList.getSelectionModel().select(0);
    }

    @FXML
    protected void onClientAction() throws IOException {
        String[] conn = sessions.get(clientList.getValue()).split(":");
        Telnet tel = new Telnet(conn[0], Integer.parseInt(conn[1]));
        tel.init();
        ObservableList<Integer> gws = FXCollections.observableList(tel.readGateways());
        gws.add(-1);
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

        miuList = new ArrayList<>();
        // Allow execution without an MIU CSV List
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
            miuList.addAll(Arrays.asList(Objects.requireNonNull(readFile(this.miuFile)).split(",")));
            TQMain.LOGGER.debug("File Contents:\n" + miuList.toString());
        } else if (!TQMain.showConfirmation("Are you sure you want to run without an MIU list")) {
            return;
        }

        if (commandList.getText().isEmpty()) {
            TQMain.throwError("No command was entered!", new IllegalArgumentException());
            return;
        } else {
            TQMain.LOGGER.debug("Command List: " + commandList.getText());
        }

        TQMain.LOGGER.debug("Queue Wait: " + queueWait.getValue());

        TQMain.LOGGER.info(String.format("Starting connection to %s(%s) @ %s\n", clientList.getValue(), sessions.get(clientList.getValue()), gwList.getValue()));

        String[] assoc = sessions.get(clientList.getValue()).split(":");
        Telnet tel = new Telnet(assoc[0], Integer.parseInt(assoc[1]));
        tel.init();
        final double[] time = new double[1];


        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() {
                try {
                    time[0] = System.currentTimeMillis();
                    // Select Gateway
                    String[] split = commandList.getText().split("\n");
                    int zMax = (gwList.getValue() == -1 ? gwList.getItems().size() - 1/*Exclude GW "-1" */ : 1);
                    int jMax = (miuList.size() == 0 ? 1 : miuList.size());
                    int iMax = split.length;
                    int totalIterations = iMax * jMax * zMax;
                    // Estimated Time remaining in seconds
                    int timeRemaining = (queueWait.getValue()) * totalIterations;

                    // Iterate through all Gateways
                    for (int z = 0; z < zMax; z++) {
                        int currentGw;
                        // Select the proper gateway, if GW is set to -1 then TQ will iterate through each gateway under a specified utility
                        if (gwList.getValue() == -1) {
                            currentGw = gwList.getItems().get(z);
                            TQMain.LOGGER.info("Selecting Gateway: " + currentGw);
                            tel.sendCommandNR("c select " + currentGw);
                        } else {
                            currentGw = gwList.getValue();
                            TQMain.LOGGER.info("Selecting Gateway: " + currentGw);
                            tel.sendCommandNR("c select " + currentGw);
                        }

                        // Iterate through each MIU listed in file, unless miuList is 0 then execute command once per gateway
                        for (int j = 0; j < jMax; j++) {
                            // Iterate through all commands, and replace $1 with the respective MIU number
                            for (int i = 0; i < iMax; i++) {
                                String command = split[i];
                                command = command.contains("$1") ? command.replaceAll("\\$1", miuList.get(j)) : command;
                                TQMain.LOGGER.info(String.format(
                                        "Sending Command(GW: %d%s): %s",
                                        currentGw, (jMax != 1) ? ", MIU: " + miuList.get(j) : "", command
                                ));
                                tel.sendCommandNR(command);
                                // Progress Bar Math
                                double progress = ((i + 1D) * (j + 1D) * (z + 1D)) / totalIterations;
                                progressBar.setProgress(progress);
                                timeRemaining = timeRemaining - queueWait.getValue();
                                int finalTimeRemaining = timeRemaining;
                                TQMain.LOGGER.info(String.format("Progress: %.2f", progress*100) + "%");
                                TQMain.LOGGER.info(String.format("Time Remaining: %d minutes", timeRemaining));
                                Platform.runLater(() -> {
                                    progressLabel.setText("Progress: " + Math.round(progress * 100) + "%");
                                    timeLabel.setText("Time Remaining: " + finalTimeRemaining + " minutes");
                                });

                                // TODO: Prevent sleep after last command is issued
                                TQMain.LOGGER.info("Performing sleep for " + queueWait.getValue()*WAIT_MULTIPLIER + "ms");
                                Thread.sleep(queueWait.getValue() * WAIT_MULTIPLIER);
                            }
                        }
                        TQMain.LOGGER.debug("Closing Connection for " + clientList.getValue());
                        tel.close();
                        Platform.runLater(() -> {
                            progressLabel.setText("Progress:");
                            timeLabel.setText("");
                        });
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
            TQMain.LOGGER.info(String.format("Successfully executed commands.\nProcess took %.2f seconds", timeSec));
            TQMain.showMessage(String.format("Successfully executed commands.\nProcess took %.2f seconds", timeSec));
        });
        new Thread(sleeper).start();
        sleeper.run();
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
            }
        } catch (IOException e) {
            TQMain.throwError("Unable to access file '" + chosenFile.getText() + "'", e);
            return null;
        }
        return fileContents.toString();
    }
}