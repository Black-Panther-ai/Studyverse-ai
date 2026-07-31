import { getFirestore } from './firebaseAdmin';
import { getRazorpayInstance, verifyPaymentSignature } from './razorpayService';
import { env } from '../config/env';
import { randomUUID } from 'crypto';

export interface CreateOrderInput {
  listingId: string;
  buyerUid: string;
}

export async function createOrder(input: CreateOrderInput) {
  const { listingId, buyerUid } = input;
  const firestore = getFirestore();

  const RESERVATION_TTL_MS = 15 * 60 * 1000; // 15 minutes
  const now = Date.now();
  const internalOrderId = `ord_${randomUUID()}`;

  let listingTitle = '';
  let amountPaise = 0;
  let listingType = 'physical';

  // 1. Transaction to reserve physical listing and fetch verified price directly from Firestore
  await firestore.runTransaction(async (transaction) => {
    const listingRef = firestore.collection('listings').doc(listingId);
    const listingDoc = await transaction.get(listingRef);

    if (!listingDoc.exists) {
      throw new Error('LISTING_NOT_FOUND: Product or notes listing does not exist.');
    }

    const listing = listingDoc.data()!;
    if (listing.status !== 'active') {
      throw new Error('LISTING_UNAVAILABLE: Listing is no longer active.');
    }

    if (listing.sellerId === buyerUid) {
      throw new Error('SELF_PURCHASE_FORBIDDEN: Sellers cannot purchase their own listings.');
    }

    listingTitle = listing.title || 'Marketplace Item';
    amountPaise = listing.pricePaise || 0;
    listingType = listing.listingType || 'physical';

    // Physical listing payment reservation check
    if (listingType === 'physical') {
      const reservationBuyerId = listing.reservationBuyerId;
      const reservationExpiresAt = listing.reservationExpiresAt || 0;

      if (reservationBuyerId && reservationBuyerId !== buyerUid && reservationExpiresAt > now) {
        throw new Error('RESERVATION_CONFLICT: Another buyer is currently checking out this item. Please try again in a few minutes.');
      }

      // Apply temporary 15-minute reservation
      transaction.update(listingRef, {
        reservationBuyerId: buyerUid,
        reservationExpiresAt: now + RESERVATION_TTL_MS,
        updatedAt: new Date(),
      });
    }
  });

  // 2. Create Razorpay order via SDK
  const razorpay = getRazorpayInstance();
  const razorpayOrder = await razorpay.orders.create({
    amount: amountPaise,
    currency: 'INR',
    receipt: internalOrderId,
    notes: {
      internalOrderId,
      listingId,
      buyerUid,
    },
  });

  // 3. Record pending_payment order document in Firestore
  const orderRef = firestore.collection('orders').doc(internalOrderId);
  await orderRef.set({
    id: internalOrderId,
    buyerId: buyerUid,
    sellerId: (await firestore.collection('listings').doc(listingId).get()).data()?.sellerId || '',
    listingId,
    listingTitleSnapshot: listingTitle,
    listingType,
    amountPaise,
    currency: 'INR',
    status: 'pending_payment',
    razorpayOrderId: razorpayOrder.id,
    razorpayPaymentId: '',
    createdAt: new Date(),
    updatedAt: new Date(),
  });

  return {
    internalOrderId,
    razorpayOrderId: razorpayOrder.id,
    razorpayKeyId: env.RAZORPAY_KEY_ID,
    amountPaise,
    currency: 'INR',
    listingTitle,
  };
}

export interface VerifyPaymentInput {
  internalOrderId: string;
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
  buyerUid: string;
}

export async function verifyAndFinalizePayment(input: VerifyPaymentInput) {
  const { internalOrderId, razorpayOrderId, razorpayPaymentId, razorpaySignature, buyerUid } = input;
  const firestore = getFirestore();

  // 1. HMAC Signature Verification
  const isValidSignature = verifyPaymentSignature(razorpayOrderId, razorpayPaymentId, razorpaySignature);
  if (!isValidSignature) {
    throw new Error('INVALID_SIGNATURE: Razorpay payment signature verification failed.');
  }

  // 2. Transactional Finalization
  return await firestore.runTransaction(async (transaction) => {
    const orderRef = firestore.collection('orders').doc(internalOrderId);
    const orderDoc = await transaction.get(orderRef);

    if (!orderDoc.exists) {
      throw new Error('ORDER_NOT_FOUND: Order document not found.');
    }

    const order = orderDoc.data()!;
    if (order.buyerId !== buyerUid) {
      throw new Error('UNAUTHORIZED: Order does not belong to authenticated user.');
    }

    if (order.razorpayOrderId !== razorpayOrderId) {
      throw new Error('ORDER_MISMATCH: Razorpay order ID mismatch.');
    }

    // Idempotency: If already paid, return success
    if (order.status === 'paid') {
      return { status: 'paid', message: 'Payment already verified and finalized.' };
    }

    const listingRef = firestore.collection('listings').doc(order.listingId);
    const listingDoc = await transaction.get(listingRef);

    if (order.listingType === 'physical') {
      const listing = listingDoc.data();
      if (!listing || listing.status === 'sold') {
        // Physical item was sold to another buyer -> record refund_required state
        transaction.update(orderRef, {
          status: 'refund_required',
          razorpayPaymentId,
          reconciliationReason: 'ITEM_ALREADY_SOLD_BEFORE_FINALIZATION',
          updatedAt: new Date(),
        });
        return { status: 'refund_required', message: 'Item was sold to another buyer. Refund recorded for reconciliation.' };
      }

      // Finalize physical sale
      transaction.update(listingRef, {
        status: 'sold',
        reservationBuyerId: null,
        reservationExpiresAt: null,
        updatedAt: new Date(),
      });
    } else if (order.listingType === 'digital_note' || order.listingType === 'ebook') {
      // Finalize digital purchase & create entitlement
      const entitlementId = `ent_${randomUUID()}`;
      const entitlementRef = firestore.collection('ebook_entitlements').doc(entitlementId);

      const listing = listingDoc.data();
      transaction.set(entitlementRef, {
        id: entitlementId,
        buyerId: buyerUid,
        sellerId: order.sellerId,
        listingId: order.listingId,
        orderId: internalOrderId,
        digitalFileObjectKey: listing?.digitalFilePath || '',
        status: 'active',
        createdAt: new Date(),
      });
    }

    // Update order status to paid
    transaction.update(orderRef, {
      status: 'paid',
      razorpayPaymentId,
      paidAt: new Date(),
      updatedAt: new Date(),
    });

    return { status: 'paid', message: 'Payment verified and order finalized successfully.' };
  });
}
