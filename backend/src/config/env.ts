import dotenv from 'dotenv';
import { z } from 'zod';

dotenv.config();

const envSchema = z.object({
  PORT: z.string().default('8080').transform((val) => parseInt(val, 10)),
  NODE_ENV: z.enum(['development', 'production', 'test']).default('development'),

  FIREBASE_PROJECT_ID: z.string().default(''),
  FIREBASE_CLIENT_EMAIL: z.string().default(''),
  FIREBASE_PRIVATE_KEY: z.string().default(''),

  S3_ENDPOINT: z.string().default('https://storage.railway.app'),
  S3_REGION: z.string().default('us-east-1'),
  S3_BUCKET: z.string().default(''),
  S3_ACCESS_KEY_ID: z.string().default(''),
  S3_SECRET_ACCESS_KEY: z.string().default(''),

  RAZORPAY_KEY_ID: z.string().default(''),
  RAZORPAY_KEY_SECRET: z.string().default(''),
  RAZORPAY_WEBHOOK_SECRET: z.string().default(''),
  GEMINI_API_KEY: z.string().default(''),
  GEMINI_PRIMARY_MODEL: z.string().default('gemini-3.6-flash'),
  GEMINI_FALLBACK_MODEL: z.string().default('gemini-3.5-flash'),
  GEMINI_LITE_MODEL: z.string().default('gemini-3.1-flash-lite'),

  PRESIGNED_UPLOAD_EXPIRY_SECONDS: z.string().default('900').transform((val) => parseInt(val, 10)),
  PUBLIC_IMAGE_EXPIRY_SECONDS: z.string().default('3600').transform((val) => parseInt(val, 10)),
  PRIVATE_DOWNLOAD_EXPIRY_SECONDS: z.string().default('900').transform((val) => parseInt(val, 10)),
  MAX_IMAGE_SIZE_BYTES: z.string().default('10485760').transform((val) => parseInt(val, 10)),
  MAX_PDF_SIZE_BYTES: z.string().default('52428800').transform((val) => parseInt(val, 10)),
});

const parsed = envSchema.safeParse(process.env);

if (!parsed.success) {
  console.error('Invalid environment variables configuration:', parsed.error.format());
}

const parsedData = parsed.success ? parsed.data : envSchema.parse({});

// Mandatory Production Validation Check
if (parsedData.NODE_ENV === 'production') {
  const requiredProdKeys = [
    'RAZORPAY_KEY_ID',
    'RAZORPAY_KEY_SECRET',
    'RAZORPAY_WEBHOOK_SECRET',
    'FIREBASE_PROJECT_ID',
    'FIREBASE_CLIENT_EMAIL',
    'FIREBASE_PRIVATE_KEY',
    'S3_ENDPOINT',
    'S3_REGION',
    'S3_BUCKET',
    'S3_ACCESS_KEY_ID',
    'S3_SECRET_ACCESS_KEY',
  ] as const;

  const missing = requiredProdKeys.filter((key) => !process.env[key] || process.env[key] === '');
  if (missing.length > 0) {
    throw new Error(`PRODUCTION_CONFIG_ERROR: Missing required production environment variables: ${missing.join(', ')}`);
  }
}

const rawPrivateKey = process.env.FIREBASE_PRIVATE_KEY || '';
const formattedPrivateKey = rawPrivateKey.replace(/\\n/g, '\n');

export const env = {
  ...parsedData,
  FIREBASE_PRIVATE_KEY: formattedPrivateKey,
};
