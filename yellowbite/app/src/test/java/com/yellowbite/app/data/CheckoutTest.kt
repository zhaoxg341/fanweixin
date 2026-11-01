package com.yellowbite.app.data

import org.junit.Assert.*
import org.junit.Test

class CheckoutTest {
    private val shop = Shop("rice", "测试店铺", "家常菜", 4.9, 10, 20, "1km", 200, 2000, "", emptyList(), "rice",
        listOf(Dish("r1", "鸡肉饭", 2290, "", "主食", 10, "rice"), Dish("r2", "饮料", 890, "", "饮品", 10, "drink")))

    @Test fun amountsUseCentsAndIgnoreOtherShops() {
        assertEquals(5470, subtotal(shop, listOf(CartRow("r1", "rice", 2), CartRow("r2", "rice", 1))))
        assertEquals("54.70", money(5470))
        assertEquals(0, subtotal(shop, emptyList()))
        assertEquals(0, subtotal(shop, listOf(CartRow("r1", "other", 2))))
        assertEquals(0, subtotal(null, listOf(CartRow("r1", "rice", 2))))
    }

    @Test fun addressValidation() {
        assertNull(validateAddress("同学", "13800138000", "大学城学生宿舍1号楼"))
        assertNotNull(validateAddress(" ", "13800138000", "大学城学生宿舍1号楼"))
        assertNotNull(validateAddress("同学", "123", "大学城学生宿舍1号楼"))
        assertNotNull(validateAddress("同学", "13800138000", "宿舍"))
    }
}
