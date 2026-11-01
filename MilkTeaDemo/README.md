# 茶语时光 · 奶茶点单 Demo（MilkTeaDemo）

一个用 **Java + Android 原生控件**写的奶茶店点单 App，工程配置与《Android 移动开发基础案例教程》第 12 章「仿美团外卖」完全一致：

- Gradle **8.2** / AGP **8.1.4** / compileSdk **34** / minSdk **21**
- JDK **17**
- 无第三方 UI 库，只依赖 `androidx.appcompat:1.6.1`

从「进店点单」到「支付成功拿到取餐码」是一条完整可走通的闭环，订单会真正写进 SQLite，杀掉进程再打开还在。

---

## 一、功能清单

| 模块 | 做了什么 |
|---|---|
| 点单首页 | 渐变门店头部、搜索入口、banner、公告、分类横滑切换、商品列表、底部购物车栏（数量角标 / 合计 / 满减提示） |
| 商品详情 | 大图、描述、划线原价；杯型 / 温度 / 糖度 / 加料四组规格；改规格和数量实时算价 |
| 搜索 | 按名称或描述模糊匹配，边输边搜，复用首页商品列表 |
| 购物车 | 同商品不同规格分行；单项加减、清空（自绘确认弹窗）、满减提示、合计 |
| 确认订单 | 到店自取 / 外卖配送切换（影响配送费和地址栏）、门店信息、商品明细、备注、金额明细 |
| 收银台 | 微信支付 / 会员余额两种方式（模拟），金额与件数汇总 |
| 支付成功 | 大对勾、实付金额、四位取餐码；外送单显示订单尾号与骑手提示 |
| 订单列表 | 按时间倒序；状态驱动按钮：待支付→去支付、制作中→确认取餐、已完成→再来一单 |
| 订单详情 | 状态与引导语、取餐码 / 收货地址、商品明细、金额拆分、订单号与下单时间 |
| 我的 | 头像昵称（点头部可改）、会员卡、优惠券、收货地址（可改）、客服、关于 |

支持**横竖屏自适应布局**（LinearLayout 权重 + ScrollView），小屏手机也不会把底部按钮挤掉。

---

## 二、工程结构

```
MilkTeaDemo/
├─ app/src/main/
│  ├─ assets/milktea.json              # 门店 / 分类 / 商品 / 规格 / 满减规则
│  ├─ java/cn/itcast/milktea/
│  │  ├─ activity/
│  │  │  ├─ MainActivity               # 三个 Tab 的容器
│  │  │  ├─ GoodsDetailActivity        # 选规格、算价、加购
│  │  │  ├─ CartActivity               # 购物车
│  │  │  ├─ ConfirmOrderActivity       # 下单（写库）
│  │  │  ├─ PayActivity                # 收银台（改状态）
│  │  │  ├─ PaySuccessActivity         # 取餐码
│  │  │  ├─ OrderDetailActivity        # 订单详情
│  │  │  └─ SearchActivity             # 搜索
│  │  ├─ fragment/                     # Home / Orders / Mine
│  │  ├─ adapter/                      # Goods / Cart / Order
│  │  ├─ bean/                         # Shop / Category / Goods / Option / CartItem / Order / OrderItem
│  │  ├─ db/                           # OrderDbHelper + OrderDao
│  │  ├─ utils/                        # Constant / GoodsDataUtils / CartManager / PriceUtils / AppPrefs
│  │  └─ view/FlowLayout.java          # 自动换行的规格容器
│  └─ res/
│     ├─ layout/                       # 18 个布局
│     ├─ drawable/                     # 背景、按钮、矢量图标
│     ├─ drawable-nodpi/               # 11 张商品图 + 1 张 banner
│     └─ values/                       # colors / strings / themes
├─ build.gradle / settings.gradle / gradle.properties / local.properties
└─ 启动工程：用 Android Studio 直接 Open 本目录
```

---

## 三、四条核心链路

看懂这四条链，这个项目就通了。

### 1）商品列表怎么显示

```
assets/milktea.json
  → GoodsDataUtils.parse()（第一次用时解析，缓存在静态 List）
  → HomeFragment.selectCategory()
  → GoodsAdapter.setData() → notifyDataSetChanged()
  → getView()（convertView + ViewHolder 复用）
```

### 2）点「+」后购物车怎么变

```
GoodsAdapter 点击 +
  → CartManager.addDefault(goods)          // 加一条"中杯/冰/七分糖"
  → CartManager.getCountByGoods(goodsId)   // 数量不从 Item 里存，每次现查
  → notifyDataSetChanged() + 首页刷新底部栏
```

### 3）下单到支付（最重要）

```
CartManager（内存）
  → ConfirmOrderActivity 快照成 OrderBean + OrderItemBean
  → OrderDao.insertOrder()   [事务：主表 + 明细表一起成功]
  → 状态 = STATUS_UNPAID（待支付）
  → PayActivity 点击支付 → updateStatus(STATUS_MAKING)
  → PaySuccessActivity 展示取餐码
  → OrdersFragment.onResume() 重新查库 → 列表更新
```

### 4）再来一单

```
订单详情/列表的「再来一单」
  → CartManager.reorder(context, order)
  → 按 order_item 里的 goodsId 找回商品，按原规格、原单价重新入车
  → 跳购物车
```

---

## 四、数据结构

### assets/milktea.json

```json
{
  "shop": { "name": "...", "deliveryTime": "25分钟", "deliveryFee": 3,
            "notice": "...", "banner": "banner1", "address": "...", "openTime": "..." },
  "categories": [ { "id": 1, "name": "招牌必喝" } ],
  "goods": [ { "goodsId": 101, "categoryId": 1, "name": "招牌珍珠奶茶",
               "desc": "...", "price": 12, "originalPrice": 15,
               "pic": "drink1", "sales": 3218, "badge": "人气TOP1" } ],
  "specs": {
    "sizes":    [ { "name": "中杯", "extra": 0 }, { "name": "大杯", "extra": 3 } ],
    "temps":    ["冰", "少冰", "常温", "热"],
    "sugars":   ["全糖", "七分糖", "半糖", "三分糖", "无糖"],
    "toppings": [ { "name": "珍珠", "extra": 2 } ]
  },
  "coupons": [ { "threshold": 25, "reduce": 4 }, { "threshold": 40, "reduce": 10 } ]
}
```

> 图片字段只写资源名（`drink1`），`GoodsDataUtils.getDrawableId()` 用 `getIdentifier()` 去 `drawable-nodpi` 里找。
> 资源名不能带 `.jpg` 后缀，这是 Android 资源命名的硬规则，也是新手最容易踩的坑。

### SQLite（milktea.db）

```sql
-- 订单主表
CREATE TABLE t_order (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no TEXT NOT NULL,        -- 订单号 MT + 时间戳 + 随机数
    created_at INTEGER NOT NULL,   -- 毫秒时间戳
    goods_total TEXT, packing_fee TEXT, delivery_fee TEXT,
    discount TEXT, total TEXT,     -- 金额统一 TEXT 存 BigDecimal，避免精度丢失
    status INTEGER DEFAULT 0,      -- 0待支付 1制作中 2已完成 3已取消
    mode INTEGER DEFAULT 0,        -- 0自取 1外送
    pickup_code TEXT, address TEXT, remark TEXT
);

-- 订单明细（一对多）
CREATE TABLE t_order_item (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_no TEXT NOT NULL,
    goods_id INTEGER,              -- 再来一单时靠它找回商品
    name TEXT, spec_text TEXT, unit_price TEXT, count INTEGER
);
```

---

## 五、几个值得讲的设计点

1. **金额一律用 `BigDecimal`**
   `double` 会出现 `0.1 + 0.2 = 0.30000000000000004`，算钱必须用 `BigDecimal`，并且 `setScale(2, HALF_UP)` 统一两位小数。

2. **购物车的 key 不是 goodsId，而是 `goodsId|规格文本`**
   「大杯/热/半糖」和「中杯/冰/七分糖」是两行，不能合并。用 `LinkedHashMap` 又能去重、又能保持加入顺序。

3. **数量不存两份**
   商品列表上的数字永远从 `CartManager.getCountByGoods()` 现查，避免「列表显示 2、购物车显示 1」这种状态不同步。

4. **下单用事务**
   `OrderDao.insertOrder()` 里 `beginTransaction()` 把主表和明细表包在一起，要么都写成功，要么都不写，不会出现「有订单没明细」的脏数据。

5. **状态机驱动 UI**
   订单只有四个状态，列表按钮、详情页文案都由 `status` 决定，不用给每种情况写一个页面。

6. **Fragment 用 add + show/hide，不用 replace**
   `replace` 会销毁上一个 Fragment 的 View，切 Tab 时列表滚动位置会丢；`add + show/hide` 只是隐藏，状态能保留。

7. **列表复用三件套**
   `convertView` 复用 + `ViewHolder` 减少 `findViewById` + 数据与视图分离。这是 ListView 性能的基本功。

8. **规格容器用自定义 `FlowLayout`**
   不同手机宽度不一样，「大杯 / 少冰 / 半糖 / 加珍珠」可能换行，`FlowLayout` 会自动测量并折行，比手写两行 LinearLayout 稳。

---

## 六、怎么编译

环境要求：**JDK 17**（Gradle 8.2 不支持 JDK 25）、Android SDK 34。

Android Studio 打开本目录直接 Run 即可。命令行方式（本机已验证）：

```bat
set JAVA_HOME=C:\Users\lenovo\.jdks\jbr-17.0.14
set GRADLE_USER_HOME=D:\SDK\gradle-8.2
D:\SDK\gradle-8.2\bin\gradle.bat assembleDebug --offline
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

本机实测：`BUILD SUCCESSFUL`，APK 约 **3.56 MB**，静态校验 0 问题（18 个布局、48 个 drawable、52 个字符串、141 个布局 id 全部对得上）。

---

## 七、想改数据，改哪里

| 想做的事 | 改哪里 |
|---|---|
| 加一款奶茶 | `assets/milktea.json` 的 `goods` 数组加一条，图片放进 `drawable-nodpi` |
| 加一个分类 | `categories` 加一条，商品 `categoryId` 对上即可 |
| 改满减规则 | `coupons` 数组（目前代码取前两档） |
| 改配送费 | `shop.deliveryFee` |
| 改打包费 | `utils/Constant.java` 的 `PACKING_FEE_PER_ITEM` / `PACKING_FEE_MAX_ITEMS` |
| 加一种杯型/加料 | `specs.sizes` / `specs.toppings` 加一条，`extra` 是加价 |
| 改主题色 | `res/values/colors.xml` 的 `primary` 系列 |

---

## 八、边界说明

- 支付是**本地模拟**，没有接微信/支付宝 SDK，不会产生真实扣款。
- 没有做登录和账号体系，「我的」里的昵称、地址存在 SharedPreferences。
- 门店只有一家，没有做多门店距离排序。
- 订单状态不会自动流转（不会 5 分钟后自动「制作完成」），需要手动点「确认取餐」。
- 商品图由 AI 生成，仅用于课程设计演示。
