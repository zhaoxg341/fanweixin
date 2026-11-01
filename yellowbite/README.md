# 小黄外卖 · Android 课程设计 Demo

原生 Android 外卖演示应用，使用 Kotlin、Jetpack Compose、Material 3、Navigation 3、ViewModel / StateFlow、Hilt、Ktor、Coil、Room 和 DataStore。配套后端使用 FastAPI、Pydantic、SQLAlchemy 与 SQLite。

交付文件：artifacts/yellowbite-demo.apk（安装包）与 artifacts/yellowbite-source.zip（含源码、截图和测试记录）。

## 快速演示

1. 安装 `artifacts/yellowbite-demo.apk`，需要 Android 8.0 或更高版本。
2. 默认是离线演示，无需账号和服务器。
3. 在首页选择“谷香·现炒小碗菜”，点开“招牌黄焖鸡米饭”，点击加号。
4. 打开底部购物车，可加减数量、清空商品；点击“去结算”。
5. 确认收货信息、填写备注，提交订单。
6. 点击“立即支付”，选择模拟支付方式，再确认模拟支付。
7. 在“订单”页查看已支付订单。关闭应用后重开，购物车、订单及配送信息仍会保存。

**支付是课程演示，不调用真实支付平台，不产生扣款。店铺、月售、评分、营业时间与配送信息均为演示数据，菜品图片仅供示意。**

## 在这台电脑上打开模拟器

双击项目根目录的 **启动安卓演示.cmd**。它会打开 YellowBite 虚拟手机，等待开机，再安装并启动“小黄外卖”。首次开机可能需要1至3分钟，保持离线演示即可体验。

也可在项目目录的 PowerShell 中执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run-demo.ps1
```

当前电脑的模拟器程序在 `.tools/sdk/emulator/emulator.exe`，系统镜像在 `.tools/sdk/system-images`，虚拟手机及其应用数据在 `.tools/avd/YellowBite.avd`。启动器会自动创建临时英文路径入口，兼容项目的中文目录；实际文件仍在项目内。

此快捷入口使用这台电脑已准备好的 `.tools` 环境；源码压缩包不包含大型SDK和系统镜像。其他电脑可以使用Android Studio创建模拟器，再安装APK。

## 已实现功能

- 首页：地址标题栏、搜索、自动轮播广告、品类筛选、评分/销量/配送速度排序、六家店铺。
- 店铺详情：商家信息、菜品分类、菜单、数量加减、起送价校验。
- 菜品详情：图片、名称、价格、说明、加入购物车。
- 购物车：数量同步、总价计算、底部弹窗、清空确认、跨店购物车替换确认、单品最多99份。
- 下单：收货人、手机号、配送地址、备注、费用明细；输入校验、提交期间防重复点击。
- 订单：待付款/已支付筛选、明细、状态、订单编号、时间、模拟支付。
- 持久化：Room 保存购物车和订单，DataStore 保存模式、服务地址和配送信息。
- 联网：获取服务器数据、创建订单、同步订单、模拟支付；服务器校验商品和金额、起送价与重复请求。
- 网络异常：提示错误，保留购物车供重试；订单保留来源服务器，切换模式不会误把联网订单当离线订单支付。

## 联网演示

电脑安装 Python 3.11 或更高版本。在项目目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-backend.ps1
```

首次运行会在 `backend/.venv` 中安装依赖。接口文档：<http://127.0.0.1:8000/docs>。

在 App 的“我的”页关闭“离线演示”，填写后端服务地址，点击“保存并应用”。

- Android 官方模拟器：`http://10.0.2.2:8000`。
- USB 真机：执行 `adb reverse tcp:8000 tcp:8000`，App 填写 `http://127.0.0.1:8000`。
- 同一 Wi-Fi 真机：填写电脑局域网 IP，例如 `http://192.168.1.10:8000`，允许系统防火墙访问8000端口。

后端启动后，店铺与菜品使用 `backend/catalog.json`，订单保存在 `backend/demo.db`。同一演示服务共享一个演示用户的订单列表。

| 接口 | 用途 |
| --- | --- |
| `GET /health` | 健康检查 |
| `GET /shops` | 店铺与菜单 |
| `GET /shops/{id}` | 店铺详情 |
| `POST /orders` | 创建订单，服务端计算金额 |
| `GET /orders` | 订单列表 |
| `GET /orders/{id}` | 订单详情 |
| `POST /orders/{id}/pay` | 模拟支付，重复调用保持已支付 |

## 源码运行与构建

用 Android Studio 打开项目根目录，使用支持 AGP 9.4 的稳定版本。项目采用 AGP 内置 Kotlin，Compose 编译器插件版本在 Version Catalog 中管理。

- 编译 SDK：Android 37.2；目标版本：Android 36；最低版本：Android 26。
- Gradle：9.7.1；推荐使用 JDK 21。
- 配置本机 Android SDK，Android Studio 会生成 `local.properties`。
- 不依赖 `.tools` 中的临时验证工具，标准 Android Studio 环境可直接构建。

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

或使用 `scripts/build.ps1` 构建并复制 APK 到 `artifacts`。

```powershell
python -m pip install -r backend\requirements.txt
python -m pytest backend\test_api.py -q
.\gradlew.bat :app:connectedDebugAndroidTest
```

UI 测试需要已连接的模拟器/真机、清洁的离线演示状态，以及正在运行的后端（默认模拟器地址 `http://10.0.2.2:8000`）。测试覆盖离线下单、跨店购物车和联网下单。测试前可以执行 `adb shell pm clear com.yellowbite.app` 清理本应用演示数据。

## 代码导航

```text
app/src/main/java/com/yellowbite/app/
  MainActivity.kt                 单 Activity 入口
  data/Models.kt                 店铺、菜品、订单、金额和地址校验
  data/LocalDatabase.kt          Room 购物车与订单存储
  data/DemoRepository.kt         离线/网络数据、购物车、下单、支付
  di/AppModule.kt                Hilt 依赖配置
  ui/AppViewModel.kt             状态与操作入口
  ui/App.kt                      Navigation 3 页面导航
  ui/HomeScreen.kt               首页
  ui/ShopScreens.kt              店铺、菜品、购物车
  ui/OrderScreens.kt             确认订单、订单列表、订单详情、支付
  ui/SettingsScreen.kt           演示模式与服务配置
backend/main.py                  FastAPI 后端
```

界面通过 ViewModel 发起操作；Repository 管理网络、数据库与业务逻辑；Room 的 Flow 更新界面。金额使用整数分，单次下单仅涉及一家店铺。客户端显示预估金额，联网订单以服务器返回金额为准。

图片来源见 `docs/IMAGE_CREDITS.md`。构建、测试和设备验证记录见 `docs/VERIFICATION.md`。
