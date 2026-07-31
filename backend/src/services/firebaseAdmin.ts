import * as admin from 'firebase-admin';
import { env } from '../config/env';

let firebaseAdminApp: admin.app.App | null = null;

export function getFirebaseAdmin(): admin.app.App {
  if (firebaseAdminApp) {
    return firebaseAdminApp;
  }

  if (admin.apps.length > 0) {
    firebaseAdminApp = admin.apps[0]!;
    return firebaseAdminApp;
  }

  if (env.FIREBASE_PROJECT_ID && env.FIREBASE_CLIENT_EMAIL && env.FIREBASE_PRIVATE_KEY) {
    firebaseAdminApp = admin.initializeApp({
      credential: admin.credential.cert({
        projectId: env.FIREBASE_PROJECT_ID,
        clientEmail: env.FIREBASE_CLIENT_EMAIL,
        privateKey: env.FIREBASE_PRIVATE_KEY,
      }),
    });
  } else {
    // Fallback initialize for default GCP credentials or testing
    firebaseAdminApp = admin.initializeApp({
      projectId: env.FIREBASE_PROJECT_ID || 'studyswap-ai-dev',
    });
  }

  return firebaseAdminApp;
}

export function getAuth(): admin.auth.Auth {
  return getFirebaseAdmin().auth();
}

export function getFirestore(): admin.firestore.Firestore {
  return getFirebaseAdmin().firestore();
}
