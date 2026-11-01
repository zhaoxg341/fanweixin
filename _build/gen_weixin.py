# -*- coding: utf-8 -*-
"""
仿微信（WeiXinDemo）图片素材 + assets 数据源生成脚本
产出：
  res/drawable-nodpi/*.jpg   头像 / 朋友圈配图 / 朋友圈封面
  res/mipmap-*/ic_launcher*.png 启动图标
  assets/weixin.json         用户、会话、联系人、朋友圈数据
"""
import os
import json
import math
from PIL import Image, ImageDraw, ImageFont

ROOT = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\WeiXinDemo"
RES = os.path.join(ROOT, "app", "src", "main", "res")
DRAWABLE = os.path.join(RES, "drawable-nodpi")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
os.makedirs(DRAWABLE, exist_ok=True)
os.makedirs(ASSETS, exist_ok=True)

FONT_CANDIDATES = [
    r"C:\Windows\Fonts\msyhbd.ttc",
    r"C:\Windows\Fonts\msyh.ttc",
    r"C:\Windows\Fonts\simhei.ttf",
    r"C:\Windows\Fonts\simsun.ttc",
]
_font_cache = {}


def font(size):
    if size in _font_cache:
        return _font_cache[size]
    for path in FONT_CANDIDATES:
        if os.path.exists(path):
            try:
                f = ImageFont.truetype(path, size)
                _font_cache[size] = f
                return f
            except Exception:
                continue
    f = ImageFont.load_default()
    _font_cache[size] = f
    return f


def text_at(draw, text, f, cx, cy, fill=(255, 255, 255, 255), anchor="mm"):
    draw.text((cx, cy), text, font=f, fill=fill, anchor=anchor)


def gradient(size, c1, c2, c3=None):
    """斜向线性渐变：先在大画布上画横条纹并旋转，再中心裁剪，避免出现黑角"""
    w, h = size
    bw, bh = int(w * 1.8), int(h * 1.8)
    img = Image.new("RGB", (bw, bh), c1)
    d = ImageDraw.Draw(img)
    n = max(bw, bh) * 2
    for i in range(n):
        t = i / float(n - 1)
        if c3 is None:
            r = int(c1[0] + (c2[0] - c1[0]) * t)
            g = int(c1[1] + (c2[1] - c1[1]) * t)
            b = int(c1[2] + (c2[2] - c1[2]) * t)
        else:
            if t < 0.5:
                t2 = t * 2
                r = int(c1[0] + (c2[0] - c1[0]) * t2)
                g = int(c1[1] + (c2[1] - c1[1]) * t2)
                b = int(c1[2] + (c2[2] - c1[2]) * t2)
            else:
                t2 = (t - 0.5) * 2
                r = int(c2[0] + (c3[0] - c2[0]) * t2)
                g = int(c2[1] + (c3[1] - c2[1]) * t2)
                b = int(c2[2] + (c3[2] - c2[2]) * t2)
        d.line([(0, i), (bw, i)], fill=(r, g, b))
    img = img.rotate(-18, resample=Image.BICUBIC, expand=False)
    # 中心裁剪回目标尺寸
    left = (bw - w) // 2
    top = (bh - h) // 2
    return img.crop((left, top, left + w, top + h))


AVATAR_PALETTE = [
    ((0x4E, 0x8B, 0xF7), (0x2C, 0x5F, 0xD6)),
    ((0x07, 0xC1, 0x60), (0x05, 0x96, 0x4A)),
    ((0xFA, 0x9D, 0x3B), (0xE0, 0x77, 0x1C)),
    ((0x8E, 0x7C, 0xF0), (0x63, 0x4F, 0xD8)),
    ((0xF5, 0x65, 0x5B), (0xD1, 0x3C, 0x33)),
    ((0x29, 0xB6, 0xC6), (0x14, 0x8E, 0x9C)),
    ((0xF5, 0xC2, 0x4C), (0xD9, 0x9E, 0x1F)),
    ((0x5A, 0x6C, 0xE8), (0x3B, 0x49, 0xC4)),
]


def avatar(name, idx, path):
    """生成圆形头像底图：渐变方图 + 姓名首字"""
    w = h = 240
    c1, c2 = AVATAR_PALETTE[idx % len(AVATAR_PALETTE)]
    img = gradient((w, h), c1, c2)
    d = ImageDraw.Draw(img, "RGBA")
    # 装饰弧
    d.ellipse([w * 0.55, -h * 0.25, w * 1.35, h * 0.55], fill=(255, 255, 255, 34))
    d.ellipse([-w * 0.35, h * 0.62, w * 0.45, h * 1.42], fill=(255, 255, 255, 26))
    # 姓名首字
    label = name[:1] if len(name) <= 2 else name[:1]
    text_at(d, label, font(120), w / 2, h / 2 + 4)
    img.convert("RGB").save(path, quality=90)
    return path


def round_avatar(name, idx, path):
    """方形圆角头像（用于聊天气泡里的小头像）"""
    w = h = 240
    c1, c2 = AVATAR_PALETTE[idx % len(AVATAR_PALETTE)]
    img = gradient((w, h), c1, c2)
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse([w * 0.55, -h * 0.25, w * 1.35, h * 0.55], fill=(255, 255, 255, 34))
    text_at(d, name[:1], font(120), w / 2, h / 2 + 4)
    img.convert("RGB").save(path, quality=90)
    return path


def group_avatar(path):
    """群头像：2x2 四色格子"""
    w = h = 240
    img = Image.new("RGB", (w, h), (255, 255, 255))
    d = ImageDraw.Draw(img)
    cols = [(0x4E, 0x8B, 0xF7), (0x07, 0xC1, 0x60), (0xFA, 0x9D, 0x3B), (0x8E, 0x7C, 0xF0)]
    half = w // 2
    for i in range(4):
        x = (i % 2) * half
        y = (i // 2) * half
        tile = gradient((half, half), cols[i], tuple(max(0, v - 40) for v in cols[i]))
        img.paste(tile, (x, y))
    d = ImageDraw.Draw(img, "RGBA")
    # 浅色网格线
    d.line([(half, 0), (half, h)], fill=(255, 255, 255, 180), width=3)
    d.line([(0, half), (w, half)], fill=(255, 255, 255, 180), width=3)
    img.convert("RGB").save(path, quality=90)


def brand_avatar(path, label, c1, c2):
    """官方号头像：渐变 + 居中文字"""
    w = h = 240
    img = gradient((w, h), c1, c2)
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse([w * 0.5, -h * 0.2, w * 1.3, h * 0.6], fill=(255, 255, 255, 34))
    text_at(d, label, font(96), w / 2, h / 2 + 4)
    img.convert("RGB").save(path, quality=90)


MOMENT_PALETTE = [
    ((0x4F, 0xAC, 0xFE), (0x1B, 0x6E, 0xC8)),
    ((0x43, 0xE9, 0x7B), (0x1B, 0xA5, 0x52)),
    ((0xFB, 0xAB, 0x2A), (0xE0, 0x6E, 0x14)),
    ((0xA1, 0x8C, 0xD8), (0x6E, 0x53, 0xBF)),
    ((0xFF, 0x7B, 0x7B), (0xD1, 0x3B, 0x3B)),
    ((0x2B, 0xC8, 0xD4), (0x12, 0x8E, 0xA0)),
    ((0xE9, 0x8A, 0xB8), (0xBE, 0x4E, 0x85)),
    ((0x6D, 0xDC, 0xB5), (0x2C, 0xA9, 0x82)),
]


def moment_img(idx, label, path):
    w = h = 400
    c1, c2 = MOMENT_PALETTE[idx % len(MOMENT_PALETTE)]
    img = gradient((w, h), c1, c2)
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse([w * 0.45, -h * 0.3, w * 1.4, h * 0.65], fill=(255, 255, 255, 40))
    d.ellipse([-w * 0.35, h * 0.55, w * 0.55, h * 1.45], fill=(255, 255, 255, 30))
    # 白色圆底 + 文字，保证对比度
    d.ellipse([w * 0.27, h * 0.30, w * 0.73, h * 0.76], fill=(255, 255, 255, 62))
    text_at(d, label, font(58), w / 2, h / 2)
    img.convert("RGB").save(path, quality=88)


def cover(path):
    w, h = 900, 600
    img = gradient((w, h), (0x2E, 0x3E, 0x5C), (0x5C, 0x8A, 0xA8), (0x8F, 0xB0, 0xA8))
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse([w * 0.55, -h * 0.25, w * 1.2, h * 0.7], fill=(255, 255, 255, 26))
    text_at(d, "朋友圈封面", font(40), w / 2, h / 2, (255, 255, 255, 150))
    img.convert("RGB").save(path, quality=88)


def launcher(size, path, round_mode=False):
    """微信绿底 + 白色对话气泡，作为启动图标"""
    s = size
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 绿底
    d.rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * 0.22), fill=(0x07, 0xC1, 0x60, 255))
    # 大白气泡
    bw, bh = int(s * 0.52), int(s * 0.42)
    bx, by = int(s * 0.16), int(s * 0.20)
    d.rounded_rectangle([bx, by, bx + bw, by + bh], radius=int(s * 0.12), fill=(255, 255, 255, 255))
    d.polygon([(bx + int(bw * 0.20), by + bh - 2),
               (bx + int(bw * 0.20) - int(s * 0.06), by + bh + int(s * 0.10)),
               (bx + int(bw * 0.44), by + bh - 2)], fill=(255, 255, 255, 255))
    # 小气泡
    bw2, bh2 = int(s * 0.34), int(s * 0.27)
    bx2, by2 = int(s * 0.50), int(s * 0.52)
    d.rounded_rectangle([bx2, by2, bx2 + bw2, by2 + bh2], radius=int(s * 0.09),
                        fill=(255, 255, 255, 235))
    d.polygon([(bx2 + int(bw2 * 0.62), by2 + bh2 - 2),
               (bx2 + int(bw2 * 0.62) + int(s * 0.05), by2 + bh2 + int(s * 0.08)),
               (bx2 + int(bw2 * 0.40), by2 + bh2 - 2)], fill=(255, 255, 255, 235))
    if round_mode:
        mask = Image.new("L", (s, s), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, s - 1, s - 1], fill=255)
        out = Image.new("RGBA", (s, s), (0, 0, 0, 0))
        out.paste(img, (0, 0), mask)
        img = out
    img.save(path)


# ---------------- 生成头像 ----------------
PEOPLE = [
    "张三", "李思思", "王五", "陈晓", "刘敏", "阿布", "程雨", "邓超",
    "何静", "胡明", "罗浩", "马晓东", "孙悦", "吴倩", "周婷", "赵学刚",
]
avatar_map = {}
for i, name in enumerate(PEOPLE):
    idx = i + 1
    p = os.path.join(DRAWABLE, "avatar%d.jpg" % idx)
    avatar(name, i, p)
    avatar_map[name] = "avatar%d.jpg" % idx

# 我自己
avatar("赵学刚", 15, os.path.join(DRAWABLE, "avatar_me.jpg"))
# 群聊
group_avatar(os.path.join(DRAWABLE, "avatar_group.jpg"))
# 微信团队 / 订阅号
brand_avatar(os.path.join(DRAWABLE, "avatar_team.jpg"), "微", (0x07, 0xC1, 0x60), (0x05, 0x96, 0x4A))
brand_avatar(os.path.join(DRAWABLE, "avatar_mp.jpg"), "阅", (0xFA, 0x9D, 0x3B), (0xE0, 0x77, 0x1C))

# ---------------- 生成朋友圈配图 ----------------
MOMENT_LABELS = ["实验室", "代码", "橘子洲", "湘江", "夕阳", "美食"]
for i, lb in enumerate(MOMENT_LABELS):
    moment_img(i, lb, os.path.join(DRAWABLE, "moment%d.jpg" % (i + 1)))
cover(os.path.join(DRAWABLE, "moments_cover.jpg"))

# ---------------- 生成启动图标 ----------------
DENSITY = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for den, size in DENSITY.items():
    d = os.path.join(RES, "mipmap-" + den)
    os.makedirs(d, exist_ok=True)
    launcher(size, os.path.join(d, "ic_launcher.png"), False)
    launcher(size, os.path.join(d, "ic_launcher_round.png"), True)

# ---------------- 生成 assets/weixin.json ----------------
def av(name):
    return avatar_map.get(name, "avatar1.jpg")


data = {
    "user": {
        "nickName": "赵学刚",
        "weixinId": "zhaoxg_2026",
        "avatar": "avatar_me.jpg",
        "region": "湖南 湘潭",
        "signature": "在做一个仿微信的 Android 项目"
    },
    "chats": [
        {"id": 1, "name": "张三", "avatar": av("张三"), "lastMsg": "明天记得把操作系统课设的实验报告交了",
         "time": "12:30", "unread": 2, "group": False},
        {"id": 2, "name": "物联网一班通知群", "avatar": "avatar_group.jpg",
         "lastMsg": "班长：本周五下午两点在学院楼 302 开班会", "time": "11:05", "unread": 5, "group": True},
        {"id": 3, "name": "李思思", "avatar": av("李思思"), "lastMsg": "好的，那下午三点图书馆见",
         "time": "10:12", "unread": 0, "group": False},
        {"id": 4, "name": "微信团队", "avatar": "avatar_team.jpg",
         "lastMsg": "你的微信已在新设备上登录", "time": "昨天", "unread": 0, "group": False},
        {"id": 5, "name": "王五", "avatar": av("王五"), "lastMsg": "[图片]",
         "time": "昨天", "unread": 0, "group": False},
        {"id": 6, "name": "陈晓", "avatar": av("陈晓"), "lastMsg": "你上次说的那个仿美团的项目做完了吗",
         "time": "昨天", "unread": 1, "group": False},
        {"id": 7, "name": "刘敏", "avatar": av("刘敏"), "lastMsg": "食堂三楼新开了一家麻辣香锅",
         "time": "星期一", "unread": 0, "group": False},
        {"id": 8, "name": "订阅号消息", "avatar": "avatar_mp.jpg",
         "lastMsg": "今日热点：Android 15 开发者预览版发布", "time": "星期一", "unread": 0, "group": False},
    ],
    "contacts": [
        {"id": 1, "name": "阿布", "letter": "A", "avatar": av("阿布"), "weixinId": "abu_2026", "region": "湖南 长沙"},
        {"id": 2, "name": "陈晓", "letter": "C", "avatar": av("陈晓"), "weixinId": "chenxiao_0512", "region": "湖南 湘潭"},
        {"id": 3, "name": "程雨", "letter": "C", "avatar": av("程雨"), "weixinId": "chengyu_0918", "region": "湖北 武汉"},
        {"id": 4, "name": "邓超", "letter": "D", "avatar": av("邓超"), "weixinId": "dengchao_0307", "region": "湖南 株洲"},
        {"id": 5, "name": "何静", "letter": "H", "avatar": av("何静"), "weixinId": "hejing_1123", "region": "湖南 衡阳"},
        {"id": 6, "name": "胡明", "letter": "H", "avatar": av("胡明"), "weixinId": "huming_0621", "region": "江西 南昌"},
        {"id": 7, "name": "李思思", "letter": "L", "avatar": av("李思思"), "weixinId": "lisisi_0204", "region": "湖南 湘潭"},
        {"id": 8, "name": "刘敏", "letter": "L", "avatar": av("刘敏"), "weixinId": "liumin_0715", "region": "湖南 常德"},
        {"id": 9, "name": "罗浩", "letter": "L", "avatar": av("罗浩"), "weixinId": "luohao_0329", "region": "广东 广州"},
        {"id": 10, "name": "马晓东", "letter": "M", "avatar": av("马晓东"), "weixinId": "maxiaodong_0811", "region": "湖南 湘潭"},
        {"id": 11, "name": "孙悦", "letter": "S", "avatar": av("孙悦"), "weixinId": "sunyue_1226", "region": "湖南 岳阳"},
        {"id": 12, "name": "王五", "letter": "W", "avatar": av("王五"), "weixinId": "wangwu_0503", "region": "湖南 湘潭"},
        {"id": 13, "name": "吴倩", "letter": "W", "avatar": av("吴倩"), "weixinId": "wuqian_0914", "region": "湖南 益阳"},
        {"id": 14, "name": "张三", "letter": "Z", "avatar": av("张三"), "weixinId": "zhangsan_0101", "region": "湖南 湘潭"},
        {"id": 15, "name": "周婷", "letter": "Z", "avatar": av("周婷"), "weixinId": "zhouting_0618", "region": "湖南 郴州"},
    ],
    "moments": [
        {"id": 1, "name": "张三", "avatar": av("张三"),
         "content": "在实验室调了一下午的 Bug，终于跑通了，感动到哭",
         "images": ["moment1.jpg", "moment2.jpg"], "time": "30分钟前",
         "likes": ["李思思", "王五", "陈晓"],
         "comments": [{"user": "李思思", "text": "恭喜恭喜！"},
                      {"user": "王五", "text": "强啊，不愧是张工"}]},
        {"id": 2, "name": "李思思", "avatar": av("李思思"),
         "content": "周末去了橘子洲头，天气好到不想回学校",
         "images": ["moment3.jpg", "moment4.jpg", "moment5.jpg"], "time": "2小时前",
         "likes": ["赵学刚", "刘敏"],
         "comments": [{"user": "刘敏", "text": "下次带我一个"}]},
        {"id": 3, "name": "王五", "avatar": av("王五"),
         "content": "分享一篇文章：Android 从入门到放弃（附避坑指南）",
         "images": [], "time": "5小时前", "likes": [], "comments": []},
        {"id": 4, "name": "刘敏", "avatar": av("刘敏"),
         "content": "食堂三楼的麻辣香锅真的绝了，强烈推荐",
         "images": ["moment6.jpg"], "time": "昨天",
         "likes": ["张三", "周婷", "陈晓"],
         "comments": [{"user": "周婷", "text": "明天就去尝尝"},
                     {"user": "张三", "text": "几点人少？"}]},
        {"id": 5, "name": "陈晓", "avatar": av("陈晓"),
         "content": "操作系统课设终于把 A1 写完了，后面还有 A4、A5……",
         "images": [], "time": "昨天", "likes": ["赵学刚", "李思思"], "comments": []},
    ]
}

with open(os.path.join(ASSETS, "weixin.json"), "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

# ---------------- 统计 ----------------
jpgs = [n for n in os.listdir(DRAWABLE) if n.endswith(".jpg")]
mip_total = 0
for root, dirs, files in os.walk(RES):
    if "mipmap" in root:
        mip_total += len(files)
print("drawable-nodpi jpg count =", len(jpgs))
print("mipmap launcher =", mip_total)
print("json bytes =", os.path.getsize(os.path.join(ASSETS, "weixin.json")))
for n in sorted(jpgs):
    print("  ", n)
