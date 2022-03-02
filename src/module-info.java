module co.raring.telnetqueue {
    requires javafx.controls;
    requires javafx.fxml;

    opens co.raring.telnetqueue to javafx.fxml;
    exports co.raring.telnetqueue;
}