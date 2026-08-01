# Final Android-to-Production Backend Integration Report

**Project**: StudySwap AI — Student Marketplace  
**Application ID**: `com.aistudio.studyswap.ai`  
**Execution Date**: 2026-08-01  
**Integration Status**: **VERIFIED & READY FOR PHYSICAL DEVICE TESTING**  

---

## 1. Exact Files Changed

| File Path | Description of Change |
| :--- | :--- |
| `app/build.gradle.kts` | Configured `buildConfigField("String", "BACKEND_BASE_URL", "\"https://studyverse-ai-production.up.railway.app\"")` as the single centralized source of truth for the production backend URL. Enabled `buildConfig = true`. |
| `app/src/main/java/com/example/util/NetworkConfig.kt` | Created centralized URL normalization utility (`normalizeUrl`) ensuring no duplicate trailing slashes or broken endpoints. |
| `app/src/main/java/com/example/data/repository/RailwayStorageRepository.kt` | Configured default base URL to `NetworkConfig.baseUrl`. Implemented token refresh and single retry on HTTP 401 Unauthorized for protected S3 presigned upload & download requests. |
| `app/src/main/java/com/example/data/repository/RazorpayPaymentRepository.kt` | Configured default base URL to `NetworkConfig.baseUrl`. Implemented token refresh and single retry on HTTP 401 for `/create-order` and `/verify` endpoints. |
| `app/src/main/java/com/example/MainActivity.kt` | Implemented `com.razorpay.PaymentResultWithDataListener` and preloaded Razorpay Checkout SDK (`Checkout.preload()`). |
| `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` | Integrated `initiateRazorpayCheckout()`, `onRazorpayPaymentSuccess()`, and `onRazorpayPaymentError()`. Connected `purchaseDigitalNoteWithInstamojo` wrapper to trigger Razorpay checkout. |
| `app/src/main/java/com/example/ui/screens/NoteDetailScreen.kt` | Updated button text from legacy Instamojo to `"Pay ₹... via Razorpay"`. |
| `app/src/test/java/com/example/Prompt2UnitTests.kt` | Added unit tests covering backend base URL normalization, Authorization header formatting (`Bearer <token>`), create-order response parsing, payment verification parsing, and failed payment assertions. |

---

## 2. Final Backend URL Source of Truth

* **Single Source of Truth**: `BuildConfig.BACKEND_BASE_URL` defined in `app/build.gradle.kts`.
* **Configured Value**: `https://studyverse-ai-production.up.railway.app`
* **Safe Normalization**: Handled by `com.example.util.NetworkConfig.normalizeUrl(url)`, stripping trailing slashes so all API requests format cleanly without double slashes (`https://studyverse-ai-production.up.railway.app/api/v1/...`).

---

## 3. Active Seller Upload Flow

1. Authenticated seller completes product/notes listing form in `UploadModal.kt`.
2. App fetches fresh Firebase Auth ID token (`FirebaseAuth.getInstance().currentUser.getIdToken(false)`).
3. App sends `POST /api/v1/storage/presign-upload` to Railway backend with `listingId`, `fileCategory` (`listing_image` or `digital_pdf`), `contentType`, and `fileSizeBytes`.
4. Railway backend verifies listing existence in Firestore and confirms `sellerId == authenticatedUid` before issuing temporary S3 presigned PUT URL.
5. App uploads file bytes directly to Railway S3 storage using OkHttp `PUT`.
6. App persists **only stable object keys** (`listing-images/{sellerUid}/{listingId}/{uuid}.jpg` or `digital-files/{sellerUid}/{listingId}/{uuid}.pdf`) in Firestore.
7. Temporary presigned URLs are **never saved** in Firestore.

---

## 4. Active Buy Now & Payment Verification Flow

1. Buyer taps **Buy Now** or **Pay via Razorpay** on a physical listing or digital note.
2. App calls backend endpoint `POST /api/v1/payments/create-order` with `{ listingId }`.
3. Backend reads price directly from Firestore listing (`pricePaise`), validates that buyer is not seller, applies temporary 15-minute physical listing payment reservation, and creates Razorpay order via Node SDK.
4. Backend returns `internalOrderId`, `razorpayOrderId`, `razorpayKeyId`, `amountPaise`, and `listingTitle`.
5. App launches `com.razorpay.Checkout` modal with returned `razorpayOrderId` and public `razorpayKeyId`. Razorpay Key Secret remains strictly on the server.
6. Upon payment completion, `MainActivity.onPaymentSuccess` receives payment ID and signature, then calls `POST /api/v1/payments/verify`.
7. Backend verifies HMAC-SHA256 signature server-side, checks payment status with Razorpay, and transactionally updates Firestore order status to `paid` and creates `ebook_entitlements` or marks physical item `sold`.
8. UI updates to `paid` status **only after backend confirmation succeeds**. Failed/cancelled payments remain unpaid. Double taps and duplicate order creations are blocked via checkout state guard (`isCheckoutInProgress`).

---

## 5. Exact Razorpay Webhook Events in Backend Code

Inspection of `backend/src/routes/razorpayWebhook.ts` confirms the exact webhook events implemented:

1. **`payment.captured`**: Reconciles Firestore internal order status to `paid` and sets `paidAt` timestamp if not already finalized.
2. **`payment.failed`**: Reconciles Firestore internal order status to `payment_failed`.

---

## 6. Endpoints Built From Base URL

| HTTP Method | API Endpoint Path | Description | Protected (Bearer Token) |
| :--- | :--- | :--- | :--- |
| `GET` | `/health` | Server process status check | No |
| `GET` | `/ready` | Service readiness check | No |
| `POST` | `/api/v1/storage/presign-upload` | Generates S3 presigned PUT upload URL | **Yes** |
| `POST` | `/api/v1/storage/image-urls` | Batch requests temporary signed GET URLs for listing images | No |
| `POST` | `/api/v1/storage/private-download-url` | Entitlement-checked private PDF download link | **Yes** |
| `POST` | `/api/v1/payments/create-order` | Server-controlled order & Razorpay order creation | **Yes** |
| `POST` | `/api/v1/payments/verify` | Server-side HMAC signature verification & order finalization | **Yes** |

---

## 7. Remaining Mock or Incomplete Functionality

* **None in active production flows**: Hardcoded `http://10.0.2.2:8080` defaults and local fallback bypasses have been removed. Runtime execution relies strictly on live Firebase Auth, Firestore, and the deployed Railway backend.

---

## 8. Build, Test and Lint Results

| Command | Exit Code | Result | Artifact / Report Location |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat clean assembleDebug` | `0` | **BUILD SUCCESSFUL** | `app/build/outputs/apk/debug/app-debug.apk` |
| `.\gradlew.bat testDebugUnitTest` | `0` | **BUILD SUCCESSFUL** | All unit tests passed |
| `.\gradlew.bat lintDebug` | `0` | **BUILD SUCCESSFUL** | `app/build/reports/lint-results-debug.html` |

---

## 9. Exact APK Output Path

`f:\Downloads\studyswap-ai (1)\app\build\outputs\apk\debug\app-debug.apk`

---

## 10. Physical-Device Testing Checklist

- [ ] Install `app-debug.apk` on a physical Android device (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
- [ ] Test registration & login with real Firebase Auth credentials.
- [ ] Test image upload in seller modal (`UploadModal`) and verify images render via Railway S3 signed URLs.
- [ ] Test digital PDF upload and verify object key format in Firestore (`digital-files/...`).
- [ ] Test **Buy Now** on a physical product with Razorpay test/live card/UPI. Verify backend `/create-order` and `/verify` endpoints succeed.
- [ ] Test digital note purchase and verify private PDF download link generation after entitlement check.
- [ ] Test payment cancellation or failure on Razorpay modal and confirm UI handles cancellation cleanly without marking order paid.
