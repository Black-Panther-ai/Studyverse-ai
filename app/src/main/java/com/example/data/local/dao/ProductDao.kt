package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY dateAdded DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY dateAdded DESC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    fun getProductById(id: String): Flow<ProductEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}
