package com.yellowbite.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File

class DemoFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun waitTag(tag: String) { compose.waitUntil(15000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() } }
    private fun waitText(text: String) { compose.waitUntil(15000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() } }
    private fun mode(offline: Boolean) {
        waitTag("shop_rice")
        compose.onNodeWithText("我的").performClick()
        val on = compose.onNodeWithTag("offline_switch").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.ToggleableState] == androidx.compose.ui.state.ToggleableState.On
        if (on != offline) compose.onNodeWithTag("offline_switch").performClick()
        compose.waitUntil(15000) { !compose.onNodeWithTag("save_settings").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        compose.onNodeWithTag("save_settings").performClick()
        waitText(if (offline) "当前：离线演示" else "当前：联网模式")
        compose.onNodeWithText("首页").performClick()
        waitTag("shop_rice")
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        Thread.sleep(400) // Let the Android render thread present the committed Compose frame.
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun browseCartCheckoutPayment() {
        mode(true)
        waitTag("shop_rice")
        screenshot("01_home")
        compose.onNodeWithTag("search").performTextInput("谷香")
        compose.onNodeWithTag("search").performImeAction()
        waitTag("shop_rice")
        compose.onNodeWithTag("shop_rice").assertExists()
        compose.onNodeWithTag("shop_noodle").assertDoesNotExist()
        compose.onNodeWithTag("search").performTextClearance()
        compose.onNodeWithTag("search").performImeAction()
        waitTag("shop_rice")
        compose.onNodeWithTag("shop_rice").performClick()
        screenshot("02_shop")
        compose.onNodeWithTag("dish_r1").performClick()
        compose.onNodeWithText("关于这道菜").assertExists()
        screenshot("03_dish")
        compose.onNodeWithTag("plus_r1").performClick()
        compose.waitUntil { compose.onAllNodesWithTag("qty_r1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("checkout").assertIsEnabled()
        compose.onNodeWithTag("open_cart").performClick()
        screenshot("04_cart")
        compose.onNodeWithTag("clear_cart").performClick()
        compose.onNodeWithTag("confirm_clear").performClick()
        waitText("购物车是空的")
        compose.onNodeWithTag("cart_sheet").performTouchInput { swipeDown() }
        compose.waitUntil { compose.onAllNodesWithTag("cart_sheet").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("checkout").assertIsNotEnabled()
        compose.onNodeWithTag("plus_r1").performClick()
        compose.waitUntil { compose.onAllNodesWithTag("qty_r1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("checkout").performClick()
        screenshot("05_checkout")
        compose.onNodeWithTag("phone").performTextReplacement("123")
        compose.onNodeWithTag("submit_order").assertIsNotEnabled()
        compose.onNodeWithTag("phone").performTextReplacement("13800138000")
        compose.onNodeWithTag("submit_order").performClick()
        waitTag("pay_order")
        waitText("等待付款")
        compose.onNodeWithTag("pay_order").performClick()
        compose.onNodeWithTag("confirm_pay").performClick()
        waitText("支付成功")
        waitText("查看全部订单")
        compose.waitUntil(15000) { !compose.onNodeWithTag("pay_order").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled) }
        screenshot("06_paid")
        compose.onNodeWithTag("pay_order").performClick()
        waitText("已支付")
        compose.onNodeWithText("我的订单").assertExists()
        screenshot("07_orders")
    }

    @Test fun crossShopCartRequiresConfirmation() {
        mode(true)
        waitTag("shop_rice")
        compose.onNodeWithTag("shop_rice").performClick()
        compose.onNodeWithTag("plus_r1").performClick()
        waitTag("qty_r1")
        compose.onNodeWithTag("back").performClick()
        compose.onNodeWithTag("shop_list").performScrollToNode(hasTestTag("shop_burger"))
        compose.onNodeWithTag("shop_burger").performClick()
        compose.onNodeWithTag("plus_b1").performClick()
        compose.onNodeWithText("清空并添加").performClick()
        waitTag("qty_b1")
        compose.onNodeWithTag("open_cart").performClick()
        compose.onNodeWithText("招牌黄焖鸡米饭").assertDoesNotExist()
        compose.onNodeWithTag("clear_cart").performClick()
        compose.onNodeWithTag("confirm_clear").performClick()
        waitText("购物车是空的")
    }

    @Test fun onlineOrderUsesBackend() {
        mode(false)
        waitTag("shop_rice")
        compose.onNodeWithTag("shop_rice").performClick()
        compose.onNodeWithTag("plus_r1").performClick()
        waitTag("qty_r1")
        compose.onNodeWithTag("checkout").performClick()
        compose.onNodeWithTag("submit_order").performClick()
        waitText("等待付款")
        compose.onNodeWithTag("order_details").performScrollToNode(hasText("联网演示订单"))
        compose.onNodeWithText("联网演示订单").assertExists()
        compose.onNodeWithTag("pay_order").performClick()
        compose.onNodeWithTag("confirm_pay").performClick()
        compose.onNodeWithTag("order_details").performScrollToIndex(0)
        waitText("支付成功")
        screenshot("08_online_paid")
    }
}
