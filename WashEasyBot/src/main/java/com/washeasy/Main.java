package com.washeasy;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/washeasy/fxml/Login.fxml")
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, 860, 520);
        scene.getStylesheets().add(
            getClass().getResource("/com/washeasy/css/style.css").toExternalForm()
        );

        primaryStage.setTitle("WashEasy Bot — Laundry Chatbot");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
