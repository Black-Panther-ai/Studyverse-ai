package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.model.Listing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class StudySwapRepository(private val db: AppDatabase) {

    val firestoreRepository = FirestoreRepository()

    val allUsers: Flow<List<UserEntity>> = db.userDao().getAllUsers()
    val allNotes: Flow<List<NoteEntity>> = db.noteDao().getAllNotes()
    val freeNotes: Flow<List<NoteEntity>> = db.noteDao().getFreeNotes()
    val digitalNotesStore: Flow<List<NoteEntity>> = db.noteDao().getDigitalNotesStore()

    val allProducts: Flow<List<ProductEntity>> = db.productDao().getAllProducts()
    val topSellers: Flow<List<UserEntity>> = db.userDao().getTopSellers()
    val topContributors: Flow<List<UserEntity>> = db.userDao().getTopContributors()
    val aiHistory: Flow<List<AiHistoryEntity>> = db.aiHistoryDao().getAllHistory()
    val allReports: Flow<List<ReportEntity>> = db.reportDao().getAllReports()
    val allPayments: Flow<List<PaymentEntity>> = db.paymentDao().getAllPayments()
    val successfulPayments: Flow<List<PaymentEntity>> = db.paymentDao().getSuccessfulPayments()
    val failedPayments: Flow<List<PaymentEntity>> = db.paymentDao().getFailedPayments()

    fun getOrdersByBuyer(buyerId: String): Flow<List<OrderEntity>> = db.orderDao().getOrdersByBuyer(buyerId)
    fun getOrdersBySeller(sellerId: String): Flow<List<OrderEntity>> = db.orderDao().getOrdersBySeller(sellerId)
    fun getAllOrders(): Flow<List<OrderEntity>> = db.orderDao().getAllOrders()
    fun getPaymentsByBuyer(buyerId: String): Flow<List<PaymentEntity>> = db.paymentDao().getPaymentsByBuyer(buyerId)

    fun getUserById(userId: String): Flow<UserEntity?> = db.userDao().getUserById(userId)
    suspend fun getUserByEmail(email: String): UserEntity? = db.userDao().getUserByEmail(email)
    fun getBookmarksByUser(userId: String): Flow<List<BookmarkEntity>> = db.bookmarkDao().getBookmarksByUser(userId)
    fun isBookmarked(userId: String, itemId: String): Flow<Boolean> = db.bookmarkDao().isBookmarked(userId, itemId)

    suspend fun insertUser(user: UserEntity) = db.userDao().insertUser(user)
    suspend fun updateUser(user: UserEntity) = db.userDao().updateUser(user)
    suspend fun deleteUser(user: UserEntity) = db.userDao().deleteUser(user)

    suspend fun insertNote(note: NoteEntity) = db.noteDao().insertNote(note)
    suspend fun updateNote(note: NoteEntity) = db.noteDao().updateNote(note)
    suspend fun deleteNote(note: NoteEntity) = db.noteDao().deleteNote(note)

    suspend fun insertProduct(product: ProductEntity) = db.productDao().insertProduct(product)
    suspend fun updateProduct(product: ProductEntity) = db.productDao().updateProduct(product)
    suspend fun deleteProduct(product: ProductEntity) = db.productDao().deleteProduct(product)

    suspend fun insertOrder(order: OrderEntity) = db.orderDao().insertOrder(order)
    suspend fun insertPayment(payment: PaymentEntity) = db.paymentDao().insertPayment(payment)

    suspend fun insertAiHistory(history: AiHistoryEntity) = db.aiHistoryDao().insertHistory(history)

    suspend fun insertReport(report: ReportEntity) = db.reportDao().insertReport(report)
    suspend fun deleteReport(report: ReportEntity) = db.reportDao().deleteReport(report)

    suspend fun toggleBookmark(userId: String, itemId: String, title: String, type: String, category: String, price: Double) {
        val exists = db.bookmarkDao().isBookmarked(userId, itemId).firstOrNull() ?: false
        if (exists) {
            db.bookmarkDao().removeBookmark(userId, itemId)
        } else {
            db.bookmarkDao().insertBookmark(
                BookmarkEntity(
                    id = "bm_${System.currentTimeMillis()}",
                    userId = userId,
                    itemType = type,
                    itemId = itemId,
                    title = title,
                    categoryOrSubject = category,
                    price = price
                )
            )
        }
    }

    // SYNC FIRESTORE LISTINGS INTO LOCAL ROOM READ CACHE
    suspend fun syncListingsToRoomCache(listings: List<Listing>) = withContext(Dispatchers.IO) {
        listings.forEach { listing ->
            if (listing.listingType == "digital_note") {
                val note = NoteEntity(
                    id = listing.id,
                    title = listing.title,
                    description = listing.description,
                    isFree = listing.pricePaise == 0L,
                    price = listing.rupeesPrice,
                    subject = listing.categoryId,
                    semester = "Semester 1",
                    course = "General Studies",
                    college = listing.collegeName,
                    pdfUriOrUrl = listing.digitalFilePath,
                    sampleImageUrls = listing.imageUrls.joinToString(","),
                    authorId = listing.sellerId,
                    authorName = listing.sellerDisplayName,
                    authorCollege = listing.collegeName
                )
                db.noteDao().insertNote(note)
            } else {
                val product = ProductEntity(
                    id = listing.id,
                    title = listing.title,
                    description = listing.description,
                    category = listing.categoryId,
                    condition = listing.condition,
                    price = listing.rupeesPrice,
                    originalPrice = listing.rupeesOriginalPrice,
                    city = listing.city,
                    college = listing.collegeName,
                    photoUrls = listing.imageUrls.joinToString(","),
                    sellerId = listing.sellerId,
                    sellerName = listing.sellerDisplayName
                )
                db.productDao().insertProduct(product)
            }
        }
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        // Phase 0/1: Sealed database. Mock seeding disabled.
    }
}
