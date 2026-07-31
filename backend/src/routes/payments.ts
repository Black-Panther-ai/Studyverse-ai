import { Router, Response } from 'express';
import { z } from 'zod';
import { requireFirebaseAuth, AuthenticatedRequest } from '../middleware/firebaseAuth';
import { createOrder, verifyAndFinalizePayment } from '../services/orderService';

const router = Router();

// Create Order Endpoint
const createOrderSchema = z.object({
  listingId: z.string().min(1, 'listingId is required'),
});

router.post('/create-order', requireFirebaseAuth, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const parseResult = createOrderSchema.safeParse(req.body);
    if (!parseResult.success) {
      res.status(400).json({ error: 'BAD_REQUEST', details: parseResult.error.format() });
      return;
    }

    const buyerUid = req.user!.uid;
    const orderResult = await createOrder({
      listingId: parseResult.data.listingId,
      buyerUid,
    });

    res.status(200).json(orderResult);
  } catch (error: any) {
    const message = error.message || '';
    if (message.startsWith('LISTING_NOT_FOUND')) {
      res.status(404).json({ error: 'NOT_FOUND', message });
    } else if (message.startsWith('SELF_PURCHASE_FORBIDDEN') || message.startsWith('LISTING_UNAVAILABLE')) {
      res.status(400).json({ error: 'BAD_REQUEST', message });
    } else if (message.startsWith('RESERVATION_CONFLICT')) {
      res.status(409).json({ error: 'CONFLICT', message });
    } else {
      res.status(500).json({ error: 'SERVER_ERROR', message: 'Failed to create order.' });
    }
  }
});

// Verify Payment Endpoint
const verifyPaymentSchema = z.object({
  internalOrderId: z.string().min(1),
  razorpayOrderId: z.string().min(1),
  razorpayPaymentId: z.string().min(1),
  razorpaySignature: z.string().min(1),
});

router.post('/verify', requireFirebaseAuth, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const parseResult = verifyPaymentSchema.safeParse(req.body);
    if (!parseResult.success) {
      res.status(400).json({ error: 'BAD_REQUEST', details: parseResult.error.format() });
      return;
    }

    const buyerUid = req.user!.uid;
    const verificationResult = await verifyAndFinalizePayment({
      ...parseResult.data,
      buyerUid,
    });

    res.status(200).json(verificationResult);
  } catch (error: any) {
    const message = error.message || '';
    if (message.startsWith('INVALID_SIGNATURE')) {
      res.status(400).json({ error: 'INVALID_SIGNATURE', message });
    } else if (message.startsWith('UNAUTHORIZED')) {
      res.status(403).json({ error: 'FORBIDDEN', message });
    } else if (message.startsWith('ORDER_NOT_FOUND')) {
      res.status(404).json({ error: 'NOT_FOUND', message });
    } else {
      res.status(500).json({ error: 'SERVER_ERROR', message: 'Payment verification failed.' });
    }
  }
});

export default router;
