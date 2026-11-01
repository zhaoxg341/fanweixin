package com.yellowbite.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yellowbite.app.data.*
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(state: AppState, loading: Boolean, refresh: () -> Unit, openShop: (String) -> Unit, settings: () -> Unit) {
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("全部") }
    var sort by rememberSaveable { mutableIntStateOf(0) }
    val matches = state.shops.filter { shop ->
        (category == "全部" || shop.category == category) && (query.isBlank() || shop.name.contains(query, true) || shop.dishes.any { it.name.contains(query, true) })
    }
    val shops = when (sort) {
        1 -> matches.sortedByDescending { it.rating }
        2 -> matches.sortedByDescending { it.monthlySales }
        3 -> matches.sortedBy { it.eta }
        else -> matches
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().background(Yellow).statusBarsPadding().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocationOn, null, Modifier.size(19.dp))
                Text(state.settings.address.substringBefore('·').trim().take(12), fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp).clickable(onClick = settings))
                Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp))
                Spacer(Modifier.weight(1f))
                Text(if (state.settings.offline) "离线演示" else "联网模式", fontSize = 10.sp,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Ink.copy(alpha = .08f)).padding(horizontal = 9.dp, vertical = 5.dp))
            }
            Row(Modifier.padding(bottom = 12.dp), verticalAlignment = Alignment.Bottom) {
                Text("小黄外卖", fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("  每一餐，都值得", fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
            }
            OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("搜索店铺、菜品", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true,
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "清空搜索") } },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
                shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag("search"),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent, focusedBorderColor = Ink.copy(alpha = .2f)))
        }
        LazyColumn(Modifier.fillMaxSize().testTag("shop_list"), contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 18.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val categories = listOf("全部" to Icons.Rounded.Restaurant, "家常菜" to Icons.Rounded.RiceBowl,
                        "粉面" to Icons.Rounded.RamenDining, "汉堡" to Icons.Rounded.LunchDining,
                        "轻食" to Icons.Rounded.Eco, "早餐" to Icons.Rounded.Coffee)
                    categories.forEach { (name, icon) ->
                        Column(Modifier.clickable { category = name }, horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(45.dp).background(if (category == name) Yellow else Color(0xFFFFF7DF), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(icon, name, Modifier.size(25.dp), tint = if (category == name) Ink else Color(0xFF92713D))
                            }
                            Text(name, fontSize = 11.sp, fontWeight = if (category == name) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
            item {
                val pager = rememberPagerState(pageCount = { 3 })
                LaunchedEffect(pager) { while (true) { delay(5000); if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % 3) } }
                val ads = listOf(Triple("今天，也要\n好好吃饭", "一人食精选 · 现炒好滋味", "rice"),
                    Triple("轻一点，\n也很满足", "鲜蔬能量碗 · 新鲜每一天", "salad"), Triple("给生活\n加点芝士", "手作汉堡 · 一口好心情", "burger"))
                HorizontalPager(pager, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("ad_pager")) { page ->
                    val ad = ads[page]
                    Row(Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(20.dp)).background(if (page == 1) Green else Ink)
                        .clickable { openShop(ad.third) }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("YELLOW BITE / DAILY PICKS", color = Yellow, fontSize = 8.sp, letterSpacing = 1.sp)
                            Text(ad.first, color = Color.White, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                            Text(ad.second, color = Color(0xFFD0D6C8), fontSize = 10.sp, modifier = Modifier.padding(top = 9.dp))
                        }
                        FoodImage(ad.third, Modifier.size(110.dp))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.Center) {
                    repeat(3) { i -> Box(Modifier.padding(horizontal = 3.dp).width(if (i == pager.currentPage) 16.dp else 5.dp).height(5.dp).background(if (i == pager.currentPage) Ink else Color(0xFFD8DAD2), RoundedCornerShape(5.dp))) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("附近好店", style = MaterialTheme.typography.titleLarge)
                    Text("  ${shops.size}家精选", color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = refresh, enabled = !loading, modifier = Modifier.size(28.dp)) {
                        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Refresh, "刷新店铺", Modifier.size(19.dp), tint = Muted)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val sorts = listOf("综合排序", "评分优先", "销量优先")
                    AssistChip(onClick = { sort = (sort + 1) % 3 }, label = { Text(if (sort < 3) sorts[sort] else sorts[0], fontSize = 11.sp) },
                        trailingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(14.dp)) })
                    FilterChip(selected = sort == 3, onClick = { sort = if (sort == 3) 0 else 3 }, label = { Text("配送最快", fontSize = 11.sp) })
                    if (category != "全部") AssistChip(onClick = { category = "全部" }, label = { Text("全部店铺", fontSize = 11.sp) })
                }
            }
            if (shops.isEmpty()) item { Box(Modifier.height(180.dp)) { EmptyState("没有找到相关店铺", "换个关键词或分类试试") } }
            items(shops, key = { it.id }) { shop -> ShopCard(shop, { openShop(shop.id) }) }
            item { Text("认真做饭，准时送达 · 演示店铺与数据", color = Muted, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
    }
}

@Composable
private fun ShopCard(shop: Shop, click: () -> Unit) {
    Row(Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White)
        .clickable(onClick = click).testTag("shop_${shop.id}").padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FoodImage(shop.image, Modifier.size(94.dp), shop.name)
        Column(Modifier.weight(1f)) {
            Text(shop.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, null, Modifier.size(13.dp), tint = Orange)
                Text("${shop.rating}", color = Orange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("  月售${shop.monthlySales}", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("${shop.eta}分钟", fontSize = 10.sp)
            }
            Text("起送¥${money(shop.minimum)}  配送¥${money(shop.deliveryFee)}  ${shop.distance}", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 7.dp))
            Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { shop.tags.forEach { Tag(it) } }
        }
    }
}
