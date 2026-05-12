package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.Service;
import javafx.collections.ObservableList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * ChatbotController — wrapper di atas ChatbotEngine yang menambahkan
 * sistem pemesanan sekuensial (step-by-step order flow).
 *
 * CARA INTEGRASI:
 *   1. Tambahkan field berikut ke UserDashboardController:
 *        private final ChatbotController chatbotController = new ChatbotController();
 *   2. Di handleKirim(), ganti:
 *        String response = chatbot.processInput(input);
 *      menjadi:
 *        String response = chatbotController.processInput(input, currentUser.getUsername());
 *
 * Sistem ini menggunakan state machine sederhana (enum Step) untuk memandu
 * pengguna melewati alur pemesanan: pilih layanan → masukkan berat → konfirmasi → simpan.
 */
public class ChatbotController {

    // ── State machine untuk alur pemesanan ──────────────────────
    private enum Step {
        IDLE,               // Tidak sedang dalam alur pemesanan
        PILIH_LAYANAN,      // Menunggu user pilih layanan
        INPUT_BERAT,        // Menunggu user masukkan berat (kg)
        KONFIRMASI          // Menunggu konfirmasi ya/tidak
    }

    private Step   currentStep    = Step.IDLE;
    private String selectedLayanan;   // nama layanan yang dipilih
    private double selectedHarga;     // harga per kg/pcs/pasang
    private String selectedSatuan;    // satuan harga
    private double inputBerat;        // berat yang dimasukkan user
    private double totalHarga;        // hasil kalkulasi

    private final ChatbotEngine     engine;
    private final DatabaseManager   db;
    private final ServiceController serviceController;

    // Kata kunci untuk memulai alur pemesanan
    private static final List<String> TRIGGER_PESAN = List.of(
            "pesan", "order", "laundry sekarang", "mau laundry",
            "buat pesanan", "antar cucian", "daftar pesanan"
    );

    public ChatbotController() {
        this.engine            = new ChatbotEngine();
        this.db                = DatabaseManager.getInstance();
        this.serviceController = new ServiceController();
    }

    /**
     * Titik masuk utama. Jika dalam alur pemesanan, proses step;
     * jika tidak, teruskan ke ChatbotEngine biasa.
     *
     * @param input    teks dari user
     * @param username username yang sedang login (untuk menyimpan history)
     * @return respons teks chatbot
     */
    public String processInput(String input, String username) {
        if (input == null || input.isBlank()) return "Silakan ketik pertanyaan Anda.";

        String trimmed = input.trim();

        // ── Cek apakah user mau membatalkan ────────────────────
        if (currentStep != Step.IDLE &&
                containsAny(trimmed.toLowerCase(), "batal", "cancel", "tidak jadi", "keluar")) {
            resetFlow();
            return "❌ Pemesanan dibatalkan. Ada lagi yang bisa saya bantu?";
        }

        // ── Routing berdasarkan step aktif ──────────────────────
        return switch (currentStep) {
            case IDLE          -> handleIdle(trimmed, username);
            case PILIH_LAYANAN -> handlePilihLayanan(trimmed, username);
            case INPUT_BERAT   -> handleInputBerat(trimmed, username);
            case KONFIRMASI    -> handleKonfirmasi(trimmed, username);
        };
    }

    // ── Step 0: IDLE — cek trigger pesan ────────────────────────
    private String handleIdle(String input, String username) {
        String low = input.toLowerCase();

        if (containsAny(low, TRIGGER_PESAN.toArray(String[]::new))) {
            currentStep = Step.PILIH_LAYANAN;
            return buildLayananMenu();
        }

        return engine.processInput(input);
    }

    // ── Step 1: PILIH_LAYANAN — user memilih jenis layanan ──────
    private String handlePilihLayanan(String input, String username) {
        ObservableList<Service> services = serviceController.getAllServices();
        String low = input.toLowerCase();

        Service match = null;
        for (Service s : services) {
            if (low.contains(s.getNamaLayanan().toLowerCase())
                    || s.getNamaLayanan().toLowerCase().contains(low)) {
                match = s;
                break;
            }
            for (String word : s.getNamaLayanan().toLowerCase().split(" ")) {
                if (word.length() > 3 && low.contains(word)) {
                    match = s;
                    break;
                }
            }
            if (match != null) break;
        }

        // Cek input angka (pilih by nomor)
        try {
            int idx = Integer.parseInt(input.trim()) - 1;
            if (idx >= 0 && idx < services.size()) {
                match = services.get(idx);
            }
        } catch (NumberFormatException ignored) {}

        if (match == null) {
            return "⚠ Layanan tidak ditemukan. Silakan pilih sesuai daftar:\n\n"
                    + buildLayananMenu();
        }

        selectedLayanan = match.getNamaLayanan();
        selectedHarga   = match.getHarga();
        selectedSatuan  = match.getSatuanHarga();
        currentStep     = Step.INPUT_BERAT;

        String satuanPrompt = selectedSatuan.equals("kg")
                ? "Berapa kg cucian Anda? (Minimal 1 kg)"
                : "Berapa " + selectedSatuan + " yang ingin Anda laundry?";

        return String.format(
                "✅ Anda memilih: *%s*\n" +
                        "💰 Harga: Rp %,.0f / %s\n\n" +
                        "📦 %s\n" +
                        "(Ketik angka, contoh: 3 atau 5.5)\n\n" +
                        "Ketik 'batal' untuk membatalkan.",
                selectedLayanan, selectedHarga, selectedSatuan, satuanPrompt
        );
    }

    // ── Step 2: INPUT_BERAT — user memasukkan berat/jumlah ──────
    private String handleInputBerat(String input, String username) {
        String trimmedInput = input.trim();
        double berat;

        try {
            // Cek dulu apakah input mengandung angka negatif (misal: -1, -0.5)
            // sebelum di-strip karakter non-angka
            String angka = trimmedInput.replaceAll("[^0-9.]", "").trim();
            if (angka.isEmpty()) throw new NumberFormatException("kosong");
            berat = Double.parseDouble(angka);

            // Jika input aslinya mengandung tanda minus → negatif
            if (trimmedInput.startsWith("-")) berat = -berat;

        } catch (NumberFormatException e) {
            return "⚠️ Input tidak valid. Masukkan angka positif.\n" +
                    "Contoh: 1 atau 2.5";
        }

        // Validasi: angka negatif atau nol
        if (berat <= 0) {
            return "⚠️ Berat tidak valid! Anda memasukkan: \"" + trimmedInput + "\"\n\n" +
                    "Berat harus berupa angka positif.\n" +
                    "Silakan masukkan ulang (contoh: 1 atau 2.5)";
        }

        // Validasi minimal 1 kg — tampilkan warning, TIDAK otomatis diubah
        if (selectedSatuan.equals("kg") && berat < 1) {
            return "⚠️ Minimal pemesanan adalah 1 kg!\n\n" +
                    "Anda memasukkan " + trimmedInput + " kg yang tidak memenuhi " +
                    "syarat minimum.\n\n" +
                    "Silakan masukkan berat minimal 1 kg:\n" +
                    "(Contoh: 1 atau 1.5)";
        }

        inputBerat = berat;
        totalHarga = selectedHarga * berat;
        currentStep = Step.KONFIRMASI;

        return buildKonfirmasiMessage();
    }

    // ── Step 3: KONFIRMASI — user konfirmasi pesanan ─────────────
    private String handleKonfirmasi(String input, String username) {
        String low = input.toLowerCase();

        if (containsAny(low, "ya", "iya", "yes", "ok", "oke", "setuju", "konfirmasi")) {
            boolean saved = saveOrder(username);
            resetFlow();

            if (saved) {
                return String.format(
                        "✅ Pesanan berhasil dibuat!\n\n" +
                                "📋 Detail Pesanan:\n" +
                                "   Layanan   : %s\n" +
                                "   Jumlah    : %.1f %s\n" +
                                "   Total     : Rp %,.0f\n" +
                                "   Status    : Sedang Diproses 🔄\n\n" +
                                "Anda dapat memantau status pesanan di menu 'Tracking Pesanan'.\n" +
                                "Terima kasih! 😊",
                        selectedLayanan, inputBerat, selectedSatuan, totalHarga
                );
            } else {
                return "❌ Gagal menyimpan pesanan. Silakan coba lagi atau hubungi admin.";
            }
        }

        if (containsAny(low, "tidak", "no", "gak", "ngga", "batal")) {
            resetFlow();
            return "❌ Pemesanan dibatalkan. Ada yang bisa saya bantu lagi?";
        }

        return "❓ Mohon konfirmasi dengan ketik 'Ya' untuk lanjut atau 'Tidak' untuk batal.";
    }

    // ── Helper: simpan order ke tabel history ───────────────────
    private boolean saveOrder(String username) {
        try {
            int rows = db.preparedExecute(
                    "INSERT INTO history(username, nama_layanan, berat_kg, total_harga, status) " +
                            "VALUES(?, ?, ?, ?, 'Sedang Diproses')",
                    username, selectedLayanan, inputBerat, totalHarga
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ChatbotController] Gagal simpan order: " + e.getMessage());
            return false;
        }
    }

    // ── Helper: bangun teks menu layanan ────────────────────────
    private String buildLayananMenu() {
        ObservableList<Service> services = serviceController.getAllServices();
        StringBuilder sb = new StringBuilder(
                "🧺 *Alur Pemesanan Laundry*\n\n" +
                        "Pilih layanan yang Anda inginkan:\n\n"
        );
        int i = 1;
        for (Service s : services) {
            sb.append(String.format("%d. %-20s — Rp %,.0f/%s%n",
                    i++, s.getNamaLayanan(), s.getHarga(), s.getSatuanHarga()));
        }
        sb.append("\nKetik nomor atau nama layanan. Ketik 'batal' untuk keluar.");
        return sb.toString();
    }

    // ── Helper: bangun teks konfirmasi ──────────────────────────
    private String buildKonfirmasiMessage() {
        return String.format(
                "📋 *Ringkasan Pesanan:*\n\n" +
                        "   Layanan   : %s\n" +
                        "   Jumlah    : %.1f %s\n" +
                        "   Harga     : Rp %,.0f / %s\n" +
                        "   ───────────────────\n" +
                        "   Total     : Rp %,.0f\n\n" +
                        "Ketik 'Ya' untuk konfirmasi atau 'Tidak' untuk batal.",
                selectedLayanan, inputBerat, selectedSatuan,
                selectedHarga, selectedSatuan,
                totalHarga
        );
    }

    // ── Helper: reset state ke IDLE ─────────────────────────────
    private void resetFlow() {
        currentStep     = Step.IDLE;
        selectedLayanan = null;
        selectedHarga   = 0;
        selectedSatuan  = null;
        inputBerat      = 0;
        totalHarga      = 0;
    }

    // ── Helper: cek keyword ─────────────────────────────────────
    private boolean containsAny(String input, String... keywords) {
        for (String kw : keywords) if (input.contains(kw)) return true;
        return false;
    }

    /** Apakah saat ini sedang dalam alur pemesanan? */
    public boolean isOrdering() {
        return currentStep != Step.IDLE;
    }
}