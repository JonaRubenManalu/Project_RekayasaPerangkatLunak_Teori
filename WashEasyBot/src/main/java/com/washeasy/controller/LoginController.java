package com.washeasy.controller;

import com.washeasy.model.User;
import com.washeasy.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;


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
            showError("Username dan Password tidak boleh kosong!");
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
                showError("Gagal Membuka Halaman: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            showError("Username atau password salah. Silahkan Coba lagi.");
            txtPassword.clear();
        }
    }

    // [ADDED] Navigasi ke halaman Sign Up saat button "Sign Up" ditekan
    @FXML
    public void handleGoSignUp() {
        try {
            SceneManager.switchScene(rootPane, "/com/washeasy/fxml/SignUp.fxml",
                    "WashEasy Bot — Daftar Akun", null);
        } catch (Exception e) {
            showError("Gagal membuka halaman Sign Up: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblError.setText("\u26A0 " + msg);
        lblError.setVisible(true);
    }
}
