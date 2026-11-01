package com.yellowbite.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.yellowbite.app.data.*
import kotlinx.serialization.Serializable

@Serializable data object Root : NavKey
@Serializable data class ShopPage(val id: String) : NavKey
@Serializable data class DishPage(val shop: String, val dish: String) : NavKey
@Serializable data object CheckoutPage : NavKey
@Serializable data class OrderPage(val id: String) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YellowBiteApp(viewModel: AppViewModel) = DemoTheme {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val stack = rememberNavBackStack(Root)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheetShop by remember { mutableStateOf<String?>(null) }
    var replaceItem by remember { mutableStateOf<Pair<String, String>?>(null) }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); viewModel.dismissMessage() } }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    val add: (String, String) -> Unit = { shop, dish ->
        if (state.cart.isNotEmpty() && state.cart.first().shopId != shop) replaceItem = shop to dish
        else viewModel.changeQuantity(shop, dish, 1)
    }
    val checkout: () -> Unit = { sheetShop = null; stack.add(CheckoutPage) }
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            NavDisplay(backStack = stack, onBack = back, entryProvider = entryProvider {
                entry<Root> {
                    Scaffold(containerColor = Paper, bottomBar = {
                        NavigationBar(containerColor = Color.White) {
                            listOf("首页" to Icons.Rounded.Home, "订单" to Icons.Rounded.ReceiptLong, "我的" to Icons.Rounded.Person).forEachIndexed { index, (name, icon) ->
                                NavigationBarItem(selected = tab == index, onClick = { tab = index },
                                    icon = { Icon(icon, name) }, label = { Text(name) },
                                    colors = NavigationBarItemDefaults.colors(indicatorColor = Yellow.copy(alpha = .3f), selectedIconColor = Ink, selectedTextColor = Ink, unselectedIconColor = Muted))
                            }
                        }
                    }) { padding ->
                        Box(Modifier.padding(bottom = padding.calculateBottomPadding())) {
                            when (tab) {
                                0 -> HomeScreen(state, loading, viewModel::refresh, { stack.add(ShopPage(it)) }, { tab = 2 })
                                1 -> OrdersScreen(state.orders, { stack.add(OrderPage(it)) }, { tab = 0 }, loading, viewModel::refresh)
                                else -> SettingsScreen(state.settings, loading, { offline, endpoint -> viewModel.configure(offline, endpoint) { } })
                            }
                        }
                    }
                }
                entry<ShopPage> { route ->
                    val shop = state.shops.firstOrNull { it.id == route.id }
                    if (shop == null) EmptyState("正在加载店铺", "请稍候")
                    else ShopScreen(shop, state.cart, back, { stack.add(DishPage(shop.id, it)) },
                        { add(shop.id, it) }, { viewModel.changeQuantity(shop.id, it, -1) },
                        { sheetShop = shop.id }, checkout)
                }
                entry<DishPage> { route ->
                    val shop = state.shops.firstOrNull { it.id == route.shop }
                    val dish = shop?.dishes?.firstOrNull { it.id == route.dish }
                    if (shop != null && dish != null) DishScreen(shop, dish, state.cart, back,
                        { add(shop.id, dish.id) }, { viewModel.changeQuantity(shop.id, dish.id, -1) },
                        { sheetShop = shop.id }, checkout)
                }
                entry<CheckoutPage> {
                    CheckoutScreen(state, busy, back) { name, phone, address, note ->
                        viewModel.submit(name, phone, address, note) { id ->
                            stack.removeAt(stack.lastIndex); stack.add(OrderPage(id))
                        }
                    }
                }
                entry<OrderPage> { route ->
                    OrderDetailScreen(state.orders.firstOrNull { it.id == route.id }, busy, back,
                        { viewModel.pay(route.id) { } }, { stack.clear(); stack.add(Root); tab = 1 })
                }
            })
            SnackbarHost(snack, modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 90.dp))
        }
        val shop = state.shops.firstOrNull { it.id == sheetShop }
        if (shop != null) CartSheet(shop, state.cart, { sheetShop = null },
            { add(shop.id, it) }, { viewModel.changeQuantity(shop.id, it, -1) }, viewModel::clearCart, checkout)
        replaceItem?.let { (shop, dish) ->
            AlertDialog(onDismissRequest = { replaceItem = null }, title = { Text("切换店铺？") },
                text = { Text("购物车中的商品来自另一家店铺，切换后将清空原购物车。") },
                confirmButton = { TextButton(onClick = { viewModel.changeQuantity(shop, dish, 1, true); replaceItem = null }) { Text("清空并添加") } },
                dismissButton = { TextButton(onClick = { replaceItem = null }) { Text("保留购物车") } })
        }
    }
}
