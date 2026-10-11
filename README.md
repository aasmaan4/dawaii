# Dawaii (दवाई) 💊🌸

**Dawaii** is a full-featured, smart medication adherence & schedule tracking ecosystem designed for patients and caregivers. It features an Android companion with a lock screen interactive overlay, prescription AI scanning, smart dose reminders, and a cross-platform Web/PWA dashboard with real-time cloud sync.

---

## ✨ Features

### 📱 Android Application (`/android`)
- **Interactive Lock Screen & Overlay (`LockScreenOverlayActivity`)**:
  - Full-screen or floating pill overlay when medication is due.
  - Interactive wheel picker for logging doses taken.
  - Customizable aesthetics (Sakura Foliage floral theme with Bodoni serif typography, or sleek Minimal theme).
  - Gesture-based swipe confirmations and dismissals.
- **Smart Prescription Scanner & Pharmacopeia AI**:
  - Camera & image upload scanner to parse prescriptions.
  - Offline & cloud pharmacopeia matching for accurate drug names, dosage timing, and doctor recommendations.
- **Appointments & Doctor Management**:
  - Track upcoming doctor visits and link prescriptions directly to doctors.
  - Postpone, reschedule, or complete appointment workflows.
- **Dose Adherence & Logs**:
  - Detailed daily medication logs with Room persistence.
  - Adherence reporting and export functionality.
- **Real-Time Cloud Sync**:
  - Two-way background synchronization (`CloudSyncManager`) between Android and Web instances.

### 🌐 Web & API (`/web`)
- **Modern Responsive Dashboard**:
  - PWA support (installable on mobile and desktop).
  - Medication schedules, daily timeline, and stock counters.
- **Serverless Backend (Vercel & Node.js)**:
  - `/api/sync`: Real-time cross-device sync endpoint.
  - `/api/gemini-scan`: Prescription deciphering engine and dosage intelligence.

---

## 🛠️ Tech Stack

- **Android**:
  - Kotlin, Jetpack Compose, Material 3
  - Room DB, Coroutines, StateFlow
  - Android Architecture Components, WorkManager / BroadcastReceivers
- **Web & Backend**:
  - HTML5, CSS3, JavaScript (ES6+)
  - Node.js, Vercel Serverless Functions

---

## 🚀 Getting Started

### Android App Setup
1. Open the `/android` directory in **Android Studio**.
2. Sync the project with Gradle files.
3. Build and run on a physical device or emulator running Android 8.0+ (API 26+).
   ```bash
   cd android
   ./gradlew assembleDebug
   ```

### Web App Setup
1. Navigate to the `/web` directory:
   ```bash
   cd web
   node server.js
   ```
2. Open [http://localhost:3000](http://localhost:3000) in your browser.

---

## 📄 License
This project is for personal and healthcare management use.
