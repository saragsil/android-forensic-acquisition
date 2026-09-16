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

| | |
|---|---|
| **Language** | Kotlin 1.9.24 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Android Gradle Plugin** | 8.7.3 |
| **compileSdk / targetSdk** | 35 |
| **minSdk** | 29 (Android 10) |
| **Test device** | Android Emulator — Pixel 7 Pro |

The emulator is used as a **controlled forensic testbed**, allowing the creation of synthetic data (ground truth) for validation purposes.

---

## 📂 Collected Forensic Artifacts

The application supports the following artifact categories:

- 📇 **Contacts** — `ContactsContract`
- 📞 **Call Logs** — `CallLog.Calls`
- 💬 **SMS Messages** — `Telephony.Sms` (best-effort, subject to Android restrictions)
- 📦 **Installed Applications & Permissions** — `PackageManager`
- 📱 **Device Information** — Android version, model, build fingerprint
- 🌐 **Browser History** — best-effort, subject to provider availability

> Note: SMS and browser history acquisition are implemented on a best-effort basis. Both are limited by Android security restrictions on newer API levels, where the corresponding content providers are restricted or unavailable to non-default applications.

---

## 📤 Output & Export Format

Each acquisition generates a **case-based export directory** (`CASE-<uuid>/<timestamp>/`) containing:

**Artifacts**
- `contacts.json`
- `calls.json`
- `sms.json`
- `apps.json`
- `device.json`
- `browser_history.json`

**Case & integrity metadata**
- `case_meta.json` — case identity and acquisition parameters
- `manifest.json` — acquisition metadata plus per-file `path`, `size` and `sha256`
- `hashes.sha256` — SHA-256 of every file in the case directory
- `chain_of_custody.txt` — append-only, timestamped custody event log
- `logs.txt` — execution and error log

**Packaged evidence**
- `export.zip` — the complete, self-contained evidence package
- `export.zip.sha256` — detached hash of the package

All outputs are stored in the application's external files directory. They can be extracted with `adb pull`, or shared directly from the app via a `FileProvider`.

---

## 🔐 Forensic Methodology

The application follows key digital forensic principles:

- Logical acquisition using **read-only APIs**
- **No rooting, exploitation, or security bypass**
- Explicit permission handling and documentation
- Cryptographic hashing (SHA-256) for integrity verification
- **Chain of custody** recorded as an append-only event log, written *before* packaging so it is included in the sealed archive
- Clear separation between data sources and outputs
- Reproducible test cases using synthetic data

### Integrity verification

The manifest deliberately describes the *inputs* only: `manifest.json`, `hashes.sha256`, `export.zip` and `export.zip.sha256` are excluded from the file list, so the manifest stays stable as packaging artifacts are produced. To verify an extracted case:

```bash
sha256sum -c hashes.sha256
```

---

## 🧩 Project Structure

The codebase follows a modular and extensible architecture:

```
app/src/main/java/com/gamezorck/forensicasq/
├─ ui/          # Compose screens (splash, home, case, settings)
├─ collectors/  # Artifact-specific collectors
├─ export/      # Case management, manifest, ZIP packaging
├─ integrity/   # Hashing and provenance
├─ logging/     # Forensic log and chain of custody
├─ model/       # Data models
└─ util/        # Permissions, sharing, time helpers
```

Each forensic artifact is handled by an independent collector implementing a common `ArtifactCollector` interface, enabling easy extension.

---

## 🚀 Build & Run

```bash
git clone https://github.com/saragsil/android-forensic-acquisition.git
cd android-forensic-acquisition
./gradlew assembleDebug
```

Then install on a running emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and run the `app` configuration. Grant the runtime permissions when prompted — an artifact whose permission is denied is recorded in `manifest.json` as a failed entry carrying the error message, rather than being silently omitted.

---

## 🚀 Future Extensions

Potential future enhancements include:

- Calendar events acquisition
- Media metadata analysis
- Network and Wi-Fi artifacts
- Cross-device testing (real devices vs emulator)
- Detached signing of the export package

---

## ⚠️ Disclaimer

This project is developed **strictly for educational and research purposes**.

It is **not intended for use in real investigations**, and must not be used on any device without the explicit, documented authorization of its owner. The authors accept no liability for misuse.

---

## 👤 Author

GitHub: [saragsil](https://github.com/saragsil)

---

## 📜 License

Licensed under the **Apache License, Version 2.0**. See [LICENSE](LICENSE) for the full text.
