package com.yellowbite.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsStore by preferencesDataStore("demo_settings")

@Singleton
class DemoRepository @Inject constructor(@ApplicationContext private val context: Context,
                                         private val dao: DemoDao, private val client: HttpClient,
                                         private val json: Json) {
    private val mutation = Mutex()
    private val offlineKey = booleanPreferencesKey("offline")
    private val endpointKey = stringPreferencesKey("endpoint")
    private val nameKey = stringPreferencesKey("recipient")
    private val phoneKey = stringPreferencesKey("phone")
    private val addressKey = stringPreferencesKey("address")
    val settings = context.settingsStore.data.map { p -> Settings(p[offlineKey] ?: true,
        p[endpointKey] ?: "http://10.0.2.2:8000", p[nameKey] ?: "演示同学",
        p[phoneKey] ?: "13800138000", p[addressKey] ?: "大学城 · 学生宿舍1号楼") }
    private val bundled = json.decodeFromString<List<Shop>>(context.assets.open("catalog.json").bufferedReader().use { it.readText() })
    val shops = MutableStateFlow(bundled)
    val cart = dao.observeCart()
    val orders = dao.observeOrders().map { rows -> rows.map { json.decodeFromString<Order>(it.json) } }

    suspend fun refresh() {
        val config = settings.first()
        if (config.offline) { shops.value = bundled; return }
        val response = client.get("${config.endpoint}/shops")
        checkResponse(response.status, response.bodyAsText())
        shops.value = response.body<List<Shop>>()
        val remoteOrders = client.get("${config.endpoint}/orders")
        checkResponse(remoteOrders.status, remoteOrders.bodyAsText())
        remoteOrders.body<List<Order>>().forEach { save(it.copy(remoteUrl = config.endpoint)) }
    }

    suspend fun changeQuantity(shopId: String, dishId: String, delta: Int, replace: Boolean = false) = mutation.withLock {
        val store = shops.value.first { it.id == shopId }
        require(store.dishes.any { it.id == dishId }) { "菜品不存在" }
        val original = dao.getCart()
        require(replace || original.isEmpty() || original.first().shopId == shopId) { "请先清空另一家店铺的购物车" }
        val rows = if (replace) emptyList() else original
        val quantity = ((rows.firstOrNull { it.dishId == dishId }?.quantity ?: 0) + delta).coerceIn(0, 99)
        dao.replaceCart(rows.filterNot { it.dishId == dishId } + if (quantity > 0) listOf(CartRow(dishId, shopId, quantity)) else emptyList())
    }

    suspend fun clearCart() = mutation.withLock { dao.clearCart() }

    suspend fun configure(offline: Boolean, endpoint: String) {
        val candidate = endpoint.trim().trimEnd('/')
        val url = if (offline && candidate.isBlank()) settings.first().endpoint else candidate
        require(offline || url.startsWith("http://") || url.startsWith("https://")) { "服务地址需以 http:// 或 https:// 开头" }
        context.settingsStore.edit { it[offlineKey] = offline; it[endpointKey] = url }
    }

    suspend fun createOrder(recipient: String, phone: String, address: String, note: String, requestId: String): Order = mutation.withLock {
        validateAddress(recipient, phone, address)?.let { error(it) }
        val rows = dao.getCart()
        require(rows.isNotEmpty()) { "购物车是空的" }
        val store = shops.value.first { it.id == rows.first().shopId }
        val amount = subtotal(store, rows)
        require(amount >= store.minimum) { "未达到起送金额" }
        val config = settings.first()
        val result = if (config.offline) {
            val items = rows.map { row -> store.dishes.first { it.id == row.dishId }.let { OrderItem(it.id, it.name, it.price, row.quantity, it.image) } }
            Order("YB" + UUID.randomUUID().toString().replace("-", "").take(12).uppercase(), store.id, store.name,
                items, amount, store.deliveryFee, amount + store.deliveryFee, "pending", Instant.now().toString(),
                recipient.trim(), phone, address.trim(), note.trim())
        } else {
            val response = client.post("${config.endpoint}/orders") {
                contentType(ContentType.Application.Json)
                setBody(CreateOrder(requestId, store.id, rows.map { OrderLine(it.dishId, it.quantity) }, recipient.trim(), phone, address.trim(), note.trim()))
            }
            checkResponse(response.status, response.bodyAsText())
            response.body<Order>().copy(remoteUrl = config.endpoint)
        }
        dao.commitOrder(OrderRow(result.id, json.encodeToString(result), result.createdAt))
        context.settingsStore.edit { it[nameKey] = recipient.trim(); it[phoneKey] = phone; it[addressKey] = address.trim() }
        result
    }

    suspend fun pay(orderId: String) = mutation.withLock {
        val row = dao.getOrder(orderId) ?: error("订单不存在")
        val order = json.decodeFromString<Order>(row.json)
        if (order.status == "paid") return@withLock
        val updated = if (order.remoteUrl == null) order.copy(status = "paid") else {
            val response = client.post("${order.remoteUrl}/orders/$orderId/pay")
            checkResponse(response.status, response.bodyAsText())
            response.body<Order>().copy(remoteUrl = order.remoteUrl)
        }
        save(updated)
    }

    private suspend fun save(order: Order) { dao.saveOrder(OrderRow(order.id, json.encodeToString(order), order.createdAt)) }

    private fun checkResponse(status: HttpStatusCode, body: String) {
        if (!status.isSuccess()) {
            val detail = runCatching { json.parseToJsonElement(body).toString() }.getOrDefault("")
            error("服务器返回 ${status.value}：${detail.take(160)}")
        }
    }
}
