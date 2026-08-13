import { Router, Response } from 'express';
import { z } from 'zod';
import { requireFirebaseAuth, AuthenticatedRequest } from '../middleware/firebaseAuth';
import {
  generatePresignedUploadUrl,
  generatePublicImageUrls,
  generatePrivateDownloadUrl,
} from '../services/storageService';
import { getFirestore } from '../services/firebaseAdmin';

const router = Router();

// Presigned Upload Endpoint
const presignUploadSchema = z.object({
  listingId: z.string().min(1, 'listingId is required'),
  fileCategory: z.enum(['listing_image', 'digital_pdf']),
  contentType: z.string().min(1, 'contentType is required'),
  fileSizeBytes: z.number().positive('fileSizeBytes must be positive'),
});

router.post('/presign-upload', requireFirebaseAuth, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const parseResult = presignUploadSchema.safeParse(req.body);
    if (!parseResult.success) {
      res.status(400).json({ error: 'BAD_REQUEST', details: parseResult.error.format() });
      return;
    }

    const authenticatedUid = req.user!.uid;
    const result = await generatePresignedUploadUrl({
      ...parseResult.data,
      authenticatedUid,
    });

    res.status(200).json(result);
  } catch (error: any) {
    const message = error.message || '';
    if (message.startsWith('LISTING_NOT_FOUND')) {
      res.status(404).json({ error: 'NOT_FOUND', message });
    } else if (message.startsWith('FORBIDDEN')) {
      res.status(403).json({ error: 'FORBIDDEN', message });
    } else if (message.startsWith('INVALID_MIME_TYPE') || message.startsWith('FILE_TOO_LARGE')) {
      res.status(400).json({ error: 'BAD_REQUEST', message });
    } else {
      res.status(500).json({ error: 'SERVER_ERROR', message: 'Failed to generate upload URL.' });
    }
  }
});

// Temporary Signed Public Image URLs Endpoint
const imageUrlsSchema = z.object({
  objectKeys: z.array(z.string()).min(1).max(20),
});

router.post('/image-urls', async (req: AuthenticatedRequest, res: Response) => {
  try {
    const parseResult = imageUrlsSchema.safeParse(req.body);
    if (!parseResult.success) {
      res.status(400).json({ error: 'BAD_REQUEST', details: parseResult.error.format() });
      return;
    }

    const { objectKeys } = parseResult.data;
    const urlMap = await generatePublicImageUrls(objectKeys);
    res.status(200).json({ urls: urlMap });
  } catch (error: any) {
    res.status(400).json({ error: 'BAD_REQUEST', message: error.message });
  }
});

// Private Download URL Endpoint (Entitlement Verified)
const privateDownloadSchema = z.object({
  listingId: z.string().min(1),
  orderId: z.string().min(1),
});

router.post('/private-download-url', requireFirebaseAuth, async (req: AuthenticatedRequest, res: Response) => {
  try {
    const parseResult = privateDownloadSchema.safeParse(req.body);
    if (!parseResult.success) {
      res.status(400).json({ error: 'BAD_REQUEST', details: parseResult.error.format() });
      return;
    }

    const buyerUid = req.user!.uid;
    const { listingId, orderId } = parseResult.data;
    const firestore = getFirestore();

    // 1. Verify entitlement or seller ownership
    const entitlementQuery = await firestore.collection('ebook_entitlements')
      .where('buyerId', '==', buyerUid)
      .where('listingId', '==', listingId)
      .where('orderId', '==', orderId)
      .where('status', '==', 'active')
      .limit(1)
      .get();

    const listingDoc = await firestore.collection('listings').doc(listingId).get();
    const listing = listingDoc.data();
    const isSellerOwner = listing && listing.sellerId === buyerUid;
    const isFreeListing = listing && (listing.isFree === true || listing.pricePaise === 0 || listing.price === 0);

    if (entitlementQuery.empty && !isSellerOwner && !isFreeListing) {
      res.status(403).json({ error: 'FORBIDDEN', message: 'You do not hold an active download entitlement for this item.' });
      return;
    }

    const digitalFileObjectKey = listing?.digitalFilePath;
    if (!digitalFileObjectKey) {
      res.status(404).json({ error: 'NOT_FOUND', message: 'No digital file is associated with this listing.' });
      return;
    }

    const downloadUrl = await generatePrivateDownloadUrl(digitalFileObjectKey);
    res.status(200).json({ downloadUrl, expiresInSeconds: 900 });
  } catch (error: any) {
    res.status(500).json({ error: 'SERVER_ERROR', message: error.message });
  }
});

export default router;
