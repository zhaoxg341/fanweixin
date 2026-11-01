package com.yellowbite.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yellowbite.app.data.*
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
private fun WhiteCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(18.dp), content = content)
}

@Composable
private fun OrderItems(items: List<OrderItem>) {
    items.forEach { item ->
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FoodImage(item.image, Modifier.size(58.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("¥${money(item.price)}  ×${item.quantity}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Text("¥${money(item.price * item.quantity)}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun CheckoutScreen(state: AppState, busy: Boolean, back: () -> Unit,
                   submit: (String, String, String, String) -> Unit) {
    val shop = state.shops.firstOrNull { it.id == state.cart.firstOrNull()?.shopId }
    var name by rememberSaveable { mutableStateOf(state.settings.recipient) }
    var phone by rememberSaveable { mutableStateOf(state.settings.phone) }
    var address by rememberSaveable { mutableStateOf(state.settings.address) }
    var note by rememberSaveable { mutableStateOf("") }
    val invalid = validateAddress(name, phone, address)
    val amount = subtotal(shop, state.cart)
    Scaffold(containerColor = Paper, topBar = { PageBar("确认订单", back) }, bottomBar = {
        if (shop != null) Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().imePadding().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("合计（含配送费）", fontSize = 11.sp, color = Muted)
                Text("¥${money(amount + shop.deliveryFee)}", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Orange)
            }
            Button(onClick = { submit(name, phone, address, note) }, enabled = !busy && invalid == null && amount >= shop.minimum,
                shape = RoundedCornerShape(25.dp), modifier = Modifier.height(50.dp).testTag("submit_order")) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                else Text("提交订单", modifier = Modifier.padding(horizontal = 9.dp))
            }
        }
    }) { padding ->
        if (shop == null) EmptyState("购物车是空的", "返回店铺选择商品", "返回", back)
        else LazyColumn(Modifier.padding(padding).fillMaxSize().testTag("checkout_form"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                WhiteCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = Green)
                        Text("配送到", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
                        Spacer(Modifier.weight(1f)); Tag("预计${shop.eta}分钟送达")
                    }
                    OutlinedTextField(address, { if (it.length <= 200) address = it }, label = { Text("详细地址") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("address"), shape = RoundedCornerShape(12.dp), maxLines = 3)
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(name, { if (it.length <= 40) name = it }, label = { Text("收货人") }, singleLine = true,
                            modifier = Modifier.weight(.8f).testTag("recipient"), shape = RoundedCornerShape(12.dp))
                        OutlinedTextField(phone, { phone = it.filter(Char::isDigit).take(11) }, label = { Text("手机号") }, singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                            modifier = Modifier.weight(1.2f).testTag("phone"), shape = RoundedCornerShape(12.dp))
                    }
                    if (invalid != null) Text(invalid, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item {
                WhiteCard {
                    Text(shop.name, style = MaterialTheme.typography.titleMedium)
                    val items = state.cart.map { row -> shop.dishes.first { it.id == row.dishId }.let { OrderItem(it.id, it.name, it.price, row.quantity, it.image) } }
                    OrderItems(items)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Paper)
                    AmountRow("商品金额", "¥${money(amount)}")
                    AmountRow("配送费", "¥${money(shop.deliveryFee)}")
                    AmountRow("合计", "¥${money(amount + shop.deliveryFee)}", true)
                }
            }
            item {
                WhiteCard {
                    Text("给商家留言", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(note, { if (it.length <= 200) note = it }, placeholder = { Text("例如：少辣、不放香菜") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("note"), shape = RoundedCornerShape(12.dp), maxLines = 3)
                }
            }
            item { Text("本应用为课程演示，付款流程为模拟支付，不会产生真实扣款。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@Composable
fun OrdersScreen(orders: List<Order>, open: (String) -> Unit, home: () -> Unit, loading: Boolean, refresh: () -> Unit) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = orders.filter { filter == 0 || (filter == 1 && it.status == "pending") || (filter == 2 && it.status == "paid") }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("我的订单", style = MaterialTheme.typography.headlineMedium); Text("每一餐，都有记录", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
            IconButton(onClick = refresh, enabled = !loading) { Icon(Icons.Rounded.Refresh, "刷新订单") }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("全部", "待付款", "已支付").forEachIndexed { i, label -> FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(label) }) }
        }
        if (visible.isEmpty()) EmptyState("还没有${if (filter == 1) "待付款" else if (filter == 2) "已支付" else ""}订单", "去挑选一份喜欢的美食吧", "逛逛附近好店", home)
        else LazyColumn(Modifier.fillMaxSize().testTag("order_list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            items(visible, key = { it.id }) { order ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).clickable { open(order.id) }.testTag("order_${order.id}").padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(order.shopName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Tag(if (order.status == "paid") "已支付" else "待付款", if (order.status == "paid") Green else Orange)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Paper)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FoodImage(order.items.first().image, Modifier.size(65.dp))
                        Column(Modifier.weight(1f)) {
                            Text(order.items.first().name, fontSize = 13.sp)
                            Text("共${order.items.sumOf { it.quantity }}件商品", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                        Text("¥${money(order.total)}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatTime(order.createdAt), color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                        Text(if (order.status == "pending") "去支付 →" else "查看详情 →", color = if (order.status == "pending") Orange else Green, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDetailScreen(order: Order?, busy: Boolean, back: () -> Unit, pay: () -> Unit, allOrders: () -> Unit) {
    var payment by rememberSaveable { mutableStateOf(false) }
    var method by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(containerColor = Paper, topBar = { PageBar("订单详情", back) }, bottomBar = {
        if (order != null) Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(18.dp)) {
            Button(onClick = { if (order.status == "paid") allOrders() else payment = true }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("pay_order"), shape = RoundedCornerShape(27.dp)) {
                Text(if (order.status == "paid") "查看全部订单" else "立即支付 ¥${money(order.total)}")
            }
        }
    }) { padding ->
        if (order == null) Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        else LazyColumn(Modifier.padding(padding).fillMaxSize().testTag("order_details"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(54.dp).background(if (order.status == "paid") Green.copy(alpha = .12f) else Yellow.copy(alpha = .3f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(if (order.status == "paid") Icons.Rounded.CheckCircle else Icons.Rounded.Schedule, null, Modifier.size(31.dp), tint = if (order.status == "paid") Green else Ink)
                    }
                    Column {
                        Text(if (order.status == "paid") "支付成功" else "等待付款", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("order_status"))
                        Text(if (order.status == "paid") "商家已接单 · 演示流程已完成" else "订单已创建，请完成模拟支付", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
            item {
                WhiteCard {
                    Text("配送信息", style = MaterialTheme.typography.titleMedium)
                    Text(order.address, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
                    Text("${order.recipient}  ${order.phone}", color = Muted, modifier = Modifier.padding(top = 8.dp))
                    if (order.note.isNotBlank()) Text("备注：${order.note}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
                }
            }
            item {
                WhiteCard {
                    Text(order.shopName, style = MaterialTheme.typography.titleMedium)
                    OrderItems(order.items)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Paper)
                    AmountRow("商品金额", "¥${money(order.subtotal)}")
                    AmountRow("配送费", "¥${money(order.deliveryFee)}")
                    AmountRow(if (order.status == "paid") "实付（模拟）" else "待付", "¥${money(order.total)}", true)
                }
            }
            item {
                WhiteCard {
                    Text("订单信息", style = MaterialTheme.typography.titleMedium)
                    Text("订单编号  ${order.id}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 14.dp))
                    Text("创建时间  ${formatTime(order.createdAt)}", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                    Text(if (order.remoteUrl == null) "离线演示订单" else "联网演示订单", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
    if (payment && order != null) AlertDialog(onDismissRequest = { if (!busy) payment = false },
        title = { Text("模拟支付") }, text = {
            Column {
                Text("¥${money(order.total)}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Orange)
                Text("课程演示，不会产生真实扣款。", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                listOf("微信支付（模拟）", "支付宝（模拟）").forEachIndexed { i, label ->
                    Row(Modifier.fillMaxWidth().clickable { method = i }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = method == i, onClick = { method = i }); Text(label, fontSize = 13.sp)
                    }
                }
            }
        }, confirmButton = {
            Button(onClick = { payment = false; pay() }, enabled = !busy, modifier = Modifier.testTag("confirm_pay")) { Text("确认模拟支付") }
        }, dismissButton = { TextButton(onClick = { payment = false }, enabled = !busy) { Text("稍后支付") } })
}

private fun formatTime(value: String): String = runCatching {
    OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
}.getOrDefault(value.take(16))
