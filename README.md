# WashEasy Bot 
**Chatbot Laundry UMKM — Tugas RPLBO TI0373 UKDW**
Kelompok MUGEN | Semester Genap TA 2025/2026

---

##  Tentang Aplikasi

WashEasy Bot adalah aplikasi desktop berbasis chatbot yang kami bangun sebagai solusi digital untuk usaha laundry skala UMKM. Ide dasarnya sederhana: banyak pelanggan laundry yang bingung soal harga, waktu pengerjaan, atau layanan apa yang tersedia — biasanya mereka harus telepon dulu atau datang langsung. Nah, WashEasy Bot hadir untuk menggantikan itu semua lewat antarmuka chatbot yang bisa diakses kapan saja.

Aplikasi ini punya dua sisi: **sisi pelanggan** yang bisa tanya-tanya lewat chatbot dan langsung pesan layanan, dan **sisi admin** yang bisa kelola data layanan dari dashboard. Semua data disimpan lokal pakai SQLite, jadi nggak perlu setup server dulu buat nyoba.



---

##  Struktur Project

```
WashEasyBot/
├── pom.xml                          ← Konfigurasi Maven & dependency
├── src/
│   └── main/
│       ├── java/
│       │   ├── module-info.java      ← Deklarasi module JavaFX
│       │   └── com/washeasy/
│       │       ├── Main.java                          ← Entry point aplikasi
│       │       ├── database/
│       │       │   └── DatabaseManager.java           ← Singleton SQLite connection
│       │       ├── model/
│       │       │   ├── Service.java                   ← Model data layanan
│       │       │   ├── User.java                      ← Model data pengguna
│       │       │   └── OrderHistory.java              ← Model riwayat pesanan
│       │       ├── controller/
│       │       │   ├── LoginController.java           ← Handler login & autentikasi
│       │       │   ├── SignUpController.java          ← Handler registrasi akun baru
│       │       │   ├── AdminPanel.java                ← Logic autentikasi admin
│       │       │   ├── AdminDashboardController.java  ← Dashboard & CRUD layanan (Admin)
│       │       │   ├── UserDashboardController.java   ← Tampilan chatbot (User)
│       │       │   ├── ChatbotController.java         ← State machine alur pemesanan
│       │       │   ├── ChatbotEngine.java             ← Mesin keyword matching
│       │       │   └── ServiceController.java         ← CRUD layanan ke database
│       │       └── util/
│       │           └── SceneManager.java              ← Manajemen perpindahan scene
│       └── resources/com/washeasy/
│           ├── fxml/
│           │   ├── Login.fxml                         ← Halaman login
│           │   ├── SignUp.fxml                        ← Halaman registrasi
│           │   ├── AdminDashboard.fxml                ← Panel admin
│           │   └── UserDashboard.fxml                 ← Halaman chatbot user
│           ├── css/
│           │   └── style.css                          ← Semua styling aplikasi
│           └── images/
│               └── logo.png                           ← Logo WashEasy
```

---

##  Cara Kerja Aplikasi

### Alur Chatbot (Sisi User)

Chatbot kami bekerja dengan pendekatan **keyword matching** yang diimplementasikan di `ChatbotEngine.java`. Setiap pesan dari user akan dianalisis kata kuncinya, lalu dikategorikan ke salah satu dari 9 kategori berikut:

| Kategori | Contoh Pertanyaan User |
|----------|------------------------|
| SALAM | "Halo", "Hai", "Selamat pagi" |
| LAYANAN | "Layanan apa aja?", "Ada menu apa?" |
| HARGA | "Berapa harganya?", "Tarif cuci kering?" |
| ESTIMASI | "Berapa lama selesainya?", "Kapan bisa diambil?" |
| JAM_OPERASIONAL | "Jam buka jam berapa?" |
| LOKASI | "Alamatnya di mana?", "Dimana lokasinya?" |
| MINIMAL_BERAT | "Minimal berat berapa kg?" |
| ANTAR_JEMPUT | "Ada layanan antar jemput?" |
| CARA_LAUNDRY | "Gimana cara pesannya?" |
| TIDAK_DIKENALI | Pertanyaan di luar topik → fallback ke saran + kontak admin |

Kalau user mau langsung pesan, ada alur tersendiri yang dikelola `ChatbotController.java` pakai **state machine**:

```
IDLE → PILIH_LAYANAN → INPUT_BERAT → KONFIRMASI → (Pesanan tersimpan)
```

Setiap pesan yang masuk dan respons bot juga dicatat ke tabel `chat_logs` di database, berguna buat evaluasi dan debug.

### Alur Admin

Admin login pakai username dan password yang ter-hash di database. Setelah masuk ke `AdminDashboard`, admin bisa:
- Lihat statistik total layanan dan layanan aktif
- Tambah layanan baru lewat form
- Edit layanan — tinggal klik baris di tabel, data otomatis ngisi form
- Hapus layanan dengan dialog konfirmasi
- Semua perubahan langsung tersimpan ke SQLite

### Database (SQLite)

File `washeasy.db` otomatis dibuat di direktori project waktu pertama kali aplikasi dijalankan. Tidak perlu install database server apapun. Tabel yang tersedia:

| Tabel | Isi |
|-------|-----|
| `services` | Data layanan laundry (nama, harga, satuan, estimasi waktu, status aktif) |
| `users` | Akun pengguna (username, password hash, role) |
| `chat_logs` | Log semua percakapan chatbot |
| `history` | Riwayat pesanan yang masuk dari chatbot |

---

## ✅ Fitur yang Sudah Diimplementasikan (40% Progress)

| Kode | Kebutuhan Fungsional | Status |
|------|---------------------|--------|
| FR-01 | Pengguna melihat daftar layanan laundry | ✅ |
| FR-02 | Chatbot memberikan informasi harga | ✅ |
| FR-03 | Chatbot memberikan estimasi waktu | ✅ |
| FR-04 | Admin CRUD layanan (tambah/ubah/hapus) | ✅ |

### Detail Fitur Chatbot yang Aktif:
- Tanya daftar layanan → tampil semua layanan dari DB
- Tanya harga (spesifik atau semua) → query SQLite langsung
- Tanya estimasi waktu pengerjaan → dari database
- Tanya jam operasional laundry
- Tanya lokasi dan alamat laundry
- Tanya minimal berat cucian
- Tanya layanan antar jemput
- Tanya cara / prosedur laundry
- Alur pemesanan langsung via chatbot (pilih layanan → input berat → konfirmasi → tersimpan ke history)
- **UC-08**: Pesan tidak dikenali → fallback message + saran + kontak admin

### Detail Fitur Admin:
- Login dengan autentikasi berbasis database
- Dashboard statistik (total layanan, jumlah aktif)
- TableView semua layanan yang ada
- Form tambah layanan baru
- Edit layanan — klik baris → form otomatis terisi
- Hapus layanan dengan dialog konfirmasi
- Semua perubahan langsung tersimpan ke SQLite

---

## ⚙️ Teknologi yang Dipakai

| Teknologi | Versi | Fungsi |
|-----------|-------|--------|
| Java | 17 | Bahasa pemrograman utama |
| JavaFX | 17 | Framework GUI desktop |
| FXML + SceneBuilder | — | Desain layout antarmuka |
| SQLite | — | Database lokal (file `washeasy.db`) |
| sqlite-jdbc | 3.43.0.0 | Driver koneksi Java ke SQLite |
| Maven | 3.8+ | Build tool & dependency management |

---

## 🚀 Cara Menjalankan

### Prasyarat
- **Java JDK 17**
- **Maven 3.8+**
- **IntelliJ IDEA** (direkomendasikan)
- **SceneBuilder** (opsional, kalau mau edit FXML)

### Langkah-langkah

**1. Clone / Extract project**
```bash
# Kalau dari ZIP, extract dulu, lalu buka folder WashEasyBot di IntelliJ
```

**2. Build project**
```bash
cd WashEasyBot
mvn clean install
```

**3. Jalankan aplikasi**
```bash
mvn javafx:run
```

> **Catatan:** File `washeasy.db` akan otomatis terbuat di direktori project saat pertama kali dijalankan. Kalau mau reset data, cukup hapus file `.db` tersebut.

---

##  Akun Default

| Role  | Username | Password  |
|-------|----------|-----------|
| Admin | admin    | admin123  |
| User  | user     | user123   |

Akun ini otomatis di-seed ke database waktu aplikasi pertama kali jalan. User baru juga bisa daftar lewat halaman Sign Up.

---

##  Tim Pengembang

**Kelompok MUGEN**
|71231048 |Jona Ruben Manalu|
|71241126|Valentino Kevin Yulianto|
|71241138|Daniel Adi Pramudya|


