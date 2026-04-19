package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.Service;
import javafx.collections.ObservableList;

import java.sql.SQLException;
import java.util.List;


public class ChatbotEngine {

    private final ServiceController serviceController;
    private final DatabaseManager   db;
    private long   lastResponseTime;
    private int    unrecognizedCount = 0;  // untuk UC-08: hitung berapa kali gagal

    // Kategori pertanyaan
    private enum Category {
        SALAM, LAYANAN, HARGA, ESTIMASI, JAM_OPERASIONAL,
        LOKASI, MINIMAL_BERAT, ANTAR_JEMPUT, CARA_LAUNDRY, TIDAK_DIKENALI
    }

    public ChatbotEngine() {
        this.serviceController = new ServiceController();
        this.db                = DatabaseManager.getInstance();
    }

    /**
     * Titik masuk utama: terima input → kembalikan respons teks.
     * Waktu respons dijaga < 2 detik (non-fungsional).
     */
    public String processInput(String input) {
        long start = System.currentTimeMillis();
        if (input == null || input.isBlank()) return "Silakan ketik pertanyaan Anda.";

        String response;
        Category cat = findCategory(input);

        switch (cat) {
            case SALAM             -> response = handleSalam();
            case LAYANAN           -> response = handleLayanan();
            case HARGA             -> response = handleHarga(input);
            case ESTIMASI          -> response = handleEstimasi(input);
            case JAM_OPERASIONAL   -> response = handleJam();
            case LOKASI            -> response = handleLokasi();
            case MINIMAL_BERAT     -> response = handleMinimal();
            case ANTAR_JEMPUT      -> response = handleAntarJemput();
            case CARA_LAUNDRY      -> response = handleCara();
            default                -> response = handleTidakDikenali();
        }

        // Simpan ke chat_logs
        saveChatLog(input, response, cat != Category.TIDAK_DIKENALI);
        lastResponseTime = System.currentTimeMillis() - start;
        return response;
    }

    /** Tentukan kategori pertanyaan berdasarkan keyword */
    private Category findCategory(String input) {
        String low = input.toLowerCase().trim();

        if (containsAny(low, "halo","hai","hi","selamat","pagi","siang","malam","hello","hey","assalamualaikum"))
            return Category.SALAM;
        if (containsAny(low, "layanan","menu","daftar","tersedia","apa saja","ada apa","pilihan","jenis"))
            return Category.LAYANAN;
        if (containsAny(low, "harga","berapa","biaya","tarif","cost","per kilo","per kg"))
            return Category.HARGA;
        if (containsAny(low, "estimasi","lama","kapan","selesai","berapa hari","berapa jam","waktu pengerjaan"))
            return Category.ESTIMASI;
        if (containsAny(low, "jam","buka","tutup","operasional","waktu buka","jam operasional"))
            return Category.JAM_OPERASIONAL;
        if (containsAny(low, "lokasi","alamat","di mana","dimana","tempat","letak","jalan","google map"))
            return Category.LOKASI;
        if (containsAny(low, "minimal","minimum","paling sedikit","batas bawah","min"))
            return Category.MINIMAL_BERAT;
        if (containsAny(low, "antar","jemput","delivery","pickup","kirim","ambil ke","anter"))
            return Category.ANTAR_JEMPUT;
        if (containsAny(low, "cara","bagaimana","gimana","prosedur","langkah","caranya"))
            return Category.CARA_LAUNDRY;

        return Category.TIDAK_DIKENALI;
    }

    /** Periksa apakah input mengandung salah satu keyword */
    private boolean containsAny(String input, String... keywords) {
        for (String kw : keywords) if (input.contains(kw)) return true;
        return false;
    }

    // ── Handler per kategori ────────────────────────────────────────────────

    private String handleSalam() {
        unrecognizedCount = 0;
        return """
            Halo! 👋 Selamat datang di WashEasy Bot!
            Saya siap membantu Anda mendapatkan informasi layanan laundry.
            
            Anda bisa bertanya tentang:
            • Daftar layanan yang tersedia
            • Harga tiap layanan
            • Estimasi waktu pengerjaan
            • Jam operasional & lokasi
            • Layanan antar jemput
            
            Silakan ketik pertanyaan Anda! 
            """;
    }

    private String handleLayanan() {
        unrecognizedCount = 0;
        ObservableList<Service> services = serviceController.getAllServices();
        StringBuilder sb = new StringBuilder("Berikut daftar layanan laundry kami:\n\n");
        for (Service s : services) {
            sb.append(String.format("%-20s | %-18s | %s%n",
                    s.getNamaLayanan(), s.getFormattedHarga(), s.getEstimasiWaktu()));
        }
        sb.append("\nMinimal laundry 3 kg. Jika kurang, dikenakan harga minimum Rp 21.000.");
        return sb.toString();
    }

    private String handleHarga(String input) {
        unrecognizedCount = 0;
        // Coba cari layanan spesifik yang disebut
        String low = input.toLowerCase();
        ObservableList<Service> all = serviceController.getAllServices();
        for (Service s : all) {
            String[] words = s.getNamaLayanan().toLowerCase().split(" ");
            for (String w : words) {
                if (w.length() > 3 && low.contains(w)) {
                    return String.format(
                            "Harga %s adalah %s\nEstimasi pengerjaan: %s\n\n" +
                                    "Info tambahan:\n• Minimal laundry 3 kg\n• Jika kurang dari 3 kg, harga minimum Rp 21.000",
                            s.getNamaLayanan(), s.getFormattedHarga(), s.getEstimasiWaktu()
                    );
                }
            }
        }
        // Jika tidak spesifik, tampilkan semua harga
        StringBuilder sb = new StringBuilder("Berikut daftar harga layanan kami:\n\n");
        for (Service s : all) {
            sb.append(String.format("• %-20s : %s%n", s.getNamaLayanan(), s.getFormattedHarga()));
        }
        sb.append("\nMinimal laundry reguler 3 kg → harga minimum Rp 21.000.");
        return sb.toString();
    }

    private String handleEstimasi(String input) {
        unrecognizedCount = 0;
        String low = input.toLowerCase();
        ObservableList<Service> all = serviceController.getAllServices();
        for (Service s : all) {
            String[] words = s.getNamaLayanan().toLowerCase().split(" ");
            for (String w : words) {
                if (w.length() > 3 && low.contains(w)) {
                    return String.format("Estimasi waktu pengerjaan %s adalah: %s",
                            s.getNamaLayanan(), s.getEstimasiWaktu());
                }
            }
        }
        StringBuilder sb = new StringBuilder("Estimasi waktu pengerjaan setiap layanan:\n\n");
        for (Service s : all) {
            sb.append(String.format("• %-20s : %s%n", s.getNamaLayanan(), s.getEstimasiWaktu()));
        }
        return sb.toString();
    }

    private String handleJam() {
        unrecognizedCount = 0;
        return """
            Jam Operasional WashEasy Laundry:
            
            • Senin – Jumat : 09.00 – 22.00 WIB
            • Sabtu          : 10.00 – 22.00 WIB
            • Minggu         : TUTUP
            
            Kami melayani dengan sepenuh hati setiap harinya! ❤️
            """;
    }

    private String handleLokasi() {
        unrecognizedCount = 0;
        return """
            Lokasi WashEasy Laundry:
            
            📍 Jl. Dr. Wahidin Sudirohusodo No. 5-25,
               Kotabaru, Gondokusuman,
               Kota Yogyakarta, DIY
            
            📞 Telp: 021-1234-5678
            
            Kami mudah dijangkau dengan kendaraan umum maupun pribadi.
            """;
    }

    private String handleMinimal() {
        unrecognizedCount = 0;
        return """
            Minimal laundry adalah 3 kg.
            
            Jika pakaian Anda kurang dari 3 kg, akan tetap dikenakan
            harga minimum sebesar Rp 21.000 (setara 3 kg reguler).
            
            Untuk layanan Express dan Kilat, kebijakan minimal sama.
            """;
    }

    private String handleAntarJemput() {
        unrecognizedCount = 0;
        return """
            Layanan Antar Jemput tersedia! 🛵
            
            Syarat:
            • Minimal laundry 5 kg
            • Area sekitar laundry (radius ± 5 km)
            
            Untuk informasi lebih lanjut dan penjadwalan,
            silakan hubungi kami di 021-1234-5678.
            """;
    }

    private String handleCara() {
        unrecognizedCount = 0;
        return """
            Cara menggunakan layanan WashEasy Laundry:
            
            1. Datang ke lokasi laundry kami
            2. Serahkan pakaian kepada petugas
            3. Petugas menimbang dan mencatat pesanan
            4. Pilih jenis layanan yang diinginkan
            5. Petugas memberikan struk & estimasi waktu
            6. Pakaian dapat diambil sesuai estimasi
            
            Mudah dan praktis! Jika ada pertanyaan, hubungi kami. 😊
            """;
    }

    /** UC-08: Pesan Tidak Dikenali */
    private String handleTidakDikenali() {
        unrecognizedCount++;
        String base = """
            Maaf, saya belum bisa mengenali pertanyaan tersebut. 😅
            
            Silakan coba tanyakan tentang:
            • "daftar layanan"
            • "harga laundry reguler"
            • "estimasi laundry express"
            • "jam buka"
            • "lokasi laundry"
            • "minimal berat"
            • "ada antar jemput?"
            """;
        // Jika sudah 3x tidak dikenali → tawarkan kontak admin
        if (unrecognizedCount >= 3) {
            base += "\nAtau hubungi admin kami langsung:\n📞 021-1234-5678";
            unrecognizedCount = 0;
        }
        return base;
    }

    /** Simpan percakapan ke tabel chat_logs */
    private void saveChatLog(String input, String response, boolean recognized) {
        try {
            db.preparedExecute(
                    "INSERT INTO chat_logs(user_input, bot_response, is_recognized) VALUES(?,?,?)",
                    input, response, recognized ? 1 : 0
            );
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal simpan log: " + e.getMessage());
        }
    }

    public long getLastResponseTime() { return lastResponseTime; }
}
