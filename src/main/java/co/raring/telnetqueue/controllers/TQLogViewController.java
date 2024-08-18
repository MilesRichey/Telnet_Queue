package co.raring.telnetqueue.controllers;

//import co.raring.telnetqueue.tool.LogViewAppender;
import co.raring.telnetqueue.tool.LogStringCell;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.*;
import java.net.URL;
import java.util.ResourceBundle;

public class TQLogViewController implements Initializable {

    @FXML
    private ListView<String> listViewLog;

    @FXML
    private ToggleButton toggleButtonAutoScroll;

    @FXML
    private ChoiceBox<Level> choiceBoxLogLevel;

    @FXML
    void handleRemoveSelected() {
        listViewLog.getItems().removeAll(listViewLog.getSelectionModel().getSelectedItems());
    }

    @FXML
    void handleClearLog() {
        listViewLog.getItems().clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        listViewLog.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration loggerConfiguration = loggerContext.getConfiguration();
        LoggerConfig loggerConfig = loggerConfiguration.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

        for (Level level : Level.values()) {
            choiceBoxLogLevel.getItems().add(level);
        }

        choiceBoxLogLevel.getSelectionModel().select(loggerConfig.getLevel());
        choiceBoxLogLevel.getSelectionModel().selectedItemProperty().addListener((arg0, oldLevel, newLevel) -> {
            loggerConfig.setLevel(newLevel);
            loggerContext.updateLoggers();
        });

        listViewLog.setCellFactory(listView -> new LogStringCell());

        PipedOutputStream pOut = new PipedOutputStream();
        System.setOut(new PrintStream(pOut));
        PipedInputStream pIn = null;
        try {
            pIn = new PipedInputStream(pOut);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(pIn));

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                while (!isCancelled()) {
                    try {
                        String line = reader.readLine();
                        if (line != null) {
                            Platform.runLater(() -> {
                                listViewLog.getItems().add(line);

                                /* Auto-Scroll + Select */
                                if (toggleButtonAutoScroll.selectedProperty().get()) {
                                    listViewLog.scrollTo(listViewLog.getItems().size() - 1);
                                    listViewLog.getSelectionModel().select(listViewLog.getItems().size() - 1);
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
