module com.example.exchange {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.jsoup;
    requires java.sql;


    opens com.example.exchange to javafx.fxml;
    exports com.example.exchange;
}