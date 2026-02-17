package com.pda.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("layout.fxml"));
        Parent root = loader.load();
        scene = new Scene(root, 640, 480);
        stage.setScene(scene);
        stage.setTitle("PDA JavaFX Client");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
