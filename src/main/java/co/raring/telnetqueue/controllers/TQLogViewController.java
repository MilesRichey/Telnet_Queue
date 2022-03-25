package co.raring.telnetqueue.controllers;

import co.raring.telnetqueue.TQMain;
import co.raring.telnetqueue.tool.LogViewAppender;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

import java.net.URL;
import java.util.ResourceBundle;

public class TQLogViewController implements Initializable {
    @FXML
    private TextArea logBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        LogViewAppender.setTextArea(logBox);
        //logBox.setText("Work in Progress...");
    }
}
