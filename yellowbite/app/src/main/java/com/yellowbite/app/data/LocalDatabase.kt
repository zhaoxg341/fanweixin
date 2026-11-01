package com.yellowbite.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "cart")
data class CartRow(@PrimaryKey val dishId: String, val shopId: String, val quantity: Int)

@Entity(tableName = "orders")
data class OrderRow(@PrimaryKey val id: String, val json: String, val createdAt: String)

@Dao
abstract class DemoDao {
    @Query("SELECT * FROM cart") abstract fun observeCart(): Flow<List<CartRow>>
    @Query("SELECT * FROM cart") abstract suspend fun getCart(): List<CartRow>
    @Query("DELETE FROM cart") abstract suspend fun clearCart()
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun insertCart(rows: List<CartRow>)
    @Transaction
    open suspend fun replaceCart(rows: List<CartRow>) { clearCart(); insertCart(rows) }
    @Query("SELECT * FROM orders ORDER BY createdAt DESC") abstract fun observeOrders(): Flow<List<OrderRow>>
    @Query("SELECT * FROM orders WHERE id = :id") abstract suspend fun getOrder(id: String): OrderRow?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun saveOrder(order: OrderRow)
    @Transaction
    open suspend fun commitOrder(order: OrderRow) { saveOrder(order); clearCart() }
}

@Database(entities = [CartRow::class, OrderRow::class], version = 1, exportSchema = true)
abstract class LocalDatabase : RoomDatabase() { abstract fun dao(): DemoDao }
