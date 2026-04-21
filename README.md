# WashEasy Bot 
**Chatbot Laundry UMKM — Tugas RPLBO TI0373 UKDW**
Kelompok MUGEN | Semester Genap TA 2025/2026

---

## Struktur Project
```
WashEasyBot/
├── pom.xml                          
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java
│       │   └── com/washeasy/
│       │       ├── Main.java                          ← Entry point
│       │       ├── database/
│       │       │   └── DatabaseManager.java           ← SQLite connection
│       │       ├── model/
│       │       │   ├── Service.java                   ← Model layanan
│       │       │   └── User.java                      ← Model user
│       │       ├── controller/
│       │       │   ├── LoginController.java           ← Login handler
│       │       │   ├── AdminPanel.java                ← Auth logic
│       │       │   ├── AdminDashboardController.java  ← Admin CRUD
│       │       │   ├── UserDashboardController.java   ← Chatbot UI
│       │       │   ├── ChatbotEngine.java             ← Keyword matching
│       │       │   └── ServiceController.java         ← CRUD services
│       │       └── util/
│       │           └── SceneManager.java              ← Pindah scene
│       └── resources/com/washeasy/
│           ├── fxml/
│           │   ├── Login.fxml                         ← Halaman login
│           │   ├── AdminDashboard.fxml                ← Admin panel
│           │   └── UserDashboard.fxml                 ← Chatbot user
│           └── css/
│               └── style.css                          ← Semua styling
```

---

### Prasyarat
- **Java JDK 17**
- **Maven 3.8+** 
- **IntelliJ IDEA** 
- **SceneBuilder** 




### Login
| Role  | Username | Password  |
|-------|----------|-----------|
| Admin | admin    | admin123  |
| User  | user     | user123   |

Database `washeasy.db` akan otomatis dibuat di direktori project saat pertama kali dijalankan.

## Fitur yang Sudah Diimplementasikan (40% Progress)

| Kode | Kebutuhan Fungsional | Status |
|------|---------------------|--------|
| FR-01 | Pengguna melihat daftar layanan laundry | ✅ |
| FR-02 | Chatbot memberikan informasi harga | ✅ |
| FR-03 | Chatbot memberikan estimasi waktu | ✅ |
| FR-04 | Admin CRUD layanan (tambah/ubah/hapus) | ✅ |

### Fitur Chatbot yang Aktif:
- Tanya daftar layanan → tampil semua layanan dari DB
- Tanya harga (spesifik atau semua) → query SQLite
- Tanya estimasi waktu → dari database
- Tanya jam operasional
- Tanya lokasi laundry
- Tanya minimal berat
- Tanya antar jemput
- Tanya cara penggunaan
- **UC-08**: Pesan tidak dikenali → fallback + saran + kontak admin

### Fitur Admin:
- Login dengan autentikasi DB
- Dashboard statistik (total layanan, aktif)
- TableView semua layanan
- Form tambah layanan baru
- Edit layanan (klik baris di tabel → isi form otomatis)
- Hapus layanan (dengan konfirmasi dialog)
- Semua perubahan tersimpan ke SQLite



## Teknologi
- **Java 17** = Bahasa pemrograman utama
- **JavaFX 17** = Framework GUI desktop
- **FXML + SceneBuilder** = Desain layout antarmuka
- **SQLite** = Database lokal (file `washeasy.db`)
- **JDBC** = Koneksi Java ke SQLite
- **Maven** = Build tool dan dependency management



