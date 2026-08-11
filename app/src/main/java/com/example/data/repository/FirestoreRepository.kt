package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Listing
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository {

    private val TAG = "FirestoreRepository"

    // USER PROFILE OPERATIONS
    fun getUserProfileFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        val firestore = FirebaseManager.firestore
        if (firestore == null || uid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching user profile snapshot: ${error.message}")
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    trySend(profile)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun saveUserProfile(profile: UserProfile): Boolean {
        val firestore = FirebaseManager.firestore ?: return false
        return try {
            val userMap = mapOf(
                "uid" to profile.uid,
                "email" to profile.email,
                "displayName" to profile.displayName,
                "phone" to profile.phone,
                "collegeName" to profile.collegeName,
                "course" to profile.course,
                "semester" to profile.semester,
                "canBuy" to profile.canBuy,
                "canSell" to profile.canSell,
                "accountStatus" to profile.accountStatus,
                "profileImageUrl" to profile.profileImageUrl,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(profile.uid).set(userMap).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user profile: ${e.message}")
            false
        }
    }

    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Boolean {
        val firestore = FirebaseManager.firestore ?: return false
        return try {
            val mutableUpdates = updates.toMutableMap()
            mutableUpdates["updatedAt"] = FieldValue.serverTimestamp()
            firestore.collection("users").document(uid).update(mutableUpdates).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user profile: ${e.message}")
            false
        }
    }

    // MARKETPLACE LISTING OPERATIONS
    fun getActiveApprovedListingsFlow(): Flow<List<Listing>> = callbackFlow {
        val firestore = FirebaseManager.firestore
        if (firestore == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("listings")
            .whereEqualTo("status", "active")
            .whereEqualTo("isApproved", true)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching active listings snapshot: ${error.message}")
                    // Fallback query without orderBy if index is building
                    firestore.collection("listings")
                        .whereEqualTo("status", "active")
                        .whereEqualTo("isApproved", true)
                        .addSnapshotListener { snap2, err2 ->
                            if (snap2 != null) {
                                val listings = snap2.documents.mapNotNull { doc ->
                                    doc.toObject(Listing::class.java)?.copy(id = doc.id)
                                }
                                trySend(listings)
                            } else {
                                trySend(emptyList())
                            }
                        }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listings = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java)?.copy(id = doc.id)
                    }
                    trySend(listings)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    fun getListingsBySellerFlow(sellerId: String): Flow<List<Listing>> = callbackFlow {
        val firestore = FirebaseManager.firestore
        if (firestore == null || sellerId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = firestore.collection("listings")
            .whereEqualTo("sellerId", sellerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error fetching seller listings snapshot: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val listings = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Listing::class.java)?.copy(id = doc.id)
                    }
                    trySend(listings)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun createListing(listing: Listing): Result<String> {
        val auth = FirebaseManager.auth ?: return Result.failure(Exception("Auth unavailable"))
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("LISTING_FORENSIC", "[LISTING_FORENSIC] auth user exists: false")
            throw Exception("LISTING_AUTH_USER_MISSING")
        }

        Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] auth user exists: true")
        Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] authUid: ${currentUser.uid}")
        Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] isAnonymous: ${currentUser.isAnonymous}")
        Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] email presence: ${!currentUser.email.isNullOrBlank()}")

        // Assertions
        if (listing.id.isBlank()) {
            val errMsg = "Assertion failed: listing.id is blank"
            Log.e("LISTING_FORENSIC", errMsg)
            throw IllegalArgumentException(errMsg)
        }
        if (listing.sellerId != currentUser.uid) {
            val errMsg = "Assertion failed: listing.sellerId (${listing.sellerId}) != currentUser.uid (${currentUser.uid})"
            Log.e("LISTING_FORENSIC", errMsg)
            throw IllegalArgumentException(errMsg)
        }
        if (listing.isApproved) {
            val errMsg = "Assertion failed: listing.isApproved is true, expected false"
            Log.e("LISTING_FORENSIC", errMsg)
            throw IllegalArgumentException(errMsg)
        }

        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        val docRef = firestore.collection("listings").document(listing.id)
        val exactPath = docRef.path

        return try {
            val app = com.google.firebase.FirebaseApp.getInstance()
            val projectId = app.options.projectId ?: "unknown"
            val applicationId = app.options.applicationId ?: "unknown"
            val appName = app.name

            val listingMap = mapOf(
                "id" to listing.id,
                "sellerId" to currentUser.uid,
                "sellerDisplayName" to listing.sellerDisplayName,
                "categoryId" to listing.categoryId,
                "title" to listing.title,
                "description" to listing.description,
                "pricePaise" to listing.pricePaise,
                "originalPricePaise" to listing.originalPricePaise,
                "listingType" to listing.listingType,
                "condition" to listing.condition,
                "collegeName" to listing.collegeName,
                "city" to listing.city,
                "imageUrls" to listing.imageUrls,
                "digitalFilePath" to listing.digitalFilePath,
                "status" to listing.status,
                "isApproved" to listing.isApproved,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] FIRESTORE_CREATE_START")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] projectId=$projectId")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] applicationId=$applicationId")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] authUid=${currentUser.uid}")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] sellerId=${listing.sellerId}")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] documentId=${listing.id}")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] isApproved=${listing.isApproved}")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] path=/$exactPath")
            Log.d("LISTING_FORENSIC", "[LISTING_FORENSIC] fieldNames=${listingMap.keys.joinToString(", ")}")

            try {
                docRef.set(listingMap).await()
            } catch (e: Exception) {
                Log.e("LISTING_FORENSIC", "[LISTING_FORENSIC] FIRESTORE_CREATE_FAILED")
                Log.e("LISTING_FORENSIC", "[LISTING_FORENSIC] exceptionClass=${e.javaClass.name}")
                val firestoreCode = if (e is FirebaseFirestoreException) {
                    e.code.name
                } else {
                    "NONE"
                }
                Log.e("LISTING_FORENSIC", "[LISTING_FORENSIC] firestoreCode=$firestoreCode")
                Log.e("LISTING_FORENSIC", "[LISTING_FORENSIC] message=${e.message}")
                Log.e("LISTING_FORENSIC", "[LISTING_FORENSIC] cause=${e.cause?.javaClass?.name}: ${e.cause?.message}")
                Log.e("LISTING_FORENSIC", "[LISTING_FORENSIC] stackTrace=${Log.getStackTraceString(e)}")

                try {
                    val crashlytics = FirebaseCrashlytics.getInstance()
                    crashlytics.setCustomKey("listing_stage", "FIRESTORE_CREATE")
                    crashlytics.setCustomKey("firebase_project_id", projectId)
                    crashlytics.setCustomKey("firebase_auth_uid", currentUser.uid)
                    crashlytics.setCustomKey("listing_seller_id", listing.sellerId)
                    crashlytics.setCustomKey("listing_document_id", listing.id)
                    crashlytics.setCustomKey("listing_is_approved", listing.isApproved)
                    crashlytics.setCustomKey("firestore_error_code", firestoreCode)
                    crashlytics.setCustomKey("exception_class", e.javaClass.name)
                    crashlytics.recordException(e)
                } catch (crashEx: Throwable) {
                    Log.e("LISTING_FORENSIC", "Crashlytics exception recording failed: ${crashEx.message}")
                }

                return Result.failure(e)
            }

            try {
                // Also set seller capability on user profile
                firestore.collection("users").document(currentUser.uid)
                    .update(mapOf("canSell" to true, "updatedAt" to FieldValue.serverTimestamp()))
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update user canSell profile: ${e.message}")
                return Result.failure(Exception("LISTING_SELLER_PROFILE_UPDATE_FAILED: ${e.localizedMessage ?: e.message}"))
            }

            Result.success(listing.id)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create listing: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateListing(listing: Listing): Result<Boolean> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        val auth = FirebaseManager.auth ?: return Result.failure(Exception("Auth unavailable"))
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))

        if (listing.sellerId != currentUser.uid) {
            return Result.failure(Exception("Unauthorized listing edit"))
        }

        return try {
            val updates = mapOf(
                "title" to listing.title,
                "description" to listing.description,
                "categoryId" to listing.categoryId,
                "pricePaise" to listing.pricePaise,
                "originalPricePaise" to listing.originalPricePaise,
                "condition" to listing.condition,
                "collegeName" to listing.collegeName,
                "city" to listing.city,
                "imageUrls" to listing.imageUrls,
                "digitalFilePath" to listing.digitalFilePath,
                "status" to listing.status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("listings").document(listing.id).update(updates).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update listing: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun markListingRemoved(listingId: String): Result<Boolean> {
        val firestore = FirebaseManager.firestore ?: return Result.failure(Exception("Firestore unavailable"))
        val auth = FirebaseManager.auth ?: return Result.failure(Exception("Auth unavailable"))
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))

        return try {
            val doc = firestore.collection("listings").document(listingId).get().await()
            val sellerId = doc.getString("sellerId")
            if (sellerId != currentUser.uid) {
                return Result.failure(Exception("Unauthorized listing removal"))
            }

            firestore.collection("listings").document(listingId).update(
                mapOf(
                    "status" to "removed",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove listing: ${e.message}")
            Result.failure(e)
        }
    }

    // FIREBASE STORAGE UPLOADS
    suspend fun uploadListingImage(
        sellerUid: String,
        listingId: String,
        fileUri: Uri,
        mimeType: String = "image/jpeg"
    ): Result<String> {
        val storage = FirebaseManager.storage ?: return Result.failure(Exception("Storage unavailable"))
        val auth = FirebaseManager.auth ?: return Result.failure(Exception("Auth unavailable"))
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))

        if (sellerUid != currentUser.uid) {
            return Result.failure(Exception("Unauthorized upload path"))
        }

        // Validate image file type
        val allowedTypes = listOf("image/jpeg", "image/png", "image/webp", "image/jpg")
        val cleanMime = mimeType.lowercase()
        if (cleanMime !in allowedTypes && !cleanMime.startsWith("image/")) {
            return Result.failure(Exception("Invalid image format. Allowed formats: JPEG, PNG, WebP."))
        }

        val extension = when {
            cleanMime.contains("png") -> "png"
            cleanMime.contains("webp") -> "webp"
            else -> "jpg"
        }

        val uniqueFileName = "${UUID.randomUUID()}.$extension"
        val storagePath = "listing-images/$sellerUid/$listingId/$uniqueFileName"

        return try {
            val ref = storage.reference.child(storagePath)
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType(if (cleanMime.startsWith("image/")) cleanMime else "image/jpeg")
                .build()
            ref.putFile(fileUri, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload listing image: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun uploadDigitalFile(
        sellerUid: String,
        listingId: String,
        fileUri: Uri,
        fileName: String
    ): Result<String> {
        val storage = FirebaseManager.storage ?: return Result.failure(Exception("Storage unavailable"))
        val auth = FirebaseManager.auth ?: return Result.failure(Exception("Auth unavailable"))
        val currentUser = auth.currentUser ?: return Result.failure(Exception("Not authenticated"))

        if (sellerUid != currentUser.uid) {
            return Result.failure(Exception("Unauthorized upload path"))
        }

        // Validate PDF type
        if (!fileName.endsWith(".pdf", ignoreCase = true)) {
            return Result.failure(Exception("Invalid file format. Digital notes/e-books must be PDF files."))
        }

        val uniqueFileName = "${UUID.randomUUID()}.pdf"
        val storagePath = "digital-files/$sellerUid/$listingId/$uniqueFileName"

        return try {
            val ref = storage.reference.child(storagePath)
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("application/pdf")
                .build()
            ref.putFile(fileUri, metadata).await()
            // Store storagePath as digitalFilePath (private, not public download URL)
            Result.success(storagePath)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload digital file: ${e.message}")
            Result.failure(e)
        }
    }
}
