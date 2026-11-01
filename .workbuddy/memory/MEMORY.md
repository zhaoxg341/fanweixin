# 项目长期笔记

## 工程清单（均在工作区根目录）
- `MeiTuanWaiMai/`：课设第 12 章仿美团外卖（Gradle 8.2 / AGP 8.1.4 / compileSdk 34 / JDK 17）。
  v2.0 已扩展为完整 App：底部导航（首页/订单/我的）、搜索+历史+热门、8 个分类频道、
  排序/筛选、店铺收藏与评价、优惠券（满减解析）、地址增删改、订单中心（SQLite + 状态机）、支付成功页。
  数据 `assets/shop.json`（15 家店/约 90 菜），本地存储 `meituan_order.db` + `meituan_prefs`。
  预览 `MeiTuanWaiMai_界面预览.html`（8 屏）。
- `WeiXinDemo/`：仿微信，含改备注/昵称、相册头像、20+ 关键词回复、局域网 Socket 在线聊天；
  v3 新增朋友圈互动（发动态：文字+相册9图，`t_moment`/`t_moment_comment`，点赞/评论/回复/删除自己的内容）
  与小游戏（猜拳/猜数字/翻牌，`GameActivity` + `GameLobbyActivity`，战绩可发到聊天）。
  v4 完善：`PhotoViewerActivity` + 自写 `ZoomImageView`（滑动翻页/双指缩放/双击）、
  聊天图片消息（`t_message.type`，DB v4 用 ALTER 加列）、`GameStats` 战绩持久化 + 大厅展示、
  猜数字区间提示、翻牌计时、聊天「⋯」菜单（看资料/清空记录）。
  图片存 `files/avatars`、`files/moments`。预览 `WeiXinDemo-界面预览.html`（21 屏）。
  配套电脑端 `ChatServer/`（纯 JDK，`启动服务端.bat`）。
- `MilkTeaDemo/`：奶茶点单 App（包名 `cn.itcast.milktea`），完整点单→购物车→下单→支付→订单闭环，
  数据 `assets/milktea.json` + SQLite `milktea.db`，界面预览在根目录 `MilkTeaDemo-界面预览.html`。

## 固定编译方式（不要用 gradlew，.part 未下完）
```
$env:JAVA_HOME="C:\Users\lenovo\.jdks\jbr-17.0.14"
$env:GRADLE_USER_HOME="C:\Users\lenovo\.gradle"
& "D:\SDK\gradle-8.2\bin\gradle.bat" -p "<工程目录>" assembleDebug --offline
```
产物：`<工程>/app/build/outputs/apk/debug/app-debug.apk`
（TaskOutput 可能显示 failed，是 PowerShell 把 gradle 的 stderr 警告当异常，以日志里的 BUILD SUCCESSFUL 为准）

## 工具/环境注意事项
- Bash 工具在本机不可用（缺 ls/sed/dirname），用 Glob/Grep/Read + PowerShell。
- PowerShell 工具不回显 stdout，结果写文件再 Read；PS 5.1 必须 `| Out-File -Encoding utf8`。
- `gradle.bat` 参数里的 `-Dfile.encoding=UTF-8` 要加引号，否则被拆成类路径。
- 静态校验脚本在 `_build/`：`check_weixin.py`、`check_milktea.py`、`check_meituan.py`；图片处理 `prep_images.py`、`wx_real_images.py`。
- 同一文件的多个 Edit 放同一条消息会互相覆盖（读到的都是旧内容，后写覆盖先写），同文件编辑必须逐条串行；
  编译报"找不到符号"时先确认改动是否真的落盘。
