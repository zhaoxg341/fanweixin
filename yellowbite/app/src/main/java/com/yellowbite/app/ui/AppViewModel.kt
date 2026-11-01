package com.yellowbite.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yellowbite.app.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AppState(val shops: List<Shop> = emptyList(), val cart: List<CartRow> = emptyList(),
                    val orders: List<Order> = emptyList(), val settings: Settings = Settings())

@HiltViewModel
class AppViewModel @Inject constructor(private val repository: DemoRepository) : ViewModel() {
    val state = combine(repository.shops, repository.cart, repository.orders, repository.settings) { shops, cart, orders, settings ->
        AppState(shops, cart, orders, settings)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppState())
    val busy = MutableStateFlow(false)
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<String?>(null)
    private var requestSignature = ""
    private var requestKey = UUID.randomUUID().toString()
    init { refresh() }
    fun dismissMessage() { message.value = null }
    fun refresh() {
        if (loading.value) return
        viewModelScope.launch {
            loading.value = true
            try { repository.refresh() } catch (e: Exception) { report(e, "连接失败，请检查后端地址或切换离线演示") }
            finally { loading.value = false }
        }
    }
    fun changeQuantity(shop: String, dish: String, delta: Int, replace: Boolean = false) = viewModelScope.launch {
        try { repository.changeQuantity(shop, dish, delta, replace) } catch (e: Exception) { report(e) }
    }
    fun clearCart() = viewModelScope.launch {
        try { repository.clearCart() } catch (e: Exception) { report(e) }
    }
    fun configure(offline: Boolean, endpoint: String, onDone: () -> Unit) = viewModelScope.launch {
        try { repository.configure(offline, endpoint); onDone(); refresh() } catch (e: Exception) { report(e) }
    }
    fun submit(recipient: String, phone: String, address: String, note: String, onCreated: (String) -> Unit) {
        if (busy.value) return
        val signature = "$recipient|$phone|$address|$note|${state.value.cart}|${state.value.settings}"
        if (signature != requestSignature) { requestSignature = signature; requestKey = UUID.randomUUID().toString() }
        busy.value = true
        viewModelScope.launch {
            try { val order = repository.createOrder(recipient, phone, address, note, requestKey); onCreated(order.id) }
            catch (e: Exception) { report(e) }
            finally { busy.value = false }
        }
    }
    fun pay(id: String, onPaid: () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try { repository.pay(id); onPaid(); message.value = "模拟支付成功，订单已保存" }
            catch (e: Exception) { report(e, "支付未完成，请检查网络后重试") }
            finally { busy.value = false }
        }
    }
    private fun report(e: Exception, fallback: String = "操作失败，请重试") {
        if (e is CancellationException) throw e
        message.value = if (e is IllegalArgumentException || e is IllegalStateException) e.message ?: fallback else fallback
    }
}
