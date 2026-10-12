package org.example;

import java.util.ArrayList;
import java.util.Scanner;
// Veri tabanı işlemleri için standart kütüphaneler
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
    // Bilgisayarın içinde stajyerler.db adında kalıcı bir veri tabanı dosyası
    private static final String DB_URL = "jdbc:sqlite:stajyerler.db";

    public static void main(String[] args) {
        // veri tabanı tablosunu kontrol edip hazırlıyoruz
        veritabaniniHazirla();

        // Stajyer nesnelerini içinde saklayacağımız liste
        ArrayList<Stajyer> stajyerListesi = new ArrayList<>();

        // Klavyeden girdi
        Scanner scanner = new Scanner(System.in);

        // Program her açıldığında önce veri tabanındaki eski kayıtları çekip listeye yüklüyoruz
        veritabanindanYukle(stajyerListesi);

        // KULLANICI MENÜSÜ: Programı biz kapatana kadar dönecek olan sonsuz döngü
        while (true) {
            System.out.println("\n--- STAJA BAŞLAMA VE BİTİŞ TAKİP SİSTEMİ ---");
            System.out.println("1. Yeni Stajyer Kaydet");
            System.out.println("2. Stajyerleri Listele");
            System.out.println("3. Staj Başlangıcını Onayla");
            System.out.println("4. Stajın Bittiğini Onayla");
            System.out.println("5. Çıkış");
            System.out.print("Lütfen yapmak istediğiniz işlemi seçin (1-5): ");

            // Kullanıcının menü tercihini okuyoruz
            int secim = scanner.nextInt();
            scanner.nextLine(); // Sayı okumasından sonra oluşan satır atlama hatasını önlemek için boş okuma

            // KOŞUL BLOKLARI
            if (secim == 1) {
                // 1. SEÇENEK: Yeni Stajyer Ekleme Bölümü
                System.out.print("Stajyerin Adı Soyadı: ");
                String ad = scanner.nextLine();

                System.out.print("Bölümü: ");
                String bolum = scanner.nextLine();

                System.out.print("Zorunlu Staj Gün Sayısı: ");
                int gun = scanner.nextInt();
                scanner.nextLine(); // Sayıdan sonra satır atlama hatasını önlemek için boş okuma

                // Önce bilgileri veri tabanına kalıcı olarak yazıyoruz
                veritabaninaStajyerEkle(ad, bolum, gun);

                // Listeyi güncel tutmak için veri tabanından verileri RAM'e yeniden yüklüyüyoruz
                veritabanindanYukle(stajyerListesi);
            }
            else if (secim == 2) {
                // 2. SEÇENEK: Stajyerleri Listeleme Bölümü
                if (stajyerListesi.isEmpty()) {
                    System.out.println("Sistemde kayıtlı stajyer bulunamadı.");
                } else {
                    System.out.println("\n--- KAYITLI STAJYERLER ---");
                    // For döngüsüyle listedeki tüm stajyerleri tek tek dönüyoruz
                    for (int i = 0; i < stajyerListesi.size(); i++) {
                        Stajyer s = stajyerListesi.get(i); // i. indeksteki stajyer nesnesini çekiyoruz

                        // Durum kontrolü yapıyoruz: 0 = Başlamadı, 1 = Devam Ediyor, 2 = Bitti
                        String durum;
                        if (s.durumKodu == 2) {
                            durum = "Staj Bitti!";
                        } else if (s.durumKodu == 1) {
                            durum = "Staja Devam Ediyor";
                        } else {
                            durum = "Henüz Başlamadı";
                        }

                        // Öğrencinin tüm bilgilerini yan yana ekrana yazdırıyoruz
                        System.out.println((i + 1) + ". " + s.adSoyad + " - " + s.bolum + " (" + s.stajGunu + " Gün) -> Durum: " + durum);
                    }
                }
            }
            else if (secim == 3) {
                // 3. SEÇENEK: Staj Başlatma Bölümü
                if (stajyerListesi.isEmpty()) {
                    System.out.println("Onaylanacak stajyer kaydı yok.");
                } else {
                    System.out.println("\n--- HANGİ ÖĞRENCİ STAJA BAŞLADI? ---");
                    for (int i = 0; i < stajyerListesi.size(); i++) {
                        System.out.println((i + 1) + ". " + stajyerListesi.get(i).adSoyad);
                    }

                    System.out.print("Stajını başlatmak istediğiniz öğrencinin numarasını girin: ");
                    int no = scanner.nextInt();
                    scanner.nextLine();

                    // SINIR KONTROLÜ / DOĞRULAMA  Program hatalı numarada çökmesin diye
                    if (no > 0 && no <= stajyerListesi.size()) {
                        Stajyer secilen = stajyerListesi.get(no - 1);

                        // Veri tabanında durum kodunu 1 (Başladı) olarak güncelliyoruz
                        veritabanindaDurumGuncelle(secilen.adSoyad, 1);

                        // RAM'deki listeyi veri tabanından tekrar çekip tazeliyoruz
                        veritabanindanYukle(stajyerListesi);
                    } else {
                        System.out.println("Geçersiz numara!");
                    }
                }
            }
            else if (secim == 4) {
                // 4. SEÇENEK (YENİ): Staj Bitirme Bölümü
                if (stajyerListesi.isEmpty()) {
                    System.out.println("Sistemde stajyer kaydı yok.");
                } else {
                    System.out.println("\n--- HANGİ ÖĞRENCİNİN STAJI BİTTİ? ---");
                    for (int i = 0; i < stajyerListesi.size(); i++) {
                        System.out.println((i + 1) + ". " + stajyerListesi.get(i).adSoyad);
                    }

                    System.out.print("Stajı tamamlanan öğrencinin numarasını girin: ");
                    int no = scanner.nextInt();
                    scanner.nextLine();

                    // Sınır kontrolü (Girdi doğrulaması)
                    if (no > 0 && no <= stajyerListesi.size()) {
                        Stajyer secilen = stajyerListesi.get(no - 1);

                        // Veri tabanında durum kodunu 2 (Bitti) olarak güncelliyoruz (SQL UPDATE)
                        veritabanindaDurumGuncelle(secilen.adSoyad, 2);

                        // Listeyi yeniden yüklüyoruz
                        veritabanindanYukle(stajyerListesi);
                    } else {
                        System.out.println("Geçersiz numara!");
                    }
                }
            }
            else if (secim == 5) {
                // 5. SEÇENEK: Programdan Güvenli Çıkış
                System.out.println("Staj Takip Sistemi kapatılıyor. İyi çalışmalar!");
                break; // while döngüsünü kırar ve programı sonlandırır
            }
            else {
                System.out.println("Geçersiz seçim! Lütfen tekrar deneyin.");
            }
        } // while döngüsünün sonu
    } // main metodunun sonu

    // ==========================================
    // --- VERİ TABANI SORGULARI (SQL METOTLARI) ---
    // ==========================================

    // 1. Veri tabanında 'stajyerler' adında bir tablo yoksa otomatik oluşturan metot (DDL)
    private static void veritabaniniHazirla() {
        String sql = "CREATE TABLE IF NOT EXISTS stajyerler (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "ad_soyad TEXT NOT NULL," +
                "bolum TEXT," +
                "staj_gunu INTEGER," +
                "basladi INTEGER DEFAULT 0);"; // 0=Başlamadı, 1=Başladı, 2=Bitti anlamına gelecek

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql); // Sorguyu çalıştır
        } catch (Exception e) {
            System.out.println("Veri tabanı kurulurken hata: " + e.getMessage());
        }
    }

    // 2. Yeni öğrenciyi veri tabanına kalıcı olarak ekleyen metot (SQL INSERT)
    private static void veritabaninaStajyerEkle(String ad, String bolum, int gun) {
        String sql = "INSERT INTO stajyerler(ad_soyad, bolum, staj_gunu) VALUES(?,?,?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Soru işaretlerine kullanıcının girdilerini güvenli bir şekilde bağlıyoruz (SQL Injection koruması)
            pstmt.setString(1, ad);
            pstmt.setString(2, bolum);
            pstmt.setInt(3, gun);
            pstmt.executeUpdate(); // Veri tabanına kaydet
            System.out.println("'" + ad + "' stajyer listesine başarıyla kaydedildi!");
        } catch (Exception e) {
            System.out.println("Ekleme hatası: " + e.getMessage());
        }
    }

    // 3. Veri tabanındaki tüm verileri çekip ArrayList'e yükleyen metot (SQL SELECT)
    private static void veritabanindanYukle(ArrayList<Stajyer> liste) {
        liste.clear(); // Listenin üst üste binip çoğalmaması için önce içini temizliyoruz
        String sql = "SELECT ad_soyad, bolum, staj_gunu, basladi FROM stajyerler";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) { // Sorguyu çalıştırıp dönen tabloyu ResultSet'e alıyoruz

            // Veri tabanındaki tüm satırları tek tek aşağıya doğru okuyoruz
            while (rs.next()) {
                String ad = rs.getString("ad_soyad");
                String bolum = rs.getString("bolum");
                int gun = rs.getInt("staj_gunu");
                int basladi = rs.getInt("basladi"); // Veri tabanından 0, 1 veya 2 sayısı geliyor

                // Ham verileri alıp yeni bir Java nesnesine (Object) çeviriyoruz
                Stajyer s = new Stajyer(ad, bolum, gun);
                s.durumKodu = basladi; // Öğrencinin durum kodunu eşitliyoruz
                liste.add(s); // Nesneyi ArrayList listemize ekliyoruz
            }
        } catch (Exception e) {
            System.out.println("Yükleme hatası: " + e.getMessage());
        }
    }

    // 4. Öğrencinin durumunu başlama veya bitme durumuna göre güncelleyen ortak metot (SQL UPDATE)
    private static void veritabanindaDurumGuncelle(String ad, int yeniDurum) {
        String sql = "UPDATE stajyerler SET basladi = ? WHERE ad_soyad = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, yeniDurum); // Yeni durum kodunu (1 veya 2) soru işaretine bağlıyoruz
            pstmt.setString(2, ad);
            pstmt.executeUpdate(); // Veri tabanındaki hücreyi güncelle

            // Kullanıcıya şık bir bilgilendirme yapıyoruz
            if (yeniDurum == 1) {
                System.out.println("Staj resmi olarak BAŞLATILDI! 💼");
            } else if (yeniDurum == 2) {
                System.out.println("Staj başarıyla TAMAMLANDI! 🎉");
            }
        } catch (Exception e) {
            System.out.println("Güncelleme hatası: " + e.getMessage());
        }
    }

    // NESNE YÖNELİMLİ PROGRAMLAMA (OOP) MODEL ŞABLONUMUZ
    static class Stajyer {
        String adSoyad;
        String bolum;
        int stajGunu;
        int durumKodu; // 0 = Başlamadı, 1 = Başladı, 2 = Bitti (Durum yönetim değişkeni)

        // Yeni bir stajyer oluştururken doldurulması zorunlu olan alanlar (Constructor - Yapıcı Metot)
        public Stajyer(String adSoyad, String bolum, int stajGunu) {
            this.adSoyad = adSoyad;
            this.bolum = bolum;
            this.stajGunu = stajGunu;
            this.durumKodu = 0; // Sisteme yeni giren bir stajyer varsayılan olarak başlamamıştır (0)
        }
    }
}