package com.washeasy.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleDoubleProperty;


public class OrderHistory {

    private final SimpleIntegerProperty id          = new SimpleIntegerProperty();
    private final SimpleStringProperty  username    = new SimpleStringProperty();
    private final SimpleStringProperty  namaLayanan = new SimpleStringProperty();
    private final SimpleDoubleProperty  beratKg     = new SimpleDoubleProperty();
    private final SimpleDoubleProperty  totalHarga  = new SimpleDoubleProperty();
    private final SimpleStringProperty  status      = new SimpleStringProperty();
    private final SimpleStringProperty  createdAt   = new SimpleStringProperty();
    // [NEW] Kolom metode_pengambilan & alamat sesuai desain ERD
    private final SimpleStringProperty  metodePengambilan = new SimpleStringProperty();
    private final SimpleStringProperty  alamat            = new SimpleStringProperty();

    public OrderHistory() {}

    public OrderHistory(int id, String username, String namaLayanan,
                        double beratKg, double totalHarga,
                        String status, String createdAt) {
        this.id.set(id);
        this.username.set(username);
        this.namaLayanan.set(namaLayanan);
        this.beratKg.set(beratKg);
        this.totalHarga.set(totalHarga);
        this.status.set(status);
        this.createdAt.set(createdAt);
        this.metodePengambilan.set("Ambil Sendiri");
        this.alamat.set("");
    }

    /** [NEW] Constructor lengkap dengan metode_pengambilan & alamat */
    public OrderHistory(int id, String username, String namaLayanan,
                        double beratKg, double totalHarga,
                        String status, String metodePengambilan,
                        String alamat, String createdAt) {
        this.id.set(id);
        this.username.set(username);
        this.namaLayanan.set(namaLayanan);
        this.beratKg.set(beratKg);
        this.totalHarga.set(totalHarga);
        this.status.set(status);
        this.metodePengambilan.set(metodePengambilan != null ? metodePengambilan : "Ambil Sendiri");
        this.alamat.set(alamat != null ? alamat : "");
        this.createdAt.set(createdAt);
    }

    // ── Getters (dibutuhkan PropertyValueFactory) ──────────────
    public int    getId()          { return id.get(); }
    public String getUsername()    { return username.get(); }
    public String getNamaLayanan() { return namaLayanan.get(); }
    public double getBeratKg()     { return beratKg.get(); }
    public double getTotalHarga()  { return totalHarga.get(); }
    public String getStatus()      { return status.get(); }
    public String getCreatedAt()   { return createdAt.get(); }
    // [NEW] Getters untuk metode_pengambilan & alamat
    public String getMetodePengambilan() { return metodePengambilan.get(); }
    public String getAlamat()            { return alamat.get(); }

    // ── Setters ────────────────────────────────────────────────
    public void setId(int v)            { id.set(v); }
    public void setUsername(String v)   { username.set(v); }
    public void setNamaLayanan(String v){ namaLayanan.set(v); }
    public void setBeratKg(double v)    { beratKg.set(v); }
    public void setTotalHarga(double v) { totalHarga.set(v); }
    public void setStatus(String v)     { status.set(v); }
    public void setCreatedAt(String v)  { createdAt.set(v); }
    // [NEW] Setters untuk metode_pengambilan & alamat
    public void setMetodePengambilan(String v) { metodePengambilan.set(v); }
    public void setAlamat(String v)            { alamat.set(v); }

    // ── Property getters (untuk advanced binding) ──────────────
    public SimpleIntegerProperty idProperty()          { return id; }
    public SimpleStringProperty  usernameProperty()    { return username; }
    public SimpleStringProperty  namaLayananProperty() { return namaLayanan; }
    public SimpleDoubleProperty  beratKgProperty()     { return beratKg; }
    public SimpleDoubleProperty  totalHargaProperty()  { return totalHarga; }
    public SimpleStringProperty  statusProperty()      { return status; }
    public SimpleStringProperty  createdAtProperty()   { return createdAt; }
    // [NEW] Property untuk metode_pengambilan & alamat
    public SimpleStringProperty  metodePengambilanProperty() { return metodePengambilan; }
    public SimpleStringProperty  alamatProperty()            { return alamat; }

}

