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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = StudySwapRepository(db)
    val railwayStorageRepository = RailwayStorageRepository()
    val razorpayPaymentRepository = RazorpayPaymentRepository()
    private val prefs = application.getSharedPreferences("studyswap_prefs", Context.MODE_PRIVATE)

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
            activeListings.collect { listings ->
                if (listings.isNotEmpty()) {
                    repository.syncListingsToRoomCache(listings)
                }
            }
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

    fun downloadFreeNote(note: NoteEntity) {
        uiMessage.value = "Downloading ${note.title}..."
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
                isApproved = true
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
        val user = currentUser.value
        if (user == null) {
            uiMessage.value = "Please sign in to upload notes."
            return
        }

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
            Log.d("NoteUpload", "Starting note upload process for user: ${user.id}, title: '$title'")

            val tempId = "note_${System.currentTimeMillis()}"
            val pricePaise = if (isFree) 0L else Listing.rupeesToPaise(price)

            // 1. Create draft listing in Firestore so Railway backend authorization check passes
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
                isApproved = true
            )

            val draftResult = repository.firestoreRepository.createListing(draftListing)
            if (draftResult.isFailure) {
                isUploading.value = false
                val err = draftResult.exceptionOrNull()?.message ?: "Failed to initialize listing draft."
                Log.e("NoteUpload", "Draft creation failed: $err")
                uiMessage.value = "Upload Failed: $err"
                return@launch
            }

            var digitalFilePath = ""
            try {
                if (pdfUri.startsWith("content://") || pdfUri.startsWith("file://")) {
                    val uri = Uri.parse(pdfUri)
                    val (fileSize, _) = getFileInfoFromUri(context, uri)
                    val safeSize = if (fileSize > 0) fileSize else 1024L * 1024L

                    uploadStatusText.value = "Requesting S3 upload URL from Railway backend..."
                    Log.d("NoteUpload", "Sending POST /api/v1/storage/presign-upload for listing $tempId, size: $safeSize bytes")

                    val presignedResponse = railwayStorageRepository.requestPresignedUploadUrl(
                        listingId = tempId,
                        fileCategory = "digital_pdf",
                        contentType = "application/pdf",
                        fileSizeBytes = safeSize
                    )

                    uploadStatusText.value = "Uploading $pdfFileName to Railway S3 storage..."
                    uploadProgress.value = 0.4f
                    Log.d("NoteUpload", "Uploading PDF bytes directly to S3: ${presignedResponse.objectKey}")

                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Could not open input stream for PDF file.")

                    val uploadSuccess = inputStream.use { stream ->
                        railwayStorageRepository.uploadFileDirectlyToS3(
                            uploadUrl = presignedResponse.uploadUrl,
                            inputStream = stream,
                            contentType = "application/pdf",
                            contentLength = safeSize
                        )
                    }

                    if (!uploadSuccess) {
                        throw IllegalStateException("S3 PUT upload failed for digital PDF.")
                    }

                    digitalFilePath = presignedResponse.objectKey
                    Log.d("NoteUpload", "PDF successfully uploaded to S3. Object key: $digitalFilePath")
                    uploadProgress.value = 0.7f
                } else {
                    digitalFilePath = pdfUri
                }
            } catch (e: Exception) {
                Log.e("NoteUpload", "Digital PDF upload exception: ${e.message}", e)
                isUploading.value = false
                uiMessage.value = "PDF Upload Failed: ${e.localizedMessage ?: e.message}"
                return@launch
            }

            // 2. Upload Sample Preview Photos if attached
            val uploadedImageKeys = mutableListOf<String>()
            val localPreviewUris = previewImagesList.filter { it.startsWith("content://") || it.startsWith("file://") }
            if (localPreviewUris.isNotEmpty()) {
                uploadStatusText.value = "Uploading preview photos to Railway S3..."
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
                            if (success) uploadedImageKeys.add(presignedImg.objectKey)
                        }
                    } catch (e: Exception) {
                        Log.e("NoteUpload", "Sample photo upload failed for index $idx: ${e.message}")
                    }
                    uploadProgress.value = 0.7f + (0.2f * (idx + 1) / localPreviewUris.size)
                }
            }

            val webPreviews = previewImagesList.filter { !it.startsWith("content://") && !it.startsWith("file://") && it.isNotBlank() }
            val finalPreviewUrls = (uploadedImageKeys + webPreviews).ifEmpty {
                listOf("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=500")
            }

            // 3. Update draft listing in Firestore to active status with object keys
            val finalListing = draftListing.copy(
                imageUrls = finalPreviewUrls,
                digitalFilePath = digitalFilePath,
                status = "active",
                isApproved = true
            )

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
                Log.d("NoteUpload", "Note published successfully with object key: $digitalFilePath")
            } else {
                isUploading.value = false
                val err = updateResult.exceptionOrNull()?.message ?: "Failed to activate listing."
                Log.e("NoteUpload", "Failed to update listing status: $err")
                uiMessage.value = "Failed to publish note: $err"
            }
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
}
