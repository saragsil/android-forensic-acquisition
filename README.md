# Android Forensic Acquisition Application

This project implements an **Android application for logical forensic data acquisition**, developed in the context of an academic course in **Digital Forensics**.

The application is designed to operate in a **controlled Android Emulator environment** and performs structured collection of forensic artifacts using **official Android APIs**, without bypassing the Android security model.

---

## 🎯 Project Objectives

- Design and implement a **forensically sound Android acquisition tool**
- Perform **logical data acquisition** from an Android device
- Export collected artifacts in **structured and verifiable formats**
- Ensure **reproducibility**, **traceability**, and **data integrity**
- Provide clear documentation suitable for academic evaluation

---

## 🧪 Target Environment

- **Android Studio:** Ladybug 2024.2.1 Patch 3  
- **Android Emulator:** Pixel 7 Pro  
- **Android API Level:** 36  
- **Programming Language:** Kotlin  
- **UI Framework:** Android Views (non-Compose)  

The emulator is used as a **controlled forensic testbed**, allowing the creation of synthetic data (ground truth) for validation purposes.

---

## 📂 Collected Forensic Artifacts

The application supports the following artifact categories:

- 📇 **Contacts** (ContactsContract)
- 📞 **Call Logs** (CallLog.Calls)
- 💬 **SMS Messages** (best-effort, subject to Android restrictions)
- 📦 **Installed Applications & Permissions** (PackageManager)
- 📱 **Device Information** (Android version, model, build fingerprint)

> Note: SMS acquisition is implemented on a best-effort basis and may be limited by Android security restrictions, especially on newer API levels.

---

## 📤 Output & Export Format

Each acquisition generates a **case-based export directory** containing:

- `contacts.json`
- `calls.json`
- `sms.json` (optional)
- `apps.json`
- `device.json`
- `manifest.json` (acquisition metadata)
- `hashes.sha256` (SHA-256 integrity verification)
- `logs.txt` (execution and error logs)

All outputs are stored in the application’s external files directory and can be extracted using `adb pull`.

---

## 🔐 Forensic Methodology

The application follows key digital forensic principles:

- Logical acquisition using **read-only APIs**
- No rooting, exploitation, or security bypass
- Explicit permission handling and documentation
- Cryptographic hashing for integrity verification
- Clear separation between data sources and outputs
- Reproducible test cases using synthetic data

---

## 🧩 Project Structure

The codebase follows a modular and extensible architecture:
```
app/
├─ ui/ # Main UI
├─ collectors/ # Artifact-specific collectors
├─ export/ # Case & file management
├─ integrity/ # Hashing utilities
├─ logging/ # Forensic logging
├─ model/ # Data models
└─ util/ # Helper utilities
```

Each forensic artifact is handled by an independent collector module, enabling easy extension.

---

## 🚀 Future Extensions

Potential future enhancements include:

- Browser history extraction
- Calendar events acquisition
- Media metadata analysis
- Network and Wi-Fi artifacts
- Automated ZIP export of case data
- Cross-device testing (real devices vs emulator)

---

## ⚠️ Disclaimer

This project is developed **strictly for educational and research purposes**.  
It is **not intended for use in real investigations** or on devices without proper authorization.

---

## 👤 Author

GitHub: [saragsil](https://github.com/saragsil)

---

## 📜 License

This project is provided for academic use. Licensing can be defined if required.

