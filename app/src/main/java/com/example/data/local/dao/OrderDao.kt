package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders WHERE buyerId = :buyerId ORDER BY timestamp DESC")
    fun getOrdersByBuyer(buyerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE sellerId = :sellerId ORDER BY timestamp DESC")
    fun getOrdersBySeller(sellerId: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Update
    suspend fun updateOrder(order: OrderEntity)
}
