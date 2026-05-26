package com.washeasy.database;

import java.sql.*;
<<<<<<< Updated upstream

=======
>>>>>>> Stashed changes

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:washeasy.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        connect();
        createTables();
        seedData();
    }

    /** Singleton getter */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /** Buka koneksi ke SQLite */
    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[DB] Koneksi SQLite berhasil: " + DB_URL);
        } catch (Exception e) {
            System.err.println("[DB] Gagal koneksi: " + e.getMessage());
        }
    }

    /** Buat tabel jika belum ada */
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

        // [ADDED] Tabel history untuk menyimpan pesanan dari chatbot ordering
        String sqlHistory = """
            CREATE TABLE IF NOT EXISTS history (
                id                  INTEGER PRIMARY KEY AUTOINCREMENT,
                username            VARCHAR(50)  NOT NULL,
                nama_layanan        VARCHAR(100) NOT NULL,
                berat_kg            REAL         NOT NULL DEFAULT 1,
                total_harga         REAL         NOT NULL,
                status              VARCHAR(50)  NOT NULL DEFAULT 'Sedang Diproses',
                metode_pengambilan  VARCHAR(50)  NOT NULL DEFAULT 'Ambil Sendiri',
                alamat              TEXT,
                created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
            );
        """;

<<<<<<< Updated upstream
=======
        // [PERBAIKAN] Menambahkan kolom is_active agar sesuai dengan query ChatbotEngine
        String sqlKeywords = """
            CREATE TABLE IF NOT EXISTS keywords (
                id_keyword  INTEGER PRIMARY KEY AUTOINCREMENT,
                keyword     VARCHAR(100) NOT NULL UNIQUE,
                priority    INTEGER      NOT NULL DEFAULT 1,
                id_service  INTEGER,
                category    VARCHAR(50)  NOT NULL DEFAULT 'UMUM',
                is_active   INTEGER      NOT NULL DEFAULT 1,
                FOREIGN KEY (id_service) REFERENCES services(id)
            );
        """;

        // [PERBAIKAN] Mengubah struktur menjadi Key-Value agar dinamis sesuai kebutuhan ChatbotEngine
        String sqlInfoKedai = """
            CREATE TABLE IF NOT EXISTS info_kedai (
                id_info    INTEGER PRIMARY KEY AUTOINCREMENT,
                info_key   VARCHAR(100) NOT NULL UNIQUE,
                info_value TEXT         NOT NULL
            );
        """;

        String sqlFasilitas = """
            CREATE TABLE IF NOT EXISTS fasilitas (
                id_fasilitas    INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_fasilitas  VARCHAR(100) NOT NULL UNIQUE,
                keterangan      TEXT,
                id_admin        INTEGER,
                FOREIGN KEY (id_admin) REFERENCES users(id)
            );
        """;

>>>>>>> Stashed changes
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlServices);
            stmt.execute(sqlUsers);
            stmt.execute(sqlChatLogs);
            stmt.execute(sqlHistory); // [ADDED] buat tabel history pesanan
            System.out.println("[DB] Tabel berhasil disiapkan.");
        } catch (SQLException e) {
            System.err.println("[DB] Gagal membuat tabel: " + e.getMessage());
        }

        migrateHistoryColumns();
    }

<<<<<<< Updated upstream
=======
    private void migrateHistoryColumns() {
        try (Statement stmt = connection.createStatement()) {
            try {
                stmt.execute("ALTER TABLE history ADD COLUMN metode_pengambilan VARCHAR(50) NOT NULL DEFAULT 'Ambil Sendiri'");
                System.out.println("[DB] Migrasi: kolom metode_pengambilan ditambahkan ke history.");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE history ADD COLUMN alamat TEXT");
                System.out.println("[DB] Migrasi: kolom alamat ditambahkan ke history.");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            System.err.println("[DB] Gagal migrasi kolom history: " + e.getMessage());
        }
    }

>>>>>>> Stashed changes
    /** Isi data awal jika tabel masih kosong */
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
<<<<<<< Updated upstream
=======

            // [PERBAIKAN] Seed info_kedai disesuaikan menjadi format Key-Value yang dibutuhkan ChatbotEngine
            ResultSet rsInfo = query("SELECT COUNT(*) FROM info_kedai");
            if (rsInfo.next() && rsInfo.getInt(1) == 0) {
                String[][] infoData = {
                        {"jam_senin_jumat", "09:00 - 22:00 WIB"},
                        {"jam_sabtu",       "10:00 - 22:00 WIB"},
                        {"jam_minggu",      "TUTUP"},
                        {"telepon",         "021-1234-5678"},
                        {"lokasi_alamat",   "Jl. Dr. Wahidin Sudirohusodo No. 5-25, Kotabaru, Gondokusuman, Kota Yogyakarta, DIY 55224"},
                        {"lokasi_keterangan","Mudah dijangkau dengan kendaraan umum maupun pribadi."},
                        {"minimal_berat",   "1 kg"},
                        {"harga_minimum",   "Rp 21.000 (setara 1 kg reguler)"},
                        {"antar_jemput_syarat", "Minimal laundry 5 kg, Area sekitar laundry (radius ± 5 km)"}
                };
                for (String[] info : infoData) {
                    preparedExecute("INSERT INTO info_kedai(info_key, info_value) VALUES(?, ?)", info[0], info[1]);
                }
                System.out.println("[DB] Data awal info_kedai berhasil di-seed.");
            }

            // Seed fasilitas
            ResultSet rsFas = query("SELECT COUNT(*) FROM fasilitas");
            if (rsFas.next() && rsFas.getInt(1) == 0) {
                String[][] fasData = {
                        {"Mesin Cuci Front Loading",   "Mesin cuci kapasitas besar dengan teknologi hemat air"},
                        {"Pengering Otomatis",          "Pengering pakaian dengan suhu yang dapat disesuaikan"},
                        {"Setrika Uap Profesional",     "Setrika uap berteknologi tinggi untuk hasil terbaik"},
                        {"Area Parkir",                 "Tersedia area parkir kendaraan roda dua dan empat"},
                        {"WiFi Gratis",                 "Fasilitas WiFi gratis selama menunggu laundry"},
                        {"Layanan Antar Jemput",        "Antar jemput minimal 5 kg dalam radius 5 km"},
                };
                for (String[] f : fasData) {
                    execute(String.format(
                            "INSERT INTO fasilitas(nama_fasilitas, keterangan, id_admin) VALUES('%s','%s',1)",
                            f[0], f[1]
                    ));
                }
                System.out.println("[DB] Data awal fasilitas berhasil di-seed.");
            }

            // Seed keywords dinamis
            ResultSet rsKw = query("SELECT COUNT(*) FROM keywords");
            if (rsKw.next() && rsKw.getInt(1) == 0) {
                String[][] kwData = {
                        {"halo",           "1", "SALAM"},
                        {"hai",            "1", "SALAM"},
                        {"selamat",        "1", "SALAM"},
                        {"hello",          "1", "SALAM"},
                        {"layanan",        "1", "LAYANAN"},
                        {"menu",           "1", "LAYANAN"},
                        {"daftar",         "1", "LAYANAN"},
                        {"harga",          "1", "HARGA"},
                        {"berapa",         "1", "HARGA"},
                        {"biaya",          "1", "HARGA"},
                        {"tarif",          "1", "HARGA"},
                        {"estimasi",       "1", "ESTIMASI"},
                        {"lama",           "1", "ESTIMASI"},
                        {"kapan selesai",  "1", "ESTIMASI"},
                        {"jam",            "1", "JAM_OPERASIONAL"},
                        {"buka",           "1", "JAM_OPERASIONAL"},
                        {"tutup",          "1", "JAM_OPERASIONAL"},
                        {"operasional",    "1", "JAM_OPERASIONAL"},
                        {"lokasi",         "1", "LOKASI"},
                        {"alamat",         "1", "LOKASI"},
                        {"dimana",         "1", "LOKASI"},
                        {"minimal",        "1", "MINIMAL_BERAT"},
                        {"minimum",        "1", "MINIMAL_BERAT"},
                        {"antar",          "1", "ANTAR_JEMPUT"},
                        {"jemput",         "1", "ANTAR_JEMPUT"},
                        {"delivery",       "1", "ANTAR_JEMPUT"},
                        {"cara",           "1", "CARA_LAUNDRY"},
                        {"bagaimana",      "1", "CARA_LAUNDRY"},
                        {"prosedur",       "1", "CARA_LAUNDRY"}
                };
                for (String[] kw : kwData) {
                    execute(String.format(
                            "INSERT INTO keywords(keyword, priority, category) VALUES('%s',%s,'%s')",
                            kw[0], kw[1], kw[2]
                    ));
                }
                System.out.println("[DB] Data awal keywords berhasil di-seed.");
            }
>>>>>>> Stashed changes
        } catch (SQLException e) {
            System.err.println("[DB] Gagal seed data: " + e.getMessage());
        }
    }

<<<<<<< Updated upstream
=======
    /** [PERBAIKAN] Menambahkan method getAllFasilitas() yang dicari ChatbotEngine */
    public ResultSet getAllFasilitas() throws SQLException {
        String sql = "SELECT nama_fasilitas, keterangan FROM fasilitas";
        return query(sql);
    }

>>>>>>> Stashed changes
    /** Eksekusi query SELECT → kembalikan ResultSet */
    public ResultSet query(String sql) throws SQLException {
        Statement stmt = connection.createStatement();
        return stmt.executeQuery(sql);
    }

    /** Eksekusi query SELECT dengan parameter (PreparedStatement) */
    public ResultSet preparedQuery(String sql, Object... params) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
        return ps.executeQuery();
    }

    /** Eksekusi INSERT / UPDATE / DELETE → kembalikan rows affected */
    public int execute(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /** Eksekusi INSERT/UPDATE/DELETE dengan parameter */
    public int preparedExecute(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        }
    }

    public Connection getConnection() { return connection; }
<<<<<<< Updated upstream
}
=======
}
>>>>>>> Stashed changes
