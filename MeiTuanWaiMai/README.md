# 仿美团外卖（教材第 12 章项目 + 完整功能扩展）

《Android 移动开发基础案例教程》（黑马程序员）第 12 章综合项目的完整实现，
并在教材 12.1 ~ 12.7 的基础上，补齐了正常外卖 App 的核心功能：
搜索、分类频道、排序筛选、店铺评价、收藏、优惠券、收货地址、订单中心、个人中心。

---

## 一、开发环境

| 项目 | 版本 |
| --- | --- |
| 语言 | Java |
| 编译 SDK | 34（Android 14） |
| 最低 SDK | 21（Android 5.0） |
| 构建工具 | Gradle 8.2 + Android Gradle Plugin 8.1.4 |
| JDK | 17（Android Studio 自带 JBR 即可） |
| 依赖库 | androidx.appcompat:1.6.1、androidx.viewpager:1.0.0 |

> 没有引入任何第三方 UI 框架，全部用原生控件 + 自定义控件实现，方便对照教材理解。

---

## 二、目录结构

```
MeiTuanWaiMai/
├── settings.gradle / build.gradle / gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/
└── app/
    ├── build.gradle
    └── src/main/
        ├── AndroidManifest.xml          注册 14 个 Activity + 联网权限
        ├── assets/
        │   └── shop.json                ★ 15 家店铺 / 约 90 道菜品 / 3 张广告图
        ├── java/cn/itcast/order/
        │   ├── activity/                14 个界面
        │   │   ├── ShopListActivity.java       首页：搜索入口 + 分类频道 + 排序筛选 + 店铺列表 + 底部导航
        │   │   ├── SearchActivity.java         搜索：历史记录 / 热门搜索 / 结果列表（也承载分类频道）
        │   │   ├── ShopDetailActivity.java     店铺详情：点菜、购物车弹窗、收藏、评价入口
        │   │   ├── ReviewListActivity.java     店铺评价列表
        │   │   ├── FoodDetailActivity.java     菜品详情
        │   │   ├── OrderActivity.java          确认订单：地址 / 送达时间 / 优惠券 / 备注 / 餐具
        │   │   ├── PayActivity.java            支付
        │   │   ├── PaySuccessActivity.java     支付成功
        │   │   ├── OrderListActivity.java      订单中心：全部 / 待支付 / 配送中 / 已完成
        │   │   ├── OrderDetailActivity.java    订单详情
        │   │   ├── MineActivity.java           我的
        │   │   ├── AddressListActivity.java    收货地址列表（支持结算页选择）
        │   │   ├── AddressEditActivity.java    新增 / 编辑 / 删除地址
        │   │   ├── CouponActivity.java         优惠券（选择 / 浏览）
        │   │   └── FavoriteActivity.java       我的收藏
        │   ├── adapter/                 9 个数据适配器
        │   │   ├── AdBannerAdapter / ShopAdapter / MenuAdapter / CartAdapter / OrderAdapter
        │   │   ├── OrderCenterAdapter.java     订单中心条目（含操作按钮）
        │   │   ├── AddressAdapter / CouponAdapter / ReviewAdapter
        │   ├── bean/
        │   │   ├── ShopBean / FoodBean               店铺、菜品
        │   │   ├── OrderBean / OrderItemBean         订单、订单商品快照
        │   │   ├── AddressBean / CouponBean / ReviewBean
        │   ├── db/
        │   │   ├── OrderDbHelper.java        SQLite 建表（t_order）
        │   │   └── OrderDao.java             订单插入 / 查询 / 状态更新
        │   ├── utils/
        │   │   ├── Constant / HttpUtil               服务器地址开关与请求
        │   │   ├── ShopDataUtils / ShopRepository    JSON 解析、内存缓存、搜索 / 分类
        │   │   ├── CartManager.java                  购物车
        │   │   ├── AppPrefs.java                     昵称 / 收藏 / 搜索历史 / 地址（SP）
        │   │   ├── CouponUtils.java                  满减解析、自动选最优券
        │   │   └── ReviewDataUtils.java              评价假数据
        │   └── view/
        │       ├── ShopListView.java                 可完整展开的 ListView
        │       ├── ViewPagerIndicator.java           广告栏小圆点
        │       └── FlowLayout.java                   自动换行标签（搜索历史 / 备注标签）
        └── res/
            ├── layout/                  约 30 个布局文件
            ├── drawable/                图标、圆角背景、状态选择器、分类圆形底
            ├── drawable-nodpi/          店铺图 / 菜品图 / 广告图 / 收款二维码
            ├── color/                   nav_tint、sort_text_color 状态色
            └── values/                  colors、strings、themes
```

---

## 三、运行方式

### 方式 1：Android Studio（推荐）

1. `File` → `Open`，选择 `MeiTuanWaiMai` 目录（有 `settings.gradle` 的那一层）。
2. 首次同步会下载 Gradle 8.2 与 AGP 8.1.4，需要联网。
3. 缺少 SDK 34 时按提示安装。
4. 连接模拟器或真机，点击 ▶ 运行。

### 方式 2：命令行

```bash
cd MeiTuanWaiMai
# Windows
gradlew.bat assembleDebug
# macOS / Linux
./gradlew assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

### 方式 3：走真正的服务器（对应教材 12.3）

项目默认读 `assets/shop.json`，离线即可运行。若要还原 Tomcat 取数据的流程：

1. 安装 Tomcat，在 `webapps` 下新建 `order` 目录；
2. 把 `app/src/main/assets/shop.json` 复制进去；
3. 打开 `utils/Constant.java`，把 `USE_SERVER` 改成 `true`；
4. 按调试方式确认 `SHOP_JSON_URL`：
   - 模拟器访问本机：`http://10.0.2.2:8080/order/shop.json`（默认值）
   - 真机：改成电脑局域网 IP，并保证同一 WiFi。

---

## 四、功能与交互流程

```
首页（底部导航：首页 / 订单 / 我的）
 ├─ 顶部定位 + 搜索框 ──▶ 搜索页（历史、热门、结果）
 ├─ 广告栏：ViewPager 滑动 + 小圆点 + 3 秒自动轮播
 ├─ 分类频道：美食 / 快餐 / 甜品 / 奶茶 / 水果 / 超市 / 药品 / 鲜花
 │    └─ 点击 ──▶ 搜索结果页（按分类过滤）
 ├─ 排序：综合 / 销量 / 评分 / 起送价
 ├─ 筛选：满减优惠 / 免配送费
 └─ 店铺列表 ──▶ 店铺详情
                 ├─ 收藏（标题栏右侧，写入 SP）
                 ├─ 评价入口 ──▶ 评价列表（评分概览 + 8 条评价）
                 ├─ 菜单：+/− 加减，底部购物车栏实时联动
                 ├─ 购物车弹窗：增减 / 清空 / 起送价校验
                 └─ 去结算 ──▶ 确认订单
                               ├─ 收货地址（可选 / 可新增 / 可编辑）
                               ├─ 送达时间（立即送出 / 30 分钟后 / 60 分钟后）
                               ├─ 已选商品
                               ├─ 优惠券（自动选最优，可手动更换或不使用）
                               ├─ 备注（6 个快捷标签 + 手动输入）
                               ├─ 餐具份数（0~10）
                               ├─ 金额：商品 + 配送费 − 优惠
                               └─ 提交订单 ──▶ 生成待支付订单（SQLite）──▶ 支付页
                                                                          └─ 确认支付 ──▶ 状态改为配送中
                                                                                        ──▶ 支付成功页
                                                                                              ├─ 查看订单
                                                                                              └─ 返回首页

订单中心：全部 / 待支付 / 配送中 / 已完成
 ├─ 待支付：取消订单 / 去支付 / 详情
 ├─ 配送中：取消订单 / 查看详情
 └─ 已完成、已取消：再来一单（商品回填购物车并进入店铺）

我的：昵称修改、订单数量统计、收货地址、我的收藏、优惠券、关于
```

### 关键设计点

- **购物车状态放在 `CartManager`**：菜单列表、角标、总价三处始终一致；换店自动清空。
- **订单落库用 SQLite**：`t_order` 表把商品明细以 JSON 存在 `items_json` 列，订单一旦生成就是快照，菜品改价不影响历史订单。
- **订单状态机**：`0 待支付 → 1 配送中 → 2 已完成`，`3 已取消`；所有按钮的显示都由状态驱动。
- **金额全程 `BigDecimal`**：满减、配送费、实付都保留两位小数，避免 double 精度误差。
- **优惠券自动择优**：`CouponUtils.bestCoupon()` 从店铺满减券与平台券中挑减免最多的一张，用户在结算页仍可手动更换。
- **`FlowLayout` 自定义控件**：搜索历史、热门词、备注标签都靠它自动换行。
- **`ShopRepository` 内存缓存**：`shop.json` 只解析一次，首页、搜索、分类、收藏、再来一单共用同一份数据。
- **网络请求放子线程**：`ShopListActivity.initData()` 读数据后 `runOnUiThread()` 刷新 UI。

---

## 五、数据格式（assets/shop.json）

```json
{
  "banners": ["banner1", "banner2", "banner3"],
  "shops": [
    {
      "id": 1,
      "shopName": "肯德基宅急送",
      "score": 4.8,
      "saleNum": 1200,
      "offerPrice": 20,
      "distributionCost": 5,
      "feature": "满30减10",
      "time": "30分钟",
      "category": "快餐",
      "banner": "shop1",
      "foodList": [
        {
          "foodId": 101,
          "foodName": "香辣鸡腿堡",
          "popularity": "人气爆款",
          "saleNum": 97,
          "price": 18.5,
          "foodPic": "food1"
        }
      ]
    }
  ]
}
```

当前共 **15 家店铺 / 约 90 道菜品**。`feature` 支持 `满30减10` 这种格式，
`CouponUtils` 会用正则解析成店铺券；像“第二杯半价”这类文案不会解析成券，但不影响展示。

`banner`、`foodPic`、`banners` 填的是 `res/drawable-nodpi` 下的图片名，
运行时由 `ShopDataUtils.getDrawableId()` 通过 `getIdentifier()` 转成资源 id。

---

## 六、本地存储说明

| 数据 | 存储方式 | 位置 |
| --- | --- | --- |
| 订单 | SQLite | `meituan_order.db` → `t_order` |
| 昵称 / 手机号 | SharedPreferences | `meituan_prefs` |
| 收藏店铺 | SharedPreferences | `favorite_ids`（逗号分隔） |
| 搜索历史 | SharedPreferences | `search_history`（竖线分隔，最多 10 条） |
| 收货地址 | SharedPreferences | `address_list`（JSON 数组） |

卸载 App 或清除数据后以上内容会重置。

---

## 七、教材章节对应

| 教材小节 | 内容 | 本项目对应文件 |
| --- | --- | --- |
| 12.1 | 项目展示与功能分析 | README 第四节 |
| 12.2 | 项目搭建 | `build.gradle`、`AndroidManifest.xml` |
| 12.3 | 服务器数据准备 | `assets/shop.json`、`Constant.java`、`HttpUtil.java` |
| 12.4.1 | 搭建标题栏 | `layout/title_bar.xml` |
| 12.4.2 | 搭建广告栏 | `layout/adbanner.xml`、`ViewPagerIndicator.java` |
| 12.4.3 | 搭建店铺列表界面 | `layout/activity_shop_list.xml` |
| 12.4.4 | 搭建店铺列表条目 | `layout/shop_item.xml` |
| 12.4.5 | 封装店铺 / 菜品信息 | `bean/ShopBean.java`、`bean/FoodBean.java` |
| 12.4.6 | 编写广告栏适配器 | `adapter/AdBannerAdapter.java` |
| 12.4.7 | 编写店铺列表适配器 | `adapter/ShopAdapter.java` |
| 12.4.8 | 店铺列表界面显示功能 | `activity/ShopListActivity.java` |
| 12.5.1 | 搭建店铺详情界面 | `layout/activity_shop_detail.xml` |
| 12.5.2 | 搭建菜单列表条目 | `layout/menu_item.xml` |
| 12.5.3 | 搭建购物车列表条目 | `layout/cart_item.xml` |
| 12.5.4 | 搭建确认清空购物车界面 | `layout/dialog_clear_cart.xml` |
| 12.5.5 | 编写菜单列表适配器 | `adapter/MenuAdapter.java` |
| 12.5.6 | 编写购物车列表适配器 | `adapter/CartAdapter.java` |
| 12.5.7 | 店铺详情界面显示功能 | `activity/ShopDetailActivity.java` |
| 12.6.1 | 搭建菜品详情界面 | `layout/activity_food_detail.xml` |
| 12.6.2 | 菜品详情界面显示功能 | `activity/FoodDetailActivity.java` |
| 12.7.1 | 搭建订单界面 | `layout/activity_order.xml` |
| 12.7.2 | 搭建订单列表条目 | `layout/order_item.xml` |
| 12.7.3 | 搭建支付界面 | `layout/activity_pay.xml` |
| 12.7.4 | 编写订单列表适配器 | `adapter/OrderAdapter.java` |
| 12.7.5 | 订单与支付显示功能 | `activity/OrderActivity.java`、`activity/PayActivity.java` |

---

## 八、图片素材

`drawable-nodpi` 里的店铺图、菜品图、广告图、二维码是脚本生成的**占位素材**（渐变底色 + 文字），
不是真实商品照片，仅用于课程设计演示，避免版权问题。换真图时保持文件名不变直接替换即可。
