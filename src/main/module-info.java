module co.raring.telnetqueue {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop; // For java.awt
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires static java.compiler;

    opens co.raring.telnetqueue to javafx.fxml;

    exports co.raring.telnetqueue;
}