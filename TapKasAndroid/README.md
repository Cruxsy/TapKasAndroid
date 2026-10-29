# TapKas Android

Proyek Android WebView untuk aplikasi TapKas. Aplikasi memuat UI TapKas dari `app/src/main/assets/index.html` dan memakai NFC native pada Android yang mendukungnya.

## Membuat APK tanpa Android Studio

Proyek ini menyertakan GitHub Actions workflow di `.github/workflows/build-apk.yml`.

1. Buat repository GitHub baru dan unggah seluruh isi folder proyek ini (isi folder, bukan folder luarnya).
2. Buka tab **Actions** di repository dan pilih **Build TapKas APK**.
3. Klik **Run workflow** (atau workflow berjalan otomatis setelah push ke branch `main`/`master`).
4. Setelah selesai, buka run yang berhasil dan unduh artifact **TapKas-debug-apk**.
5. Ekstrak artifact; file di dalamnya adalah `app-debug.apk`. Kirim ke HP Android untuk instalasi. Jika diminta, izinkan pemasangan dari sumber tersebut.

## Catatan
- APK hasil workflow adalah debug APK untuk instalasi/pengujian, belum ditandatangani sebagai rilis Play Store.
- Fitur NFC memerlukan perangkat Android dengan NFC dan browser/komponen native yang mendukungnya.
- Data saat ini tersimpan lokal pada perangkat; belum ada sinkronisasi antarperangkat.
