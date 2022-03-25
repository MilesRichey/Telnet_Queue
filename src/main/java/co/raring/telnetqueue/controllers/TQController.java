package co.raring.telnetqueue.controllers;

import co.raring.telnetqueue.TQMain;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TQController implements Initializable {
    @FXML
    private TabPane tabPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        newQueueTab();
        // Listen for Tab change, check if the new tab selected is "New Queue" then handle accordingly
        tabPane.getSelectionModel().selectedItemProperty().addListener((ov, t1, t2) -> {
            TQMain.LOGGER.trace(String.format("Tab Selection from %s to %s", t1.getText(), t2.getText()));
            if (t2.getText().equals("New Queue")) {
                tabPane.getSelectionModel().select(newQueueTab());
            }
        });
        tabPane.getSelectionModel().select(0);
    }

    private int newQueueTab() {
        Tab qt = new Tab();
        int index = tabPane.getTabs().size() - 1;
        try {
            qt.setContent(FXMLLoader.load(TQMain.class.getResource("tq-tab.fxml")));
        } catch (IOException e) {
            TQMain.throwError("Error while loading Queue Tab template", e);
            TQMain.LOGGER.fatal("Error while loading Queue Tab template", e);
            index = -1;
        }
        qt.setText("Queue #" + (tabPane.getTabs().size()));
        tabPane.getTabs().add(index, qt);
        return index;
    }
}