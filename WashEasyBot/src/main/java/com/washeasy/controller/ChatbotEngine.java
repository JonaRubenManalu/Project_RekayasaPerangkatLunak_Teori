package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.Service;
import javafx.collections.ObservableList;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ChatbotEngine {

    private final ServiceController serviceController;
    private final DatabaseManager   db;
    private long   lastResponseTime;
    private int    unrecognizedCount = 0;

    private enum Category {
        SALAM, LAYANAN, HARGA, ESTIMASI, JAM_OPERASIONAL,
        LOKASI, MINIMAL_BERAT, ANTAR_JEMPUT, CARA_LAUNDRY, TIDAK_DIKENALI
    }

    private Map<String, Integer> keywordPriorityMap = new HashMap<>();
    private Map<String, String> keywordToCategoryMap = new HashMap<>();
    private long lastKeywordRefresh = 0;
    private static final long CACHE_TTL = 60000;

    public ChatbotEngine() {
        this.serviceController = new ServiceController();
        this.db = DatabaseManager.getInstance();
        refreshKeywordCache();
    }

    private void refreshKeywordCache() {
        keywordPriorityMap.clear();
        keywordToCategoryMap.clear();
        try {
            ResultSet rs = db.query(
                    "SELECT keyword, category, priority FROM keywords WHERE is_active = 1 ORDER BY priority DESC"
            );
            while (rs.next()) {
                String keyword = rs.getString("keyword").toLowerCase();
                String category = rs.getString("category");
                int priority = rs.getInt("priority");

                if (!keywordPriorityMap.containsKey(keyword) ||
                        keywordPriorityMap.get(keyword) < priority) {
                    keywordPriorityMap.put(keyword, priority);
                    keywordToCategoryMap.put(keyword, category);
                }
            }
            System.out.println("[ChatbotEngine] Keyword cache refreshed: " + keywordPriorityMap.size() + " keywords");
            lastKeywordRefresh = System.currentTimeMillis();
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal refresh keyword cache: " + e.getMessage());
        }
    }

    private void checkAndRefreshCache() {
        if (System.currentTimeMillis() - lastKeywordRefresh > CACHE_TTL) {
            refreshKeywordCache();
        }
    }

    public String processInput(String input) {
        long start = System.currentTimeMillis();
        if (input == null || input.isBlank()) {
            return "Silakan ketik pertanyaan Anda.";
        }

        String response;
        Category cat = findCategoryDynamic(input);

        System.out.println("[ChatbotEngine] Input: '" + input + "' -> Category: " + cat);

        switch (cat) {
            case SALAM:
                response = handleSalam();
                break;
            case LAYANAN:
                response = handleLayanan();
                break;
            case HARGA:
                response = handleHarga(input);
                break;
            case ESTIMASI:
                response = handleEstimasi(input);
                break;
            case JAM_OPERASIONAL:
                response = handleJamOperasional();
                break;
            case LOKASI:
                response = handleLokasi();
                break;
            case MINIMAL_BERAT:
                response = handleMinimalBerat();
                break;
            case ANTAR_JEMPUT:
                response = handleAntarJemput();
                break;
            case CARA_LAUNDRY:
                response = handleCaraLaundry();
                break;
            default:
                response = handleTidakDikenali();
                break;
        }

        saveChatLog(input, response, cat != Category.TIDAK_DIKENALI);
        lastResponseTime = System.currentTimeMillis() - start;
        return response;
    }

    private Category findCategoryDynamic(String input) {
        checkAndRefreshCache();
        String low = input.toLowerCase().trim();

        String bestMatchCategory = null;
        int bestMatchPriority = -1;
        String bestMatchKeyword = null;

        for (Map.Entry<String, Integer> entry : keywordPriorityMap.entrySet()) {
            String keyword = entry.getKey();
            int priority = entry.getValue();

            if (low.contains(keyword) && priority > bestMatchPriority) {
                bestMatchPriority = priority;
                bestMatchCategory = keywordToCategoryMap.get(keyword);
                bestMatchKeyword = keyword;
            }
        }

        if (bestMatchCategory != null) {
            System.out.println("[ChatbotEngine] Matched: '" + input + "' -> " + bestMatchCategory);
            try {
                return Category.valueOf(bestMatchCategory);
            } catch (IllegalArgumentException e) {
                return Category.TIDAK_DIKENALI;
            }
        }

        return Category.TIDAK_DIKENALI;
    }

    private String handleSalam() {
        unrecognizedCount = 0;
        return "Halo! Selamat datang di WashEasy Bot!\n" +
                "\n" +
                "Saya siap membantu Anda mendapatkan informasi layanan laundry.\n" +
                "\n" +
                "Anda bisa bertanya tentang:\n" +
                "1. Daftar layanan yang tersedia\n" +
                "2. Harga tiap layanan\n" +
                "3. Estimasi waktu pengerjaan\n" +
                "4. Jam operasional\n" +
                "5. Lokasi laundry\n" +
                "6. Minimal berat cucian\n" +
                "7. Layanan antar jemput\n" +
                "8. Cara pemesanan\n" +
                "\n" +
                "Silakan ketik pertanyaan Anda!";
    }

    private String handleLayanan() {
        unrecognizedCount = 0;
        ObservableList<Service> services = serviceController.getAllServices();

        if (services.isEmpty()) {
            return "Maaf, belum ada layanan yang tersedia. Silakan hubungi admin.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Daftar Layanan Laundry Kami:\n");
        sb.append("\n");

        for (Service s : services) {
            sb.append("- ").append(s.getNamaLayanan()).append("\n");
            sb.append("  Harga: ").append(s.getFormattedHarga()).append("\n");
            sb.append("  Estimasi: ").append(s.getEstimasiWaktu()).append("\n");
            sb.append("\n");
        }

        String minBerat = getInfoFromDB("minimal_berat");
        String minHarga = getInfoFromDB("harga_minimum");
        if (minBerat != null && minHarga != null) {
            sb.append("Catatan: Minimal laundry ").append(minBerat);
            sb.append(" (dikenakan harga minimum ").append(minHarga).append(" jika kurang)");
        }

        return sb.toString();
    }

    private String handleHarga(String input) {
        unrecognizedCount = 0;
        String low = input.toLowerCase();
        ObservableList<Service> services = serviceController.getAllServices();

        for (Service s : services) {
            String namaLayanan = s.getNamaLayanan().toLowerCase();
            if (low.contains(namaLayanan)) {
                String minInfo = "";
                String minBerat = getInfoFromDB("minimal_berat");
                String minHarga = getInfoFromDB("harga_minimum");
                if (minBerat != null && minHarga != null) {
                    minInfo = "\n\nInformasi tambahan:\n" +
                            "- Minimal laundry " + minBerat + "\n" +
                            "- Jika kurang dari " + minBerat + ", harga minimum " + minHarga;
                }
                return "Harga " + s.getNamaLayanan() + ":\n" +
                        "   " + s.getFormattedHarga() + "\n" +
                        "Estimasi pengerjaan: " + s.getEstimasiWaktu() +
                        minInfo;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Daftar Harga Lengkap:\n");
        sb.append("\n");
        for (Service s : services) {
            sb.append("- ").append(s.getNamaLayanan()).append(" : ");
            sb.append(s.getFormattedHarga()).append("\n");
        }

        String minBerat = getInfoFromDB("minimal_berat");
        String minHarga = getInfoFromDB("harga_minimum");
        if (minBerat != null && minHarga != null) {
            sb.append("\nCatatan: Minimal laundry ").append(minBerat);
            sb.append(" -> harga minimum ").append(minHarga);
        }

        return sb.toString();
    }

    private String handleEstimasi(String input) {
        unrecognizedCount = 0;
        String low = input.toLowerCase();
        ObservableList<Service> services = serviceController.getAllServices();

        for (Service s : services) {
            String namaLayanan = s.getNamaLayanan().toLowerCase();
            if (low.contains(namaLayanan)) {
                return "Estimasi waktu pengerjaan " + s.getNamaLayanan() + ":\n" +
                        "   " + s.getEstimasiWaktu();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Estimasi Waktu Setiap Layanan:\n");
        sb.append("\n");
        for (Service s : services) {
            sb.append("- ").append(s.getNamaLayanan()).append(" : ");
            sb.append(s.getEstimasiWaktu()).append("\n");
        }

        return sb.toString();
    }

    private String handleJamOperasional() {
        unrecognizedCount = 0;

        String seninJumat = getInfoFromDB("jam_senin_jumat");
        String sabtu = getInfoFromDB("jam_sabtu");
        String minggu = getInfoFromDB("jam_minggu");
        String telepon = getInfoFromDB("telepon");

        if (seninJumat != null && sabtu != null && minggu != null) {
            StringBuilder response = new StringBuilder();
            response.append("Jam Operasional WashEasy Laundry:\n");
            response.append("\n");
            response.append("Senin - Jumat : ").append(seninJumat).append("\n");
            response.append("Sabtu         : ").append(sabtu).append("\n");
            response.append("Minggu        : ").append(minggu).append("\n");
            if (telepon != null) {
                response.append("\nButuh bantuan? Hubungi: ").append(telepon);
            }
            response.append("\n\nKami siap melayani Anda dengan sepenuh hati.");
            return response.toString();
        }

        return "Jam Operasional WashEasy Laundry:\n" +
                "\n" +
                "Senin - Jumat : 09:00 - 22:00 WIB\n" +
                "Sabtu         : 10:00 - 22:00 WIB\n" +
                "Minggu        : TUTUP\n" +
                "\n" +
                "Butuh bantuan? Hubungi: 021-1234-5678\n" +
                "\n" +
                "Kami siap melayani Anda dengan sepenuh hati.";
    }

    private String handleLokasi() {
        unrecognizedCount = 0;

        String alamat = getInfoFromDB("lokasi_alamat");
        String telepon = getInfoFromDB("telepon");
        String keterangan = getInfoFromDB("lokasi_keterangan");

        if (alamat != null) {
            StringBuilder response = new StringBuilder();
            response.append("Lokasi WashEasy Laundry:\n");
            response.append("\n");
            response.append(alamat).append("\n");
            if (telepon != null) {
                response.append("\nKontak: ").append(telepon).append("\n");
            }
            if (keterangan != null) {
                response.append("\n").append(keterangan);
            }
            return response.toString();
        }

        return "Lokasi WashEasy Laundry:\n" +
                "\n" +
                "Jl. Dr. Wahidin Sudirohusodo No. 5-25\n" +
                "Kotabaru, Gondokusuman\n" +
                "Kota Yogyakarta, DIY 55224\n" +
                "\n" +
                "Telp: 021-1234-5678\n" +
                "\n" +
                "Mudah dijangkau dengan kendaraan umum maupun pribadi.";
    }

    private String handleMinimalBerat() {
        unrecognizedCount = 0;

        String minBerat = getInfoFromDB("minimal_berat");
        String minHarga = getInfoFromDB("harga_minimum");

        if (minBerat != null && minHarga != null) {
            return "Ketentuan Minimal Laundry\n" +
                    "\n" +
                    "Minimal laundry : " + minBerat + "\n" +
                    "Harga minimum   : " + minHarga + "\n" +
                    "\n" +
                    "Jika pakaian Anda kurang dari " + minBerat + ",\n" +
                    "akan tetap dikenakan harga minimum " + minHarga + ".\n" +
                    "\n" +
                    "Berlaku untuk semua jenis layanan.";
        }

        return "Ketentuan Minimal Laundry\n" +
                "\n" +
                "Minimal laundry adalah 1 kg.\n" +
                "\n" +
                "Jika pakaian Anda kurang dari 1 kg, akan tetap dikenakan\n" +
                "harga minimum sebesar Rp 21.000 (setara 1 kg reguler).\n" +
                "\n" +
                "Berlaku untuk semua jenis layanan.";
    }

    private String handleAntarJemput() {
        unrecognizedCount = 0;

        String syarat = getInfoFromDB("antar_jemput_syarat");
        String telepon = getInfoFromDB("telepon");

        StringBuilder response = new StringBuilder();
        response.append("Layanan Antar Jemput Tersedia!\n");
        response.append("\n");
        response.append("Syarat & Ketentuan:\n");

        if (syarat != null) {
            String[] syaratList = syarat.split(",");
            for (String s : syaratList) {
                response.append("- ").append(s.trim()).append("\n");
            }
        } else {
            response.append("- Minimal laundry 5 kg\n");
            response.append("- Area sekitar laundry (radius ± 5 km)\n");
        }

        response.append("\nUntuk informasi lebih lanjut dan penjadwalan,\n");
        if (telepon != null) {
            response.append("silakan hubungi kami di ").append(telepon);
        } else {
            response.append("silakan hubungi kami di 021-1234-5678");
        }

        return response.toString();
    }

    private String handleCaraLaundry() {
        unrecognizedCount = 0;

        StringBuilder response = new StringBuilder();
        response.append("Cara Menggunakan Layanan WashEasy Laundry:\n");
        response.append("\n");
        response.append("1. Datang ke lokasi laundry kami\n");
        response.append("2. Serahkan pakaian kepada petugas\n");
        response.append("3. Petugas menimbang dan mencatat pesanan\n");
        response.append("4. Pilih jenis layanan yang diinginkan\n");
        response.append("5. Petugas memberikan struk & estimasi waktu\n");
        response.append("6. Pakaian dapat diambil sesuai estimasi\n");

        try {
            ResultSet rs = db.getAllFasilitas();
            boolean hasFasilitas = false;
            while (rs.next()) {
                if (!hasFasilitas) {
                    response.append("\nFasilitas yang kami sediakan:\n");
                    hasFasilitas = true;
                }
                String nama = rs.getString("nama_fasilitas");
                String keterangan = rs.getString("keterangan");
                response.append("- ").append(nama);
                if (keterangan != null && !keterangan.isEmpty()) {
                    response.append(" : ").append(keterangan);
                }
                response.append("\n");
            }
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal load fasilitas: " + e.getMessage());
        }

        response.append("\nMudah dan praktis! Ada pertanyaan? Hubungi kami.");
        return response.toString();
    }

    private String getInfoFromDB(String key) {
        try {
            ResultSet rs = db.preparedQuery(
                    "SELECT info_value FROM info_kedai WHERE info_key = ?", key
            );
            if (rs.next()) {
                return rs.getString("info_value");
            }
        } catch (SQLException e) {
            System.err.println("[ChatbotEngine] Gagal ambil info_kedai '" + key + "': " + e.getMessage());
        }
        return null;
    }

    private String handleTidakDikenali() {
        unrecognizedCount++;

        String telepon = getInfoFromDB("telepon");
        String base = "Maaf, saya belum bisa mengenali pertanyaan tersebut.\n" +
                "\n" +
                "Saran pertanyaan yang bisa Anda coba:\n" +
                "- daftar layanan\n" +
                "- harga laundry reguler\n" +
                "- estimasi laundry express\n" +
                "- jam buka\n" +
                "- lokasi laundry\n" +
                "- minimal berat\n" +
                "- antar jemput\n" +
                "- pesan laundry\n";

        if (unrecognizedCount >= 3) {
            if (telepon != null) {
                base += "\nAtau hubungi admin kami langsung di " + telepon;
            } else {
                base += "\nAtau hubungi admin kami langsung di 021-1234-5678";
            }
            unrecognizedCount = 0;
        }

        return base;
    }

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

    public long getLastResponseTime() {
        return lastResponseTime;
    }
}