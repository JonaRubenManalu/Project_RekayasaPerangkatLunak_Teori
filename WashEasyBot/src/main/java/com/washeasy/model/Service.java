package com.washeasy.model;

import javafx.beans.property.*;


public class Service {

    private final IntegerProperty id           = new SimpleIntegerProperty();
    private final StringProperty  namaLayanan  = new SimpleStringProperty();
    private final StringProperty  deskripsi    = new SimpleStringProperty();
    private final DoubleProperty  harga        = new SimpleDoubleProperty();
    private final StringProperty  satuanHarga  = new SimpleStringProperty();
    private final StringProperty  estimasiWaktu= new SimpleStringProperty();
    private final BooleanProperty isActive     = new SimpleBooleanProperty(true);

    public Service() {}

    public Service(int id, String namaLayanan, String deskripsi,
                   double harga, String satuanHarga, String estimasiWaktu, boolean isActive) {
        setId(id);
        setNamaLayanan(namaLayanan);
        setDeskripsi(deskripsi);
        setHarga(harga);
        setSatuanHarga(satuanHarga);
        setEstimasiWaktu(estimasiWaktu);
        setIsActive(isActive);
    }

    public int getId() {
        return id.get();
    }

    public void setId(int v) {
        id.set(v);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public String getNamaLayanan() {
        return namaLayanan.get();
    }

    public void setNamaLayanan(String v) {
        namaLayanan.set(v);
    }

    public StringProperty namaLayananProperty() {
        return namaLayanan;
    }

    public String getDeskripsi() {
        return deskripsi.get();
    }

    public void setDeskripsi(String v) {
        deskripsi.set(v);
    }

    public StringProperty deskripsiProperty() {
        return deskripsi;
    }

    public double getHarga() {
        return harga.get();
    }

    public void setHarga(double v) {
        harga.set(v);
    }

    public DoubleProperty hargaProperty() {
        return harga;
    }

    public String getSatuanHarga() {
        return satuanHarga.get();
    }

    public void setSatuanHarga(String v) {
        satuanHarga.set(v);
    }

    public StringProperty satuanHargaProperty() {
        return satuanHarga;
    }

    public String getEstimasiWaktu() {
        return estimasiWaktu.get();
    }

    public void setEstimasiWaktu(String v) {
        estimasiWaktu.set(v);
    }

    public StringProperty estimasiWaktuProperty() {
        return estimasiWaktu;
    }

    public boolean getIsActive() {
        return isActive.get();
    }

    public void setIsActive(boolean v) {
        isActive.set(v);
    }

    public BooleanProperty isActiveProperty() {
        return isActive;
    }

    /** Format harga: "Rp 7.000/kg" */
    public String getFormattedHarga() {
        return String.format("Rp %,.0f/%s", harga.get(), satuanHarga.get());
    }

    @Override
    public String toString() {
        return getNamaLayanan() + " — " + getFormattedHarga();
    }
}
