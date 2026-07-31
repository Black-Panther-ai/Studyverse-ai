# StudySwap AI Backend Service

Node.js 22 + TypeScript + Express backend service for S3 Storage Presigning, Razorpay Order Creation & Verification, and Firestore Entitlements.

## Railway Setup Instructions

1. Create a new Railway project from GitHub repo.
2. Set service root directory to `/backend`.
3. Create a private Railway S3 Object Storage Bucket.
4. Configure backend environment variables on Railway:
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
5. Generate public Railway domain (e.g. `https://studyswap-backend.up.railway.app`).
6. Set health check path to `/health`.
