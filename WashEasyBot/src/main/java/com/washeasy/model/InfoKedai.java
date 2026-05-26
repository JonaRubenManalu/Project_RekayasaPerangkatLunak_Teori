package com.washeasy.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * [NEW] Model untuk tabel info_kedai — jam operasional & lokasi dari DB.
 */
public class InfoKedai {

    private final SimpleIntegerProperty idInfo   = new SimpleIntegerProperty();
    private final SimpleStringProperty  jamBuka  = new SimpleStringProperty();
    private final SimpleStringProperty  jamTutup = new SimpleStringProperty();
    private final SimpleStringProperty  lokasi   = new SimpleStringProperty();

    public InfoKedai() {}

    public InfoKedai(int idInfo, String jamBuka, String jamTutup, String lokasi) {
        this.idInfo.set(idInfo);
        this.jamBuka.set(jamBuka);
        this.jamTutup.set(jamTutup);
        this.lokasi.set(lokasi);
    }

    // ── Getters ────────────────────────────────────────────────
    public int    getIdInfo()   { return idInfo.get(); }
    public String getJamBuka()  { return jamBuka.get(); }
    public String getJamTutup() { return jamTutup.get(); }
    public String getLokasi()   { return lokasi.get(); }

    // ── Setters ────────────────────────────────────────────────
    public void setIdInfo(int v)     { idInfo.set(v); }
    public void setJamBuka(String v) { jamBuka.set(v); }
    public void setJamTutup(String v){ jamTutup.set(v); }
    public void setLokasi(String v)  { lokasi.set(v); }

    // ── Property getters ───────────────────────────────────────
    public SimpleIntegerProperty idInfoProperty()   { return idInfo; }
    public SimpleStringProperty  jamBukaProperty()  { return jamBuka; }
    public SimpleStringProperty  jamTutupProperty() { return jamTutup; }
    public SimpleStringProperty  lokasiProperty()   { return lokasi; }
}
