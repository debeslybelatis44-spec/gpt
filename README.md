# Sunmi PWA Printer

Une APK Android qui permet à votre PWA de communiquer avec les imprimantes Sunmi V2S et autres POS Sunmi pour imprimer des tickets.

## 📥 Téléchargement
L'APK est générée automatiquement via GitHub Actions. Vous pouvez la télécharger depuis la section **Actions** > **Build APK** > **Artifacts**.

## 🛠️ Configuration
1. Placez le fichier `sunmi-printer-sdk.aar` dans le dossier `app/libs/`.
2. Modifiez l'URL du PWA dans `MainActivity.kt` si nécessaire.

## 📱 Utilisation
1. Installez l'APK sur votre appareil Android connecté à une imprimante Sunmi.
2. Ouvrez l'application : votre PWA sera chargée dans un WebView.
3. Depuis votre PWA, appelez les méthodes JavaScript suivantes pour imprimer :
   - `Android.printTicket("Texte à imprimer")`
   - `Android.printImage("base64_image_data")`
   - `Android.printBarcode("123456789")`
   - `Android.printQRCode("data_qr_code")`

## 🔧 Développement
- Ouvrez le projet dans Android Studio.
- Assurez-vous que le SDK Sunmi est bien présent dans `app/libs/`.
- Builder l'APK avec `./gradlew assembleDebug`.
