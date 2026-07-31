# Major Implementation Prompt 2 Report: Railway Backend + S3 Storage + Razorpay Payments + Firestore Orders

**Project**: StudySwap AI — Student Marketplace  
**Phase Completed**: Major Implementation Prompt 2  
**Execution Date**: 2026-07-30  
**Final Deployment Verdict**: **READY WITH MANUAL CREDENTIAL SETUP**  

---

## 1. Files Changed

| File Path | Description of Change |
| :--- | :--- |
| `backend/package.json` | Created Node.js 22 backend project configuration (`engines: { node: ">=22" }`), Express, `@aws-sdk/client-s3`, `firebase-admin`, `razorpay`, `zod`, `helmet`, `express-rate-limit`, `cors`, and `vitest`. |
| `backend/tsconfig.json` | Created TypeScript ES2022 CommonJS compilation configuration. |
| `backend/Dockerfile` | Created multi-stage Docker build using `node:22-alpine` base image. |
| `backend/.dockerignore` | Created Docker build context exclusion rules. |
| `backend/.env.example` | Created environment variables template. |
| `backend/README.md` | Created Railway manual setup guide. |
| `backend/src/config/env.ts` | Zod schema validation for environment variables with escaped newline parsing for `FIREBASE_PRIVATE_KEY`. |
| `backend/src/services/firebaseAdmin.ts` | Created Firebase Admin SDK singleton provider. |
| `backend/src/services/storageService.ts` | S3 client service for presigned PUT upload URLs, public image signed URLs, and entitlement-checked private download URLs. Enforced listing ownership check. |
| `backend/src/services/razorpayService.ts` | Razorpay SDK instance and HMAC-SHA256 signature verification for payments and webhooks. |
| `backend/src/services/orderService.ts` | Firestore transactional order creation, temporary 15-minute physical listing payment reservation, self-purchase blocking, and idempotent order finalization. |
| `backend/src/routes/health.ts` | `GET /health` process status and `GET /ready` service readiness endpoints. |
| `backend/src/routes/storage.ts` | Presigned upload, public image signed URLs, and private download URL endpoints. |
| `backend/src/routes/payments.ts` | Order creation (`POST /api/v1/payments/create-order`) and payment verification (`POST /api/v1/payments/verify`) endpoints. |
| `backend/src/routes/razorpayWebhook.ts` | Unauthenticated raw-body webhook handler (`POST /api/v1/webhooks/razorpay`) with HMAC signature verification and `processed_webhooks/{eventId}` idempotency. |
| `backend/src/app.ts` | Mounted raw-body parser before global `express.json()`, helmet security headers, rate limiting, and CORS. |
| `backend/src/server.ts` | Server entry point listening on `process.env.PORT` with graceful shutdown handlers (`SIGTERM`/`SIGINT`). |
| `backend/tests/app.test.ts` | Vitest test suite covering auth, presigning, path traversal, and raw body webhook HMAC verification. |
| `gradle/libs.versions.toml` | Added `razorpay-android = { group = "com.razorpay", name = "checkout", version = "1.6.40" }`. |
| `app/build.gradle.kts` | Integrated `libs.razorpay.android` dependency. |
| `app/src/main/java/com/example/data/repository/RailwayStorageRepository.kt` | Android repository for requesting S3 presigned upload URLs and performing direct HTTP PUT file uploads. |
| `app/src/main/java/com/example/data/repository/RazorpayPaymentRepository.kt` | Android repository calling backend `/create-order` and `/verify` endpoints. |
| `app/src/main/java/com/example/MainActivity.kt` | Integrated Razorpay `PaymentResultListener` and `Checkout.preload()`. |
| `app/src/main/java/com/example/ui/viewmodel/MainViewModel.kt` | Integrated Railway storage repository and Razorpay checkout flows. |
| `firestore.rules` | Updated security rules (v2) denying client write access to `orders`, `ebook_entitlements`, and `processed_webhooks`. |
| `firebase.json` | Updated config with Firestore rules while preserving existing Firebase Hosting setup. |
| `app/src/test/java/com/example/Prompt2UnitTests.kt` | Created Android Robolectric unit tests for presigned upload mapping, order response mapping, paise conversion, and object key path validation. |
| `implementation_plan.md` | Updated roadmap to reflect completion of Major Prompt 2. |
| `task.md` | Marked all prompt execution tasks as completed. |

---

## 2. Final Architecture

```
                                +---------------------------+
                                |    Firebase Auth          |
                                | (Client & Admin SDK Auth) |
                                +-------------+-------------+
                                              |
                                              v
+------------------------+   1. Bearer Token   +---------------------------+
|  Android Client        |-------------------->| Railway Node.js 22        |
|  com.aistudio.         |                     | Express Backend API       |
|  studyswap.ai          |<--------------------|                           |
|                        |   2. Presigned PUT  +-------------+-------------+
|  Razorpay Checkout SDK |    / Signed GET                   |
+-----------+------------+                                   | 4. Admin SDK &
            |                                                |    Transactions
            | 3. Payment                                     v
            v                                  +---------------------------+
+------------------------+   5. Webhooks       | Cloud Firestore           |
| Razorpay Gateway API   |-------------------->| users / listings / orders |
+------------------------+                     | ebook_entitlements        |
                                               +---------------------------+
                                                             ^
                                                             | 6. S3 Storage
                                               +-------------+-------------+
                                               | Railway Private S3        |
                                               | Object Storage Bucket     |
                                               +---------------------------+
```

---

## 3. Backend Endpoints Summary

* `GET /health`: Health status.
* `GET /ready`: Service configuration readiness.
* `POST /api/v1/storage/presign-upload` (Auth Required): Checks listing ownership in Firestore before generating presigned S3 PUT URL.
* `POST /api/v1/storage/image-urls` (Public/Auth Optional): Batch requests temporary signed GET URLs for `listing-images/` keys (rejects path traversal).
* `POST /api/v1/storage/private-download-url` (Auth Required): Verifies active `ebook_entitlements` record in Firestore before returning signed GET download link.
* `POST /api/v1/payments/create-order` (Auth Required): Fetches price directly from Firestore listing, blocks self-purchase, applies temporary 15-minute physical listing payment reservation, creates Razorpay order.
* `POST /api/v1/payments/verify` (Auth Required): Server-side HMAC signature verification, verifies Razorpay order match, performs transactional order finalization.
* `POST /api/v1/webhooks/razorpay` (Unauthenticated Webhook): Raw request body HMAC verification, idempotent tracking via `processed_webhooks/{eventId}`.

---

## 4. Railway S3 Storage Bucket Implementation

* Bucket structure:
  * Images: `listing-images/{sellerUid}/{listingId}/{uuid}.{jpg|png|webp}`
  * Digital PDFs: `digital-files/{sellerUid}/{listingId}/{uuid}.pdf`
* Storage persistence: Firestore records only stable object keys (`imageObjectKeys`, `digitalFileObjectKey`). Expiring signed URLs are generated on-demand and never saved in Firestore.
* Credentials: Kept strictly as backend environment variables (`S3_ENDPOINT`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`). Zero S3 credentials exist in the Android APK.

---

## 5. Firebase ID Token Verification

* Express middleware (`firebaseAuth.ts`) parses `Authorization: Bearer <token>`.
* Verified via `firebase-admin.auth().verifyIdToken(token)`.
* Attaches verified `req.user = { uid, email }`. Rejects missing or invalid tokens with HTTP 401/403. Client-supplied user IDs in request bodies are never trusted for identity.

---

## 6. Firestore Order Schema

* Collection `orders/{internalOrderId}`:
  * `id`: String (`ord_...`)
  * `buyerId`: String (Firebase Auth UID)
  * `sellerId`: String (Firebase Auth UID)
  * `listingId`: String
  * `listingTitleSnapshot`: String
  * `listingType`: `"physical" | "digital_note" | "ebook"`
  * `amountPaise`: Integer Paise
  * `currency`: `"INR"`
  * `status`: `"pending_payment" | "paid" | "payment_failed" | "refund_required" | "cancelled"`
  * `razorpayOrderId`: String
  * `razorpayPaymentId`: String
  * `createdAt`: Server Timestamp
  * `updatedAt`: Server Timestamp
  * `paidAt`: Server Timestamp

* Collection `ebook_entitlements/{entitlementId}`:
  * `id`: String (`ent_...`)
  * `buyerId`: String
  * `sellerId`: String
  * `listingId`: String
  * `orderId`: String
  * `digitalFileObjectKey`: String
  * `status`: `"active"`
  * `createdAt`: Server Timestamp

* Collection `processed_webhooks/{eventId}`:
  * `eventId`: String
  * `eventName`: String
  * `processedAt`: Server Timestamp

---

## 7. Razorpay Create-Order & Finalization Flow

1. **Create Order**:
   * Android calls `POST /api/v1/payments/create-order`.
   * Backend reads `pricePaise` from Firestore listing.
   * If physical listing, backend checks for active reservation (`reservationExpiresAt > now`). If reserved by another buyer, returns HTTP 409 CONFLICT. Otherwise sets temporary 15-minute reservation.
   * Razorpay order created via Node SDK (`amount = pricePaise`, `currency = "INR"`).
   * Backend stores `pending_payment` order record and returns `razorpayOrderId` and `razorpayKeyId`.
2. **Checkout**:
   * Android launches `com.razorpay.Checkout` modal with returned order details.
3. **Verification**:
   * Android receives payment ID and signature callback, then calls `POST /api/v1/payments/verify`.
   * Backend verifies HMAC-SHA256 signature server-side.
   * Transactionally marks physical listing `sold` and clears reservation, OR creates digital `ebook_entitlements` record.
   * If physical listing was sold prior to finalization, order status is updated to `refund_required` for reconciliation.

---

## 8. Signature & Webhook Verification

* Webhook route mounted with `express.raw({ type: "application/json" })` BEFORE global `express.json()`.
* Exact raw request body bytes used to verify HMAC-SHA256 signature against `X-Razorpay-Signature` header.
* Idempotency ensured via `processed_webhooks/{eventId}` lookup.

---

## 9. Android Payment Integration

* Added `com.razorpay:checkout:1.6.40` dependency in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
* `MainActivity.kt` implements `PaymentResultListener` and preloads Checkout SDK (`Checkout.preload(applicationContext)`).
* `RailwayStorageRepository.kt` handles presigned upload requests and direct HTTP PUT uploads.
* `RazorpayPaymentRepository.kt` handles backend order creation and signature verification.

---

## 10. Digital Entitlement Flow

* Digital PDF files stored in private `digital-files/` S3 bucket directory.
* Direct public S3 links are disabled.
* Buyers request private download links via `POST /api/v1/storage/private-download-url`.
* Backend checks for an active record in `ebook_entitlements` matching `buyerId`, `listingId`, and `orderId` before issuing temporary 15-minute signed GET URL.

---

## 11. Security Rules Changes

* [firestore.rules](file:///f:/Downloads/studyswap-ai%20(1)/firestore.rules) updated:
  * Client write access to `orders`, `ebook_entitlements`, and `processed_webhooks` is strictly **DENIED** (`allow write: if false;`). All financial writes are managed exclusively by the backend via Firebase Admin SDK.
  * Buyers can read only their own orders (`resource.data.buyerId == request.auth.uid`) and entitlements (`resource.data.buyerId == request.auth.uid`).
* [firebase.json](file:///f:/Downloads/studyswap-ai%20(1)/firebase.json) updated:
  * Added Firestore configuration while preserving existing Firebase Hosting setup. Removed Firebase Storage references.

---

## 12. Backend Build & Test Results

| Command | Exit Code | Result | Output |
| :--- | :--- | :--- | :--- |
| `npm run build` | `0` | **BUILD SUCCESSFUL** | Clean TypeScript compilation (`tsc`) |
| `npm test` | `0` | **7 PASSED (100%)** | Vitest test suite passed |

---

## 13. Android Build, Test & Lint Results

| Command | Exit Code | Result | Output Location |
| :--- | :--- | :--- | :--- |
| `.\gradlew.bat clean assembleDebug` | `0` | **BUILD SUCCESSFUL** | `app/build/outputs/apk/debug/app-debug.apk` |
| `.\gradlew.bat testDebugUnitTest` | `0` | **BUILD SUCCESSFUL** | All unit tests passed |
| `.\gradlew.bat lintDebug` | `0` | **BUILD SUCCESSFUL** | `app/build/reports/lint-results-debug.html` |

---

## 14. Railway Manual Deployment Checklist

- [ ] Create a new Railway project and connect GitHub repository.
- [ ] Set service root directory to `/backend`.
- [ ] Create a private Railway S3 Object Storage Bucket.
- [ ] Configure environment variables in Railway service settings:
  - `PORT=8080`
  - `NODE_ENV=production`
  - `FIREBASE_PROJECT_ID`
  - `FIREBASE_CLIENT_EMAIL`
  - `FIREBASE_PRIVATE_KEY`
  - `S3_ENDPOINT`
  - `S3_REGION`
  - `S3_BUCKET`
  - `S3_ACCESS_KEY_ID`
  - `S3_SECRET_ACCESS_KEY`
  - `RAZORPAY_KEY_ID`
  - `RAZORPAY_KEY_SECRET`
  - `RAZORPAY_WEBHOOK_SECRET`
- [ ] Generate Railway public domain URL.
- [ ] Set health check path to `/health`.

---

## 15. Firebase Admin Setup Checklist

- [ ] Generate service account JSON key in **Firebase Console -> Project Settings -> Service Accounts**.
- [ ] Copy `project_id`, `client_email`, and `private_key` into Railway environment variables.
- [ ] Deploy updated `firestore.rules` using Firebase CLI (`firebase deploy --only firestore:rules`).

---

## 16. Razorpay Test-Mode Setup Checklist

- [ ] Generate API Key ID & Secret in **Razorpay Dashboard -> Settings -> API Keys**.
- [ ] Add `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` to Railway environment variables.
- [ ] Create Webhook in **Razorpay Dashboard -> Settings -> Webhooks** pointing to `https://<YOUR_RAILWAY_URL>/api/v1/webhooks/razorpay` for events `payment.captured` and `payment.failed`.
- [ ] Copy Webhook secret into `RAZORPAY_WEBHOOK_SECRET` variable.

---

## 17. Known Limitations

* Live deployment requires adding backend URL domain into Android `BuildConfig.BACKEND_BASE_URL` or environment configuration.

---

## 18. Exact Deployment Readiness Verdict

### **READY WITH MANUAL CREDENTIAL SETUP**
