package com.yellowbite.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yellowbite.app.data.*

@Composable
fun FoodImage(key: String, modifier: Modifier = Modifier, description: String? = null) {
    AsyncImage(model = "file:///android_asset/food/$key.jpg", contentDescription = description,
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFFE6E6DC)), contentScale = ContentScale.Crop)
}

@Composable
fun Tag(text: String, color: Color = Green) {
    Text(text, fontSize = 10.sp, color = color, modifier = Modifier.background(color.copy(alpha = .08f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp))
}

@Composable
fun PageBar(title: String, back: () -> Unit, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().background(Color.White).statusBarsPadding().height(56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = back, modifier = Modifier.testTag("back")) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") }
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing()
    }
}

@Composable
fun QuantityControl(id: String, quantity: Int, plus: () -> Unit, minus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (quantity > 0) {
            IconButton(onClick = minus, Modifier.size(48.dp).testTag("minus_$id")) {
                Box(Modifier.size(30.dp).background(Paper, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Remove, "减少数量", Modifier.size(17.dp))
                }
            }
            Text("$quantity", fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 16.dp).testTag("qty_$id"),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        IconButton(onClick = plus, enabled = quantity < 99, modifier = Modifier.size(48.dp).testTag("plus_$id")) {
            Box(Modifier.size(30.dp).background(Yellow, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, "加入购物车", Modifier.size(18.dp), tint = Ink)
            }
        }
    }
}

@Composable
fun CartBar(shop: Shop, cart: List<CartRow>, onCart: () -> Unit, onCheckout: () -> Unit) {
    val rows = cart.filter { it.shopId == shop.id }
    val amount = subtotal(shop, rows)
    val count = rows.sumOf { it.quantity }
    Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(14.dp)
        .height(62.dp).clip(RoundedCornerShape(32.dp)).background(Ink), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.padding(start = 12.dp).size(45.dp).clip(CircleShape).background(Yellow)
            .clickable(onClick = onCart).testTag("open_cart"), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.ShoppingBag, "查看购物车", tint = Ink)
            if (count > 0) Text("$count", color = Color.White, fontSize = 10.sp,
                modifier = Modifier.align(Alignment.TopEnd).background(Orange, CircleShape).padding(horizontal = 4.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(if (count > 0) "¥${money(amount)}" else "还没有选购商品", color = Color.White,
                fontWeight = FontWeight.Bold, fontSize = if (count > 0) 21.sp else 14.sp)
            Text("另需配送费 ¥${money(shop.deliveryFee)}", color = Color(0xFFAEB1A9), fontSize = 10.sp)
        }
        val enabled = count > 0 && amount >= shop.minimum
        Button(onClick = onCheckout, enabled = enabled, shape = RoundedCornerShape(25.dp),
            modifier = Modifier.padding(end = 7.dp).height(48.dp).testTag("checkout"),
            contentPadding = PaddingValues(horizontal = 18.dp),
            colors = ButtonDefaults.buttonColors(disabledContainerColor = Color(0xFF454941), disabledContentColor = Color(0xFFD3D5CE))) {
            Text(if (enabled) "去结算" else if (count == 0) "¥${money(shop.minimum)}起送" else "还差¥${money(shop.minimum - amount)}")
        }
    }
}

@Composable
fun EmptyState(title: String, description: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Restaurant, null, modifier = Modifier.size(58.dp), tint = Yellow)
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(description, color = Muted)
        if (action != null) Button(onClick = onAction, modifier = Modifier.padding(top = 20.dp)) { Text(action) }
    }
}

@Composable
fun AmountRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = if (emphasize) Ink else Muted)
        Text(value, fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (emphasize) 20.sp else 14.sp, color = if (emphasize) Orange else Ink)
    }
}
