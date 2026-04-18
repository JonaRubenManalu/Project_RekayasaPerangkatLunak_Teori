module com.washeasy {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    // SQLite JDBC
    requires org.xerial.sqlitejdbc;

    // Buka package ke JavaFX agar @FXML injection bisa bekerja
    opens com.washeasy to javafx.fxml;
    opens com.washeasy.controller to javafx.fxml;
    opens com.washeasy.model to javafx.base;

    exports com.washeasy;
    exports com.washeasy.controller;
    exports com.washeasy.model;
    exports com.washeasy.database;
    exports com.washeasy.util;
}
