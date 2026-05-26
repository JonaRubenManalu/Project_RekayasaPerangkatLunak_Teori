package com.washeasy.controller;

import com.washeasy.database.DatabaseManager;
import com.washeasy.model.Fasilitas;
import com.washeasy.model.InfoKedai;
import com.washeasy.model.Keyword;
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
    private Service editingService = null;
    private User    currentUser;

    // [ADDED] Field untuk tab Manajemen Pesanan
    @FXML private TableView<OrderHistory>               tblPesanan;
    @FXML private TableColumn<OrderHistory, Integer>    colPId;
    @FXML private TableColumn<OrderHistory, String>     colPUser;
    @FXML private TableColumn<OrderHistory, String>     colPLayanan;
    @FXML private TableColumn<OrderHistory, Double>     colPBerat;
    @FXML private TableColumn<OrderHistory, Double>     colPTotal;
    @FXML private TableColumn<OrderHistory, String>     colPStatus;
    @FXML private TableColumn<OrderHistory, String>     colPMetode;   // [NEW]
    @FXML private TableColumn<OrderHistory, String>     colPAlamat;   // [NEW]
    @FXML private TableColumn<OrderHistory, String>     colPTanggal;
    @FXML private Label                                 lblPesananStatus;

    // [NEW] Field untuk tab Info Kedai
    @FXML private TextField fldJamBuka;
    @FXML private TextField fldJamTutup;
    @FXML private TextArea  fldLokasi;
    @FXML private Label     lblInfoKedaiStatus;
    private int currentInfoKedaiId = -1;

    // [NEW] Field untuk tab Fasilitas
    @FXML private TableView<Fasilitas>              tblFasilitas;
    @FXML private TableColumn<Fasilitas, Integer>   colFasId;
    @FXML private TableColumn<Fasilitas, String>    colFasNama;
    @FXML private TableColumn<Fasilitas, String>    colFasKet;
    @FXML private TextField fldFasNama;
    @FXML private TextField fldFasKet;
    @FXML private Label     lblFasilitasStatus;
    private Fasilitas editingFasilitas = null;

    // [NEW] Field untuk tab Template Pertanyaan (Keywords)
    @FXML private TableView<Keyword>              tblKeywords;
    @FXML private TableColumn<Keyword, Integer>   colKwId;
    @FXML private TableColumn<Keyword, String>    colKwKeyword;
    @FXML private TableColumn<Keyword, String>    colKwCategory;
    @FXML private TableColumn<Keyword, Integer>   colKwPriority;
    @FXML private TextField   fldKwKeyword;
    @FXML private ComboBox<String> cmbKwCategory;
    @FXML private TextField   fldKwPriority;
    @FXML private Label       lblKeywordsStatus;
    private Keyword editingKeyword = null;

    private final DatabaseManager db = DatabaseManager.getInstance();

    @FXML
    public void initialize() {
        setupTable();
        setupComboBox();
        loadData();
        lblFormStatus.setVisible(false);
        setupPesananTable(); // [ADDED]
        loadPesananData();   // [ADDED]
        setupInfoKedaiTab(); // [NEW]
        setupFasilitasTab(); // [NEW]
        setupKeywordsTab();  // [NEW]
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

        // [NEW] Kolom metode pengambilan
        if (colPMetode != null)
            colPMetode.setCellValueFactory(new PropertyValueFactory<>("metodePengambilan"));
        // [NEW] Kolom alamat
        if (colPAlamat != null)
            colPAlamat.setCellValueFactory(new PropertyValueFactory<>("alamat"));

        colPTanggal.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
    }

    /** Load semua pesanan dari tabel history ke TableView admin */
    private void loadPesananData() {
        if (tblPesanan == null) return;
        ObservableList<OrderHistory> list = FXCollections.observableArrayList();
        try {
            ResultSet rs = db.query(
                    "SELECT id, username, nama_layanan, berat_kg, total_harga, status, " +
                            "metode_pengambilan, alamat, created_at FROM history ORDER BY created_at DESC"
            );
            while (rs.next()) {
                list.add(new OrderHistory(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("nama_layanan"),
                        rs.getDouble("berat_kg"),
                        rs.getDouble("total_harga"),
                        rs.getString("status"),
                        rs.getString("metode_pengambilan"),  // [NEW]
                        rs.getString("alamat"),              // [NEW]
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

    // ── [NEW] Tab Info Kedai ──────────────────────────────────────

    /** Setup tab Info Kedai — load data dari DB ke form */
    private void setupInfoKedaiTab() {
        if (fldJamBuka == null) return;
        try {
            ResultSet rs = db.query("SELECT id_info, jam_buka, jam_tutup, lokasi FROM info_kedai LIMIT 1");
            if (rs.next()) {
                currentInfoKedaiId = rs.getInt("id_info");
                fldJamBuka.setText(rs.getString("jam_buka"));
                fldJamTutup.setText(rs.getString("jam_tutup"));
                fldLokasi.setText(rs.getString("lokasi"));
            }
        } catch (SQLException e) {
            System.err.println("[AdminDashboard] Gagal load info_kedai: " + e.getMessage());
        }
        if (lblInfoKedaiStatus != null) lblInfoKedaiStatus.setVisible(false);
    }

    /** Tombol Simpan Info Kedai */
    @FXML
    public void handleSimpanInfoKedai() {
        if (fldJamBuka == null) return;
        String jamBuka  = fldJamBuka.getText().trim();
        String jamTutup = fldJamTutup.getText().trim();
        String lokasi   = fldLokasi.getText().trim();

        if (jamBuka.isBlank() || jamTutup.isBlank() || lokasi.isBlank()) {
            showInfoKedaiStatus("⚠ Semua field wajib diisi.");
            return;
        }
        try {
            int rows;
            if (currentInfoKedaiId > 0) {
                rows = db.preparedExecute(
                        "UPDATE info_kedai SET jam_buka=?, jam_tutup=?, lokasi=? WHERE id_info=?",
                        jamBuka, jamTutup, lokasi, currentInfoKedaiId
                );
            } else {
                rows = db.preparedExecute(
                        "INSERT INTO info_kedai(jam_buka, jam_tutup, lokasi, id_admin) VALUES(?,?,?,1)",
                        jamBuka, jamTutup, lokasi
                );
                if (rows > 0) {
                    ResultSet rs = db.query("SELECT last_insert_rowid()");
                    if (rs.next()) currentInfoKedaiId = rs.getInt(1);
                }
            }
            showInfoKedaiStatus(rows > 0 ? "✅ Info kedai berhasil disimpan!" : "❌ Gagal menyimpan.");
        } catch (SQLException e) {
            showInfoKedaiStatus("❌ Error: " + e.getMessage());
        }
    }

    private void showInfoKedaiStatus(String msg) {
        if (lblInfoKedaiStatus != null) {
            lblInfoKedaiStatus.setText(msg);
            lblInfoKedaiStatus.setVisible(true);
        }
    }

    // ── [NEW] Tab Fasilitas ───────────────────────────────────────

    /** Setup kolom dan load data tabel fasilitas */
    private void setupFasilitasTab() {
        if (tblFasilitas == null) return;
        if (colFasId != null)   colFasId.setCellValueFactory(new PropertyValueFactory<>("idFasilitas"));
        if (colFasNama != null) colFasNama.setCellValueFactory(new PropertyValueFactory<>("namaFasilitas"));
        if (colFasKet != null)  colFasKet.setCellValueFactory(new PropertyValueFactory<>("keterangan"));
        // Klik baris → isi form edit
        tblFasilitas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel != null && fldFasNama != null) {
                        editingFasilitas = sel;
                        fldFasNama.setText(sel.getNamaFasilitas());
                        fldFasKet.setText(sel.getKeterangan());
                    }
                }
        );
        if (lblFasilitasStatus != null) lblFasilitasStatus.setVisible(false);
        loadFasilitasData();
    }

    private void loadFasilitasData() {
        if (tblFasilitas == null) return;
        ObservableList<Fasilitas> list = FXCollections.observableArrayList();
        try {
            ResultSet rs = db.query("SELECT id_fasilitas, nama_fasilitas, keterangan FROM fasilitas ORDER BY id_fasilitas ASC");
            while (rs.next()) {
                list.add(new Fasilitas(
                        rs.getInt("id_fasilitas"),
                        rs.getString("nama_fasilitas"),
                        rs.getString("keterangan")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[AdminDashboard] Gagal load fasilitas: " + e.getMessage());
        }
        tblFasilitas.setItems(list);
    }

    @FXML
    public void handleSimpanFasilitas() {
        if (fldFasNama == null) return;
        String nama = fldFasNama.getText().trim();
        String ket  = fldFasKet.getText().trim();
        if (nama.isBlank()) { showFasilitasStatus("⚠ Nama fasilitas wajib diisi."); return; }
        try {
            int rows;
            if (editingFasilitas != null) {
                rows = db.preparedExecute(
                        "UPDATE fasilitas SET nama_fasilitas=?, keterangan=? WHERE id_fasilitas=?",
                        nama, ket, editingFasilitas.getIdFasilitas()
                );
                showFasilitasStatus(rows > 0 ? "✅ Fasilitas diperbarui!" : "❌ Gagal update.");
            } else {
                rows = db.preparedExecute(
                        "INSERT INTO fasilitas(nama_fasilitas, keterangan, id_admin) VALUES(?,?,1)",
                        nama, ket
                );
                showFasilitasStatus(rows > 0 ? "✅ Fasilitas ditambahkan!" : "❌ Gagal tambah.");
            }
            if (rows > 0) { clearFasilitasForm(); loadFasilitasData(); }
        } catch (SQLException e) {
            showFasilitasStatus("❌ Error: " + e.getMessage());
        }
    }

    @FXML
    public void handleHapusFasilitas() {
        Fasilitas sel = tblFasilitas.getSelectionModel().getSelectedItem();
        if (sel == null) { showFasilitasStatus("⚠ Pilih fasilitas terlebih dahulu."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus fasilitas \"" + sel.getNamaFasilitas() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Konfirmasi Hapus"); confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    int rows = db.preparedExecute("DELETE FROM fasilitas WHERE id_fasilitas=?", sel.getIdFasilitas());
                    showFasilitasStatus(rows > 0 ? "✅ Fasilitas dihapus." : "❌ Gagal hapus.");
                    if (rows > 0) { clearFasilitasForm(); loadFasilitasData(); }
                } catch (SQLException e) { showFasilitasStatus("❌ Error: " + e.getMessage()); }
            }
        });
    }

    @FXML
    public void handleBatalFasilitas() {
        editingFasilitas = null;
        clearFasilitasForm();
        tblFasilitas.getSelectionModel().clearSelection();
    }

    private void clearFasilitasForm() {
        editingFasilitas = null;
        if (fldFasNama != null) fldFasNama.clear();
        if (fldFasKet  != null) fldFasKet.clear();
    }

    private void showFasilitasStatus(String msg) {
        if (lblFasilitasStatus != null) {
            lblFasilitasStatus.setText(msg);
            lblFasilitasStatus.setVisible(true);
        }
    }

    // ── [NEW] Tab Template Pertanyaan (Keywords) ──────────────────

    /** Setup kolom dan load data tabel keywords */
    private void setupKeywordsTab() {
        if (tblKeywords == null) return;
        if (colKwId != null)       colKwId.setCellValueFactory(new PropertyValueFactory<>("idKeyword"));
        if (colKwKeyword != null)  colKwKeyword.setCellValueFactory(new PropertyValueFactory<>("keyword"));
        if (colKwCategory != null) colKwCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        if (colKwPriority != null) colKwPriority.setCellValueFactory(new PropertyValueFactory<>("priority"));
        // Klik baris → isi form edit
        tblKeywords.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> {
                    if (sel != null && fldKwKeyword != null) {
                        editingKeyword = sel;
                        fldKwKeyword.setText(sel.getKeyword());
                        if (cmbKwCategory != null) cmbKwCategory.setValue(sel.getCategory());
                        if (fldKwPriority != null) fldKwPriority.setText(String.valueOf(sel.getPriority()));
                    }
                }
        );
        if (cmbKwCategory != null) {
            cmbKwCategory.getItems().addAll(
                    "SALAM","LAYANAN","HARGA","ESTIMASI","JAM_OPERASIONAL",
                    "LOKASI","MINIMAL_BERAT","ANTAR_JEMPUT","CARA_LAUNDRY","FASILITAS","UMUM"
            );
            cmbKwCategory.setValue("UMUM");
        }
        if (lblKeywordsStatus != null) lblKeywordsStatus.setVisible(false);
        loadKeywordsData();
    }

    private void loadKeywordsData() {
        if (tblKeywords == null) return;
        ObservableList<Keyword> list = FXCollections.observableArrayList();
        try {
            ResultSet rs = db.query("SELECT id_keyword, keyword, priority, category FROM keywords ORDER BY category, priority DESC");
            while (rs.next()) {
                list.add(new Keyword(
                        rs.getInt("id_keyword"),
                        rs.getString("keyword"),
                        rs.getInt("priority"),
                        rs.getString("category")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[AdminDashboard] Gagal load keywords: " + e.getMessage());
        }
        tblKeywords.setItems(list);
    }

    @FXML
    public void handleSimpanKeyword() {
        if (fldKwKeyword == null) return;
        String kw  = fldKwKeyword.getText().trim();
        String cat = cmbKwCategory != null ? cmbKwCategory.getValue() : "UMUM";
        int    pri;
        try { pri = Integer.parseInt(fldKwPriority != null ? fldKwPriority.getText().trim() : "1"); }
        catch (NumberFormatException e) { pri = 1; }
        if (kw.isBlank()) { showKeywordsStatus("⚠ Keyword wajib diisi."); return; }
        try {
            int rows;
            if (editingKeyword != null) {
                rows = db.preparedExecute(
                        "UPDATE keywords SET keyword=?, category=?, priority=? WHERE id_keyword=?",
                        kw, cat, pri, editingKeyword.getIdKeyword()
                );
                showKeywordsStatus(rows > 0 ? "✅ Keyword diperbarui!" : "❌ Gagal update.");
            } else {
                rows = db.preparedExecute(
                        "INSERT INTO keywords(keyword, category, priority) VALUES(?,?,?)",
                        kw, cat, pri
                );
                showKeywordsStatus(rows > 0 ? "✅ Keyword ditambahkan!" : "❌ Gagal tambah.");
            }
            if (rows > 0) { clearKeywordsForm(); loadKeywordsData(); }
        } catch (SQLException e) {
            showKeywordsStatus("❌ Error: " + e.getMessage());
        }
    }

    @FXML
    public void handleHapusKeyword() {
        Keyword sel = tblKeywords.getSelectionModel().getSelectedItem();
        if (sel == null) { showKeywordsStatus("⚠ Pilih keyword terlebih dahulu."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Hapus keyword \"" + sel.getKeyword() + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Konfirmasi Hapus"); confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    int rows = db.preparedExecute("DELETE FROM keywords WHERE id_keyword=?", sel.getIdKeyword());
                    showKeywordsStatus(rows > 0 ? "✅ Keyword dihapus." : "❌ Gagal hapus.");
                    if (rows > 0) { clearKeywordsForm(); loadKeywordsData(); }
                } catch (SQLException e) { showKeywordsStatus("❌ Error: " + e.getMessage()); }
            }
        });
    }

    @FXML
    public void handleBatalKeyword() {
        editingKeyword = null;
        clearKeywordsForm();
        tblKeywords.getSelectionModel().clearSelection();
    }

    private void clearKeywordsForm() {
        editingKeyword = null;
        if (fldKwKeyword  != null) fldKwKeyword.clear();
        if (fldKwPriority != null) fldKwPriority.setText("1");
        if (cmbKwCategory != null) cmbKwCategory.setValue("UMUM");
    }

    private void showKeywordsStatus(String msg) {
        if (lblKeywordsStatus != null) {
            lblKeywordsStatus.setText(msg);
            lblKeywordsStatus.setVisible(true);
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