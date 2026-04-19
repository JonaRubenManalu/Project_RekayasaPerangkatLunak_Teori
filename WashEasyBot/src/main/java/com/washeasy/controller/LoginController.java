package com.washeasy.controller;

import com.washeasy.model.User;
import com.washeasy.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

/**
 * LoginController — menangani logika halaman Login.fxml
 * Dikontrol via SceneBuilder: tombol Login terhubung ke handleLogin().
 */
public class LoginController {

    // ── FXML Injections ──────────────────────────────────────────
    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label         lblError;
    @FXML private Button        btnLogin;
    @FXML private Pane          rootPane;

    private final AdminPanel adminPanel = new AdminPanel();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        // Enter key di field password langsung login
        txtPassword.setOnAction(e -> handleLogin());
    }

    /** Dipanggil saat tombol Login ditekan */
    @FXML
    public void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();

        // Validasi input kosong
        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan password tidak boleh kosong!");
            return;
        }

        User user = adminPanel.validateLogin(username, password);

        if (user != null) {
            lblError.setVisible(false);
            try {
                if (user.isAdmin()) {
                    // Buka halaman Admin Dashboard
                    SceneManager.switchScene(rootPane, "/com/washeasy/fxml/AdminDashboard.fxml",
                            "WashEasy Bot — Admin Panel", user);
                } else {
                    // Buka halaman User / Chatbot
                    SceneManager.switchScene(rootPane, "/com/washeasy/fxml/UserDashboard.fxml",
                            "WashEasy Bot — Laundry Chatbot", user);
                }
            } catch (Exception ex) {
                showError("Gagal membuka halaman: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            showError("Username atau password salah. Coba lagi.");
            txtPassword.clear();
        }
    }

    private void showError(String msg) {
        lblError.setText("⚠ " + msg);
        lblError.setVisible(true);
    }
}
