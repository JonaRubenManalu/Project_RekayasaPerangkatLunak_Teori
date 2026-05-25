package com.washeasy.database;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:washeasy.db";
    private static DatabaseManager instance;
    private Connection connection;

    // Cache untuk data dinamis (di-refresh periodik)
    private Map<String, String> infoKedaiCache = new HashMap<>();
    private Map<String, Map<String, Integer>> keywordCache = new HashMap<>(); // kategori -> (keyword -> priority)
    private long lastCacheRefresh = 0;
    private static final long CACHE_TTL = 60000; // 1 menit

    private DatabaseManager() {
        connect();
        createTables();
        seedData();
        loadDynamicData();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DB] Koneksi SQLite berhasil: " + DB_URL);
        } catch (Exception e) {
            System.err.println("[DB] Gagal koneksi: " + e.getMessage());
        }
    }

    private void createTables() {
        String sqlServices = """
            CREATE TABLE IF NOT EXISTS services (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_layanan  VARCHAR(100) NOT NULL UNIQUE,
                deskripsi     TEXT,
                harga         REAL        NOT NULL CHECK(harga > 0),
                satuan_harga  VARCHAR(20) NOT NULL DEFAULT 'kg',
                estimasi_waktu VARCHAR(50) NOT NULL,
                is_active     INTEGER     NOT NULL DEFAULT 1
            );
        """;

        String sqlUsers = """
            CREATE TABLE IF NOT EXISTS users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      VARCHAR(50)  NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role          VARCHAR(20)  NOT NULL DEFAULT 'admin',
                created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """;

        String sqlChatLogs = """
            CREATE TABLE IF NOT EXISTS chat_logs (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                user_input    TEXT NOT NULL,
                bot_response  TEXT NOT NULL,
                is_recognized INTEGER NOT NULL DEFAULT 1,
                created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """;

        String sqlHistory = """
            CREATE TABLE IF NOT EXISTS history (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                username      VARCHAR(50)  NOT NULL,
                nama_layanan  VARCHAR(100) NOT NULL,
                berat_kg      REAL         NOT NULL DEFAULT 1,
                total_harga   REAL         NOT NULL,
                status        VARCHAR(50)  NOT NULL DEFAULT 'Sedang Diproses',
                created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """;

        String sqlKeywords = """
            CREATE TABLE IF NOT EXISTS keywords (
                id_keyword    INTEGER PRIMARY KEY AUTOINCREMENT,
                keyword       VARCHAR(50)  NOT NULL,
                category      VARCHAR(30)  NOT NULL,
                priority      INTEGER      NOT NULL DEFAULT 0,
                id_service    INTEGER,
                is_active     INTEGER      NOT NULL DEFAULT 1,
                created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (id_service) REFERENCES services(id) ON DELETE SET NULL,
                UNIQUE(keyword, category)
            );
        """;

        String sqlInfoKedai = """
            CREATE TABLE IF NOT EXISTS info_kedai (
                id_info       INTEGER PRIMARY KEY AUTOINCREMENT,
                info_key      VARCHAR(50)  NOT NULL UNIQUE,
                info_value    TEXT         NOT NULL,
                description   TEXT,
                updated_by    INTEGER,
                updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
            );
        """;

        String sqlFasilitas = """
            CREATE TABLE IF NOT EXISTS fasilitas (
                id_fasilitas  INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_fasilitas VARCHAR(100) NOT NULL UNIQUE,
                keterangan    TEXT,
                is_active     INTEGER      NOT NULL DEFAULT 1,
                updated_by    INTEGER,
                updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
            );
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlServices);
            stmt.execute(sqlUsers);
            stmt.execute(sqlChatLogs);
            stmt.execute(sqlHistory);
            stmt.execute(sqlKeywords);
            stmt.execute(sqlInfoKedai);
            stmt.execute(sqlFasilitas);
            System.out.println("[DB] Tabel berhasil disiapkan.");
        } catch (SQLException e) {
            System.err.println("[DB] Gagal membuat tabel: " + e.getMessage());
        }
    }

    private void seedData() {
        try {
            // Seed users
            ResultSet rsUser = query("SELECT COUNT(*) FROM users");
            if (rsUser.next() && rsUser.getInt(1) == 0) {
                execute("INSERT INTO users(username,password_hash,role) VALUES('admin','admin123','admin')");
                execute("INSERT INTO users(username,password_hash,role) VALUES('user','user123','user')");
            }

            // Seed services
            ResultSet rsSvc = query("SELECT COUNT(*) FROM services");
            if (rsSvc.next() && rsSvc.getInt(1) == 0) {
                String[][] data = {
                        {"Laundry Reguler",  "Pencucian pakaian biasa dengan proses standar",         "7000",  "kg",     "2-3 hari"},
                        {"Laundry Express",  "Pencucian dengan proses lebih cepat dari reguler",      "10000", "kg",     "1 hari"},
                        {"Laundry Kilat",    "Layanan super cepat untuk kebutuhan mendesak",          "15000", "kg",     "6 jam"},
                        {"Cuci + Setrika",   "Pakaian dicuci dan disetrika hingga rapi",              "8000",  "kg",     "2-3 hari"},
                        {"Setrika Saja",     "Hanya layanan penyetrikaan",                            "5000",  "kg",     "1-2 hari"},
                        {"Laundry Bed Cover","Pencucian khusus untuk bed cover",                      "25000", "pcs",    "2-3 hari"},
                        {"Laundry Sepatu",   "Pencucian sepatu dengan teknik khusus",                 "30000", "pasang", "2-3 hari"},
                };
                for (String[] row : data) {
                    execute(String.format(
                            "INSERT INTO services(nama_layanan,deskripsi,harga,satuan_harga,estimasi_waktu) VALUES('%s','%s',%s,'%s','%s')",
                            row[0], row[1], row[2], row[3], row[4]
                    ));
                }
                System.out.println("[DB] Data awal layanan berhasil di-seed.");
            }

            // Seed keywords
            ResultSet rsKw = query("SELECT COUNT(*) FROM keywords");
            if (rsKw.next() && rsKw.getInt(1) == 0) {
                String[][] keywords = {
                        {"halo", "SALAM", "10"}, {"hai", "SALAM", "10"}, {"hi", "SALAM", "10"},
                        {"selamat pagi", "SALAM", "9"}, {"selamat siang", "SALAM", "9"}, {"selamat malam", "SALAM", "9"},
                        {"hello", "SALAM", "8"}, {"hey", "SALAM", "8"}, {"assalamualaikum", "SALAM", "10"},
                        {"layanan", "LAYANAN", "10"}, {"menu", "LAYANAN", "9"}, {"daftar", "LAYANAN", "8"},
                        {"tersedia", "LAYANAN", "7"}, {"apa saja", "LAYANAN", "9"}, {"ada apa", "LAYANAN", "8"},
                        {"pilihan", "LAYANAN", "7"}, {"jenis", "LAYANAN", "7"},
                        {"harga", "HARGA", "10"}, {"berapa", "HARGA", "9"}, {"biaya", "HARGA", "8"},
                        {"tarif", "HARGA", "8"}, {"cost", "HARGA", "5"}, {"per kilo", "HARGA", "8"}, {"per kg", "HARGA", "8"},
                        {"estimasi", "ESTIMASI", "10"}, {"lama", "ESTIMASI", "9"}, {"kapan", "ESTIMASI", "8"},
                        {"selesai", "ESTIMASI", "9"}, {"berapa hari", "ESTIMASI", "9"}, {"berapa jam", "ESTIMASI", "8"},
                        {"waktu pengerjaan", "ESTIMASI", "10"},
                        {"jam", "JAM_OPERASIONAL", "10"}, {"buka", "JAM_OPERASIONAL", "9"}, {"tutup", "JAM_OPERASIONAL", "9"},
                        {"operasional", "JAM_OPERASIONAL", "10"}, {"waktu buka", "JAM_OPERASIONAL", "9"},
                        {"lokasi", "LOKASI", "10"}, {"alamat", "LOKASI", "10"}, {"di mana", "LOKASI", "9"},
                        {"dimana", "LOKASI", "9"}, {"tempat", "LOKASI", "7"}, {"letak", "LOKASI", "7"},
                        {"jalan", "LOKASI", "6"}, {"google map", "LOKASI", "5"},
                        {"minimal", "MINIMAL_BERAT", "10"}, {"minimum", "MINIMAL_BERAT", "9"}, {"paling sedikit", "MINIMAL_BERAT", "8"},
                        {"batas bawah", "MINIMAL_BERAT", "7"}, {"min", "MINIMAL_BERAT", "8"},
                        {"antar", "ANTAR_JEMPUT", "10"}, {"jemput", "ANTAR_JEMPUT", "10"}, {"delivery", "ANTAR_JEMPUT", "8"},
                        {"pickup", "ANTAR_JEMPUT", "8"}, {"kirim", "ANTAR_JEMPUT", "7"}, {"ambil ke", "ANTAR_JEMPUT", "6"},
                        {"anter", "ANTAR_JEMPUT", "7"},
                        {"cara", "CARA_LAUNDRY", "10"}, {"bagaimana", "CARA_LAUNDRY", "9"}, {"gimana", "CARA_LAUNDRY", "9"},
                        {"prosedur", "CARA_LAUNDRY", "8"}, {"langkah", "CARA_LAUNDRY", "7"}, {"caranya", "CARA_LAUNDRY", "9"},
                };

                for (String[] kw : keywords) {
                    preparedExecute(
                            "INSERT INTO keywords(keyword, category, priority) VALUES(?, ?, ?)",
                            kw[0], kw[1], Integer.parseInt(kw[2])
                    );
                }
                System.out.println("[DB] Data awal keywords berhasil di-seed.");
            }

            // Seed info_kedai
            ResultSet rsInfo = query("SELECT COUNT(*) FROM info_kedai");
            if (rsInfo.next() && rsInfo.getInt(1) == 0) {
                String[][] infoData = {
                        {"jam_senin_jumat", "09:00 - 22:00 WIB", "Jam operasional Senin - Jumat"},
                        {"jam_sabtu", "10:00 - 22:00 WIB", "Jam operasional Sabtu"},
                        {"jam_minggu", "TUTUP", "Jam operasional Minggu"},
                        {"lokasi_alamat", "Jl. Dr. Wahidin Sudirohusodo No. 5-25, Kotabaru, Gondokusuman, Kota Yogyakarta, DIY", "Alamat lengkap laundry"},
                        {"lokasi_keterangan", "Mudah dijangkau dengan kendaraan umum maupun pribadi", "Keterangan tambahan lokasi"},
                        {"telepon", "021-1234-5678", "Nomor telepon laundry"},
                        {"minimal_berat", "1 kg", "Minimal berat laundry"},
                        {"harga_minimum", "Rp 21.000", "Harga minimum untuk kurang dari 1 kg"},
                        {"antar_jemput_syarat", "Minimal 5 kg, radius ± 5 km", "Syarat layanan antar jemput"},
                };
                for (String[] info : infoData) {
                    preparedExecute(
                            "INSERT INTO info_kedai(info_key, info_value, description) VALUES(?, ?, ?)",
                            info[0], info[1], info[2]
                    );
                }
                System.out.println("[DB] Data awal info_kedai berhasil di-seed.");
            }

            // Seed fasilitas
            ResultSet rsFas = query("SELECT COUNT(*) FROM fasilitas");
            if (rsFas.next() && rsFas.getInt(1) == 0) {
                String[][] fasilitasData = {
                        {"Mesin Cuci Industrial", "Mesin cuci kapasitas besar untuk hasil maksimal"},
                        {"Pengering Berteknologi Tinggi", "Mengeringkan pakaian dengan cepat dan aman"},
                        {"Setrika Uap", "Menyetrika dengan uap untuk hasil rapi"},
                        {"Pewangi Laundry Premium", "Pewangi pilihan dengan aroma tahan lama"},
                        {"Layanan Lipat Rapi", "Pakaian dilipat rapi sesuai standar laundry"},
                        {"Antar Jemput Gratis", "Gratis ongkir untuk area terdekat (minimal 5 kg)"},
                        {"Customer Service 24/7", "Layanan pelanggan siap membantu kapan saja"},
                };
                for (String[] fas : fasilitasData) {
                    preparedExecute(
                            "INSERT INTO fasilitas(nama_fasilitas, keterangan) VALUES(?, ?)",
                            fas[0], fas[1]
                    );
                }
                System.out.println("[DB] Data awal fasilitas berhasil di-seed.");
            }
        } catch (SQLException e) {
            System.err.println("[DB] Gagal seed data: " + e.getMessage());
        }
    }

    public void loadDynamicData() {
        loadInfoKedaiCache();
        loadKeywordCache();
        lastCacheRefresh = System.currentTimeMillis();
    }

    private void loadInfoKedaiCache() {
        infoKedaiCache.clear();
        try {
            ResultSet rs = query("SELECT info_key, info_value FROM info_kedai");
            while (rs.next()) {
                infoKedaiCache.put(rs.getString("info_key"), rs.getString("info_value"));
            }
            System.out.println("[DB] Loaded " + infoKedaiCache.size() + " info_kedai entries");
        } catch (SQLException e) {
            System.err.println("[DB] Gagal load info_kedai: " + e.getMessage());
        }
    }

    private void loadKeywordCache() {
        keywordCache.clear();
        try {
            ResultSet rs = query("SELECT keyword, category, priority FROM keywords WHERE is_active = 1 ORDER BY priority DESC");
            while (rs.next()) {
                String category = rs.getString("category");
                String keyword = rs.getString("keyword").toLowerCase();
                int priority = rs.getInt("priority");

                keywordCache.computeIfAbsent(category, k -> new HashMap<>());
                keywordCache.get(category).put(keyword, priority);
            }
            System.out.println("[DB] Loaded " + keywordCache.size() + " keyword categories");
        } catch (SQLException e) {
            System.err.println("[DB] Gagal load keywords: " + e.getMessage());
        }
    }

    public String getInfoKedai(String key) {
        refreshCacheIfNeeded();
        return infoKedaiCache.getOrDefault(key, null);
    }

    public Map<String, String> getAllInfoKedai() {
        refreshCacheIfNeeded();
        return new HashMap<>(infoKedaiCache);
    }

    public Map<String, Map<String, Integer>> getKeywordCache() {
        refreshCacheIfNeeded();
        return keywordCache;
    }

    public boolean updateInfoKedai(String key, String value, Integer updatedBy) {
        refreshCacheIfNeeded();
        try {
            int rows = preparedExecute(
                    "UPDATE info_kedai SET info_value = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE info_key = ?",
                    value, updatedBy, key
            );
            if (rows > 0) {
                loadInfoKedaiCache();
            }
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Gagal update info_kedai: " + e.getMessage());
            return false;
        }
    }

    // [FIXED] Hanya SATU method getAllFasilitas, tidak boleh duplikat!
    public ResultSet getAllFasilitas() throws SQLException {
        return query("SELECT * FROM fasilitas WHERE is_active = 1 ORDER BY id_fasilitas");
    }

    public boolean updateFasilitas(int id, String nama, String keterangan, Integer updatedBy) {
        try {
            int rows = preparedExecute(
                    "UPDATE fasilitas SET nama_fasilitas = ?, keterangan = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id_fasilitas = ?",
                    nama, keterangan, updatedBy, id
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[DB] Gagal update fasilitas: " + e.getMessage());
            return false;
        }
    }

    private void refreshCacheIfNeeded() {
        if (System.currentTimeMillis() - lastCacheRefresh > CACHE_TTL) {
            loadDynamicData();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Method dasar untuk query database
    // ─────────────────────────────────────────────────────────────

    public ResultSet query(String sql) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    public ResultSet preparedQuery(String sql, Object... params) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps.executeQuery();
    }

    public int execute(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    public int preparedExecute(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}