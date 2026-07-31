# Phase 0.1 Verification Report: Stabilization Verification & Portability Cleanup

**Project**: StudySwap AI — Student Marketplace  
**Phase Completed**: Phase 0.1 (Stabilization Verification & Portability Cleanup)  
**Execution Date**: 2026-07-28  

---

## 1. Actual Build-Version Matrix

The exact actual values retrieved directly from project build configuration files are:

| Parameter | Actual Value | Source File |
| :--- | :--- | :--- |
| **Namespace** | `com.example` | `app/build.gradle.kts` |
| **Application ID** | `com.aistudio.studyswap.ai` | `app/build.gradle.kts` |
| **Min SDK** | `24` | `app/build.gradle.kts` |
| **Compile SDK** | `34` | `app/build.gradle.kts` |
| **Target SDK** | `34` | `app/build.gradle.kts` |
| **Kotlin Version** | `2.0.21` | `gradle/libs.versions.toml` |
| **AGP Version** | `8.7.3` | `gradle/libs.versions.toml` |
| **Gradle Wrapper Version** | `8.12` | `gradle/wrapper/gradle-wrapper.properties` |
| **Build JDK Version** | `17.0.12` (JBR 21 runtime) | Environment (`JAVA_HOME`) |
| **JVM Compatibility Target** | `11` | `app/build.gradle.kts` (`compileOptions` & `kotlinOptions`) |

---

## 2. Package and Firebase Configuration Consistency

* **App Application ID**: `com.aistudio.studyswap.ai`
* **App Namespace**: `com.example`
* **Firebase `google-services.json` Client Registrations**:
  - `client[0].package_name`: `com.aistudio.studyswap.ai`
  - `client[1].package_name`: `com.example`
* **Deep Link Hosts in `AndroidManifest.xml`**:
  - `project-f8915641-9233-48ba-9f0.firebaseapp.com`
  - `project-f8915641-9233-48ba-9f0.web.app`
  - Custom scheme: `studyswap://verify`
* **Classification**: **Valid development configuration** (The Firebase configuration in `google-services.json` supports both `applicationId` `com.aistudio.studyswap.ai` and namespace `com.example` for local development).

---

## 3. Java Configuration Portability

* **Machine-Specific Path Removal**: Removed hardcoded `org.gradle.java.home=C:/Program Files/Android/Android Studio1/jbr` from tracked `gradle.properties`.
* **Portability Strategy**: Builds now rely on `$env:JAVA_HOME` or the developer's Android Studio Gradle JDK settings.
* **Recommended Project JDK**: **JDK 17** (or JetBrains Runtime 17/21 bundled with Android Studio).

---

## 4. Tracked Environment-File Status

* `.env`: **Not tracked** (Excluded via `.gitignore`).
* `local.properties`: **Not tracked** (Excluded via `.gitignore`).
* Keystore files (`*.jks`, `*.keystore`): **Not tracked** (Excluded via `.gitignore`).
* Service account JSONs: **Not tracked** (Excluded via `.gitignore`).
* `.env.example`: **Tracked** (Contains safe `"NOT_CONFIGURED"` string placeholders).

---

## 5. Secret-Scan Classifications

All tracked files were scanned for sensitive terms (`AIza`, `rzp_`, `api_key`, `secret`, `private_key`, `auth_token`, `password`, `salt`, `INSTAMOJO`, `RAZORPAY`).

| Finding Location | Content Summary | Classification |
| :--- | :--- | :--- |
| `app/google-services.json` | `AIzaSyCrX12o_KGrhhXzGU0HBNaIFS6HKGNQmxo` | **Public client configuration** (Protected via GCP package/SHA rules) |
| `.env.example` & `.env` | `GEMINI_API_KEY="NOT_CONFIGURED"` etc. | **Safe placeholder** |
| `InstamojoPaymentHelper.kt` | `val apiKey: String = ""` | **Safe placeholder** |
| `AdminPanelScreen.kt` & `BuyerDashboardScreen.kt` | UI text labels & payment ID field references | **Documentation / UI reference** |

---

## 6. Git-History Exposure Status

* **Git Repository Status**: Directory is currently **not a Git repository** (no `.git` directory initialized).
* **Git History Exposure**: No commit history exists in the current working directory.
* **Owner Action Required**: Prior plain-text Instamojo keys (`c2f8ef74e8ff...`) must be considered compromised and rotated immediately in provider dashboards before creating a remote repository.

---

## 7. Room Migration Decision

* **Current Implementation**: `AppDatabase.kt` includes `.fallbackToDestructiveMigration()`.
* **Verification**: Current database contains mock/development data only.
* **Documentation**: Added explicit code comments in `AppDatabase.kt` documenting that destructive migration is pre-production only.
* **Phase 8 Blocker**: Added an explicit Phase 8 requirement to replace destructive migration with non-destructive `Migration(x, y)` scripts before production release.

---

## 8. Firebase Console Pending Checklist

- [x] **A. Code/Repository Completed**: Client SDK setup, `google-services.json` placement, package matching, and disabled client-side escalation.
- [ ] **B. Firebase Console Pending**:
  - [ ] Register Android app package `com.aistudio.studyswap.ai`.
  - [ ] Register debug/release SHA-1 and SHA-256 certificate fingerprints.
  - [ ] Enable Firebase Authentication -> Email/Password provider.
  - [ ] Configure Authorized Domains (`localhost`, `studyswap-ai.web.app`).
- [ ] **C. Google Cloud Console Pending**:
  - [ ] Restrict API Key `AIza...` to Android package `com.aistudio.studyswap.ai` & SHA-1 fingerprint.
  - [ ] Restrict API Key scope to Firebase Auth, Firestore, and Cloud Storage.
- [ ] **D. Project-Owner Manual Action**:
  - [ ] Provision dedicated development Firebase project (`studyswap-ai-dev`).

---

## 9. Build, Test, and Lint Results

| Command | Exit Code | Result | Notes |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat --version` | `0` | **SUCCESS** | Gradle 8.12, Kotlin 2.0.21, JVM 21 |
| `.\gradlew.bat clean assembleDebug` | `0` | **BUILD SUCCESSFUL** | Generated `app-debug.apk` at `app/build/outputs/apk/debug/app-debug.apk` |
| `.\gradlew.bat testDebugUnitTest` | `0` | **BUILD SUCCESSFUL** | All unit tests passed (including `Phase0SecurityTest`) |
| `.\gradlew.bat lintDebug` | `0` | **BUILD SUCCESSFUL** | Lint report generated at `app/build/reports/lint-results-debug.html` |

---

## 10. Phase 1 Readiness Verdict

### **READY WITH MANUAL FIREBASE SETUP REQUIRED**

* **Preconditions Met**: Code builds cleanly, tests pass, secrets are contained, machine-specific paths are removed, and Room migration boundaries are established.
* **Pending Action Before Phase 1 Auth**: Owner must verify Firebase Console Email/Password provider enablement and SHA-1 fingerprint registration.
