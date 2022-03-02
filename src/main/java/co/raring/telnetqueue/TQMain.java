package co.raring.telnetqueue;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;

public class TQMain extends Application {
    public static Logger LOGGER;

    public static void main(String[] args) {
        LOGGER = Logger.getLogger(TQMain.class);
        //LOGGER.addAppender(new ConsoleAppender());
        LOGGER.setLevel(Level.DEBUG);
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        LOGGER.debug("Initializing TelnetQueue GUI");
        FXMLLoader fxmlLoader = new FXMLLoader(TQMain.class.getResource("tq-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), stage.getWidth(), stage.getHeight());
        stage.setTitle("Telnet Queue");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.getIcons().add(new Image(TQMain.class.getResourceAsStream("zenner.png")));
        stage.show();
        LOGGER.debug("Finished initializing TelnetQueue GUI");
    }

    /**
     * Method to display a JavaFX error popup dialog with a message, and optional accompanying exception.
     *
     * @param content Message to be displayed on the popup
     * @param ex      Optional exception to be included in error message
     */
    public static void throwError(String content, Exception ex) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        LOGGER.error("Showing error dialog(\"" + content + "\")", ex);
        errorAlert.setContentText(content);
        errorAlert.showAndWait();
    }

    /**
     * Method to display a JavaFX informational popup dialog with a message
     *
     * @param content Message to be displayed on the informational popup
     */
    public static void showMessage(String content) {
        Alert errorAlert = new Alert(Alert.AlertType.INFORMATION);
        LOGGER.debug("Showing informational dialog(\"" + content + "\")");
        //errorAlert.setHeaderText("File error");
        errorAlert.setContentText(content);
        errorAlert.showAndWait();
    }

    /**
     * Method to display a JavaFX confirmation popup dialog with a message, and returns true if the user selects Yes.
     *
     * @param content Message to be displayed on the confirmation popup
     * @return result of popup alert
     */
    public static boolean showConfirmation(String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content, ButtonType.YES, ButtonType.NO);
        LOGGER.debug("Showing confirmation dialog(\"" + content + "\")");
        alert.showAndWait();

        return alert.getResult() == ButtonType.YES;
    }
}