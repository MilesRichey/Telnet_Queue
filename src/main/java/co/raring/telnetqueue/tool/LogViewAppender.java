package co.raring.telnetqueue.tool;

import javafx.scene.control.TextArea;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggingEvent;

public class LogViewAppender extends WriterAppender {
    private static TextArea TEXT_AREA = null;

    @Override
    public void append(LoggingEvent ev) {
        if(TEXT_AREA != null) {
            final String logMessage = this.layout.format(ev);
            TEXT_AREA.setText(TEXT_AREA.getText() + logMessage);
        }
    }
    public static void setTextArea(TextArea textArea) {
        TEXT_AREA = textArea;
    }
    public static void closeArea() {
        TEXT_AREA = null;
    }
}
