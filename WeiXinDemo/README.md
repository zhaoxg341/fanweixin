# WeiXinDemo · 仿微信（Android 课设版）

一个与《Android 移动开发基础案例教程》第 12 章「仿美团外卖」**完全同一套工程配置**的仿微信 App。
初版是一个纯离线的界面仿写，现在又加了几个"真能用"的能力：

1. **改联系人备注** —— 资料页「设置备注和标签」真的能改，通讯录 / 会话列表 / 聊天标题栏三处一起生效
2. **改我的昵称** —— 点「我」页面的个人卡片就能改名，朋友圈封面也跟着变
3. **局域网在线聊天** —— 电脑上跑一个 Java 服务端，两台设备连同一个 WiFi 就能互相发消息
4. **改好友头像** —— 资料页「头像」行弹 16 宫格选择，通讯录 / 会话列表 / 聊天气泡一起换，可一键恢复默认
5. **关键词自动回复** —— 本地演示模式下，发「你好」回「你好」、发「吃饭了吗」回「吃了」、发「你喜欢谁」回「我是同性恋」
6. **发朋友圈动态** —— 文字 + 相册最多 9 张图，动态和配图都会持久化，退出 App 再进来还在
7. **点赞 / 评论 / 回复** —— 点评论写一条，点别人的评论变成「回复某人」，自己的动态和评论长按可删
8. **和联系人玩小游戏** —— 猜拳、猜数字、翻牌配对三个玩法，比分可以在聊天里发出去
9. **聊天发图片** —— 「+」→ 添加图片，从系统相册选一张，压缩后以图片气泡发出，点开可看大图
10. **全屏图片查看器** —— 朋友圈九宫格左右滑动翻页、双指缩放 3 倍、双击放大、单击隐藏顶栏
11. **游戏战绩长期保存** —— 猜拳胜负、猜数字最少次数、翻牌最少步数，游戏大厅可查看、可清空
12. **微信红包** —— 「+」→ 红包，填金额和留言；橙色红包气泡，点开有金色「開」，拆开显示金额；跟对方聊到「红包」TA 也会发一个过来
13. **语音消息** —— 点左下角喇叭切换成「按住 说话」，松开按秒数发出；气泡里是喇叭 + 秒数，点一下有播放动画
14. **消息长按菜单** —— 长按气泡可复制、撤回（自己发的、2 分钟内）、删除，撤回后显示灰色系统提示
15. **真实感头像与配图** —— 17 个好友头像全部换成真人照片风格，朋友圈配图换成生活实拍风格，封面换成山川湖景

原来的本地演示模式（读 `assets/weixin.json` + 模拟自动回复）**一行没删**，
在登录页不勾"连服务器"就还是原来那套。

## 一、工程配置（与仿美团外卖逐项对齐）

| 项目 | 版本 |
| --- | --- |
| Gradle | 8.2 |
| Android Gradle Plugin | 8.1.4 |
| compileSdk / targetSdk | 34 |
| minSdk | 21 |
| Java 源码级别 | 17 |
| 包名 / namespace | `cn.itcast.weixin` |
| 依赖 | `androidx.appcompat:appcompat:1.6.1`、`androidx.fragment:fragment:1.3.6` |

> 新增功能全部用 Android 自带能力实现（SharedPreferences / SQLite / Socket），**没有引入任何第三方库**。

## 二、功能范围

| 页面 | 内容 |
| --- | --- |
| 登录页 | 填昵称、是否连服务器、服务器地址与端口；已登录会自动跳过；也可从「我 → 设置」进来改 |
| 微信 | 会话列表（头像 / 名字 / 最后一条消息 / 时间 / 未读红点）、底部 Tab 未读总数红点 |
| 聊天 | 左右气泡、时间标签、输入栏；**「+」→ 添加图片可以发图片消息**；**右上角「⋯」可看联系人资料 / 清空聊天记录**；本地模式下按关键词回复（20+ 组常用话术），局域网模式下是真人对聊 |
| 通讯录 | 4 个功能入口 + 联系人按 A–Z 分组 + 右侧字母索引栏 |
| 发现 | 五个分组：朋友圈 / 视频号 / 扫一扫·摇一摇 / 看一看·搜一搜 / 附近·购物·**游戏（进游戏大厅）** |
| 我 | 个人卡片（**点一下可改昵称、从相册换头像、恢复默认头像**、显示服务器连接状态）+ 服务 / 收藏 / 朋友圈 / 卡包 / 表情 / 设置 |
| 朋友圈 | 封面（用当前昵称与头像）、动态列表、九宫格配图、**点赞 / 评论 / 回复 / 删除自己的内容 / 点图进全屏查看器** |
| 发动态 | 文字 + 相册多选（最多 9 张）+ 九宫格预览 + 单张删除，发布后立即出现在列表顶部 |
| 图片查看器 | 全屏黑底，左右滑动翻页、双指缩放、双击放大、单击隐藏顶栏；朋友圈和聊天图片共用 |
| 小游戏 | 猜拳（对手手势抖动后定格）、猜数字（1~100，七次机会 + **区间收窄提示**）、翻牌配对（4×2，**计步数与用时**），共用比分并**可把战绩发到聊天** |
| 游戏大厅 | **我的战绩**（猜拳胜负 / 猜数字最少次数 / 翻牌最少步数，可清空）+ 三个玩法卡片，点一个再选联系人开玩 |
| 联系人资料页 | 头像昵称、微信号、地区、四行功能项（**头像可换、备注可编辑**）、底部「发消息」 |

## 三、整体结构：三层

```
界面层   9 个 Activity + 4 个 Fragment + 6 个 Adapter
   ↓
数据层   ChatManager（内存数据中心，可写）   MomentDao（朋友圈，直接读写库）
         ├─ MessageDao   → SQLite t_message   聊天记录（文字 / 图片 / 红包 / 语音，可撤回）
         ├─ RemarkDao    → SQLite t_remark    联系人备注
         ├─ AvatarDao    → SQLite t_avatar    好友自定义头像
         ├─ ExtraChatDao → SQLite t_chat      在线聊天新建的会话
         └─ MomentDao    → SQLite t_moment / t_moment_comment   动态与评论
   ↓
通道层   AppPrefs（SharedPreferences：昵称 / 服务器地址 / 登录态）
         AutoReplyRules（关键词回复规则表）
         GameStats（SharedPreferences：小游戏战绩）
         AvatarStore / MomentPhotoStore（相册图片导入 + LruCache 显示）
         ChatClient → Socket → 电脑上的 ChatServer → 对方手机
```

这样分层的好处：**换通信方式不用动界面**。想从 Socket 改成 HTTP 轮询，
只要另写一个和 `ChatClient` 同接口的类，`ChatManager` 和所有页面都不用改。

## 四、目录结构

```
WeiXinDemo/
├─ settings.gradle / build.gradle / gradle.properties
├─ gradle/wrapper/                     # Gradle 8.2 wrapper
├─ local.properties                    # sdk.dir 指向本机 SDK
└─ app/src/main/
   ├─ AndroidManifest.xml
   ├─ assets/weixin.json               # 离线数据源（用户/会话/联系人/朋友圈）
   ├─ java/cn/itcast/weixin/
   │  ├─ WeixinApp.java                # Application：全局 Context + 预热 JSON
   │  ├─ activity/                     # LoginActivity / MainActivity / ChatActivity
   │  │                                # MomentsActivity / PublishMomentActivity / PhotoViewerActivity
   │  │                                # GameActivity / GameLobbyActivity / UserInfoActivity
   │  ├─ adapter/                      # Chat / Contact / Discover / Msg / Moment / AvatarPicker
   │  ├─ fragment/                     # Chats / Contacts / Discover / Me
   │  ├─ bean/                         # User / Chat / Contact / Moment / Msg / Address
   │  ├─ db/                           # WeixinDbHelper / MessageDao / RemarkDao
   │  │                                # AvatarDao / ExtraChatDao / MomentDao
   │  ├─ utils/                        # AppPrefs / ChatClient / ChatManager / Constant
   │  │                                # AutoReplyRules / HttpUtil / WeixinDataUtils
   │  │                                # AvatarStore / MomentPhotoStore / GameStats
   │  └─ view/                         # CircleImageView / SideIndexBar / FlowLayout / ZoomImageView
   └─ res/                             # 35 个布局、100+ 个 drawable、27 张 jpg、values

ChatServer/                            # 电脑上跑的服务端（独立 Java SE 工程）
└─ src/
   ├─ ChatServer.java                  # ServerSocket + 线程池 + 昵称转发表
   └─ TestClient.java                  # 本机联调用的测试客户端
```

## 五、聊天是怎么走通的

协议非常朴素：**一行一条 JSON，用换行分帧**，所以完全不用处理粘包/拆包 ——
`BufferedReader.readLine()` 天然就是"读一条消息"。

```
客户端 → 服务端   {"type":"login","from":"张三","text":""}
客户端 → 服务端   {"type":"msg","from":"张三","to":"李思思","text":"你好"}
服务端 → 客户端   {"type":"msg","from":"张三","text":"你好"}
服务端 → 客户端   {"type":"notice","text":"「李思思」不在线，消息未送达"}
```

服务端只做一件事：维护一张 `昵称 → 连接` 的 `ConcurrentHashMap`，收到消息就查表转发。
不做账号、不做离线存储 —— 这也是它只有 150 行的原因。

App 这边的关键点：

- `ChatClient` 是单例，开一个后台线程持有 `Socket`，循环 `readLine()`；
  收到数据后用 `Handler` 切回主线程，再交给 `ChatManager.receiveRemote()`。
- **断线自动重连**（每 3 秒一次），并用"代次"（generation）字段保证
  改完服务器地址后旧线程不会和新线程同时重连。
- 对方不在会话列表里也聊得起来：`ChatManager.findOrCreateChat()` 会自动建一个新会话并落库。
- 连接状态、以及服务器发来的"对方不在线"这类提示，由 `MainActivity` 统一弹 Toast。

## 六、两个人线上聊天怎么操作

1. **电脑上启动服务端**（会用 javac 就行）：
   ```bat
   cd ChatServer
   javac -encoding UTF-8 -d out src\ChatServer.java
   java -cp out ChatServer
   ```
   启动后它会自己打印出电脑的局域网 IP，例如 `10.90.127.161`，记下来。

2. 两台设备（两台手机 / 一台手机 + 一个模拟器）都装同一份 APK，**连同一个 WiFi**。

3. 打开 App，在登录页填：
   - 设备 A：昵称 `张三`，勾上"连服务器和另一台设备聊天"，服务器地址填电脑的 IP，端口 `8888`
   - 设备 B：昵称 `李思思`，同样填服务器 IP

4. 任意一台回到微信 Tab，点会话里的对方（或用「发消息」），发一句话，另一台就会收到。

> 想先确认服务端没问题，可以用测试客户端跑一遍：
> ```bat
> java -cp out TestClient 127.0.0.1 8888 张三 李思思
> ```
> 它会模拟两台设备登录并互发消息，双方收到的原始 JSON 都会打印出来。

> 连不上的排查顺序：① 两台设备是否同一 WiFi；② 电脑防火墙是否放行了 Java；
> ③ IP 填的是不是服务端打印出来的那个（不是 127.0.0.1）。

## 七、数据库表结构

```sql
-- 聊天记录
CREATE TABLE t_message (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id INTEGER NOT NULL,     -- 属于哪个会话
    content TEXT,                 -- 正文；图片存路径；红包存 JSON（留言/金额/已领）；语音存秒数
    from_me INTEGER DEFAULT 0,    -- 1=我发的（右侧绿气泡）0=对方发的
    time TEXT,                    -- 显示用，"12:30" 这种
    ts INTEGER NOT NULL,          -- 时间戳，排序用
    type INTEGER DEFAULT 0,       -- 0=文字 1=图片 2=红包 3=语音 4=转账 5=拍一拍（v4 加 1，v5 加 2、3，v6 加 4、5）
    status INTEGER DEFAULT 0,     -- 0=正常 1=已撤回（v5 新增）
    extra TEXT                    -- 预留附加信息（v5 新增）
);

-- 联系人备注
CREATE TABLE t_remark (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_id INTEGER,
    contact_name TEXT,            -- 联系人原昵称（会话列表里只有昵称，所以拿它做键）
    remark TEXT
);

-- 在线聊天新建的会话（不在 assets 名单里的那些人）
CREATE TABLE t_chat (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id INTEGER NOT NULL, name TEXT, avatar TEXT,
    last_msg TEXT, time TEXT, ts INTEGER NOT NULL, is_group INTEGER DEFAULT 0
);

-- 好友自定义头像（avatar 可以是内置资源名 avatar1，也可以是相册导入后的绝对路径）
CREATE TABLE t_avatar (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_name TEXT,            -- 联系人原昵称
    avatar TEXT                   -- "avatar1" 或 "/data/.../files/avatars/xxx.jpg"
);

-- 朋友圈动态
CREATE TABLE t_moment (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT,                    -- 发布者昵称
    avatar TEXT,                  -- 头像（资源名或本地路径）
    content TEXT,                 -- 正文，可以是空串（只发图）
    images TEXT,                  -- JSON 数组字符串：["/data/.../moment_xxx.jpg", ...]
    time TEXT,                    -- 发表时的显示文本
    ts INTEGER NOT NULL,          -- 时间戳，列表按它倒序
    mine INTEGER DEFAULT 0,       -- 1=我发的（可删除）
    likes TEXT                    -- JSON 数组，如 ["李思思","@me"]
);

-- 动态下面的评论 / 回复
CREATE TABLE t_moment_comment (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    moment_id INTEGER NOT NULL,   -- 属于哪条动态
    user TEXT,                    -- 评论者昵称
    reply_to TEXT,                -- 回复谁；空串表示直接评论动态
    text TEXT,
    ts INTEGER NOT NULL,
    mine INTEGER DEFAULT 0        -- 1=我写的（可删除）
);

-- 收藏（v6 新增）：长按聊天消息「收藏」的内容
CREATE TABLE t_favorite (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id INTEGER NOT NULL,     -- 来自哪个会话
    from_name TEXT,               -- 来源（对方昵称或"我"）
    from_me INTEGER DEFAULT 0,    -- 1=我发的
    type INTEGER DEFAULT 0,       -- 与 t_message 一致
    content TEXT,                 -- 原样保存（红包/转账仍是 JSON，图片仍是路径）
    time TEXT,                    -- 收藏时刻 "yyyy-MM-dd HH:mm"
    ts INTEGER NOT NULL           -- 收藏时间戳，列表倒序
);
```

> 点赞列表里「我」存成 `@me` 而不是昵称：这样用户改了微信昵称，之前点的赞仍然算在他头上，
> 不用去数据库里批量改名字；显示时再替换成 `AppPrefs.getMyNick()`。

> 数据库版本从 1 → 2 → 3 → 4 → 5 → 6，`onUpgrade()` 先再跑一遍 `onCreate()`
> （所有建表语句都是 `CREATE TABLE IF NOT EXISTS`），
> 所以**旧版本的聊天记录、备注、头像都不会丢**，只补上新表（v6 新增 `t_favorite`）。
> v4 给 `t_message` 加 `type` 列时不能靠 `onCreate()`（表已存在，`IF NOT EXISTS` 不会加列），
> 所以额外执行一次 `ALTER TABLE t_message ADD COLUMN type ...`，并吞掉「列已存在」的异常，
> 之后 v5 加 `status`、`extra` 两列用的也是同一招，老聊天记录、备注、朋友圈内容都不会丢。
> 这样新装用户和升级用户走同一段代码都不会出问题。
> 朋友圈的示例动态用一个 SharedPreferences 标记位记录「已导入」，
> 即使用户把动态全删了，下次进来也不会又冒出来。

## 八、备注、头像与昵称是怎么"一处改、多处变"的

- **备注**统一走 `RemarkDao.displayName(name)`：有备注返回备注，没有返回原昵称。
  通讯录（`ContactAdapter`）、会话列表（`ChatAdapter`）、聊天标题栏（`ChatActivity`）都调它，
  所以改一次备注，三处一起变。
- **好友头像**统一走 `AvatarDao.displayAvatar(name, fallback)`：设置过就返回自定义头像，
  没设置过就返回 assets 里的原头像。通讯录、会话列表、聊天页左侧气泡、资料页都调它。
- **头像的显示**再统一走 `AvatarStore.apply(view, avatar, fallbackRes)`：
  传进来的是 `avatar1` 就查 drawable，是 `/data/.../xxx.jpg` 就解码成 Bitmap，
  中间有 8MB 的 `LruCache`。所以"内置头像"和"相册照片"在界面层是同一种东西，调用方不用分支。
- **相册选图**走 `AvatarStore.importFromUri()`：先只读宽高算采样率，再解码、居中裁成正方形、
  缩到 512×512 压成 JPEG，存进 `files/avatars/`，数据库里只存这个绝对路径。
  换头像时会顺手删掉上一张自己导入的旧图，目录不会越攒越大。
- **我的头像**走 `AppPrefs.getMyAvatar()`，默认回落到 `assets/weixin.json` 里的原头像。
- 两个 DAO 内部都有一层 `HashMap` 内存缓存。ListView 的 `getView()` 每滑动一次就调一次，
  直接查库太浪费；写的时候同步更新缓存，读只走内存。
- **昵称**统一走 `AppPrefs.getMyNick()`，没设置过就回落到 `assets/weixin.json` 里的默认昵称，
  所以调用方不用判断"用户改没改过"。
- 改完备注或头像后，相关页面在 `onResume` 重新读一遍（比如 `ContactsFragment`）；
  会话列表不在前台，所以用 `ChatManager.notifyChanged(chatId)` 主动广播一次让它重画。
  不需要 EventBus 这种重型方案。

## 九、关键词自动回复怎么加规则

规则都在 `utils/AutoReplyRules.java` 里，就是一个二维数组：

```java
private static final String[][] RULES = {
        {"吃饭了吗", "吃了|刚吃完，你呢|还没呢，你请客吗"},
        {"你喜欢谁", "我是同性恋"},
        {"晚安",     "晚安，好梦|晚安，早点睡"},
        {"你好吗",   "挺好的，你呢|还不错，就是有点忙"},
        {"你好",     "你好|你好呀，很高兴认识你|嗨～"}
};
```

- 按**数组顺序**匹配，命中第一条就返回，所以越具体的规则要放越前面
  （「你好吗」必须在「你好」前面，否则永远只会命中「你好」）；
- 用 `contains` 而不是 `equals`，所以"你今天吃饭了吗"也能命中；
- 回复里可以用 `|` 分隔多种说法，每次随机挑一句，不会每次都一模一样；
- 现在内置 20 多组：打招呼、吃饭、道谢、道别、早午晚安、生日、天气、夸人、日常闲聊等；
- 没命中返回 `null`，`ChatActivity` 会回落到原来那套随机回复（"收到，稍等我看一下"等）。

想加一条，只改这个数组就行，`ChatActivity` 一行都不用动。

> 生效范围：**本地演示模式**（登录页不勾"连服务器"）。
> 如果勾了在线模式但没真正连上服务器，也仍然会回复，方便一个人演示；
> 真正连上服务器时对面是活人，就不再伪造回复了。

## 十、朋友圈：发动态、点赞、评论是怎么做的

### 10.1 数据从哪来

```
首次进入 → MomentDao.ensureSeed()
             ├─ 已导入过（SharedPreferences 标记）→ 跳过
             └─ 没导入过 → 解析 assets/weixin.json 的 moments，插入 t_moment + t_moment_comment
          → MomentDao.queryAll()  按 ts 倒序，一次性把所有评论查出来再按 moment_id 分发
          → MomentAdapter.setData()
```

种子动态的时间戳是「当前时间往前推 30 分钟 × 序号」，这样示例数据仍然保持 JSON 里的先后顺序。
之后发的动态用真实 `System.currentTimeMillis()`，永远排在最上面。

时间显示不存死字符串，而是用 `MomentDao.formatTime(ts)` 现算：
一分钟内「刚刚」、一小时内「x分钟前」、一天内「x小时前」，再久就显示 `MM-dd HH:mm`。
所以页面开着一会儿再回来，「几分钟前」会自己往后走。

### 10.2 发动态

`PublishMomentActivity` 三件事：

1. **选图**：`ACTION_GET_CONTENT` + `EXTRA_ALLOW_MULTIPLE`，多选结果在 `ClipData` 里，
   单选结果在 `getData()` 里，两种都处理。系统相册只把用户点选的那几张交给 App，**不申请存储权限**。
2. **收图**：`MomentPhotoStore.importFromUri()` 先 `inJustDecodeBounds` 读尺寸算采样率，
   再解码、按 EXIF 转正、等比缩到最长边 1080、质量 88 压成 JPEG，存进 `files/moments/`。
   解码和压缩都在后台线程，完成后再回主线程刷新九宫格。
3. **发布**：`MomentDao.insert()` 写库，评论会一起落库；返回后朋友圈 `onResume` 重新查一遍。

两个细节：

- 图片格子在 `FlowLayout` 里排，一行 3 张自动换行；单张时用 200dp 大图，多张时统一小方块。
- 如果用户选了图但没点「发表」就退出，`onDestroy` 会把这些已导入的文件删掉，不留垃圾。

### 10.3 点赞与评论

- **点赞**：`MomentDao.toggleLike()` 读 `likes` JSON，有 `@me` 就删、没有就加，再写回。
  图标和文字跟着变（空心 ♡「赞」/ 实心红 ♥「取消」）。
- **评论**：点条目上的「评论」，底部弹出输入框；`MomentDao.addComment()` 写库。
- **回复某人**：点某条评论，输入框上方显示「回复 张三」，写库时把 `reply_to` 填上，
  显示成「我 回复 张三：…」。回复自己的评论会当作继续评论，避免出现「我 回复 我」。
- **删除**：评论一条一个 `TextView`，长按只对 `mine=1` 的生效；
  动态本身长按删除，会连带删掉评论行和本机图片文件。

评论之所以一条一个 `TextView`（而不是拼成一大段），就是为了点击时能精确知道「点的是谁的评论」。

## 十一、小游戏：三个玩法与两处入口

入口有两个，都指向同一个 `GameActivity`：

```
聊天页「+」→ 小游戏        （带上当前 chatId / 对手昵称 / 头像）
发现 → 游戏 → 选玩法 → 选联系人   （GameLobbyActivity，选完再进）
```

`GameActivity` 里三个玩法共用顶部一张比分牌（我 vs 对手），用 `selected` 状态切 Tab：

| 玩法 | 规则 | 判定 |
| --- | --- | --- |
| 猜拳 | 石头 / 剪刀 / 布，对手随机出 | `(我 - 对手 + 3) % 3`：0 平、1 我赢、2 我输 |
| 猜数字 | 1~100，七次机会 | 每次提示「太大 / 太小」并收窄可猜区间，猜中或次数用完结束 |
| 翻牌 | 4×2 共 8 张，四组图案各两张 | 翻开两张相同即配对，全部配对成功得 1 分，同时记录步数与用时 |

几个实现点：

- 猜拳不是秒出结果：先 `postDelayed` 七帧随机手势做「抖动」，再定格并判胜负，有对战的临场感。
- 翻牌用 `GridLayout` + 8 个 `TextView` 代码生成，不同则延时 650ms 翻回；
  这期间 `memoryBusy = true` 挡住连点，避免把第三张也翻开。
- 猜数字除了「大 / 小」，还会把可行区间收窄成 `[low, high]` 显示在提示行，
  七次机会下体验更接近「二分查找」。
- 翻牌开局记下 `memoryStart`，一个每秒跑一次的 `Runnable` 刷新「步数 · 用时」；
  通关、重开、退出页面时都会把它移除，不会在后台空转。
- 所有延时任务都在 `onDestroy` 里 `handler.removeCallbacksAndMessages(null)`，
  否则页面关了还在改 View 会崩。
- 「把战绩发到聊天」就是调 `ChatManager.send(chatId, "【小游戏·猜拳】我 3 : 2 张三，再来一局？")`，
  走的是和普通消息完全一样的链路，所以会话列表的最后一条消息、时间也会一起更新。
  从大厅进来时如果通讯录里没有这个人的会话，就只提示、不伪造会话。

## 十二、图片：聊天发图与大图查看

### 12.1 聊天里发一张图

链路和发文字几乎一样，只是多了「选图 + 压缩」这一步：

```
「+」→ 添加图片 → ACTION_GET_CONTENT 选一张
   → 后台线程 MomentPhotoStore.importFromUri()   压缩到最长边 1080、按 EXIF 转正
   → ChatManager.sendImage(chatId, path)          type=1，content 存路径
   → MessageDao.insert()                         落 SQLite
   → ChatManager.notifyChanged()                 聊天页刷新，气泡换成缩略图
```

几个要点：

- 图片消息的 `content` 存的是**本机绝对路径**，所以不需要把二进制塞进数据库；
  删除 App 数据或清缓存后图片会失效，这和「本地演示」的定位是一致的。
- `MsgAdapter` 遇到 `type=1` 就把文字气泡 `GONE`、显示 150dp 的缩略图；
  点击缩略图跳到 `PhotoViewerActivity`，只有一张图时页码自动隐藏。
- 局域网模式下还没做图片传输（要改成文件分片协议），所以只给对方发一条 `[图片]` 文本，
  至少不会让对方收到一条打不开的本地路径。
- 发完图在本地演示模式下也会安排一条自动回复，聊天不会显得只有自己一句。

### 12.2 大图查看器：一个 200 行的 ZoomImageView

没有引入 PhotoView 之类的库，`ZoomImageView` 用 ImageView 自带的 `Matrix` 做：

| 手势 | 实现 |
| --- | --- |
| 双指缩放 | `ScaleGestureDetector`，以两指中心为轴 `postScale`，上限是适应尺寸的 3 倍 |
| 单指拖动 | `GestureDetector.onScroll`，只在放大后生效，`postTranslate` |
| 双击 | 已放大就回到适应尺寸，否则以点击点为轴放到 2.5 倍 |
| 左右滑动 | `onFling`，速度 > 600 且横向占优才翻页，避免和拖动打架 |
| 单击 | `onSingleTapConfirmed`，淡出/淡入顶栏与底部提示 |

「适应尺寸」的基准矩阵在 `onSizeChanged()` 里算：等比缩放到刚好装进控件再居中，
之后所有缩放都相对它。每次手势结束调 `clampMatrix()`：
比控件小的方向居中，比控件大的方向不许露出黑边。
`setImageDrawable()` 被重写并在构造未完成时用 `inited` 挡一下 ——
否则父类构造里就会回调到还没初始化的字段。

### 12.3 战绩存在哪

`GameStats` 用 `SharedPreferences("game_stats")` 存三组数字：

| 键 | 含义 |
| --- | --- |
| `rps_win` / `rps_lose` / `rps_draw` | 猜拳胜 / 负 / 平场数 |
| `guess_best` | 猜数字猜中时最少用了几次 |
| `memory_best` | 翻牌通关时最少用了几步 |

- 猜拳每局都写；猜数字只在猜中时写，且只在「比历史更少」时更新；
  翻牌通关时写步数，破纪录会弹一句「新纪录」。
- 游戏大厅在 `onResume` 重新读一遍，所以从对局返回就能看到数字变化。
- 「清空」走 `GameStats.clear()`，带一个确认框，避免误点。

## 十三、红包、语音与撤回是怎么做的

- **红包**：`type=2` 的消息，`content` 存一段 JSON（留言 / 金额 / 是否已领取）。
  气泡渲染成橙色卡片（`bg_redpacket` 渐变 + 白色信封小图标），点开是全屏弹窗：
  金色「開」按钮 → 按下后按钮隐去、金额淡入；已拆过的再点直接显示结果。
  发出红包约 2 秒后对方会自动拆开并回一句道谢；聊天里提到「红包」这个词，
  对方也会发一个随机金额的红包过来（本地演示模式的「戏精」逻辑）。
- **语音**：`type=3` 的消息，`content` 存秒数。左下角喇叭在「键盘 / 按住说话」之间切换；
  按下开始计时、屏幕中央盖一层「松开 发送」，松开按秒数发出（短于 1 秒提示太短）。
  点气泡播放时喇叭做缩放抖动动画（演示数据没有真录音，所以只有动画没有声音）。
- **撤回**：`t_message` 加了 `status` 列（v5），长按自己的消息、发出去没超过 2 分钟才显示「撤回」；
  撤回后整行变成灰色居中的「你撤回了一条消息」，会话列表的最后一条消息按「最后一条未撤回的消息」重算。
- 长按菜单里的**复制**用系统剪贴板（`ClipboardManager`），**删除**只删本地这条记录。

## 十四、转账、拍一拍、表情、收藏与设置（v6）

### 转账（type=4）
- 聊天页「+」面板新增「转账」：填金额 + 转账说明，发出一张**橙色卡片**（`bg_transfer` 橙底 + 白色 ¥ 圆标）。
- content 存 JSON：`{note, amount, state}`，state 0 待收款 / 1 已收款。
- 对方转来的、未收款的转账：点卡片弹「确认收款」→ 卡片变「已收款」+ 一条居中灰字「已收款 ¥xx，已存入零钱」。
- 演示模式下我发出的转账约 2 秒后被对方自动收下，并回一句道谢。
- 收款只改 content JSON 里的 state 并落库，不动消息 id，重启后状态还在。

### 拍一拍（type=5）
- 聊天页**双击头像**（350ms 内两次点击）触发：先出一行居中灰字「你拍了拍"XX"」，演示模式下对方过会儿拍回来。
- 拍一拍是系统消息形态：不占气泡、不显示头像，渲染走撤回提示同一行样式。
- 双击自己的头像是「你拍了拍自己」。

### 表情面板
- 输入框右侧笑脸按钮 → 底部弹出表情面板（GridView 8 列，96 个 Unicode 表情）。
- 点一个表情插入到输入框光标位置；插入后照常点「发送」。

### 收藏（t_favorite，DB v6 新表）
- 长按消息菜单新增「收藏」（文字 / 图片 / 红包 / 语音 / 转账都能收）。
- 「我-收藏」打开收藏页：倒序排列，图片收藏可点开看大图，长按删除（只删收藏，不动原聊天记录）。

### 设置页
- 「我-设置」打开独立设置页：昵称 / 微信号 / 性别 / 地区都能改，存 SharedPreferences；
  服务器设置沿用登录页编辑模式；退出登录清登录态回登录页。
- 微信号从 AppPrefs 读（`getMyWxId()` 回落到 JSON 默认值），"我"页个人卡片同步显示。

### 界面严格仿微信的细节修正
- 头像全部改成微信的**圆角方形**（边长 9% 圆角，约 4dp）：改的是 `CircleImageView` 的绘制，
  所有用到它的地方（会话列表 / 聊天页 / 通讯录 / 朋友圈 / 我）一次到位。
- 聊天气泡圆角 8dp → 4dp，与真微信一致。
- 会话列表加**从头像右侧开始**的分割线（微信不顶满整行）。
- 搜索框换浅灰圆角（#F5F5F5 / 6dp）。
- 朋友圈图标换成微信的蓝黄双色圈 + 白波浪；收藏图标换成黄色五角星。

## 十五、怎么编译

### Android Studio
直接用 Android Studio 打开 `WeiXinDemo` 目录，等 Gradle Sync 完，点运行。

### 命令行（本机已验证）
```bat
set JAVA_HOME=C:\Users\lenovo\.jdks\jbr-17.0.14
set GRADLE_USER_HOME=D:\SDK\gradle-8.2
cd /d <本工程目录>
D:\SDK\gradle-8.2\bin\gradle.bat assembleDebug
```
产物：`app/build/outputs/apk/debug/app-debug.apk`

> 注意两点：
> 1. **必须用 JDK 17**。Android Studio 自带的 JBR 是 25，而 Gradle 8.2 最高只支持到 Java 20，
>    直接拿它跑会报 "Unsupported class file major version"。本机可用的 17 在
>    `C:\Users\lenovo\.jdks\jbr-17.0.14`。
> 2. 本机的 Gradle 8.2 发行版解压在 `D:\SDK\gradle-8.2`，依赖缓存也在这（735 MB），
>    所以 `GRADLE_USER_HOME` 指向它就能**离线编译**。

## 十六、几个实现上的小细节

- **圆形头像** `CircleImageView`：用 `BitmapShader` + `CLAMP` 边缘拉伸把位图裁成圆形；
  遇到矢量图（比如默认的灰色人像 `avatar_default`）会先画进一张 Bitmap 再处理。
- **字母索引栏** `SideIndexBar`：自定义 View，滑动时回调 `onLetterChanged(letter)`，
  `ContactAdapter.getPositionForLetter()` 把字母换算成 ListView 行号再 `setSelection`。
- **JSON 里的图片名带扩展名**（`avatar1.jpg`），而资源名不能带点，
  所以 `WeixinDataUtils.getDrawableId()` 会先去掉扩展名再 `getIdentifier`。
- 消息列表 `MsgAdapter` 有 3 种行样式（时间标签 / 左侧 / 右侧），相邻消息时间不同时自动插一行时间标签。
- `ChatManager` 的监听器回调**始终在主线程**触发，界面层不用再切线程。
- `ChatActivity` 的监听回调里有个 `refreshing` 重入保护：
  `clearUnread()` 自己也会触发回调，不加保护会自己调自己。
- 头像选择器是 `GridView`（4 列、16 张）配一个 `BaseAdapter`，选中项靠改背景套绿环；
  上方还有一个「从相册选择」，走系统 `ACTION_GET_CONTENT`，**不需要申请存储权限**。
- 相册原图可能有几千万像素，所以导入时先用 `inJustDecodeBounds` 只读尺寸算采样率，
  再解码、裁剪、压缩，整个过程放在后台线程，避免主线程卡顿和 OOM。
- 手机竖拍的照片只带一个 EXIF「旋转 90 度」标记，像素本身还是横的，
  所以导入时读一下 `ExifInterface.TAG_ORIENTATION` 把图转正（API 24+ 生效），不然头像会躺着。
  「恢复默认」按钮的点击事件是在 `dialog.show()` **之后**用 `getButton()` 重新设的 ——
  因为 `AlertDialog` 的按钮自带"点完就关闭"行为，直接设 `null` 会让它失效，必须覆盖。
