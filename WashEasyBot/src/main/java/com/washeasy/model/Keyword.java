package com.washeasy.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * [NEW] Model untuk tabel keywords — keyword chatbot yang dikelola admin.
 */
public class Keyword {

    private final SimpleIntegerProperty idKeyword = new SimpleIntegerProperty();
    private final SimpleStringProperty  keyword   = new SimpleStringProperty();
    private final SimpleIntegerProperty priority  = new SimpleIntegerProperty();
    private final SimpleStringProperty  category  = new SimpleStringProperty();

    public Keyword() {}

    public Keyword(int idKeyword, String keyword, int priority, String category) {
        this.idKeyword.set(idKeyword);
        this.keyword.set(keyword);
        this.priority.set(priority);
        this.category.set(category);
    }

    // ── Getters ────────────────────────────────────────────────
    public int    getIdKeyword() { return idKeyword.get(); }
    public String getKeyword()   { return keyword.get(); }
    public int    getPriority()  { return priority.get(); }
    public String getCategory()  { return category.get(); }

    // ── Setters ────────────────────────────────────────────────
    public void setIdKeyword(int v)    { idKeyword.set(v); }
    public void setKeyword(String v)   { keyword.set(v); }
    public void setPriority(int v)     { priority.set(v); }
    public void setCategory(String v)  { category.set(v); }

    // ── Property getters ───────────────────────────────────────
    public SimpleIntegerProperty idKeywordProperty() { return idKeyword; }
    public SimpleStringProperty  keywordProperty()   { return keyword; }
    public SimpleIntegerProperty priorityProperty()  { return priority; }
    public SimpleStringProperty  categoryProperty()  { return category; }
}