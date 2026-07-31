import Razorpay from 'razorpay';
import crypto from 'crypto';
import { env } from '../config/env';

let instance: Razorpay | null = null;

export function getRazorpayInstance(): Razorpay {
  if (!instance) {
    if (!env.RAZORPAY_KEY_ID || !env.RAZORPAY_KEY_SECRET) {
      throw new Error('CONFIG_ERROR: RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET must be configured.');
    }
    instance = new Razorpay({
      key_id: env.RAZORPAY_KEY_ID,
      key_secret: env.RAZORPAY_KEY_SECRET,
    });
  }
  return instance;
}

export function verifyPaymentSignature(
  razorpayOrderId: string,
  razorpayPaymentId: string,
  razorpaySignature: string
): boolean {
  const secret = env.RAZORPAY_KEY_SECRET;
  if (!secret) return false;

  const generatedSignature = crypto
    .createHmac('sha256', secret)
    .update(`${razorpayOrderId}|${razorpayPaymentId}`)
    .digest('hex');

  return generatedSignature === razorpaySignature;
}

export function verifyWebhookSignature(
  rawBody: Buffer | string,
  signature: string
): boolean {
  const webhookSecret = process.env.RAZORPAY_WEBHOOK_SECRET || env.RAZORPAY_WEBHOOK_SECRET;
  if (!webhookSecret || !signature) return false;

  const expectedSignature = crypto
    .createHmac('sha256', webhookSecret)
    .update(rawBody)
    .digest('hex');

  const a = Buffer.from(expectedSignature, 'utf8');
  const b = Buffer.from(signature, 'utf8');

  if (a.length !== b.length) {
    return false;
  }

  return crypto.timingSafeEqual(a, b);
}
