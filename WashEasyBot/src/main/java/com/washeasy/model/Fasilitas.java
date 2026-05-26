package com.washeasy.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * [NEW] Model untuk tabel fasilitas.
 */
public class Fasilitas {

    private final SimpleIntegerProperty idFasilitas   = new SimpleIntegerProperty();
    private final SimpleStringProperty  namaFasilitas = new SimpleStringProperty();
    private final SimpleStringProperty  keterangan    = new SimpleStringProperty();

    public Fasilitas() {}

    public Fasilitas(int idFasilitas, String namaFasilitas, String keterangan) {
        this.idFasilitas.set(idFasilitas);
        this.namaFasilitas.set(namaFasilitas);
        this.keterangan.set(keterangan);
    }

    // ── Getters ────────────────────────────────────────────────
    public int    getIdFasilitas()    { return idFasilitas.get(); }
    public String getNamaFasilitas()  { return namaFasilitas.get(); }
    public String getKeterangan()     { return keterangan.get(); }

    // ── Setters ────────────────────────────────────────────────
    public void setIdFasilitas(int v)      { idFasilitas.set(v); }
    public void setNamaFasilitas(String v) { namaFasilitas.set(v); }
    public void setKeterangan(String v)    { keterangan.set(v); }

    // ── Property getters ───────────────────────────────────────
    public SimpleIntegerProperty idFasilitasProperty()   { return idFasilitas; }
    public SimpleStringProperty  namaFasilitasProperty() { return namaFasilitas; }
    public SimpleStringProperty  keteranganProperty()    { return keterangan; }
}