import { S3Client, PutObjectCommand, GetObjectCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';
import { randomUUID } from 'crypto';
import { env } from '../config/env';
import { getFirestore } from './firebaseAdmin';

const s3Client = new S3Client({
  endpoint: env.S3_ENDPOINT,
  region: env.S3_REGION,
  credentials: {
    accessKeyId: env.S3_ACCESS_KEY_ID || 'dummy_access_key',
    secretAccessKey: env.S3_SECRET_ACCESS_KEY || 'dummy_secret_key',
  },
  forcePathStyle: true,
});

export interface PresignUploadOptions {
  listingId: string;
  fileCategory: 'listing_image' | 'digital_pdf';
  contentType: string;
  fileSizeBytes: number;
  authenticatedUid: string;
}

export async function generatePresignedUploadUrl(options: PresignUploadOptions) {
  const { listingId, fileCategory, contentType, fileSizeBytes, authenticatedUid } = options;

  // 1. Verify listing ownership in Firestore before issuing upload URL
  const firestore = getFirestore();
  const listingDoc = await firestore.collection('listings').doc(listingId).get();

  if (!listingDoc.exists) {
    throw new Error('LISTING_NOT_FOUND: Listing does not exist.');
  }

  const listingData = listingDoc.data();
  if (!listingData || listingData.sellerId !== authenticatedUid) {
    throw new Error('FORBIDDEN: You do not own this listing.');
  }

  // 2. Validate MIME type & file size limits
  let folder = '';
  let extension = '';

  if (fileCategory === 'listing_image') {
    const allowedImageTypes: Record<string, string> = {
      'image/jpeg': 'jpg',
      'image/jpg': 'jpg',
      'image/png': 'png',
      'image/webp': 'webp',
    };

    const ext = allowedImageTypes[contentType.toLowerCase()];
    if (!ext) {
      throw new Error('INVALID_MIME_TYPE: Listing images must be JPEG, PNG, or WebP.');
    }
    if (fileSizeBytes > env.MAX_IMAGE_SIZE_BYTES) {
      throw new Error(`FILE_TOO_LARGE: Image exceeds maximum allowed size of ${env.MAX_IMAGE_SIZE_BYTES} bytes.`);
    }
    folder = 'listing-images';
    extension = ext;
  } else if (fileCategory === 'digital_pdf') {
    if (contentType.toLowerCase() !== 'application/pdf') {
      throw new Error('INVALID_MIME_TYPE: Digital files must be application/pdf.');
    }
    if (fileSizeBytes > env.MAX_PDF_SIZE_BYTES) {
      throw new Error(`FILE_TOO_LARGE: PDF exceeds maximum allowed size of ${env.MAX_PDF_SIZE_BYTES} bytes.`);
    }
    folder = 'digital-files';
    extension = 'pdf';
  } else {
    throw new Error('INVALID_CATEGORY: Unsupported file category.');
  }

  // 3. Generate secure S3 object key
  const uniqueId = randomUUID();
  const objectKey = `${folder}/${authenticatedUid}/${listingId}/${uniqueId}.${extension}`;

  const command = new PutObjectCommand({
    Bucket: env.S3_BUCKET,
    Key: objectKey,
    ContentType: contentType,
  });

  const uploadUrl = await getSignedUrl(s3Client, command, {
    expiresIn: env.PRESIGNED_UPLOAD_EXPIRY_SECONDS,
  });

  return {
    uploadUrl,
    objectKey,
    expiresInSeconds: env.PRESIGNED_UPLOAD_EXPIRY_SECONDS,
    requiredHeaders: {
      'Content-Type': contentType,
    },
  };
}

export async function generatePublicImageUrls(objectKeys: string[]) {
  if (objectKeys.length > 20) {
    throw new Error('BATCH_LIMIT_EXCEEDED: Cannot process more than 20 image URLs at once.');
  }

  const urlMap: Record<string, string> = {};

  for (const key of objectKeys) {
    // Path validation: Reject path traversal and non-listing-images keys
    if (!key.startsWith('listing-images/') || key.includes('..')) {
      continue;
    }

    const command = new GetObjectCommand({
      Bucket: env.S3_BUCKET,
      Key: key,
    });

    const signedUrl = await getSignedUrl(s3Client, command, {
      expiresIn: env.PUBLIC_IMAGE_EXPIRY_SECONDS,
    });

    urlMap[key] = signedUrl;
  }

  return urlMap;
}

export async function generatePrivateDownloadUrl(objectKey: string) {
  if (!objectKey.startsWith('digital-files/') || objectKey.includes('..')) {
    throw new Error('INVALID_OBJECT_KEY: Invalid digital file object key.');
  }

  const command = new GetObjectCommand({
    Bucket: env.S3_BUCKET,
    Key: objectKey,
  });

  return getSignedUrl(s3Client, command, {
    expiresIn: env.PRIVATE_DOWNLOAD_EXPIRY_SECONDS,
  });
}
