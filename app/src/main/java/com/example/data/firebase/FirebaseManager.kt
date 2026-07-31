package com.example.data.firebase

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.local.entities.NoteEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.UserEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
                Log.d(TAG, "Firebase initialized via default configuration")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization error: ${e.message}")
        }
    }

    val auth: FirebaseAuth? get() = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    val firestore: FirebaseFirestore? get() = try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    val storage: FirebaseStorage? get() = try { FirebaseStorage.getInstance() } catch (e: Exception) { null }

    fun getActionCodeSettings(): ActionCodeSettings {
        return ActionCodeSettings.newBuilder()
            .setUrl("https://project-f8915641-9233-48ba-9f0.firebaseapp.com/__/auth/action")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.aistudio.studyswap.ai",
                true,
                "21"
            )
            .build()
    }

    suspend fun updateUserVerificationInFirestore(uid: String, isVerified: Boolean) {
        try {
            val updates = mapOf<String, Any>(
                "isEmailVerified" to isVerified,
                "emailVerified" to isVerified
            )
            firestore?.collection("users")?.document(uid)?.update(updates)?.await()
            Log.d(TAG, "Updated verification status for $uid: $isVerified")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user verification in Firestore: ${e.message}")
        }
    }

    suspend fun saveUserToFirebase(user: UserEntity) {
        try {
            val role = "student"
            val userMap = mapOf(
                "id" to user.id,
                "uid" to user.id,
                "name" to user.name,
                "email" to user.email,
                "role" to role,
                "userType" to "Student",
                "collegeName" to user.collegeName,
                "course" to user.course,
                "semester" to user.semester,
                "state" to user.state,
                "phoneWhatsApp" to user.phoneWhatsApp,
                "createdDate" to user.createdDate
            )
            firestore?.collection("users")?.document(user.id)?.set(userMap)?.await()
            Log.d(TAG, "User saved to Firestore: ${user.id} with role=$role")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user to Firestore: ${e.message}")
        }
    }

    suspend fun saveNoteToFirebase(note: NoteEntity) {
        try {
            firestore?.collection("notes")?.document(note.id)?.set(note)?.await()
            Log.d(TAG, "Note saved to Firestore: ${note.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save note to Firestore: ${e.message}")
        }
    }

    suspend fun saveProductToFirebase(product: ProductEntity) {
        try {
            firestore?.collection("products")?.document(product.id)?.set(product)?.await()
            Log.d(TAG, "Product saved to Firestore: ${product.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save product to Firestore: ${e.message}")
        }
    }

    suspend fun saveOrderToFirebase(order: OrderEntity) {
        try {
            firestore?.collection("orders")?.document(order.id)?.set(order)?.await()
            Log.d(TAG, "Order saved to Firestore: ${order.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save order to Firestore: ${e.message}")
        }
    }

    suspend fun uploadFileToStorage(fileUri: Uri, destinationPath: String): String? {
        return try {
            val storageRef = storage?.reference?.child(destinationPath)
            if (storageRef != null) {
                val uploadTask = storageRef.putFile(fileUri).await()
                val downloadUrl = uploadTask.storage.downloadUrl.await()
                downloadUrl.toString()
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload file to Firebase Storage: ${e.message}")
            null
        }
    }
}
