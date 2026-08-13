package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiStudyAssistant
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.model.Listing
import com.example.data.model.UserProfile
import com.example.data.repository.RailwayStorageRepository
import com.example.data.repository.RazorpayPaymentRepository
import com.example.data.repository.StudySwapRepository
import com.example.util.AndroidDownloadManagerHelper
import com.example.util.ProductionDiagnostics
import com.example.payment.InstamojoPaymentHelper
import com.example.payment.InstamojoResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab {
    HOME,
    CATEGORIES,
    MARKETPLACE,
    FREE_NOTES,
    DIGITAL_STORE,
    SELL,
    BUYER_DASHBOARD,
    ORDERS,
    WISHLIST,
    SAVED_ITEMS,
    SELLER_DASHBOARD,
    ADMIN_PANEL,
    PROFILE,
    SETTINGS,
    NOTIFICATIONS,
    HELP_SUPPORT,
    PRIVACY_POLICY,
    TERMS_CONDITIONS,
    AI_ASSISTANT
}

enum class DashboardSyncState { IDLE, LOADING, SUCCESS, ERROR }

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = StudySwapRepository(db)
    val railwayStorageRepository = RailwayStorageRepository()
    val razorpayPaymentRepository = RazorpayPaymentRepository()
    private val prefs = application.getSharedPreferences("studyswap_prefs", Context.MODE_PRIVATE)

    // Sync State Tracking
    private val _dashboardSyncState = MutableStateFlow(DashboardSyncState.IDLE)
    val dashboardSyncState: StateFlow<DashboardSyncState> = _dashboardSyncState.asStateFlow()

    private val _syncedOrders = MutableStateFlow<List<OrderEntity>>(emptyList())
    val syncedOrders: StateFlow<List<OrderEntity>> = _syncedOrders.asStateFlow()

    // Download State Tracking
    val downloadedPdfFile = MutableStateFlow<java.io.File?>(null)
    val isDownloadingPdf = MutableStateFlow(false)

    // Razorpay State Tracking
    var pendingInternalOrderId: String? = null
    var pendingRazorpayOrderId: String? = null

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Auth State
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getUserById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isLoggedIn: StateFlow<Boolean> = currentUser.map { it != null && !it.isDisabled }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Firestore Live Listings Flow
    val activeListings: StateFlow<List<Listing>> = repository.firestoreRepository.getActiveApprovedListingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Data Flows from Repository
    val allUsers: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val freeNotes: StateFlow<List<NoteEntity>> = repository.freeNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val digitalNotesStore: StateFlow<List<NoteEntity>> = repository.digitalNotesStore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProducts: StateFlow<List<ProductEntity>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrders: StateFlow<List<OrderEntity>> = repository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val successfulPayments: StateFlow<List<PaymentEntity>> = repository.successfulPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val failedPayments: StateFlow<List<PaymentEntity>> = repository.failedPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReports: StateFlow<List<ReportEntity>> = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myOrders: StateFlow<List<OrderEntity>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getOrdersByBuyer(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myPayments: StateFlow<List<PaymentEntity>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getPaymentsByBuyer(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val myBookmarks: StateFlow<List<BookmarkEntity>> = _currentUserId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getBookmarksByUser(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiHistory: StateFlow<List<AiHistoryEntity>> = repository.aiHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filter States
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")
    val selectedCourse = MutableStateFlow("All Courses")
    val selectedSemester = MutableStateFlow("All Semesters")
    val selectedCollege = MutableStateFlow("All Colleges")
    val selectedState = MutableStateFlow("All States")
    val sortBy = MutableStateFlow("Newest")

    // Selected Detail Items
    val selectedNote = MutableStateFlow<NoteEntity?>(null)
    val selectedProduct = MutableStateFlow<ProductEntity?>(null)

    var lastSelectedPdfUriForDiag = ""
    var lastSelectedPdfNameForDiag = ""

    // Upload Progress & State
    val uploadProgress = MutableStateFlow(0f)
    val isUploading = MutableStateFlow(false)
    val uploadStatusText = MutableStateFlow<String?>(null)

    // AI Study Assistant States
    val aiPromptInput = MutableStateFlow("")
    val aiResponseOutput = MutableStateFlow("")
    val isAiLoading = MutableStateFlow(false)
    val activeAiTool = MutableStateFlow("Explain")

    // UI Toast Message
    val uiMessage = MutableStateFlow<String?>(null)

    // State for tracking email verification notice
    val unverifiedEmail = MutableStateFlow<String?>(null)
    val unverifiedPassword = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfNeeded()
            
            // Check Firebase persistent session
            val auth = FirebaseManager.auth
            val firebaseUser = auth?.currentUser
            if (firebaseUser != null) {
                val uid = firebaseUser.uid
                val email = firebaseUser.email ?: ""
                var localUser = repository.getUserById(uid).firstOrNull() ?: repository.getUserByEmail(email)
                val role = "Student"
                
                if (localUser == null) {
                    localUser = UserEntity(
                        id = uid,
                        name = firebaseUser.displayName ?: email.substringBefore("@"),
                        email = email,
                        userType = role,
                        role = role,
                        createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                    repository.insertUser(localUser)
                }
                
                if (!localUser.isDisabled) {
                    _currentUserId.value = localUser.id
                    prefs.edit().putString("LOGGED_IN_USER_ID", localUser.id).apply()
                } else {
                    auth.signOut()
                    _currentUserId.value = null
                    prefs.edit().remove("LOGGED_IN_USER_ID").apply()
                }
            } else {
                val savedUserId = prefs.getString("LOGGED_IN_USER_ID", null)
                if (!savedUserId.isNullOrBlank()) {
                    _currentUserId.value = savedUserId
                }
            }

            // Sync live Firestore listings to Room read cache
            viewModelScope.launch {
                activeListings.collect { listings ->
                    if (listings.isNotEmpty()) {
                        repository.syncListingsToRoomCache(listings)
                    }
                }
            }

            // Sync live purchases automatically on user change / app start / login
            viewModelScope.launch {
                _currentUserId.collect { uid ->
                    if (!uid.isNullOrBlank()) {
                        syncPurchasesFromFirestore()
                    } else {
                        _syncedOrders.value = emptyList()
                        _dashboardSyncState.value = DashboardSyncState.IDLE
                    }
                }
            }
            observeNetworkConnectivity(application)
        }
    }

    fun switchTab(tab: AppTab) {
        if (tab == AppTab.ADMIN_PANEL) {
            val user = currentUser.value
            val isAdmin = user?.role?.lowercase() == "admin"
            if (!isAdmin) {
                uiMessage.value = "Admin functionality is disabled in Phase 0."
                _currentTab.value = AppTab.HOME
                return
            }
        }
        _currentTab.value = tab
    }

    fun clearUiMessage() {
        uiMessage.value = null
    }

    // AUTHENTICATION LOGIC
    fun loginUser(emailInput: String, passwordInput: String, onResult: (Boolean) -> Unit) {
        val email = emailInput.trim()
        val password = passwordInput.trim()

        if (email.isEmpty() || password.isEmpty()) {
            uiMessage.value = "Please fill in email and password."
            onResult(false)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            uiMessage.value = "Please enter a valid email address."
            onResult(false)
            return
        }

        viewModelScope.launch {
            val auth = FirebaseManager.auth
            if (auth == null) {
                uiMessage.value = "Firebase Authentication is not available."
                onResult(false)
                return@launch
            }

            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val fbUser = authResult.user
                if (fbUser != null) {
                    val uid = fbUser.uid
                    val roleStr = "Student"

                    var user = repository.getUserById(uid).firstOrNull() ?: repository.getUserByEmail(email)
                    if (user == null) {
                        user = UserEntity(
                            id = uid,
                            name = fbUser.displayName ?: email.substringBefore("@"),
                            email = email,
                            userType = roleStr,
                            role = roleStr,
                            createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                        repository.insertUser(user)
                    }

                    if (user.isDisabled) {
                        auth.signOut()
                        uiMessage.value = "Account is disabled. Please contact Admin."
                        onResult(false)
                        return@launch
                    }

                    _currentUserId.value = user.id
                    prefs.edit().putString("LOGGED_IN_USER_ID", user.id).apply()
                    unverifiedEmail.value = null
                    unverifiedPassword.value = null
                    uiMessage.value = "Welcome back, ${user.name}!"
                    onResult(true)
                } else {
                    uiMessage.value = "Authentication failed."
                    onResult(false)
                }
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                uiMessage.value = "Invalid email or password. Please check your credentials."
                onResult(false)
            } catch (e: FirebaseAuthInvalidUserException) {
                uiMessage.value = "No account found with this email. Please register."
                onResult(false)
            } catch (e: FirebaseNetworkException) {
                uiMessage.value = "Network error. Please check your internet connection."
                onResult(false)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Login error: ${e.message}")
                uiMessage.value = "Login failed: ${e.localizedMessage ?: e.message}"
                onResult(false)
            }
        }
    }

    fun registerUser(
        name: String,
        email: String,
        password: String,
        confirmPassword: String = password,
        college: String = "",
        course: String = "",
        semester: String = "",
        state: String = "",
        phone: String = "",
        profilePic: String = "",
        onResult: (Boolean) -> Unit
    ) {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirmPassword = confirmPassword.trim()

        if (trimmedName.isBlank() || trimmedEmail.isBlank() || trimmedPassword.isBlank()) {
            uiMessage.value = "Full Name, Email, and Password are required."
            onResult(false)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            uiMessage.value = "Please enter a valid email address."
            onResult(false)
            return
        }

        if (trimmedPassword.length < 6) {
            uiMessage.value = "Password must be at least 6 characters."
            onResult(false)
            return
        }

        if (trimmedPassword != trimmedConfirmPassword) {
            uiMessage.value = "Passwords do not match."
            onResult(false)
            return
        }

        viewModelScope.launch {
            val auth = FirebaseManager.auth
            if (auth == null) {
                uiMessage.value = "Firebase Authentication is not available."
                onResult(false)
                return@launch
            }

            try {
                val authResult = auth.createUserWithEmailAndPassword(trimmedEmail, trimmedPassword).await()
                val fbUser = authResult.user
                if (fbUser != null) {
                    val uid = fbUser.uid

                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()
                        fbUser.updateProfile(profileUpdates).await()
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Failed to set display name: ${e.message}")
                    }

                    // Save User Profile document to Firestore
                    val userProfile = UserProfile(
                        uid = uid,
                        email = trimmedEmail,
                        displayName = trimmedName,
                        phone = phone.trim(),
                        collegeName = college.trim(),
                        course = course.trim(),
                        semester = semester.trim(),
                        canBuy = true,
                        canSell = false,
                        accountStatus = "active",
                        profileImageUrl = profilePic.trim()
                    )
                    repository.firestoreRepository.saveUserProfile(userProfile)

                    // Save local user cache
                    val newUser = UserEntity(
                        id = uid,
                        name = trimmedName,
                        email = trimmedEmail,
                        userType = "Student",
                        role = "Student",
                        collegeName = college.trim().ifEmpty { "College Campus" },
                        course = course.trim().ifEmpty { "General Studies" },
                        semester = semester.trim().ifEmpty { "Semester 1" },
                        state = state.trim().ifEmpty { "Delhi" },
                        phoneWhatsApp = phone.trim().ifEmpty { "+919876543210" },
                        profilePicture = profilePic.trim(),
                        createdDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                    repository.insertUser(newUser)

                    _currentUserId.value = uid
                    prefs.edit().putString("LOGGED_IN_USER_ID", uid).apply()

                    uiMessage.value = "Account created successfully! Welcome to StudySwap AI."
                    onResult(true)
                    return@launch
                }

                uiMessage.value = "Registration failed."
                onResult(false)
            } catch (e: FirebaseAuthUserCollisionException) {
                uiMessage.value = "An account with this email already exists. Please sign in."
                onResult(false)
            } catch (e: FirebaseAuthWeakPasswordException) {
                uiMessage.value = "Password is too weak. Use at least 6 characters with numbers or symbols."
                onResult(false)
            } catch (e: FirebaseNetworkException) {
                uiMessage.value = "Network error. Please check your internet connection."
                onResult(false)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Registration exception: ${e.message}")
                uiMessage.value = "Registration failed: ${e.localizedMessage ?: e.message}"
                onResult(false)
            }
        }
    }

    fun resendVerificationEmail(email: String? = null, password: String? = null) {
        val targetEmail = (email ?: unverifiedEmail.value ?: "").trim()
        if (targetEmail.isBlank()) {
            uiMessage.value = "Please enter your email address."
            return
        }

        viewModelScope.launch {
            val auth = FirebaseManager.auth ?: return@launch
            try {
                auth.currentUser?.sendEmailVerification()?.await()
                uiMessage.value = "Verification email sent to $targetEmail. Check your inbox."
            } catch (e: Exception) {
                uiMessage.value = "Failed to send verification email: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun handleDeepLink(uri: Uri?) {
        if (uri == null) return
        Log.d("MainViewModel", "Handling deep link URI: $uri")
    }

    fun logout() {
        try {
            FirebaseManager.auth?.signOut()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Logout error: ${e.message}")
        }
        prefs.edit().remove("LOGGED_IN_USER_ID").apply()
        _currentUserId.value = null
        unverifiedEmail.value = null
        unverifiedPassword.value = null
        switchTab(AppTab.HOME)
        uiMessage.value = "Signed out successfully."
    }

    fun sendPasswordReset(email: String, onResult: (Boolean) -> Unit = {}) {
        val trimmedEmail = email.trim()
        Log.d("PasswordReset", "Attempting password reset dispatch for email: '$trimmedEmail'")

        if (trimmedEmail.isBlank()) {
            uiMessage.value = "Please enter your registered email address."
            onResult(false)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            uiMessage.value = "Invalid email format. Please enter a valid email address."
            onResult(false)
            return
        }

        viewModelScope.launch {
            val auth = FirebaseManager.auth
            if (auth != null) {
                try {
                    val actionCodeSettings = FirebaseManager.getActionCodeSettings()
                    Log.d("PasswordReset", "Sending reset email with ActionCodeSettings for $trimmedEmail")
                    auth.sendPasswordResetEmail(trimmedEmail, actionCodeSettings).await()
                    Log.d("PasswordReset", "Password reset email successfully sent to $trimmedEmail")
                    uiMessage.value = "Password reset email sent to $trimmedEmail. Check your inbox."
                    onResult(true)
                } catch (e: FirebaseAuthInvalidUserException) {
                    Log.e("PasswordReset", "User not found exception for $trimmedEmail: ${e.message}")
                    uiMessage.value = "No account found with $trimmedEmail. Please check your email or register."
                    onResult(false)
                } catch (e: FirebaseNetworkException) {
                    Log.e("PasswordReset", "Network exception for $trimmedEmail: ${e.message}")
                    uiMessage.value = "Network error. Please check your internet connection and try again."
                    onResult(false)
                } catch (e: Exception) {
                    Log.w("PasswordReset", "ActionCodeSettings reset failed (${e.javaClass.simpleName}): ${e.message}. Attempting standard reset fallback.")
                    try {
                        auth.sendPasswordResetEmail(trimmedEmail).await()
                        Log.d("PasswordReset", "Fallback reset email successfully sent to $trimmedEmail")
                        uiMessage.value = "Password reset email sent to $trimmedEmail. Check your inbox."
                        onResult(true)
                    } catch (fallbackEx: Exception) {
                        Log.e("PasswordReset", "Fallback reset exception: ${fallbackEx.message}", fallbackEx)
                        uiMessage.value = "Failed to send reset email: ${fallbackEx.localizedMessage ?: fallbackEx.message}"
                        onResult(false)
                    }
                }
            } else {
                Log.e("PasswordReset", "FirebaseAuth instance is null in FirebaseManager!")
                uiMessage.value = "Authentication service unavailable."
                onResult(false)
            }
        }
    }

    // USER PROFILE UPDATE
    fun updateUserProfile(
        name: String,
        college: String,
        course: String,
        semester: String,
        state: String,
        whatsapp: String
    ) {
        val uid = _currentUserId.value ?: return
        viewModelScope.launch {
            val updates = mapOf(
                "displayName" to name.trim(),
                "collegeName" to college.trim(),
                "course" to course.trim(),
                "semester" to semester.trim(),
                "phone" to whatsapp.trim()
            )
            repository.firestoreRepository.updateUserProfile(uid, updates)

            val user = currentUser.value
            if (user != null) {
                val updated = user.copy(
                    name = name.trim(),
                    collegeName = college.trim(),
                    course = course.trim(),
                    semester = semester.trim(),
                    state = state.trim(),
                    phoneWhatsApp = whatsapp.trim()
                )
                repository.updateUser(updated)
            }
            uiMessage.value = "Profile updated successfully!"
        }
    }

    // BOOKMARKS & INTERACTION HELPERS
    fun toggleBookmark(itemId: String, title: String, type: String, category: String, price: Double) {
        val uid = _currentUserId.value
        if (uid == null) {
            uiMessage.value = "Please sign in to bookmark items."
            return
        }
        viewModelScope.launch {
            repository.toggleBookmark(uid, itemId, title, type, category, price)
            uiMessage.value = "Wishlist updated!"
        }
    }

    fun downloadFreeNote(note: NoteEntity, context: Context? = null) {
        val user = currentUser.value
        Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] START")
        Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] listingId=${note.id}")
        Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] authenticatedUid=${user?.id ?: "null"}")
        
        val url = note.pdfUriOrUrl
        val urlType = if (url.startsWith("http://") || url.startsWith("https://")) {
            "http_url"
        } else if (url.startsWith("content://") || url.startsWith("file://")) {
            "local_uri"
        } else if (url.isBlank()) {
            "blank"
        } else {
            "s3_object_key"
        }
        Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] source type=$urlType")

        if (user == null) {
            ProductionDiagnostics.logError("DOWNLOAD_AUTH_FAILED: User is not authenticated.")
            uiMessage.value = "Please sign in to download notes."
            return
        }

        if (context == null) {
            Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] FAILED: Context is null")
            return
        }

        val downloadId = "download_${user.id}_${note.id}"
        val timestamp = System.currentTimeMillis()

        viewModelScope.launch {
            isDownloadingPdf.value = true
            uiMessage.value = "Downloading ${note.title}..."

            val onDownloadSuccess = { file: java.io.File ->
                Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] local file path=${file.absolutePath}")
                Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] fileExists=${file.exists()}")
                Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] fileSize=${file.length()}")
                Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] canRead=${file.canRead()}")
                
                Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] FILE_VERIFIED")
                uiMessage.value = "Downloaded ${note.title} to Downloads folder!"
                
                // 1. Log download history to Firestore
                Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] HISTORY_WRITE_START")
                viewModelScope.launch {
                    try {
                        val db = FirebaseManager.firestore
                        if (db != null) {
                            val downloadMap = mapOf(
                                "id" to downloadId,
                                "userId" to user.id,
                                "noteId" to note.id,
                                "noteTitle" to note.title,
                                "authorName" to note.authorName,
                                "timestamp" to timestamp,
                                "pdfUriOrUrl" to note.pdfUriOrUrl
                            )
                            db.collection("download_history").document(downloadId).set(downloadMap).await()
                            Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] HISTORY_WRITE_SUCCESS")
                        } else {
                            Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] HISTORY_WRITE_FAILED")
                            Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] exceptionMessage=Firestore is null")
                        }
                    } catch (e: Exception) {
                        val firestoreCode = if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
                            e.code.name
                        } else {
                            "NONE"
                        }
                        Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] HISTORY_WRITE_FAILED")
                        Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] firestoreCode=$firestoreCode")
                        Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] exceptionMessage=${e.message}")
                    }
                }

                // 2. Insert order entity in Room immediately for instant dashboard refresh
                Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] ROOM_INSERT_START")
                viewModelScope.launch {
                    try {
                        val localOrder = OrderEntity(
                            id = downloadId,
                            buyerId = user.id,
                            buyerName = user.name,
                            sellerId = note.authorId,
                            itemId = note.id,
                            itemTitle = note.title,
                            itemType = "DIGITAL_NOTE",
                            price = 0.0,
                            status = "COMPLETED",
                            paymentId = "FREE",
                            timestamp = timestamp,
                            watermarkedDownloadUrl = note.pdfUriOrUrl
                        )
                        repository.insertOrders(listOf(localOrder))
                        Log.d("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] ROOM_INSERT_SUCCESS")
                        
                        // Clean-sync cache with Firestore
                        syncPurchasesFromFirestore()
                    } catch (roomEx: Exception) {
                        Log.e("DOWNLOAD_FORENSIC", "Room insert failed: ${roomEx.message}")
                    }
                }
            }

            try {
                val fileName = "${note.title.replace(" ", "_")}.pdf"
                if (urlType == "s3_object_key") {
                    uiMessage.value = "Requesting download URL from S3..."
                    val signedUrl = railwayStorageRepository.requestPrivateDownloadUrl(note.id, downloadId)
                    
                    uiMessage.value = "Starting secure download..."
                    com.example.util.AndroidDownloadManagerHelper.downloadPdfWithManager(
                        context = context,
                        downloadUrl = signedUrl,
                        title = note.title,
                        fileName = fileName,
                        onComplete = { file ->
                            isDownloadingPdf.value = false
                            if (file != null && file.exists() && file.length() > 0 && file.canRead()) {
                                downloadedPdfFile.value = file
                                onDownloadSuccess(file)
                            } else {
                                Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] S3 Download complete but file verification failed.")
                                uiMessage.value = "Download failed."
                            }
                        }
                    )
                } else if (urlType == "http_url") {
                    com.example.util.AndroidDownloadManagerHelper.downloadPdfWithManager(
                        context = context,
                        downloadUrl = url,
                        title = note.title,
                        fileName = fileName,
                        onComplete = { file ->
                            isDownloadingPdf.value = false
                            if (file != null && file.exists() && file.length() > 0 && file.canRead()) {
                                downloadedPdfFile.value = file
                                onDownloadSuccess(file)
                            } else {
                                Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] HTTP Download complete but file verification failed.")
                                uiMessage.value = "Download failed."
                            }
                        }
                    )
                } else {
                    val pdfFile = com.example.util.PdfDownloadHelper.generateAndSavePdf(
                        context = context,
                        noteTitle = note.title,
                        buyerName = user.name,
                        orderId = "FREE_${timestamp.toString().takeLast(6)}",
                        authorName = note.authorName
                    )
                    isDownloadingPdf.value = false
                    if (pdfFile != null && pdfFile.exists() && pdfFile.length() > 0 && pdfFile.canRead()) {
                        downloadedPdfFile.value = pdfFile
                        onDownloadSuccess(pdfFile)
                    } else {
                        val existStatus = pdfFile?.exists() ?: false
                        val len = pdfFile?.length() ?: 0L
                        val readStatus = pdfFile?.canRead() ?: false
                        Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] FILE_VERIFIED_FAILED: exists=$existStatus, size=$len, readable=$readStatus")
                        uiMessage.value = "Download failed for ${note.title}."
                    }
                }
            } catch (e: Exception) {
                Log.e("DOWNLOAD_FORENSIC", "[DOWNLOAD_FORENSIC] Exception during download chain: ${e.message}", e)
                uiMessage.value = "Download Failed: ${e.localizedMessage ?: e.message}"
                isDownloadingPdf.value = false
            }
        }
    }

    private var isCheckoutInProgress = false

    fun initiateRazorpayCheckout(activity: android.app.Activity, listingId: String) {
        val uid = _currentUserId.value
        if (uid == null) {
            uiMessage.value = "Please sign in to make a purchase."
            return
        }

        if (isCheckoutInProgress) return
        isCheckoutInProgress = true

        viewModelScope.launch {
            try {
                uiMessage.value = "Initializing order with backend..."
                val response = razorpayPaymentRepository.createOrder(listingId)
                pendingInternalOrderId = response.internalOrderId
                pendingRazorpayOrderId = response.razorpayOrderId

                val checkout = com.razorpay.Checkout()
                checkout.setKeyID(response.razorpayKeyId)

                val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: ""

                val options = org.json.JSONObject().apply {
                    put("name", "StudySwap AI")
                    put("description", response.listingTitle)
                    put("order_id", response.razorpayOrderId)
                    put("amount", response.amountPaise)
                    put("currency", response.currency)
                    put("prefill", org.json.JSONObject().apply {
                        put("email", userEmail)
                    })
                }

                checkout.open(activity, options)
            } catch (e: Exception) {
                uiMessage.value = e.message ?: "Failed to initiate payment."
            } finally {
                isCheckoutInProgress = false
            }
        }
    }

    fun onRazorpayPaymentSuccess(razorpayPaymentId: String, razorpaySignature: String = "") {
        val internalOrderId = pendingInternalOrderId
        val razorpayOrderId = pendingRazorpayOrderId

        if (internalOrderId.isNullOrBlank() || razorpayOrderId.isNullOrBlank()) {
            uiMessage.value = "Payment failed: Order session mismatch."
            return
        }

        viewModelScope.launch {
            try {
                uiMessage.value = "Verifying payment with server..."
                val result = razorpayPaymentRepository.verifyPayment(
                    internalOrderId = internalOrderId,
                    razorpayOrderId = razorpayOrderId,
                    razorpayPaymentId = razorpayPaymentId,
                    razorpaySignature = razorpaySignature
                )
                if (result.status == "paid") {
                    uiMessage.value = "Payment Successful! Order verified."
                    syncPurchasesFromFirestore()
                } else {
                    uiMessage.value = "Payment Status: ${result.message}"
                }
            } catch (e: Exception) {
                uiMessage.value = e.message ?: "Payment verification failed."
            } finally {
                pendingInternalOrderId = null
                pendingRazorpayOrderId = null
            }
        }
    }

    fun onRazorpayPaymentError(code: Int, response: String?) {
        pendingInternalOrderId = null
        pendingRazorpayOrderId = null
        uiMessage.value = "Payment Cancelled or Failed: ${response ?: "Error code $code"}"
    }

    fun purchaseDigitalNote(note: NoteEntity) {
        uiMessage.value = "Order placement initialized for ${note.title}"
    }

    fun purchaseDigitalNoteWithInstamojo(context: Context? = null, note: NoteEntity? = null, simulateSuccess: Boolean = false) {
        val activity = context as? android.app.Activity
        if (activity != null && note != null) {
            initiateRazorpayCheckout(activity, note.id)
        } else if (note != null) {
            uiMessage.value = "Order placement initialized for ${note.title}"
        } else {
            uiMessage.value = "Please log in to purchase digital notes."
        }
    }

    fun markProductAsSold(product: ProductEntity) {
        viewModelScope.launch {
            repository.firestoreRepository.markListingRemoved(product.id)
            repository.deleteProduct(product)
            uiMessage.value = "Product marked as sold!"
        }
    }

    // ADMIN & SELLER DELETIONS
    fun toggleDisableUser(user: UserEntity) {
        viewModelScope.launch {
            val updated = user.copy(isDisabled = !user.isDisabled)
            repository.updateUser(updated)
            uiMessage.value = "User status updated."
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
            uiMessage.value = "User removed."
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            val uid = _currentUserId.value ?: ""
            repository.firestoreRepository.markListingRemoved(product.id)
            repository.deleteProduct(product)
            uiMessage.value = "Listing deleted."
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            val uid = _currentUserId.value ?: ""
            repository.firestoreRepository.markListingRemoved(note.id)
            repository.deleteNote(note)
            uiMessage.value = "Note deleted."
        }
    }

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            repository.deleteReport(report)
            uiMessage.value = "Report dismissed."
        }
    }

    // MARKETPLACE & NOTES CREATION WITH FILE UPLOADS
    fun createPhysicalProduct(
        title: String,
        description: String,
        category: String,
        condition: String,
        price: Double,
        pickupLocation: String,
        city: String,
        state: String,
        mobileNumber: String,
        whatsappNumber: String,
        photoUrlsList: List<String>
    ) {
        val user = currentUser.value
        if (user == null) {
            uiMessage.value = "Please sign in to list items."
            return
        }

        viewModelScope.launch {
            isUploading.value = true
            uploadProgress.value = 0.2f
            uploadStatusText.value = "Creating product listing..."

            val pricePaise = Listing.rupeesToPaise(price)
            val tempId = "prod_${System.currentTimeMillis()}"

            // Upload photos if local URIs provided
            val uploadedUrls = mutableListOf<String>()
            val localUris = photoUrlsList.filter { it.startsWith("content://") || it.startsWith("file://") }

            if (localUris.isNotEmpty()) {
                uploadStatusText.value = "Uploading images to Firebase Storage..."
                localUris.forEachIndexed { idx, uriString ->
                    val uri = Uri.parse(uriString)
                    val result = repository.firestoreRepository.uploadListingImage(
                        sellerUid = user.id,
                        listingId = tempId,
                        fileUri = uri
                    )
                    result.getOrNull()?.let { uploadedUrls.add(it) }
                    uploadProgress.value = 0.2f + (0.6f * (idx + 1) / localUris.size)
                }
            }

            val webUrls = photoUrlsList.filter { !it.startsWith("content://") && !it.startsWith("file://") && it.isNotBlank() }
            val finalPhotoUrls = (uploadedUrls + webUrls).ifEmpty {
                listOf("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500")
            }

            val listing = Listing(
                id = tempId,
                sellerId = user.id,
                sellerDisplayName = user.name,
                categoryId = category,
                title = title,
                description = description,
                pricePaise = pricePaise,
                originalPricePaise = (pricePaise * 1.3).toLong(),
                listingType = "physical",
                condition = condition,
                collegeName = user.collegeName.ifEmpty { "Campus" },
                city = city.ifEmpty { "New Delhi" },
                imageUrls = finalPhotoUrls,
                status = "active",
                isApproved = false
            )

            val createResult = repository.firestoreRepository.createListing(listing)
            if (createResult.isSuccess) {
                val product = ProductEntity(
                    id = createResult.getOrDefault(tempId),
                    title = title,
                    description = description,
                    category = category,
                    condition = condition,
                    price = price,
                    originalPrice = price * 1.3,
                    pickupLocation = pickupLocation.ifEmpty { "Hostel Campus Gate" },
                    city = city.ifEmpty { "New Delhi" },
                    state = state.ifEmpty { user.state },
                    college = user.collegeName,
                    photoUrls = finalPhotoUrls.joinToString(","),
                    sellerId = user.id,
                    sellerName = user.name,
                    sellerMobile = mobileNumber.ifEmpty { user.phoneWhatsApp },
                    sellerWhatsApp = whatsappNumber.ifEmpty { user.phoneWhatsApp }
                )
                repository.insertProduct(product)
                uploadProgress.value = 1.0f
                isUploading.value = false
                uploadStatusText.value = "Product published successfully!"
                uiMessage.value = "Listing published on Physical Marketplace!"
            } else {
                isUploading.value = false
                uiMessage.value = "Failed to create listing: ${createResult.exceptionOrNull()?.message}"
            }
        }
    }

    private fun getFileInfoFromUri(context: Context, uri: Uri): Pair<Long, String> {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val size = if (sizeIndex != -1) it.getLong(sizeIndex) else 0L
                    val name = if (nameIndex != -1) it.getString(nameIndex) ?: "file.pdf" else "file.pdf"
                    Pair(size, name)
                } else Pair(0L, "file.pdf")
            } ?: Pair(0L, "file.pdf")
        } catch (e: Exception) {
            Pair(0L, "file.pdf")
        }
    }

    fun createHandwrittenNotes(
        context: Context,
        title: String,
        description: String,
        subject: String,
        semester: String,
        course: String,
        college: String,
        tags: String,
        isFree: Boolean,
        price: Double,
        pdfUri: String,
        pdfFileName: String,
        previewImagesList: List<String>,
        copyrightDeclared: Boolean
    ) {
        ProductionDiagnostics.log("[LISTING] START")
        ProductionDiagnostics.log("[LISTING] AUTH_CHECK: START")
        val user = currentUser.value
        if (user == null) {
            ProductionDiagnostics.logError("LISTING_AUTH_FAILED: User is not authenticated.")
            ProductionDiagnostics.setCustomKey("operation", "listing_auth")
            uiMessage.value = "Upload failed. Please try again."
            return
        }
        ProductionDiagnostics.log("[LISTING] AUTH_CHECK: SUCCESS")

        if (!copyrightDeclared) {
            uiMessage.value = "Mandatory: You must declare copyright ownership!"
            return
        }

        if (pdfUri.isBlank()) {
            uiMessage.value = "Please select a valid PDF file to upload."
            return
        }

        viewModelScope.launch {
            isUploading.value = true
            uploadProgress.value = 0.1f
            uploadStatusText.value = "Initializing note draft..."

            val tempId = "note_${System.currentTimeMillis()}"
            val pricePaise = if (isFree) 0L else Listing.rupeesToPaise(price)

            // 1. Create draft listing in Firestore (isApproved = false as per contract)
            val draftListing = Listing(
                id = tempId,
                sellerId = user.id,
                sellerDisplayName = user.name,
                categoryId = subject.ifEmpty { "Digital Notes" },
                title = title,
                description = description,
                pricePaise = pricePaise,
                originalPricePaise = (pricePaise * 1.3).toLong(),
                listingType = "digital_note",
                condition = "New",
                collegeName = college.ifEmpty { user.collegeName },
                city = "New Delhi",
                imageUrls = emptyList(),
                digitalFilePath = "",
                status = "draft",
                isApproved = false
            )

            ProductionDiagnostics.log("[LISTING] LISTING_DRAFT_CREATE: START (id: $tempId)")
            val draftResult = repository.firestoreRepository.createListing(draftListing)
            if (draftResult.isFailure) {
                isUploading.value = false
                val err = draftResult.exceptionOrNull()?.message ?: "Failed to initialize listing draft."
                
                val diagnosticCode = if (err.contains("LISTING_SELLER_PROFILE_UPDATE_FAILED")) {
                    "LISTING_SELLER_PROFILE_UPDATE_FAILED"
                } else {
                    "LISTING_FIRESTORE_CREATE_FAILED"
                }
                
                ProductionDiagnostics.logError("[LISTING] $diagnosticCode: FAILURE ($err)")
                
                ProductionDiagnostics.setCustomKey("operation", "listing_draft_create")
                ProductionDiagnostics.setCustomKey("listingId", tempId)
                ProductionDiagnostics.setCustomKey("uid", user.id)
                ProductionDiagnostics.setCustomKey("exceptionType", draftResult.exceptionOrNull()?.javaClass?.name ?: "Unknown")
                ProductionDiagnostics.setCustomKey("firebaseErrorCode", err)

                uiMessage.value = "Upload failed. Please try again. ($diagnosticCode)"
                return@launch
            }
            ProductionDiagnostics.log("[LISTING] LISTING_DRAFT_CREATE: SUCCESS")
            ProductionDiagnostics.log("[LISTING] SELLER_PROFILE_UPDATE: SUCCESS")

            var digitalFilePath = ""
            try {
                if (pdfUri.startsWith("content://") || pdfUri.startsWith("file://")) {
                    ProductionDiagnostics.log("[LISTING] PDF_UPLOAD: START")
                    val uri = Uri.parse(pdfUri)
                    val (fileSize, _) = getFileInfoFromUri(context, uri)
                    val safeSize = if (fileSize > 0) fileSize else 1024L * 1024L
                    val fileMime = context.contentResolver.getType(uri) ?: "application/pdf"

                    Log.d("RAILWAY_STORAGE_FORENSIC", "--- RAILWAY_STORAGE_FORENSIC START ---")
                    Log.d("RAILWAY_STORAGE_FORENSIC", "Authenticated UID: ${user.id}")
                    Log.d("RAILWAY_STORAGE_FORENSIC", "Source URI Scheme: ${uri.scheme}")
                    Log.d("RAILWAY_STORAGE_FORENSIC", "MIME Type: $fileMime")
                    Log.d("RAILWAY_STORAGE_FORENSIC", "Filename: $pdfFileName")
                    Log.d("RAILWAY_STORAGE_FORENSIC", "File Size: $fileSize")

                    Log.d("RAILWAY_STORAGE_FORENSIC", "Upload Request: Calling presign-upload endpoint...")
                    val presignedResponse = railwayStorageRepository.requestPresignedUploadUrl(
                        listingId = tempId,
                        fileCategory = "digital_pdf",
                        contentType = "application/pdf",
                        fileSizeBytes = safeSize
                    )
                    Log.d("RAILWAY_STORAGE_FORENSIC", "Presign-upload SUCCESS. Object Key: ${presignedResponse.objectKey}")

                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Could not open input stream for PDF file.")

                    Log.d("RAILWAY_STORAGE_FORENSIC", "Upload Request: S3 PUT upload started...")
                    val uploadSuccess = inputStream.use { stream ->
                        railwayStorageRepository.uploadFileDirectlyToS3(
                            uploadUrl = presignedResponse.uploadUrl,
                            inputStream = stream,
                            contentType = "application/pdf",
                            contentLength = safeSize
                        )
                    }

                    Log.d("RAILWAY_STORAGE_FORENSIC", "S3 PUT upload completed. Success: $uploadSuccess")

                    if (!uploadSuccess) {
                        throw IllegalStateException("S3 PUT upload failed for digital PDF.")
                    }

                    digitalFilePath = presignedResponse.objectKey
                    ProductionDiagnostics.log("[LISTING] PDF_UPLOAD: SUCCESS (objectKey: $digitalFilePath)")
                    Log.d("RAILWAY_STORAGE_FORENSIC", "--- RAILWAY_STORAGE_FORENSIC SUCCESS ---")
                } else {
                    digitalFilePath = pdfUri
                }
            } catch (e: Exception) {
                Log.e("RAILWAY_STORAGE_FORENSIC", "--- RAILWAY_STORAGE_FORENSIC FAILURE ---")
                Log.e("RAILWAY_STORAGE_FORENSIC", "Exception Class: ${e.javaClass.name}")
                Log.e("RAILWAY_STORAGE_FORENSIC", "Exception Message: ${e.message}")
                ProductionDiagnostics.logError("[LISTING] LISTING_PDF_UPLOAD_FAILED: FAILURE (${e.message})", e)
                
                ProductionDiagnostics.setCustomKey("operation", "listing_pdf_upload")
                ProductionDiagnostics.setCustomKey("listingId", tempId)
                ProductionDiagnostics.setCustomKey("uid", user.id)
                ProductionDiagnostics.setCustomKey("exceptionType", e.javaClass.name)

                try {
                    val db = FirebaseManager.firestore
                    if (db != null) {
                        db.collection("listings").document(tempId).delete().await()
                        ProductionDiagnostics.log("[LISTING] Draft document rollback completed.")
                    }
                } catch (rollbackEx: Exception) {
                    ProductionDiagnostics.logError("Rollback failed: ${rollbackEx.message}", rollbackEx)
                }
                isUploading.value = false
                uiMessage.value = "Upload failed. Please try again. (LISTING_PDF_UPLOAD_FAILED)"
                return@launch
            }

            // 2. Upload Sample Preview Photos if attached
            val uploadedImageKeys = mutableListOf<String>()
            val localPreviewUris = previewImagesList.filter { it.startsWith("content://") || it.startsWith("file://") }
            if (localPreviewUris.isNotEmpty()) {
                ProductionDiagnostics.log("[LISTING] PREVIEW_IMAGE_UPLOAD: START")
                localPreviewUris.forEachIndexed { idx, uriString ->
                    try {
                        val uri = Uri.parse(uriString)
                        val (imgSize, _) = getFileInfoFromUri(context, uri)
                        val safeImgSize = if (imgSize > 0) imgSize else 512L * 1024L
                        val presignedImg = railwayStorageRepository.requestPresignedUploadUrl(
                            listingId = tempId,
                            fileCategory = "listing_image",
                            contentType = "image/jpeg",
                            fileSizeBytes = safeImgSize
                        )
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val success = railwayStorageRepository.uploadFileDirectlyToS3(
                                uploadUrl = presignedImg.uploadUrl,
                                inputStream = stream,
                                contentType = "image/jpeg",
                                contentLength = safeImgSize
                            )
                            if (success) {
                                uploadedImageKeys.add(presignedImg.objectKey)
                            } else {
                                throw IllegalStateException("S3 PUT upload failed for image index $idx")
                            }
                        }
                    } catch (e: Exception) {
                        ProductionDiagnostics.logError("[LISTING] LISTING_PREVIEW_UPLOAD_FAILED: FAILURE for index $idx (${e.message})", e)
                        ProductionDiagnostics.setCustomKey("operation", "listing_preview_upload")
                        ProductionDiagnostics.setCustomKey("listingId", tempId)
                        ProductionDiagnostics.setCustomKey("uid", user.id)
                    }
                }
                ProductionDiagnostics.log("[LISTING] PREVIEW_IMAGE_UPLOAD: SUCCESS (${uploadedImageKeys.size}/${localPreviewUris.size} uploaded)")
            }

            val webPreviews = previewImagesList.filter { !it.startsWith("content://") && !it.startsWith("file://") && it.isNotBlank() }
            val finalPreviewUrls = (uploadedImageKeys + webPreviews).ifEmpty {
                listOf("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500")
            }

            // 3. Storage Verification
            ProductionDiagnostics.log("[LISTING] STORAGE_VERIFICATION: START")
            if (digitalFilePath.isBlank()) {
                ProductionDiagnostics.logError("[LISTING] LISTING_STORAGE_VERIFY_FAILED: FAILURE (digitalFilePath is blank)")
                ProductionDiagnostics.setCustomKey("operation", "listing_storage_verify")
                ProductionDiagnostics.setCustomKey("listingId", tempId)
                ProductionDiagnostics.setCustomKey("uid", user.id)

                try {
                    val db = FirebaseManager.firestore
                    if (db != null) {
                        db.collection("listings").document(tempId).delete().await()
                    }
                } catch (ex: Exception) {}
                isUploading.value = false
                uiMessage.value = "Upload failed. Please try again. (LISTING_STORAGE_VERIFY_FAILED)"
                return@launch
            }
            ProductionDiagnostics.log("[LISTING] STORAGE_VERIFICATION: SUCCESS")

            // 4. Update draft listing in Firestore to active status (keeps isApproved = false)
            val finalListing = draftListing.copy(
                imageUrls = finalPreviewUrls,
                digitalFilePath = digitalFilePath,
                status = "active",
                isApproved = false
            )

            ProductionDiagnostics.log("[LISTING] LISTING_ACTIVATION: START")
            val updateResult = repository.firestoreRepository.updateListing(finalListing)
            if (updateResult.isSuccess) {
                val note = NoteEntity(
                    id = tempId,
                    title = title,
                    description = description,
                    isFree = isFree,
                    price = if (isFree) 0.0 else price,
                    subject = subject,
                    semester = semester.ifEmpty { "Semester 1" },
                    course = course.ifEmpty { "General Studies" },
                    college = college.ifEmpty { user.collegeName },
                    pdfUriOrUrl = digitalFilePath,
                    sampleImageUrls = finalPreviewUrls.joinToString(","),
                    authorId = user.id,
                    authorName = user.name,
                    authorCollege = college.ifEmpty { user.collegeName }
                )
                repository.insertNote(note)
                uploadProgress.value = 1.0f
                isUploading.value = false
                uploadStatusText.value = "Note published successfully!"
                uiMessage.value = "Digital Note published to StudySwap Marketplace!"
                ProductionDiagnostics.log("[LISTING] LISTING_ACTIVATION: SUCCESS")
                ProductionDiagnostics.log("[LISTING] SUCCESS (listingId: $tempId)")
            } else {
                val err = updateResult.exceptionOrNull()?.message ?: "Failed to activate listing."
                ProductionDiagnostics.logError("[LISTING] LISTING_ACTIVATION_FAILED: FAILURE ($err)")
                
                ProductionDiagnostics.setCustomKey("operation", "listing_activation")
                ProductionDiagnostics.setCustomKey("listingId", tempId)
                ProductionDiagnostics.setCustomKey("uid", user.id)
                ProductionDiagnostics.setCustomKey("exceptionType", updateResult.exceptionOrNull()?.javaClass?.name ?: "Unknown")

                try {
                    val db = FirebaseManager.firestore
                    if (db != null) {
                        db.collection("listings").document(tempId).delete().await()
                    }
                } catch (rollbackEx: Exception) {}
                isUploading.value = false
                uiMessage.value = "Upload failed. Please try again. (LISTING_ACTIVATION_FAILED)"
            }
        }
    }

    fun syncPurchasesFromFirestore() {
        val userId = _currentUserId.value
        if (userId.isNullOrBlank()) {
            _syncedOrders.value = emptyList()
            _dashboardSyncState.value = DashboardSyncState.IDLE
            return
        }

        viewModelScope.launch {
            _dashboardSyncState.value = DashboardSyncState.LOADING
            try {
                val db = FirebaseManager.firestore
                if (db == null) throw IllegalStateException("Firestore is not initialized")

                Log.d("PurchasesSync", "Fetching paid orders from Firestore for user $userId")
                
                // Fetch paid orders
                val ordersSnapshot = db.collection("orders")
                    .whereEqualTo("buyerId", userId)
                    .whereEqualTo("status", "paid")
                    .get()
                    .await()

                // Fetch entitlements
                val entitlementsSnapshot = db.collection("ebook_entitlements")
                    .whereEqualTo("buyerId", userId)
                    .get()
                    .await()

                val entitlementsMap = entitlementsSnapshot.documents.associateBy(
                    { it.getString("orderId") ?: "" },
                    { it.getString("digitalFileObjectKey") ?: "" }
                )

                val syncedEntities = ordersSnapshot.documents.map { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val buyerId = doc.getString("buyerId") ?: userId
                    val sellerId = doc.getString("sellerId") ?: ""
                    val listingId = doc.getString("listingId") ?: ""
                    val listingTitle = doc.getString("listingTitleSnapshot") ?: "Digital Note"
                    val listingType = doc.getString("listingType") ?: "digital_note"
                    val amountPaise = doc.getLong("amountPaise") ?: 0L
                    val status = doc.getString("status") ?: "paid"
                    val paymentId = doc.getString("razorpayPaymentId") ?: ""
                    
                    // Retrieve timestamp properly
                    val timestamp = try {
                        doc.getTimestamp("paidAt")?.toDate()?.time
                            ?: doc.getTimestamp("createdAt")?.toDate()?.time
                            ?: System.currentTimeMillis()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    // Entitlement link
                    val s3ObjectKey = entitlementsMap[id] ?: ""

                    OrderEntity(
                        id = id,
                        buyerId = buyerId,
                        buyerName = currentUser.value?.name ?: "Student User",
                        sellerId = sellerId,
                        itemId = listingId,
                        itemTitle = listingTitle,
                        itemType = if (listingType == "digital_note" || listingType == "ebook") "DIGITAL_NOTE" else "PHYSICAL_PRODUCT",
                        price = amountPaise.toDouble() / 100.0,
                        status = if (status == "paid") "COMPLETED" else "PENDING",
                        paymentId = paymentId,
                        timestamp = timestamp,
                        watermarkedDownloadUrl = s3ObjectKey
                    )
                }

                // Fetch free downloads history
                val downloadsSnapshot = db.collection("download_history")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val freeDownloadEntities = downloadsSnapshot.documents.map { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val noteId = doc.getString("noteId") ?: ""
                    val noteTitle = doc.getString("noteTitle") ?: "Free Note"
                    val authorName = doc.getString("authorName") ?: "Author"
                    val timestamp = try {
                        doc.getLong("timestamp")
                            ?: doc.getTimestamp("timestamp")?.toDate()?.time
                            ?: System.currentTimeMillis()
                    } catch (e: java.lang.Exception) {
                        System.currentTimeMillis()
                    }
                    val pdfUriOrUrl = doc.getString("pdfUriOrUrl") ?: ""

                    OrderEntity(
                        id = id,
                        buyerId = userId,
                        buyerName = currentUser.value?.name ?: "Student User",
                        sellerId = "",
                        itemId = noteId,
                        itemTitle = noteTitle,
                        itemType = "DIGITAL_NOTE",
                        price = 0.0,
                        status = "COMPLETED",
                        paymentId = "FREE",
                        timestamp = timestamp,
                        watermarkedDownloadUrl = pdfUriOrUrl
                    )
                }

                val combinedEntities = (syncedEntities + freeDownloadEntities).distinctBy { it.id }

                Log.d("PurchasesSync", "Successfully fetched ${combinedEntities.size} combined orders/downloads. Updating Room.")

                // Sync Room Cache: Firestore is the source of truth
                repository.deleteOrdersByBuyer(userId)
                if (combinedEntities.isNotEmpty()) {
                    repository.insertOrders(combinedEntities)
                }

                _syncedOrders.value = combinedEntities
                _dashboardSyncState.value = DashboardSyncState.SUCCESS
            } catch (e: Exception) {
                Log.e("PurchasesSync", "Sync failed: ${e.message}. Falling back to Room cache.", e)
                // Fallback to Room offline cache
                try {
                    val localOrders = repository.getOrdersByBuyer(userId).first()
                    _syncedOrders.value = localOrders
                    if (localOrders.isNotEmpty()) {
                        _dashboardSyncState.value = DashboardSyncState.SUCCESS
                    } else {
                        _dashboardSyncState.value = DashboardSyncState.ERROR
                    }
                } catch (roomEx: Exception) {
                    Log.e("PurchasesSync", "Room query failed: ${roomEx.message}")
                    _dashboardSyncState.value = DashboardSyncState.ERROR
                }
            }
        }
    }

    fun downloadPurchasedNote(
        context: Context,
        orderId: String,
        listingId: String,
        noteTitle: String,
        onComplete: (File?) -> Unit
    ) {
        val userId = _currentUserId.value
        if (userId.isNullOrBlank()) {
            uiMessage.value = "Please sign in to download."
            onComplete(null)
            return
        }

        viewModelScope.launch {
            isDownloadingPdf.value = true
            uiMessage.value = "Checking download entitlement..."
            try {
                val db = FirebaseManager.firestore
                if (db == null) throw IllegalStateException("Firestore unavailable")

                // 1. Read Entitlement
                val entitlementQuery = db.collection("ebook_entitlements")
                    .whereEqualTo("buyerId", userId)
                    .whereEqualTo("listingId", listingId)
                    .whereEqualTo("orderId", orderId)
                    .whereEqualTo("status", "active")
                    .limit(1)
                    .get()
                    .await()

                if (entitlementQuery.isEmpty) {
                    // Check if owner or free note
                    val listingDoc = db.collection("listings").document(listingId).get().await()
                    val isOwner = listingDoc.getString("sellerId") == userId
                    val isFree = listingDoc.getBoolean("isFree") ?: false
                    val pricePaise = listingDoc.getLong("pricePaise") ?: 0L
                    val isFreeListing = isFree || pricePaise == 0L
                    if (!isOwner && !isFreeListing) {
                        throw IllegalStateException("No active download entitlement found.")
                    }
                }

                // 2. Request Signed URL
                uiMessage.value = "Requesting download URL from S3..."
                val signedUrl = railwayStorageRepository.requestPrivateDownloadUrl(listingId, orderId)

                // 3 & 4. Download via DownloadManager & Save into Downloads folder
                uiMessage.value = "Starting secure download..."
                val fileName = "${noteTitle.replace(" ", "_")}.pdf"
                
                AndroidDownloadManagerHelper.downloadPdfWithManager(
                    context = context,
                    downloadUrl = signedUrl,
                    title = noteTitle,
                    fileName = fileName,
                    onComplete = { file ->
                        isDownloadingPdf.value = false
                        if (file != null && file.exists()) {
                            downloadedPdfFile.value = file
                            uiMessage.value = "Download completed successfully!"
                            onComplete(file)
                        } else {
                            uiMessage.value = "Download failed."
                            onComplete(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("DownloadFlow", "Error downloading note: ${e.message}", e)
                uiMessage.value = "Download Failed: ${e.localizedMessage ?: e.message}"
                isDownloadingPdf.value = false
                onComplete(null)
            }
        }
    }

    fun clearDownloadedFile() {
        downloadedPdfFile.value = null
    }

    private fun observeNetworkConnectivity(context: Context) {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val networkRequest = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            
            val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    Log.d("PurchasesSync", "Network became available, triggering purchase sync...")
                    syncPurchasesFromFirestore()
                }
            }
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Failed to register network callback: ${e.message}", e)
        }
    }

    // AI STUDY ASSISTANT INTEGRATION
    fun executeAiAssistantTool(prompt: String, tool: String) {
        val trimmedPrompt = prompt.trim()
        if (trimmedPrompt.isEmpty()) return

        viewModelScope.launch {
            isAiLoading.value = true
            aiResponseOutput.value = "Generating response from Gemini AI..."
            try {
                val response = GeminiStudyAssistant.generateAiContent(trimmedPrompt, tool)
                aiResponseOutput.value = response

                val history = AiHistoryEntity(
                    id = "ai_${System.currentTimeMillis()}",
                    prompt = trimmedPrompt,
                    result = response,
                    type = tool
                )
                repository.insertAiHistory(history)
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI Error: ${e.message}")
                aiResponseOutput.value = "Sorry, failed to generate AI response. ${e.message}"
            } finally {
                isAiLoading.value = false
            }
        }
    }

    fun sendAiPrompt() {
        executeAiAssistantTool(aiPromptInput.value, activeAiTool.value)
    }

    fun runFirebaseDiagnostics(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val sb = java.lang.StringBuilder()
            sb.append("--- FIREBASE DIAGNOSTICS STARTED ---\n")
            try {
                // A. FirebaseApp initialization
                val app = com.google.firebase.FirebaseApp.getInstance()
                sb.append("A. FirebaseApp initialized: SUCCESS\n")
                sb.append("   App Name: ${app.name}\n")
                
                // B. Firebase project ID
                val projId = app.options.projectId
                sb.append("B. Firebase Project ID: $projId\n")
                
                // C. FirebaseAuth current user
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val user = auth.currentUser
                if (user == null) {
                    sb.append("C. FirebaseAuth Current User: NONE (Not Authenticated)\n")
                } else {
                    sb.append("C. FirebaseAuth Current User: SUCCESS\n")
                    sb.append("   User UID: ${user.uid}\n")
                    sb.append("   Is Anonymous: ${user.isAnonymous}\n")
                    
                    // D. Firestore connectivity
                    val db = FirebaseManager.firestore
                    if (db == null) {
                        sb.append("D. Firestore Instance: FAILED (db is null)\n")
                    } else {
                        sb.append("D. Firestore Connectivity: INITIALIZED\n")
                        
                        // E. Firestore create permission
                        val docId = "diag_${System.currentTimeMillis()}"
                        val diagRef = db.collection("diagnostics").document(user.uid)
                            .collection("firestore_test").document(docId)
                        val data = mapOf(
                            "testedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "status" to "success"
                        )
                        
                        sb.append("E. Firestore Write to ${diagRef.path}: START\n")
                        try {
                            diagRef.set(data).await()
                            sb.append("   Firestore Write: SUCCESS\n")
                            
                            // F. Firestore delete/cleanup
                            sb.append("F. Firestore Delete: START\n")
                            diagRef.delete().await()
                            sb.append("   Firestore Delete: SUCCESS\n")
                        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
                            sb.append("   Firestore Error Code: ${e.code}\n")
                            sb.append("   Firestore Error Message: ${e.message}\n")
                            Log.e("FORENSIC_DIAG", "Firestore diagnostics error", e)
                        } catch (e: Exception) {
                            sb.append("   General Error: ${e.message}\n")
                            Log.e("FORENSIC_DIAG", "Diagnostics write error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                sb.append("General diagnostics failure: ${e.message}\n")
            }
            sb.append("--- FIREBASE DIAGNOSTICS END ---")
            onResult(sb.toString())
        }
    }

    fun runRailwayStorageDiagnostics(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val sb = java.lang.StringBuilder()
            sb.append("--- RAILWAY STORAGE DIAGNOSTICS STARTED ---\n\n")
            try {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                if (user == null) {
                    sb.append("A. Auth: FAILED (Not Authenticated)\n\n")
                    onResult(sb.toString())
                    return@launch
                }
                sb.append("A. Auth: SUCCESS\n\n")

                val db = FirebaseManager.firestore
                if (db == null) {
                    sb.append("B. Firestore: FAILED (db is null)\n\n")
                    onResult(sb.toString())
                    return@launch
                }
                sb.append("B. Firestore: SUCCESS\n\n")

                val tempListingId = "diag_listing_${System.currentTimeMillis()}"
                val tempListingMap = mapOf(
                    "id" to tempListingId,
                    "sellerId" to user.uid,
                    "sellerDisplayName" to "Diag User",
                    "categoryId" to "Diagnostics",
                    "title" to "Storage Diagnostics Temp Listing",
                    "description" to "Temporary diagnostic test",
                    "pricePaise" to 0L,
                    "originalPricePaise" to 0L,
                    "listingType" to "digital_note",
                    "condition" to "New",
                    "collegeName" to "Diag College",
                    "city" to "Diag City",
                    "imageUrls" to emptyList<String>(),
                    "digitalFilePath" to "",
                    "status" to "draft",
                    "isApproved" to false
                )

                try {
                    db.collection("listings").document(tempListingId).set(tempListingMap).await()
                    sb.append("C. Creating temp listing: SUCCESS\n\n")
                } catch (e: Exception) {
                    sb.append("C. Creating temp listing: FAILED (${e.message})\n\n")
                    onResult(sb.toString())
                    return@launch
                }

                val testBytes = "STUDYVERSE_STORAGE_TEST_PAYLOAD".toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                val testSize = testBytes.size.toLong()
                val apiHost = try { java.net.URL(com.example.util.NetworkConfig.baseUrl).host } catch (e: Exception) { "studyverse-ai-production.up.railway.app" }

                try {
                    val presignedResponse = railwayStorageRepository.requestPresignedUploadUrl(
                        listingId = tempListingId,
                        fileCategory = "listing_image",
                        contentType = "image/jpeg",
                        fileSizeBytes = testSize
                    )
                    sb.append("D. Railway API:\n")
                    sb.append("   Host: $apiHost\n")
                    sb.append("   Presigned URL request: SUCCESS\n")
                    sb.append("   Object Key: ${presignedResponse.objectKey}\n\n")

                    val uploadSuccess = railwayStorageRepository.uploadFileDirectlyToS3(
                        uploadUrl = presignedResponse.uploadUrl,
                        inputStream = testBytes.inputStream(),
                        contentType = "image/jpeg",
                        contentLength = testSize
                    )

                    if (uploadSuccess) {
                        sb.append("E. S3 PUT upload: SUCCESS\n\n")
                    } else {
                        sb.append("E. S3 PUT upload: FAILED\n\n")
                    }
                } catch (e: com.example.data.repository.RailwayApiException) {
                    sb.append("D. Railway API: FAILED\n")
                    sb.append("   Host: $apiHost\n")
                    sb.append("   Exception: ${e.javaClass.name}\n")
                    sb.append("   Message: ${e.message}\n")
                    sb.append("   HTTP Status: ${e.statusCode}\n")
                    sb.append("   Endpoint Path: ${try { java.net.URL(e.endpointUrl).path } catch(ex: Exception) { e.endpointUrl }}\n\n")
                } catch (e: Exception) {
                    sb.append("D. Railway API: FAILED\n")
                    sb.append("   Host: $apiHost\n")
                    sb.append("   Exception: ${e.javaClass.name}\n")
                    sb.append("   Message: ${e.message ?: "Network error or timeout"}\n")
                    sb.append("   Cause: ${e.cause?.javaClass?.name ?: "none"}: ${e.cause?.message ?: ""}\n\n")
                } finally {
                    try {
                        db.collection("listings").document(tempListingId).delete().await()
                        sb.append("F. Cleanup temp listing: SUCCESS\n\n")
                    } catch (e: Exception) {
                        sb.append("F. Cleanup temp listing: FAILED (${e.message})\n\n")
                    }
                }
            } catch (e: Exception) {
                sb.append("Diagnostics failure: ${e.message}\n\n")
            }
            sb.append("--- RAILWAY STORAGE DIAGNOSTICS END ---")
            onResult(sb.toString())
        }
    }


    fun testPdfRailwayUpload(
        context: Context,
        selectedPdfUriString: String,
        pdfFileName: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val sb = java.lang.StringBuilder()
            sb.append("--- PDF RAILWAY UPLOAD STARTED ---\n")
            if (selectedPdfUriString.isBlank()) {
                sb.append("Error: No PDF URI specified.\n")
                onResult(sb.toString())
                return@launch
            }

            try {
                val uri = Uri.parse(selectedPdfUriString)
                val (fileSize, fileMime) = getFileInfoFromUri(context, uri)
                sb.append("Source URI: $selectedPdfUriString\n")
                sb.append("Source URI Scheme: ${uri.scheme}\n")
                sb.append("Source URI Authority: ${uri.authority}\n")
                sb.append("Source MIME Type: $fileMime\n")
                sb.append("Source File Name: $pdfFileName\n")
                sb.append("Source File Size: $fileSize bytes\n")

                val user = currentUser.value
                val sellerUid = user?.id ?: "unauthenticated"
                val tempListingId = "diag_listing_${System.currentTimeMillis()}"

                val db = FirebaseManager.firestore
                if (db == null) {
                    sb.append("Firestore: FAILED (db is null)\n")
                    onResult(sb.toString())
                    return@launch
                }

                sb.append("Creating temp listing for S3 ownership check...\n")
                val tempListingMap = mapOf(
                    "id" to tempListingId,
                    "sellerId" to sellerUid,
                    "sellerDisplayName" to "Diag User",
                    "categoryId" to "Diagnostics",
                    "title" to "PDF Upload Diagnostics Temp Listing",
                    "description" to "Temporary diagnostic test",
                    "pricePaise" to 0L,
                    "originalPricePaise" to 0L,
                    "listingType" to "digital_note",
                    "condition" to "New",
                    "collegeName" to "Diag College",
                    "city" to "Diag City",
                    "imageUrls" to emptyList<String>(),
                    "digitalFilePath" to "",
                    "status" to "draft",
                    "isApproved" to false
                )
                db.collection("listings").document(tempListingId).set(tempListingMap).await()

                try {
                    sb.append("Requesting presigned upload URL from Railway backend...\n")
                    val presignedResponse = railwayStorageRepository.requestPresignedUploadUrl(
                        listingId = tempListingId,
                        fileCategory = "digital_pdf",
                        contentType = "application/pdf",
                        fileSizeBytes = if (fileSize > 0) fileSize else 1024L * 1024L
                    )
                    sb.append("Presigned URL: SUCCESS\n")
                    sb.append("Object Key: ${presignedResponse.objectKey}\n")

                    sb.append("Opening InputStream: START\n")
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream == null) {
                        sb.append("Opening InputStream: FAILED (stream is null)\n")
                    } else {
                        sb.append("Opening InputStream: SUCCESS\n")
                        sb.append("Railway S3 PUT upload: START\n")
                        val uploadSuccess = inputStream.use { stream ->
                            railwayStorageRepository.uploadFileDirectlyToS3(
                                uploadUrl = presignedResponse.uploadUrl,
                                inputStream = stream,
                                contentType = "application/pdf",
                                contentLength = if (fileSize > 0) fileSize else 1024L * 1024L
                            )
                        }
                        if (uploadSuccess) {
                            sb.append("Railway S3 PUT upload: SUCCESS\n")
                        } else {
                            sb.append("Railway S3 PUT upload: FAILED\n")
                        }
                    }
                } catch (e: com.example.data.repository.RailwayApiException) {
                    sb.append("   Railway API Error:\n")
                    sb.append("      Exception Class: ${e.javaClass.name}\n")
                    sb.append("      Exception Message: ${e.message}\n")
                    sb.append("      Cause: ${e.cause?.javaClass?.name}: ${e.cause?.message}\n")
                    sb.append("      HTTP Status Code: ${e.statusCode}\n")
                    sb.append("      HTTP Error Body: ${e.responseBody}\n")
                    sb.append("      API Endpoint Path: ${e.endpointUrl}\n")
                } catch (e: Exception) {
                    sb.append("   Upload failed:\n")
                    sb.append("      Exception Class: ${e.javaClass.name}\n")
                    sb.append("      Exception Message: ${e.message}\n")
                    sb.append("      Cause: ${e.cause?.javaClass?.name}: ${e.cause?.message}\n")
                } finally {
                    sb.append("Cleanup temp listing...\n")
                    try {
                        db.collection("listings").document(tempListingId).delete().await()
                        sb.append("Cleanup SUCCESS\n")
                    } catch (e: Exception) {
                        sb.append("Cleanup FAILED: ${e.message}\n")
                    }
                }
            } catch (e: Exception) {
                sb.append("Diagnostics error: ${e.message}\n")
            }
            sb.append("--- PDF RAILWAY UPLOAD END ---")
            onResult(sb.toString())
        }
    }
}

