package com.yellowbite.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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

@Composable
fun ShopScreen(shop: Shop, cart: List<CartRow>, back: () -> Unit, dish: (String) -> Unit,
               plus: (String) -> Unit, minus: (String) -> Unit, openCart: () -> Unit, checkout: () -> Unit) {
    var category by rememberSaveable(shop.id) { mutableStateOf("全部") }
    var info by rememberSaveable(shop.id) { mutableStateOf(false) }
    Scaffold(containerColor = Color.White, topBar = { PageBar("店铺详情", back) },
        bottomBar = { CartBar(shop, cart, openCart, checkout) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                FoodImage(shop.image, Modifier.size(80.dp))
                Column(Modifier.weight(1f)) {
                    Text(shop.name, style = MaterialTheme.typography.titleLarge)
                    Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = Orange)
                        Text("${shop.rating}", color = Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("  月售${shop.monthlySales} · ${shop.eta}分钟", color = Muted, fontSize = 11.sp)
                    }
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { shop.tags.forEach { Tag(it) } }
                }
            }
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF8E4)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Campaign, null, Modifier.size(16.dp), tint = Color(0xFF997A30))
                Text("现点现做，新鲜送达。图片为菜品示意图。", fontSize = 10.sp, color = Color(0xFF997A30), modifier = Modifier.padding(start = 7.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Text("点餐", fontWeight = FontWeight.Bold, color = if (!info) Ink else Muted,
                    modifier = Modifier.clickable { info = false }.background(if (!info) Yellow.copy(alpha = .4f) else Color.Transparent, RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
                Text("商家", fontWeight = FontWeight.Bold, color = if (info) Ink else Muted,
                    modifier = Modifier.clickable { info = true }.padding(vertical = 4.dp))
            }
            if (info) {
                Column(Modifier.padding(24.dp)) {
                    Text("关于这家店", style = MaterialTheme.typography.titleLarge)
                    Text(shop.description, modifier = Modifier.padding(vertical = 16.dp), color = Muted)
                    AmountRow("经营品类", shop.category)
                    AmountRow("距离", shop.distance)
                    AmountRow("起送金额", "¥${money(shop.minimum)}")
                    AmountRow("配送费", "¥${money(shop.deliveryFee)}")
                    AmountRow("营业时间", "每日 08:00—22:00")
                    Text("课程演示商家，营业时间与配送信息为模拟数据。", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 20.dp))
                }
            } else Row(Modifier.weight(1f)) {
                val categories = listOf("全部") + shop.dishes.map { it.category }.distinct()
                Column(Modifier.width(76.dp).fillMaxHeight().background(Paper)) {
                    categories.forEach { value ->
                        Row(Modifier.fillMaxWidth().height(56.dp).background(if (category == value) Color.White else Color.Transparent).clickable { category = value }, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(22.dp).background(if (category == value) Yellow else Color.Transparent))
                            Text(value, fontSize = 12.sp, fontWeight = if (category == value) FontWeight.Bold else FontWeight.Normal, color = if (category == value) Ink else Muted,
                                modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f).testTag("menu_list"), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    item { Text(if (category == "全部") "店长推荐" else category, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    items(shop.dishes.filter { category == "全部" || it.category == category }, key = { it.id }) { item ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FoodImage(item.image, Modifier.size(80.dp).clickable { dish(item.id) }, item.name)
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.clickable { dish(item.id) }.testTag("dish_${item.id}"))
                                    Text(item.description, color = Muted, fontSize = 10.sp, maxLines = 2, lineHeight = 15.sp, modifier = Modifier.padding(top = 4.dp))
                                    Text("月售${item.sales} · 好评98%", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 5.dp))
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("¥${money(item.price)}", fontSize = 19.sp, color = Orange, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                QuantityControl(item.id, cart.firstOrNull { it.dishId == item.id && it.shopId == shop.id }?.quantity ?: 0,
                                    { plus(item.id) }, { minus(item.id) })
                            }
                        }
                    }
                    item { Text("已展示全部菜品", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 12.dp)) }
                }
            }
        }
    }
}

@Composable
fun DishScreen(shop: Shop, dish: Dish, cart: List<CartRow>, back: () -> Unit,
               plus: () -> Unit, minus: () -> Unit, openCart: () -> Unit, checkout: () -> Unit) {
    Scaffold(containerColor = Paper, bottomBar = { CartBar(shop, cart, openCart, checkout) }) { padding ->
        LazyColumn(Modifier.padding(bottom = padding.calculateBottomPadding()).fillMaxSize()) {
            item {
                Box(Modifier.fillMaxWidth().height(320.dp)) {
                    FoodImage(dish.image, Modifier.fillMaxSize(), dish.name)
                    IconButton(onClick = back, modifier = Modifier.statusBarsPadding().padding(16.dp).size(40.dp).background(Color.White.copy(alpha = .95f), CircleShape).testTag("back")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回")
                    }
                    Text("菜品图片仅供参考", color = Color.White, fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).background(Ink.copy(alpha = .5f), RoundedCornerShape(6.dp)).padding(5.dp))
                }
            }
            item {
                Column(Modifier.fillMaxWidth().background(Color.White).padding(22.dp)) {
                    Tag("店长推荐")
                    Text(dish.name, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
                    Text("月售${dish.sales}  ·  好评98%", color = Muted, modifier = Modifier.padding(top = 7.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("¥${money(dish.price)}", color = Orange, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        QuantityControl(dish.id, cart.firstOrNull { it.dishId == dish.id && it.shopId == shop.id }?.quantity ?: 0, plus, minus)
                    }
                }
            }
            item {
                Column(Modifier.padding(top = 12.dp).fillMaxWidth().background(Color.White).padding(22.dp)) {
                    Text("关于这道菜", style = MaterialTheme.typography.titleMedium)
                    Text(dish.description, color = Muted, lineHeight = 23.sp, modifier = Modifier.padding(top = 14.dp))
                    HorizontalDivider(Modifier.padding(vertical = 18.dp), color = Paper)
                    Text("来自 ${shop.name}", fontWeight = FontWeight.Bold)
                    Text("新鲜制作，认真对待每一餐。", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartSheet(shop: Shop, cart: List<CartRow>, dismiss: () -> Unit, plus: (String) -> Unit,
              minus: (String) -> Unit, clear: () -> Unit, checkout: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    val rows = cart.filter { it.shopId == shop.id }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Color.White, modifier = Modifier.testTag("cart_sheet")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("已选商品", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { confirmClear = true }, enabled = rows.isNotEmpty(), modifier = Modifier.testTag("clear_cart")) {
                Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(17.dp)); Text("清空", modifier = Modifier.padding(start = 4.dp))
            }
        }
        if (rows.isEmpty()) Box(Modifier.fillMaxWidth().height(170.dp)) { EmptyState("购物车是空的", "挑选一道喜欢的菜吧") }
        else LazyColumn(Modifier.heightIn(max = 360.dp), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            items(rows, key = { it.dishId }) { row ->
                val dish = shop.dishes.first { it.id == row.dishId }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FoodImage(dish.image, Modifier.size(58.dp))
                    Column(Modifier.weight(1f)) { Text(dish.name, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("¥${money(dish.price)}", color = Orange, modifier = Modifier.padding(top = 7.dp)) }
                    QuantityControl(dish.id, row.quantity, { plus(dish.id) }, { minus(dish.id) })
                }
            }
        }
        CartBar(shop, cart, dismiss, checkout)
        Spacer(Modifier.height(12.dp))
    }
    if (confirmClear) AlertDialog(onDismissRequest = { confirmClear = false }, title = { Text("清空购物车？") },
        text = { Text("已选商品将被移除，你可以重新选购。") },
        confirmButton = { TextButton(onClick = { clear(); confirmClear = false }, modifier = Modifier.testTag("confirm_clear")) { Text("确认清空") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } })
}
