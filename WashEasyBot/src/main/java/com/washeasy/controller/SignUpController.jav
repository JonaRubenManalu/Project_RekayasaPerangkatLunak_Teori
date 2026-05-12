package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SignUpController {

    @FXML private TextField     txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private Label         lblError;
    @FXML private Label         lblSuccess;
    @FXML private Pane          rootPane;

    private final DatabaseManager db = DatabaseManager.getInstance();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        lblSuccess.setVisible(false);
        txtConfirmPassword.setOnAction(e -> handleSignUp());
    }

    /** Tombol Daftar ditekan */
    @FXML
    public void handleSignUp() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String confirm  = txtConfirmPassword.getText();

        // ── Validasi input ─────────────────────────────────────
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError("Semua field wajib diisi!");
            return;
        }
        if (username.length() < 4) {
            showError("Username minimal 4 karakter.");
            return;
        }
        if (password.length() < 6) {
            showError("Password minimal 6 karakter.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Konfirmasi password tidak cocok!");
            return;
        }

        // ── Cek username sudah ada ──────────────────────────────
        try {
            ResultSet rs = db.preparedQuery(
                    "SELECT COUNT(*) FROM users WHERE username = ?", username);
            if (rs.next() && rs.getInt(1) > 0) {
                showError("Username sudah digunakan. Pilih username lain.");
                return;
            }
        } catch (SQLException e) {
            showError("Gagal memeriksa username: " + e.getMessage());
            return;
        }

        // ── Insert user baru dengan role 'user' ─────────────────
        try {
            int rows = db.preparedExecute(
                    "INSERT INTO users(username, password_hash, role) VALUES(?, ?, 'user')",
                    username, password   // Catatan: production sebaiknya hash password
            );
            if (rows > 0) {
                showSuccess("Akun berhasil dibuat! Silakan login.");
                clearForm();
            } else {
                showError("Gagal membuat akun. Coba lagi.");
            }
        } catch (SQLException e) {
            showError("Error database: " + e.getMessage());
        }
    }

    /** Tombol Kembali ke Login */
    @FXML
    public void handleBackToLogin() {
        try {
            SceneManager.switchScene(rootPane, "/com/washeasy/fxml/Login.fxml",
                    "WashEasy Bot — Login", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblError.setText("⚠ " + msg);
        lblError.setVisible(true);
        lblSuccess.setVisible(false);
    }

    private void showSuccess(String msg) {
        lblSuccess.setText("✅ " + msg);
        lblSuccess.setVisible(true);
        lblError.setVisible(false);
    }

    private void clearForm() {
        txtUsername.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
    }
}
