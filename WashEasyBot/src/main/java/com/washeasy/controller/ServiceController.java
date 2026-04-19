package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.Service;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ServiceController — logika bisnis CRUD layanan laundry.
 * Dipanggil oleh AdminController dan ChatbotEngine.
 */
public class ServiceController {

    private final DatabaseManager db = DatabaseManager.getInstance();

    /** Ambil semua layanan dari database → ObservableList untuk TableView */
    public ObservableList<Service> getAllServices() {
        ObservableList<Service> list = FXCollections.observableArrayList();
        try {
            ResultSet rs = db.query("SELECT * FROM services ORDER BY id");
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ServiceController] getAllServices error: " + e.getMessage());
        }
        return list;
    }

    /** Cari layanan berdasarkan nama (partial match, case-insensitive) */
    public Service getServiceByKeyword(String keyword) {
        try {
            ResultSet rs = db.preparedQuery(
                    "SELECT * FROM services WHERE LOWER(nama_layanan) LIKE LOWER(?) AND is_active=1 LIMIT 1",
                    "%" + keyword + "%"
            );
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.err.println("[ServiceController] getServiceByKeyword error: " + e.getMessage());
        }
        return null;
    }

    /** Tambah layanan baru ke database */
    public boolean addService(Service s) {
        try {
            int rows = db.preparedExecute(
                    "INSERT INTO services(nama_layanan,deskripsi,harga,satuan_harga,estimasi_waktu,is_active) VALUES(?,?,?,?,?,1)",
                    s.getNamaLayanan(), s.getDeskripsi(), s.getHarga(), s.getSatuanHarga(), s.getEstimasiWaktu()
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ServiceController] addService error: " + e.getMessage());
            return false;
        }
    }

    /** Update layanan yang sudah ada */
    public boolean updateService(Service s) {
        try {
            int rows = db.preparedExecute(
                    "UPDATE services SET nama_layanan=?, deskripsi=?, harga=?, satuan_harga=?, estimasi_waktu=?, is_active=? WHERE id=?",
                    s.getNamaLayanan(), s.getDeskripsi(), s.getHarga(),
                    s.getSatuanHarga(), s.getEstimasiWaktu(), s.getIsActive() ? 1 : 0, s.getId()
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ServiceController] updateService error: " + e.getMessage());
            return false;
        }
    }

    /** Hapus layanan berdasarkan ID */
    public boolean deleteService(int id) {
        try {
            int rows = db.preparedExecute("DELETE FROM services WHERE id=?", id);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("[ServiceController] deleteService error: " + e.getMessage());
            return false;
        }
    }

    /** Helper: konversi ResultSet row → Service object */
    private Service mapRow(ResultSet rs) throws SQLException {
        return new Service(
                rs.getInt("id"),
                rs.getString("nama_layanan"),
                rs.getString("deskripsi"),
                rs.getDouble("harga"),
                rs.getString("satuan_harga"),
                rs.getString("estimasi_waktu"),
                rs.getInt("is_active") == 1
        );
    }
}
