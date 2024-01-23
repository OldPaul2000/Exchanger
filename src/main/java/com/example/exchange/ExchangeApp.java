package com.example.exchange;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class ExchangeApp extends Application {

    ArchivesDatabase archivesDatabase = ArchivesDatabase.getInstance();
    public static Stage stage = null;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(ExchangeApp.class.getResource("BNRPage.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1320, 680);
        stage.setOnCloseRequest(event -> {
            archivesDatabase.closeDatabase();
        });
        stage.setResizable(true);
        stage.setScene(scene);

        Path localPath = FileSystems.getDefault().getPath("C:\\Users\\paulb\\IntellijProjects\\Exchange\\src").toAbsolutePath();
        Image windowIcon = new Image(localPath + File.separator + "currency_icon.png");
        stage.getIcons().add(windowIcon);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
        this.stage = stage;
    }

    public static void main(String[] args) {
        launch();
    }
}