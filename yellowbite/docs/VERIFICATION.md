# 验证记录

验证日期：2026-10-12（Asia/Shanghai）。交付文件：[yellowbite-demo.apk](../artifacts/yellowbite-demo.apk)。

## 构建和运行环境

- Kotlin 2.2.10、Compose BOM 2026.09.00、Material 3、Navigation 3。
- AGP 9.4.0、Gradle 9.7.1、JDK 21。
- compileSdk 37.2、targetSdk 36、minSdk 26。
- Android 15 / API 35 x86_64 模拟器，1080 × 2340，440 dpi。
- FastAPI 0.137.1、SQLAlchemy 2.0.48、Pydantic 2.13.4、Uvicorn 0.49.0。

## 功能与证据

| 功能 | 验证结果与证据 |
| --- | --- |
| 店铺列表、标题栏、搜索 | 显示六家店铺；搜索“谷香”后排除其他店铺。见 [首页](../artifacts/screenshots/01_home.png)。 |
| 广告栏 | 已观察自动轮播从“今天，也要好好吃饭”切换到“轻一点，也很满足”。见 [轮播证据](../artifacts/test-results/carousel.json)。 |
| 店铺详情与菜单 | 展示商家信息、分类、菜品图片和价格。见 [店铺](../artifacts/screenshots/02_shop.png)。 |
| 菜品详情 | 点击进入图片、价格、说明页面，再添加商品。见 [菜品](../artifacts/screenshots/03_dish.png)。 |
| 购物车 | 加减、清空确认、起送价、跨店确认经过点击测试；数量按钮已修正间距。见 [购物车](../artifacts/screenshots/04_cart.png)。 |
| 确认订单 | 校验手机号与地址；22.90元商品加2.00元配送费合计24.90元。见 [确认订单](../artifacts/screenshots/05_checkout.png)。 |
| 订单与支付 | 离线及联网均走通提交、模拟支付、订单列表流程。见 [支付](../artifacts/screenshots/06_paid.png)、[订单](../artifacts/screenshots/07_orders.png)。 |
| 后端 | 独立虚拟环境执行启动脚本，健康检查返回200；联网订单实际写入SQLite。 |
| 持久化 | 加入一份菜品后强制退出进程，Room中数量仍为1；重新打开后数量及金额正确。4个已支付订单在重启后保留。见 [购物车恢复](../artifacts/screenshots/09_cart_restored.png)、[订单恢复](../artifacts/screenshots/10_orders_restored.png)、[数据证据](../artifacts/test-results/persistence.json)。 |

## 测试结果

- Android单元测试：2项通过，金额计算及地址校验。见 [JUnit报告](../artifacts/test-results/android-unit.xml)。
- Android交互测试：3项通过，0失败，0跳过。覆盖离线下单支付、联网下单支付和跨店购物车。见 [设备报告](../artifacts/test-results/android-ui.xml)。
- 后端测试：2项通过。覆盖目录、金额、起送价、无效商品、无效手机号、重复请求、重复支付和订单状态。见 [后端报告](../artifacts/test-results/backend.xml)。
- scripts/build.ps1实际执行成功，生成APK与通过设备测试的APK哈希一致。
- scripts/run-backend.ps1 -Port 8001在独立虚拟环境启动成功，GET /health返回200。

## APK校验

文件大小：23,588,473字节（约23.6 MB）。

SHA-256：

    bab5bfc1dd045e7f809b44640d732901a23a55b75dd74d0d387247aaaaebbe78

支付、配送、商家数据均为演示流程。应用默认离线模式可直接演示；联网模式需启动后端。
