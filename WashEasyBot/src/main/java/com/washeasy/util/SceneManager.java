package com.washeasy.util;

import com.washeasy.controller.AdminDashboardController;
import com.washeasy.controller.UserDashboardController;
import com.washeasy.model.User;
import javafx.application.Platform;
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
        boolean wasMaximized = stage.isMaximized();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                SceneManager.class.getResource("/com/washeasy/css/style.css").toExternalForm()
        );

        stage.setTitle(title);
        stage.setResizable(true);
        stage.setMinWidth(860);
        stage.setMinHeight(520);

        // Paksa JavaFX keluar dari state maximized sebelum ganti scene,
        // supaya saat di-set true lagi, JavaFX mendeteksi perubahan state
        if (wasMaximized) {
            stage.setMaximized(false);
        }

        stage.setScene(scene);

        if (wasMaximized) {
            // Double runLater: pertama setelah scene di-set, kedua setelah layout pass selesai
            Platform.runLater(() -> Platform.runLater(() -> stage.setMaximized(true)));
        } else {
            stage.centerOnScreen();
        }
    }
}
