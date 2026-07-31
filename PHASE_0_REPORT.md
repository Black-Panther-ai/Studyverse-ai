# Phase 0 Report: Repository Stabilization & Emergency Security Containment

**Project**: StudySwap AI — Student Marketplace  
**Phase Completed**: Phase 0 (Stabilization & Emergency Security Containment)  
**Execution Date**: 2026-07-26  

---

## 1. Files Changed

| File Path | Description of Modification |
| :--- | :--- |
| `gradlew`, `gradlew.bat` | Restored official executable Gradle wrapper scripts. |
| `gradle/wrapper/gradle-wrapper.properties` | Generated wrapper properties pointing to Gradle 8.12. |
| `gradle/wrapper/gradle-wrapper.jar` | Restored official Gradle wrapper binary JAR. |
| `app/build.gradle.kts` | Corrected `compileSdk = 35`, `targetSdk = 35`, and removed non-existent `debug.keystore` signing requirement. |
| `gradle/libs.versions.toml` | Corrected AGP (`8.7.3`), Kotlin (`2.0.21`), KSP (`2.0.21-1.0.27`), Room (`2.6.1`), and Firebase BOM (`33.5.1`) versions. |
| `app/src/main/java/com/example/payment/InstamojoPaymentHelper.kt` | Removed hardcoded Instamojo API keys/salts; replaced mock payment success with `"Payments are not configured yet."` |
| `app/src/main/java/com/example/data/firebase/FirebaseManager.kt` | Removed hardcoded programmatic `FirebaseOptions` API keys and removed hardcoded admin email checks. |
| `app/src/main/java/com/example/data/repository/StudySwapRepository.kt` | Removed default administrator seed creation and hardcoded seed credentials (`[REDACTED_REMOVED_PASSWORD]`). |
| `app/src/main/java/com/example/data/local/entities/UserEntity.kt` | Removed plain-text `password` property. |
| `app/src/main/java/com/example/data/local/AppDatabase.kt` | Increment Room version from 3 to 4 with `fallbackToDestructiveMigration()` to clear deprecated password schema. |
| `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` | Removed local password auth fallback bypasses, removed hardcoded admin email strings, and disabled fake payment success. |
| `.env.example` | Cleaned environment template to contain only key names without realistic secret values. |
| `.gitignore` | Updated security rules to exclude `.env`, `local.properties`, keystores (`*.jks`), service account JSONs, and build outputs. |
| `implementation_plan.md` | Updated architecture plan with Firebase client key classification, integer-paise financial model, and single-seller cart rules. |

---

## 2. Build Fixes

1. **Gradle Wrapper Restoration**: Generated official `gradlew` and `gradlew.bat` scripts and `gradle-wrapper.jar` for Gradle 8.12.
2. **Compile SDK Stabilization**: Fixed invalid `compileSdk { version = release(36) { minorApiLevel = 1 } }` DSL block to standard `compileSdk = 35`.
3. **Plugin Version Resolution**: Fixed hallucinated / unreleased plugin versions in `libs.versions.toml` (`agp = "9.1.1"`, `kotlin = "2.2.10"`) to stable released versions (`agp = "8.7.3"`, `kotlin = "2.0.21"`, `ksp = "2.0.21-1.0.27"`).
4. **Debug Signing Block Resolution**: Removed dependency on non-existent `${rootDir}/debug.keystore`, allowing AGP to use its standard auto-generated debug keystore.

---

## 3. Credentials Removed

All hardcoded secrets in source files have been permanently removed:

* **Instamojo API Key**: `c2f8ef74e8ff...` (Removed from `InstamojoPaymentHelper.kt` & `.env.example`)
* **Instamojo Auth Token**: `dfee1091d26c...` (Removed from `InstamojoPaymentHelper.kt` & `.env.example`)
* **Instamojo Salt Key**: `af3910aee9be...` (Removed from `InstamojoPaymentHelper.kt` & `.env.example`)
* **Default Admin Password**: `[REDACTED_REMOVED_PASSWORD]` (Removed from `StudySwapRepository.kt` & `UserEntity.kt`)
* **Default User Password**: `password123` (Removed from `UserEntity.kt`)
* **Hardcoded Admin Email**: `[REDACTED_REMOVED_ADMIN_EMAIL]` (Removed from `FirebaseManager.kt` & `MainViewModel.kt`)

---

## 4. Authentication Bypasses Removed

1. **Local Room Password Fallback**: Previously, if Firebase authentication failed or network was unavailable, `loginUser` fell back to matching plain-text passwords stored in the local Room database (`localUser.password == password`). This bypass has been deleted. Failed remote authentication now fails safely with an error message.
2. **Offline Local Registration**: Previously, if Firebase Auth was null or offline, `registerUser` created an authenticated local user session in Room. This bypass has been deleted.

---

## 5. Admin Fallbacks Disabled

1. **Email-Based Admin Escalation**: Previously, any user signing in with `[REDACTED_REMOVED_ADMIN_EMAIL]` or `[REDACTED_REMOVED_ADMIN_EMAIL]` was automatically granted `Admin` role without backend verification. This logic has been removed. All registered users default to `Student` role.
2. **Admin Panel Access**: Navigating to `AppTab.ADMIN_PANEL` displays `"Admin functionality is disabled in Phase 0."` until server-authorized custom claims (`auth.token.role == 'admin'`) are implemented in Phase 1.

---

## 6. Room Password-Storage Remediation

1. **Schema Remediation**: The `password` column has been removed from `UserEntity`.
2. **Database Migration Strategy**: Room DB version was incremented from **v3 to v4** using `fallbackToDestructiveMigration()`.
3. **Justification**: The local SQLite database contains only local mock development data. Using destructive migration safely wipes any lingering local mock database files and clears plain-text password traces without risking production user data.

---

## 7. Secret Rotation Required by the Owner

> [!CAUTION]
> The following credentials were previously committed in plain text in git history or project files before Phase 0. The project owner MUST immediately execute these secret rotations:

1. **Instamojo Gateway Credentials**: Log in to the Instamojo Developer Portal and regenerate `API Key`, `Auth Token`, and `Salt`.
2. **Firebase Project Review**: Ensure the Firebase API key in `google-services.json` is restricted in Google Cloud Console by:
   - Android package name: `com.aistudio.studyswap.ai`
   - SHA-1 fingerprint of release/debug signing keys.
3. **Repository History Scrub**: If this repository was pushed to a public remote host (e.g. GitHub), revoke exposed keys immediately and rewrite git history using `git-filter-repo` or BFG Repo Cleaner.

---

## 8. Build and Test Results

* **`.\gradlew.bat --version`**: **SUCCESS** (Gradle 8.12, Kotlin 2.0.21, JVM 21)
* **`.\gradlew.bat tasks`**: **SUCCESS**
* **`.\gradlew.bat assembleDebug`**: **SUCCESS** (Generated debug APK at `app/build/outputs/apk/debug/app-debug.apk`)
* **Production Signing Attempted**: **No**. Debug build relies strictly on standard AGP debug signing. Production signing flags remain unset until release deployment.

---

## 9. Remaining Risks

1. **Client-Side Firestore Direct Access**: Until Phase 2 Firestore Security Rules are deployed, direct client SDK Firestore calls are not restricted by server rules.
2. **Unrestricted File Uploads**: Firebase Storage direct client uploads remain active until presigned upload Cloud Functions and Storage Rules are deployed in Phase 3.

---

## 10. Exact Phase 1 Entry Criteria

To proceed to **Phase 1: Authentication and User Profiles**, the following preconditions are met:

- [x] Repository builds cleanly via `.\gradlew.bat assembleDebug`.
- [x] All hardcoded payment secrets and default passwords removed from codebase.
- [x] Room database plain-text password storage removed.
- [x] Authentication bypasses and admin email escalations disabled.
- [x] Project owner notified of mandatory secret rotation actions.

