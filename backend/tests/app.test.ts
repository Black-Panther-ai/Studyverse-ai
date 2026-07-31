import { describe, it, expect } from 'vitest';
import crypto from 'crypto';

process.env.RAZORPAY_WEBHOOK_SECRET = 'test_webhook_secret_key';

import request from 'supertest';
import app from '../src/app';

describe('StudySwap AI Backend API Test Suite', () => {

  it('GET /health returns 200 UP status', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('UP');
  });

  it('GET /ready returns 200 READY status', async () => {
    const res = await request(app).get('/ready');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('READY');
  });

  it('POST /api/v1/storage/presign-upload rejects request missing Authorization header', async () => {
    const res = await request(app)
      .post('/api/v1/storage/presign-upload')
      .send({
        listingId: 'lst_123',
        fileCategory: 'listing_image',
        contentType: 'image/jpeg',
        fileSizeBytes: 500000,
      });

    expect(res.status).toBe(401);
    expect(res.body.error).toBe('UNAUTHORIZED');
  });

  it('POST /api/v1/storage/presign-upload rejects invalid bearer token', async () => {
    const res = await request(app)
      .post('/api/v1/storage/presign-upload')
      .set('Authorization', 'Bearer invalid_token_xyz')
      .send({
        listingId: 'lst_123',
        fileCategory: 'listing_image',
        contentType: 'image/jpeg',
        fileSizeBytes: 500000,
      });

    expect(res.status).toBe(401);
    expect(res.body.error).toBe('UNAUTHORIZED');
  });

  it('POST /api/v1/storage/image-urls rejects digital-files path traversal', async () => {
    const res = await request(app)
      .post('/api/v1/storage/image-urls')
      .send({
        objectKeys: [
          'digital-files/user1/lst1/file.pdf',
          'listing-images/../../etc/passwd',
        ],
      });

    expect(res.status).toBe(200);
    expect(res.body.urls).toEqual({});
  });

  it('POST /api/v1/webhooks/razorpay rejects invalid HMAC signature on raw body', async () => {
    const payload = JSON.stringify({
      event: 'payment.captured',
      event_id: 'evt_test_123',
      payload: {
        payment: {
          entity: {
            id: 'pay_123',
            amount: 4900,
          },
        },
      },
    });

    const res = await request(app)
      .post('/api/v1/webhooks/razorpay')
      .set('Content-Type', 'application/json')
      .set('X-Razorpay-Signature', 'invalid_signature_hex')
      .send(payload);

    expect(res.status).toBe(400);
    expect(res.body.error).toBe('INVALID_SIGNATURE');
  });

  it('POST /api/v1/webhooks/razorpay accepts valid raw body HMAC signature (Mandatory Correction 3 Test)', async () => {
    const secret = 'test_webhook_secret_key';
    const payloadString = JSON.stringify({
      event: 'payment.captured',
      event_id: 'evt_test_valid_999',
      payload: {
        payment: {
          entity: {
            id: 'pay_valid_999',
            amount: 4900,
            notes: { internalOrderId: 'ord_mock_123' },
          },
        },
      },
    });

    const validSignature = crypto
      .createHmac('sha256', secret)
      .update(payloadString)
      .digest('hex');

    const res = await request(app)
      .post('/api/v1/webhooks/razorpay')
      .set('Content-Type', 'application/json')
      .set('X-Razorpay-Signature', validSignature)
      .send(payloadString);

    expect(res.status).toBe(200);
    expect(res.body.status).toBeDefined();
  });
});
