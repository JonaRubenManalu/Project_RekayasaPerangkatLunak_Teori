package com.washeasy.database;

import java.sql.*;

/**
 * DatabaseManager — mengelola koneksi SQLite dan inisialisasi tabel.
 * Menggunakan pola Singleton agar hanya ada satu koneksi aktif.
 */
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

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sqlServices);
            stmt.execute(sqlUsers);
            stmt.execute(sqlChatLogs);
            System.out.println("[DB] Tabel berhasil disiapkan.");
        } catch (SQLException e) {
            System.err.println("[DB] Gagal membuat tabel: " + e.getMessage());
        }
    }

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
        } catch (SQLException e) {
            System.err.println("[DB] Gagal seed data: " + e.getMessage());
        }
    }

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
}
