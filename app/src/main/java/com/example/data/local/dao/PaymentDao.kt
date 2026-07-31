package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE buyerId = :buyerId ORDER BY timestamp DESC")
    fun getPaymentsByBuyer(buyerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE status = 'SUCCESS' ORDER BY timestamp DESC")
    fun getSuccessfulPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE status = 'FAILED' ORDER BY timestamp DESC")
    fun getFailedPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Delete
    suspend fun deletePayment(payment: PaymentEntity)
}
