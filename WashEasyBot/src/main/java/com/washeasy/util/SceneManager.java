package com.washeasy.util;

import com.washeasy.controller.AdminDashboardController;
import com.washeasy.controller.UserDashboardController;
import com.washeasy.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;


public class SceneManager {
    public static void switchScene(Pane currentPane, String fxmlPath, String title, User user)
            throws Exception {

        FXMLLoader loader = new FXMLLoader(
            SceneManager.class.getResource(fxmlPath)
        );
        Parent root = loader.load();


        if (user != null) {
            Object ctrl = loader.getController();
            if (ctrl instanceof AdminDashboardController adc) {
                adc.setUser(user);
            } else if (ctrl instanceof UserDashboardController udc) {
                udc.setUser(user);
            }
        }

        Stage stage = (Stage) currentPane.getScene().getWindow();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            SceneManager.class.getResource("/com/washeasy/css/style.css").toExternalForm()
        );

        stage.setTitle(title);
        stage.setScene(scene);
        stage.centerOnScreen();
    }
}
