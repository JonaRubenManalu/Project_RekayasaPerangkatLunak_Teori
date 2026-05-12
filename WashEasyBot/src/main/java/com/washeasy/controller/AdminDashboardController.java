package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.OrderHistory;
import com.washeasy.model.Service;
import com.washeasy.model.User;
import com.washeasy.util.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;

import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDashboardController {

    // ── FXML Injections ──────────────────────────────────────────
    @FXML private Text      txtWelcome;
    @FXML private Label     lblTotalLayanan;
    @FXML private Label     lblLayananAktif;

    // TableView Layanan
    @FXML private TableView<Service>          tblLayanan;
    @FXML private TableColumn<Service,Integer> colId;
    @FXML private TableColumn<Service,String>  colNama;
    @FXML private TableColumn<Service,String>  colDeskripsi;
    @FXML private TableColumn<Service,Double>  colHarga;
    @FXML private TableColumn<Service,String>  colSatuan;
    @FXML private TableColumn<Service,String>  colEstimasi;

    // Form tambah/edit
    @FXML private TextField   fldNama;
    @FXML private TextField   fldDeskripsi;
    @FXML private TextField   fldHarga;
    @FXML private ComboBox<String> cmbSatuan;
    @FXML private TextField   fldEstimasi;
    @FXML private Button      btnSimpan;
    @FXML private Button      btnBatal;
    @FXML private Label       lblFormStatus;

    @FXML private Pane        rootPane;

    private final ServiceController serviceController = new ServiceController();
    private Service editingService = null;  // null = mode tambah, tidak null = mode edit
    private User    currentUser;

    // [ADDED] Field untuk tab Manajemen Pesanan
    @FXML private TableView<OrderHistory>               tblPesanan;
    @FXML private TableColumn<OrderHistory, Integer>    colPId;
    @FXML private TableColumn<OrderHistory, String>     colPUser;
    @FXML private TableColumn<OrderHistory, String>     colPLayanan;
    @FXML private TableColumn<OrderHistory, Double>     colPBerat;
    @FXML private TableColumn<OrderHistory, Double>     colPTotal;
    @FXML private TableColumn<OrderHistory, String>     colPStatus;
    @FXML private TableColumn<OrderHistory, String>     colPTanggal;
    @FXML private Label                                 lblPesananStatus;

    private final DatabaseManager db = DatabaseManager.getInstance();

    @FXML
    public void initialize() {
        setupTable();
        setupComboBox();
        loadData();
        lblFormStatus.setVisible(false);
        setupPesananTable(); // [ADDED]
        loadPesananData();   // [ADDED]
    }

    /** Dipanggil oleh SceneManager setelah scene di-load */
    public void setUser(User user) {
        this.currentUser = user;
        if (txtWelcome != null) {
            txtWelcome.setText("Selamat Datang Admin, " + user.getUsername() + "! 👋");
        }
    }

    /** Setup kolom TableView */
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNama.setCellValueFactory(new PropertyValueFactory<>("namaLayanan"));
        colDeskripsi.setCellValueFactory(new PropertyValueFactory<>("deskripsi"));
        colSatuan.setCellValueFactory(new PropertyValueFactory<>("satuanHarga"));
        colEstimasi.setCellValueFactory(new PropertyValueFactory<>("estimasiWaktu"));

        // Kolom harga dengan format Rupiah
        colHarga.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("Rp %,.0f", item));
            }
        });
        colHarga.setCellValueFactory(new PropertyValueFactory<>("harga"));

        // Klik baris → isi form edit
        tblLayanan.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> { if (selected != null) fillFormForEdit(selected); }
        );
    }

    private void setupComboBox() {
        if (cmbSatuan != null) {
            cmbSatuan.getItems().addAll("kg", "pcs", "pasang");
            cmbSatuan.setValue("kg");
        }
    }

    /** Load semua layanan dari DB ke TableView */
    private void loadData() {
        ObservableList<Service> data = serviceController.getAllServices();
        tblLayanan.setItems(data);
        lblTotalLayanan.setText(String.valueOf(data.size()));
        lblLayananAktif.setText(String.valueOf(data.stream().filter(Service::getIsActive).count()));
    }

    /** Tombol Tambah → reset form ke mode tambah */
    @FXML
    public void handleTambah() {
        editingService = null;
        clearForm();
        btnSimpan.setText("Simpan");
        lblFormStatus.setVisible(false);
        setupPesananTable(); // [ADDED]
        loadPesananData();   // [ADDED]
    }

    /** Tombol Simpan → tambah baru atau update */
    @FXML
    public void handleSimpan() {
        if (!validateForm()) return;

        Service s = buildServiceFromForm();

        boolean ok;
        if (editingService == null) {
            ok = serviceController.addService(s);
            showStatus(ok ? "✅ Layanan berhasil ditambahkan!" : "❌ Gagal menambahkan layanan.");
        } else {
            s.setId(editingService.getId());
            ok = serviceController.updateService(s);
            showStatus(ok ? "✅ Layanan berhasil diperbarui!" : "❌ Gagal memperbarui layanan.");
        }

        if (ok) {
            loadData();
            clearForm();
            editingService = null;
            btnSimpan.setText("Simpan");
        }
    }

    /** Tombol Hapus → hapus layanan yang dipilih di tabel */
    @FXML
    public void handleHapus() {
        Service selected = tblLayanan.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showStatus("⚠ Pilih layanan di tabel terlebih dahulu.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus layanan \"" + selected.getNamaLayanan() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Konfirmasi Hapus");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                boolean ok = serviceController.deleteService(selected.getId());
                showStatus(ok ? "✅ Layanan dihapus." : "❌ Gagal menghapus.");
                if (ok) { loadData(); clearForm(); }
            }
        });
    }

    /** Tombol Batal → reset form */
    @FXML
    public void handleBatal() {
        editingService = null;
        clearForm();
        btnSimpan.setText("Simpan");
        lblFormStatus.setVisible(false);
        tblLayanan.getSelectionModel().clearSelection();
    }

    // ── [ADDED] Fitur Manajemen Pesanan ──────────────────────────────────────

    /** Setup kolom tabel pesanan */
    private void setupPesananTable() {
        if (tblPesanan == null) return;
        colPId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colPLayanan.setCellValueFactory(new PropertyValueFactory<>("namaLayanan"));

        colPBerat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f kg", item));
            }
        });
        colPBerat.setCellValueFactory(new PropertyValueFactory<>("beratKg"));

        colPTotal.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("Rp %,.0f", item));
            }
        });
        colPTotal.setCellValueFactory(new PropertyValueFactory<>("totalHarga"));

        colPStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                if ("Siap Diambil".equals(item)) {
                    setStyle("-fx-text-fill:#10B981;-fx-font-weight:bold;");
                } else {
                    setStyle("-fx-text-fill:#F59E0B;-fx-font-weight:bold;");
                }
            }
        });
        colPStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colPTanggal.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    /** Load semua pesanan dari tabel history ke TableView admin */
    private void loadPesananData() {
        if (tblPesanan == null) return;
        ObservableList<OrderHistory> list = FXCollections.observableArrayList();
        try {
            ResultSet rs = db.query(
                    "SELECT id, username, nama_layanan, berat_kg, total_harga, status, created_at " +
                            "FROM history ORDER BY created_at DESC"
            );
            while (rs.next()) {
                list.add(new OrderHistory(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("nama_layanan"),
                        rs.getDouble("berat_kg"),
                        rs.getDouble("total_harga"),
                        rs.getString("status"),
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[AdminDashboard] Gagal load pesanan: " + e.getMessage());
        }
        tblPesanan.setItems(list);
    }

    /** Tombol 'Tandai Siap Diambil' — update status pesanan yang dipilih */
    @FXML
    public void handleSiapDiambil() {
        OrderHistory selected = tblPesanan.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showPesananStatus("\u26A0 Pilih pesanan dari tabel terlebih dahulu.");
            return;
        }
        if ("Siap Diambil".equals(selected.getStatus())) {
            showPesananStatus("\u2139\uFE0F Pesanan ini sudah berstatus 'Siap Diambil'.");
            return;
        }
        try {
            int rows = db.preparedExecute(
                    "UPDATE history SET status = 'Siap Diambil' WHERE id = ?",
                    selected.getId()
            );
            if (rows > 0) {
                showPesananStatus("\u2705 Status pesanan #" + selected.getId() + " diubah menjadi 'Siap Diambil'.");
                loadPesananData();
            } else {
                showPesananStatus("\u274C Gagal mengubah status pesanan.");
            }
        } catch (SQLException e) {
            showPesananStatus("\u274C Error: " + e.getMessage());
        }
    }

    /** Tombol Refresh tabel pesanan */
    @FXML
    public void handleRefreshPesanan() {
        loadPesananData();
        showPesananStatus("\uD83D\uDD04 Data pesanan diperbarui.");
    }

    private void showPesananStatus(String msg) {
        if (lblPesananStatus != null) {
            lblPesananStatus.setText(msg);
            lblPesananStatus.setVisible(true);
        }
    }

    /** Tombol Logout */
    @FXML
    public void handleLogout() {
        try {
            SceneManager.switchScene(rootPane, "/com/washeasy/fxml/Login.fxml",
                    "WashEasy Bot — Login", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Isi form dari baris yang dipilih di tabel (mode edit) */
    private void fillFormForEdit(Service s) {
        editingService = s;
        fldNama.setText(s.getNamaLayanan());
        fldDeskripsi.setText(s.getDeskripsi());
        fldHarga.setText(String.valueOf((int) s.getHarga()));
        if (cmbSatuan != null) cmbSatuan.setValue(s.getSatuanHarga());
        fldEstimasi.setText(s.getEstimasiWaktu());
        btnSimpan.setText("Update");
    }

    /** Bangun objek Service dari input form */
    private Service buildServiceFromForm() {
        Service s = new Service();
        s.setNamaLayanan(fldNama.getText().trim());
        s.setDeskripsi(fldDeskripsi.getText().trim());
        s.setHarga(Double.parseDouble(fldHarga.getText().trim()));
        s.setSatuanHarga(cmbSatuan != null ? cmbSatuan.getValue() : "kg");
        s.setEstimasiWaktu(fldEstimasi.getText().trim());
        s.setIsActive(true);
        return s;
    }

    private boolean validateForm() {
        if (fldNama.getText().isBlank() || fldHarga.getText().isBlank() || fldEstimasi.getText().isBlank()) {
            showStatus("⚠ Nama, Harga, dan Estimasi wajib diisi.");
            return false;
        }
        try { Double.parseDouble(fldHarga.getText().trim()); }
        catch (NumberFormatException e) { showStatus("⚠ Harga harus berupa angka."); return false; }
        return true;
    }

    private void clearForm() {
        fldNama.clear(); fldDeskripsi.clear(); fldHarga.clear(); fldEstimasi.clear();
        if (cmbSatuan != null) cmbSatuan.setValue("kg");
    }

    private void showStatus(String msg) {
        lblFormStatus.setText(msg);
        lblFormStatus.setVisible(true);
    }
}