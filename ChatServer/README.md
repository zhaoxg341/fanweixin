# ChatServer —— 局域网聊天服务端

一个**零依赖**的极简聊天中继服务端，只用 JDK 自带的 `ServerSocket`，不需要 Maven / Gradle / 任何第三方库。
它按「昵称」维护一张转发表：A 发消息 → 服务端按收件人昵称找到 B 的连接 → 原样转发给 B。

## 目录结构

```
ChatServer/
├── src/
│   ├── ChatServer.java   # 服务端主程序
│   └── TestClient.java   # 联调测试客户端（模拟两台设备互发）
├── out/                  # 编译输出（.class）
└── 启动服务端.bat         # 一键：编译 + 启动
```

## 快速开始

1. **看电脑的局域网 IP**：服务端启动后会自动打印，例如
   `[INFO] 本机局域网 IP: 10.90.127.161  (手机填这个)`
   也可以自己在命令行敲 `ipconfig` 看 IPv4 地址。
2. **双击 `启动服务端.bat`**（或手动编译运行，见下）。
3. 手机 App 登录页填：服务器 = 上面那个 IP，端口 = `8888`，勾选「连接聊天服务器」。
4. 两台设备连**同一个 WiFi**，用**不同的昵称**登录，即可互发消息。

> 注意：Windows 防火墙第一次会弹窗询问是否允许 Java 联网，选「允许访问」。

## 手动编译运行

```bat
javac -encoding UTF-8 -d out src\ChatServer.java
java -Dfile.encoding=UTF-8 -cp out ChatServer
```

> 用 PowerShell 启动时，`-Dfile.encoding=UTF-8` **必须加引号**写成 `"-Dfile.encoding=UTF-8"`，
> 否则会被 PowerShell 当成类路径参数，报 `ClassNotFoundException .../encoding=UTF-8`。

## 联调自测（不用手机）

```bat
javac -encoding UTF-8 -d out src\ChatServer.java src\TestClient.java
java -Dfile.encoding=UTF-8 -cp out TestClient
```

会模拟「张三」「李思思」两台设备登录并互发，验证：转发成功、对方不在线时得到提示。

## 通信协议

- 一行 = 一条消息，UTF-8 文本，**用 `\n` 分帧**（避免粘包 / 拆包）。
- 客户端 → 服务端：
  - 登录：`{"type":"login","name":"张三"}`
  - 发消息：`{"type":"msg","from":"张三","to":"李思思","content":"你好"}`
- 服务端 → 客户端：
  - 转发：`{"type":"msg","from":"张三","to":"李思思","content":"你好"}`
  - 系统通知：`{"type":"notice","content":"李思思 不在线"}`

## 已知限制

- 没有鉴权、没有加密，昵称即身份（同学间演示够用）。
- 消息仅在内存中转发，服务端不落库（聊天记录存在手机 App 的 SQLite 里）。
- 只支持文本消息。
