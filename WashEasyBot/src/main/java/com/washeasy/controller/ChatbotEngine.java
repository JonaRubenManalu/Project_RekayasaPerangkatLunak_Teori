package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.Service;
import javafx.collections.ObservableList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class ChatbotEngine {

    private final ServiceController serviceController;
    private final DatabaseManager   db;
    private long   lastResponseTime;
    private int    unrecognizedCount = 0;  // untuk UC-08: hitung berapa kali gagal

    // Kategori pertanyaan
    private enum Category {
        SALAM, LAYANAN, HARGA, ESTIMASI, JAM_OPERASIONAL,
        LOKASI, MINIMAL_BERAT, ANTAR_JEMPUT, CARA_LAUNDRY,
        FASILITAS,TERIMAKASIH,  // [NEW] kategori fasilitas
        TIDAK_DIKENALI
    }

    public ChatbotEngine() {
        this.serviceController = new ServiceController();
        this.db                = DatabaseManager.getInstance();
    }


    public String processInput(String input) {
        long start = System.currentTimeMillis();
        if (input == null || input.isBlank()) return "Silakan ketik pertanyaan Anda.";

        String response;
        Category cat = findCategory(input);

        switch (cat) {
            case SALAM             -> response = handleSalam();
            case LAYANAN           -> response = handleLayanan();
            case TERIMAKASIH      -> response = handleTerimaKasih();
            case HARGA             -> response = handleHarga(input);
            case ESTIMASI          -> response = handleEstimasi(input);
            case JAM_OPERASIONAL   -> response = handleJam();
            case LOKASI            -> response = handleLokasi();
            case MINIMAL_BERAT     -> response = handleMinimal();
            case ANTAR_JEMPUT      -> response = handleAntarJemput();
            case CARA_LAUNDRY      -> response = handleCara();
            case FASILITAS         -> response = handleFasilitas();   // [NEW]
            default                -> response = handleTidakDikenali();
        }

        // Simpan ke chat_logs
        saveChatLog(input, response, cat != Category.TIDAK_DIKENALI);
        lastResponseTime = System.currentTimeMillis() - start;
        return response;
    }

    /**
     * [NEW] Tentukan kategori berdasarkan keywords dari DB (dinamis).
     * Fallback ke hardcode jika DB gagal — agar chatbot tetap berfungsi.
     */
    private Category findCategory(String input) {
        String low = input.toLowerCase().trim();

        // -- Coba keyword dinamis dari tabel keywords di DB --
        try {
            ResultSet rs = db.query(
                    "SELECT keyword, category FROM keywords ORDER BY priority DESC"
            );
            // Kumpulkan semua keyword terlebih dahulu agar ResultSet bisa ditutup
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new String[]{ rs.getString("keyword"), rs.getString("category") });
            }
            for (String[] row : rows) {
                String kw  = row[0].toLowerCase();
                String cat = row[1].toUpperCase();
                if (low.contains(kw)) {
                    try { return Category.valueOf(cat); }
                    catch (IllegalArgumentException ignored) { /* kategori tidak dikenal, skip */ }
                }
            }
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal load keywords dari DB, pakai fallback: " + e.getMessage());
            // Fallback ke hardcode di bawah
        }

        // -- Fallback hardcode (jika DB kosong atau gagal) --
        if (containsAny(low, "halo","hai","hi","selamat","pagi","siang","malam","hello","hey","assalamualaikum","shalom"))
            return Category.SALAM;
        if (containsAny(low, "terima kasih", "makasih", "thanks", "thank you", "trima kasih", "terimakasih", "thx", "tq","tidak ada","nggak ada","nggak","oke"))
            return Category.TERIMAKASIH;
        if (containsAny(low, "layanan","menu","daftar","tersedia","apa saja","ada apa","pilihan","jenis"))
            return Category.LAYANAN;
        if (containsAny(low, "harga","berapa","biaya","tarif","cost","per kilo","per kg"))
            return Category.HARGA;
        if (containsAny(low, "estimasi","lama","kapan","selesai","berapa hari","berapa jam","waktu pengerjaan","berap lama"))
            return Category.ESTIMASI;
        if (containsAny(low, "jam","buka","tutup","operasional","waktu buka","jam operasional"))
            return Category.JAM_OPERASIONAL;
        if (containsAny(low, "lokasi","alamat","di mana","dimana","tempat","letak","jalan","google map"))
            return Category.LOKASI;
        if (containsAny(low, "minimal","minimum","paling sedikit","batas bawah","min"))
            return Category.MINIMAL_BERAT;
        if (containsAny(low, "antar","jemput","delivery","pickup","kirim","ambil ke","anter"))
            return Category.ANTAR_JEMPUT;
        if (containsAny(low, "cara","bagaimana","gimana","prosedur","langkah","caranya","gimane","py"))
            return Category.CARA_LAUNDRY;
        if (containsAny(low, "fasilitas","fasilitas apa","ada apa saja"))
            return Category.FASILITAS;

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
    private String handleTerimaKasih() {
        unrecognizedCount = 0;
        return """
        
        Senang bisa membantu Anda! 😊
        
        Jika ada pertanyaan lain seputar laundry, jangan ragu bertanya ya!
 
        Terima kasih telah menggunakan WashEasy Bot! ❤️
        """;
    }

    private String handleLayanan() {
        unrecognizedCount = 0;
        ObservableList<Service> services = serviceController.getAllServices();

        StringBuilder sb = new StringBuilder();
        sb.append("\nDAFTAR LAYANAN LAUNDRY\n");
        sb.append("\n");

        for (Service s : services) {
            sb.append("• ").append(s.getNamaLayanan()).append("\n");
            sb.append("  Harga    : ").append(s.getFormattedHarga()).append("\n");
            sb.append("  Estimasi : ").append(s.getEstimasiWaktu()).append("\n");
            sb.append("\n");
        }

        sb.append("★ Minimal laundry 1 kg. Jika kurang, dikenakan harga minimum Rp 21.000\n");

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
                                    "Info tambahan:\n• Minimal laundry 1 kg\n• Jika kurang dari 1 kg, harga minimum Rp 21.000",
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
        sb.append("\nMinimal laundry reguler 1 kg → harga minimum Rp 21.000.");
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

    /** [UPDATED] Jam operasional — baca dari tabel info_kedai jika tersedia */
    private String handleJam() {
        unrecognizedCount = 0;
        try {
            ResultSet rs = db.query("SELECT jam_buka, jam_tutup FROM info_kedai LIMIT 1");
            if (rs.next()) {
                String buka = rs.getString("jam_buka");
                String tutup = rs.getString("jam_tutup");
                return String.format("""
                
                Jam Operasional WashEasy Laundry:
                
                • Senin – Jumat : %s – %s WIB
                • Sabtu         : %s – %s WIB
                • Minggu        : TUTup
                
                Kami melayani dengan sepenuh hati setiap harinya! ❤️
                """, buka, tutup, buka, tutup);
            }
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal baca jam dari DB: " + e.getMessage());
        }

        // Fallback hardcode
        return """
        
        Jam Operasional WashEasy Laundry:
        
        Senin – Jumat : 09.00 – 22.00 WIB
        Sabtu         : 10.00 – 22.00 WIB
        Minggu        : TUTUP
        
        Kami melayani dengan sepenuh hati setiap harinya! ❤️
        """;
    }

    /** [UPDATED] Lokasi — baca dari tabel info_kedai jika tersedia */
    private String handleLokasi() {
        unrecognizedCount = 0;
        try {
            ResultSet rs = db.query("SELECT lokasi FROM info_kedai LIMIT 1");
            if (rs.next()) {
                String lokasi = rs.getString("lokasi");
                return "Lokasi WashEasy Laundry:\n\n" +
                        "📍 " + lokasi + "\n\n" +
                        "📞 Telp: 021-1234-5678\n\n" +
                        "Kami mudah dijangkau dengan kendaraan umum maupun pribadi.";
            }
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal baca lokasi dari DB: " + e.getMessage());
        }
        // Fallback hardcode
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
            Minimal laundry adalah 1 kg.
            
            Jika pakaian Anda kurang dari 1 kg, akan tetap dikenakan
            harga minimum sebesar Rp 21.000 (setara 1 kg reguler).
            
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
        Cara Menggunakan WashEasy Bot:
        
        1. Mulai Pesanan
           - Ketik 'pesan' di chat
           - Atau klik tombol Pesan
        
        2. Pilih Layanan
           - Pilih dari daftar layanan yang tersedia
           - Ketik nomor atau nama layanan
        
        3. Masukkan Berat
           - Contoh: 2 (untuk 2 kg)
           - Minimal 1 kg
        
        4. Pilih Metode Pengambilan
           - 1 = Ambil Sendiri
           - 2 = Antar ke Alamat
        
        5. Isi Data (jika pilih Antar)
           - Alamat lengkap
           - Nomor HP aktif
        
        6. Konfirmasi Pesanan
           - Ketik 'Ya' untuk konfirmasi
           - Ketik 'Tidak' untuk batal
        
        Setelah Pesanan Dibuat:
        - Status awal: Sedang Diproses
        - Cek status di menu Tracking Pesanan
        - Admin akan mengubah status menjadi Siap Diambil
        
        Contoh Pertanyaan:
        - daftar layanan
        - harga laundry reguler
        - jam buka
        - lokasi
        
        Terima kasih telah menggunakan WashEasy Bot.
        """;
    }

    /** [NEW] Fasilitas — baca dari tabel fasilitas di DB */
    private String handleFasilitas() {
        unrecognizedCount = 0;
        try {
            ResultSet rs = db.query(
                    "SELECT nama_fasilitas, keterangan FROM fasilitas ORDER BY id_fasilitas ASC"
            );
            StringBuilder sb = new StringBuilder("Fasilitas WashEasy Laundry:\n\n");
            boolean ada = false;
            while (rs.next()) {
                ada = true;
                String nama = rs.getString("nama_fasilitas");
                String ket  = rs.getString("keterangan");
                sb.append("✅ ").append(nama);
                if (ket != null && !ket.isBlank()) {
                    sb.append("\n   ").append(ket);
                }
                sb.append("\n\n");
            }
            if (ada) return sb.toString().trim();
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal baca fasilitas dari DB: " + e.getMessage());
        }
        // Fallback hardcode
        return """
            Fasilitas WashEasy Laundry:
            
            ✅ Mesin Cuci Front Loading
               Mesin cuci kapasitas besar dengan teknologi hemat air
            
            ✅ Pengering Otomatis
               Pengering pakaian dengan suhu yang dapat disesuaikan
            
            ✅ Setrika Uap Profesional
               Setrika uap berteknologi tinggi untuk hasil terbaik
            
            ✅ Layanan Antar Jemput
               Minimal 5 kg, radius ± 5 km
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
