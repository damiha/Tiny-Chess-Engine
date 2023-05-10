module com.example.chessengine {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.chessengine to javafx.fxml;
    exports com.example.chessengine;
}