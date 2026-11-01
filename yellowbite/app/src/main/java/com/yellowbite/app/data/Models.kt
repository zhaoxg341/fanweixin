package com.yellowbite.app.data

import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Dish(val id: String, val name: String, val price: Int, val description: String,
                val category: String, val sales: Int, val image: String)

@Serializable
data class Shop(val id: String, val name: String, val category: String, val rating: Double,
                val monthlySales: Int, val eta: Int, val distance: String, val deliveryFee: Int,
                val minimum: Int, val description: String, val tags: List<String>, val image: String,
                val dishes: List<Dish>)

@Serializable
data class OrderItem(val dishId: String, val name: String, val price: Int, val quantity: Int, val image: String)

@Serializable
data class Order(val id: String, val shopId: String, val shopName: String, val items: List<OrderItem>,
                 val subtotal: Int, val deliveryFee: Int, val total: Int, val status: String,
                 val createdAt: String, val recipient: String, val phone: String, val address: String,
                 val note: String = "", val remoteUrl: String? = null)

@Serializable
data class OrderLine(val dishId: String, val quantity: Int)

@Serializable
data class CreateOrder(val requestId: String, val shopId: String, val lines: List<OrderLine>,
                       val recipient: String, val phone: String, val address: String, val note: String)

data class Settings(val offline: Boolean = true, val endpoint: String = "http://10.0.2.2:8000",
                    val recipient: String = "演示同学", val phone: String = "13800138000",
                    val address: String = "大学城 · 学生宿舍1号楼")

fun money(cents: Int): String = String.format(Locale.CHINA, "%.2f", cents / 100.0)

fun subtotal(shop: Shop?, cart: List<CartRow>): Int = shop?.dishes?.sumOf { dish ->
    dish.price * (cart.firstOrNull { it.dishId == dish.id && it.shopId == shop.id }?.quantity ?: 0)
} ?: 0

fun validateAddress(recipient: String, phone: String, address: String): String? = when {
    recipient.trim().isEmpty() -> "请填写收货人"
    !Regex("^1[3-9]\\d{9}$").matches(phone) -> "请填写有效的11位手机号"
    address.trim().length < 5 -> "请填写完整的配送地址（至少5个字）"
    else -> null
}
