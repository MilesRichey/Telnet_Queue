package co.raring.telnetqueue;

import co.raring.telnetqueue.jna.JNAReg;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TQMain extends Application {
    public static final Logger LOGGER = LogManager.getLogger(TQMain.class);
    // Debug Options
    private static final boolean QUERY = true; // TRUE=Query PuTTY Registry,FALSE=Use predefined utility configurations
    public static boolean ACTIVE = true; // Active status, if set to false then all running threads(queues) will stop
    public static Map<String, String> SESSIONS;
    public static List<Thread> QUEUES = new ArrayList<>();


    public static void main(String[] args) {
        if (!QUERY || !System.getProperty("os.name").contains("Windows")) {
            // Predefined Utility configuration for testing purposes
            SESSIONS = Map.of(
                    //"Addison Training", "10.1.9.10:3221",
                    "ZennerTest01", "10.1.9.32:3500");
        } else {
            SESSIONS = JNAReg.getSessions();
        }

        launch();
    }

    /**
     * Method to display a JavaFX error popup dialog with a message, and optional accompanying exception.
     *
     * @param content Message to be displayed on the popup
     * @param ex      Optional exception to be included in error message
     */
    public static void throwError(String content, Exception ex) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        LOGGER.error("Showing error dialog(\"{}\")", content, ex);
        errorAlert.setContentText(content);
        errorAlert.showAndWait();
    }

    /**
     * Method to display a JavaFX informational popup dialog with a message
     *
     * @param content Message to be displayed on the informational popup
     */
    public static void showMessage(String content) {
        Alert msgBox = new Alert(Alert.AlertType.INFORMATION);
        LOGGER.debug("Showing informational dialog(\"{}\")", content);
        //errorAlert.setHeaderText("File error");
        msgBox.setContentText(content);
        msgBox.showAndWait();
    }

    /**
     * Method to display a JavaFX confirmation popup dialog with a message, and returns true if the user selects Yes.
     *
     * @param content Message to be displayed on the confirmation popup
     * @return result of popup alert
     */
    public static boolean showConfirmation(String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
        LOGGER.debug("Showing confirmation dialog(\"{}\")", content);
        alert.showAndWait();
        return alert.getResult() == ButtonType.YES;
    }

    @Override
    public void start(Stage stage) throws IOException {
        LOGGER.debug("Initializing TelnetQueue GUI");
        FXMLLoader fxmlLoader = new FXMLLoader(TQMain.class.getResource("tq-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), stage.getWidth(), stage.getHeight());
        //
        stage.setTitle("Telnet Queue v1.0.8b");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(Objects.requireNonNull(TQMain.class.getResourceAsStream("zenner.png"))));
        stage.setOnCloseRequest((event) -> {
            for (Thread th : QUEUES) { //TODO: Add actual thread stops
                th.interrupt();
                //TQMain.ACTIVE = false;
            }
        });
        stage.show();
        LOGGER.debug("Finished initializing TelnetQueue GUI");
    }
}