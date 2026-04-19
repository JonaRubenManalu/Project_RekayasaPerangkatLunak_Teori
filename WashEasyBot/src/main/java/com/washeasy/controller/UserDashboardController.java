package com.washeasy.controller;

import com.washeasy.model.User;
import com.washeasy.util.SceneManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * UserDashboardController — menangani UserDashboard.fxml
 * Fitur utama: tampilan chatbot interaktif untuk pengguna (FR-01, FR-02, FR-03)
 */
public class UserDashboardController {

    // ── FXML Injections ──────────────────────────────────────────
    @FXML private Label     lblUsername;
    @FXML private VBox      vboxMessages;   // Container pesan chat
    @FXML private ScrollPane scrollPane;    // Scroll otomatis
    @FXML private TextField  txtInput;      // Input pengguna
    @FXML private Button     btnKirim;
    @FXML private Pane       rootPane;

    private final ChatbotEngine chatbot = new ChatbotEngine();
    private User currentUser;

    private static final DateTimeFormatter TIME_FMT =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        // Enter key untuk kirim
        txtInput.setOnAction(e -> handleKirim());

        // Auto-scroll ke bawah saat ada pesan baru
        vboxMessages.heightProperty().addListener((obs, old, nv) ->
                scrollPane.setVvalue(1.0)
        );
    }

    /** Dipanggil SceneManager setelah load */
    public void setUser(User user) {
        this.currentUser = user;
        lblUsername.setText(user.getUsername());
        // Tampilkan pesan sambutan dari bot
        Platform.runLater(() -> addBotMessage(
                "Halo " + user.getUsername() + "! 👋\n" +
                        "Selamat datang di WashEasy Bot.\n" +
                        "Saya siap membantu informasi laundry Anda.\n\n" +
                        "Ketik pertanyaan Anda di bawah, atau coba:\n" +
                        "• \"daftar layanan\"\n" +
                        "• \"harga laundry reguler\"\n" +
                        "• \"jam buka\"\n" +
                        "• \"lokasi\""
        ));
    }

    /** Tombol Kirim / Enter */
    @FXML
    public void handleKirim() {
        String input = txtInput.getText().trim();
        if (input.isEmpty()) return;

        // Tampilkan pesan pengguna
        addUserMessage(input);
        txtInput.clear();
        txtInput.setDisable(true);
        btnKirim.setDisable(true);

        // Proses chatbot di thread terpisah agar UI tidak freeze
        new Thread(() -> {
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            String response = chatbot.processInput(input);
            Platform.runLater(() -> {
                addBotMessage(response);
                txtInput.setDisable(false);
                btnKirim.setDisable(false);
                txtInput.requestFocus();
            });
        }).start();
    }

    /** Tombol Harga (quick reply) */
    @FXML public void handleQrHarga() { sendPreset("Berapa harga semua layanan?"); }
    /** Tombol Layanan (quick reply) */
    @FXML public void handleQrLayanan() { sendPreset("Tampilkan daftar layanan"); }
    /** Tombol Jam (quick reply) */
    @FXML public void handleQrJam() { sendPreset("Laundry buka jam berapa?"); }
    /** Tombol Lokasi (quick reply) */
    @FXML public void handleQrLokasi() { sendPreset("Lokasi laundry di mana?"); }

    private void sendPreset(String text) {
        txtInput.setText(text);
        handleKirim();
    }

    /** Tombol Logout */
    @FXML
    public void handleLogout() {
        try {
            SceneManager.switchScene(rootPane, "/com/washeasy/fxml/Login.fxml",
                    "WashEasy Bot — Login", null);
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Render Pesan ─────────────────────────────────────────────

    /** Tambah gelembung pesan pengguna (kanan) */
    private void addUserMessage(String text) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setPadding(new Insets(4, 12, 4, 60));

        VBox bubble = createBubble(text, "#2563EB", "white", true);
        hbox.getChildren().add(bubble);
        vboxMessages.getChildren().add(hbox);
    }

    /** Tambah gelembung pesan bot (kiri) */
    private void addBotMessage(String text) {
        HBox hbox = new HBox(8);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(4, 60, 4, 12));

        // Avatar bot
        Label avatar = new Label("🤖");
        avatar.setStyle("-fx-font-size:20px;");
        avatar.setMinSize(32, 32);
        avatar.setAlignment(Pos.TOP_CENTER);

        VBox bubble = createBubble(text, "white", "#1E293B", false);
        bubble.setStyle(bubble.getStyle() + "-fx-border-color:#E2E8F0;-fx-border-radius:12px;");

        hbox.getChildren().addAll(avatar, bubble);
        vboxMessages.getChildren().add(hbox);
    }

    /** Buat bubble chat */
    private VBox createBubble(String text, String bgColor, String txtColor, boolean isUser) {
        Label msg = new Label(text);
        msg.setWrapText(true);
        msg.setMaxWidth(400);
        msg.setStyle(String.format(
                "-fx-text-fill:%s;-fx-font-size:13px;-fx-font-family:'Segoe UI';-fx-padding:0;",
                txtColor
        ));

        String time = LocalTime.now().format(TIME_FMT);
        Label timeLabel = new Label(time);
        timeLabel.setStyle("-fx-font-size:10px;-fx-text-fill:" + (isUser ? "#93C5FD" : "#94A3B8") + ";");

        VBox box = new VBox(4, msg, timeLabel);
        box.setPadding(new Insets(10, 14, 8, 14));
        box.setStyle(String.format(
                "-fx-background-color:%s;" +
                        "-fx-background-radius:14px %s 14px 14px;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),4,0,0,2);",
                bgColor, isUser ? "4px" : "14px"
        ));

        if (!isUser) timeLabel.setAlignment(Pos.CENTER_LEFT);
        else         timeLabel.setAlignment(Pos.CENTER_RIGHT);

        return box;
    }
}
