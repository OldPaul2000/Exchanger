package com.example.exchange;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ExchangeApp extends Application {

    ArchivesDatabase archivesDatabase = ArchivesDatabase.getInstance();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ExchangeApp.class.getResource("BNRPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1320, 680);
        stage.setTitle("BNRExchange!");
        stage.setOnCloseRequest(event -> {
            archivesDatabase.closeDatabase();
        });
        stage.setResizable(false);

//        stage.widthProperty().addListener((observableVal, oldVal, newVal) -> {
//            System.out.println("Width:" + newVal);
//        });
//        stage.heightProperty().addListener((observableVal, oldVal, newVal) -> {
//            System.out.println("Height:" + newVal);
//        });
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}