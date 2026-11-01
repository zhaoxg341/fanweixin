# -*- coding: utf-8 -*-
"""
为《Android移动开发基础案例教程》第12章 —— 综合项目「仿美团外卖」
批量生成演示用图片资源(res/drawable-nodpi) 与 服务器数据(app/src/main/assets/shop.json)

运行: python gen_images.py
"""
import json
import os
import random
from PIL import Image, ImageDraw, ImageFont

ROOT = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\MeiTuanWaiMai"
RES = os.path.join(ROOT, "app", "src", "main", "res")
DRAWABLE = os.path.join(RES, "drawable-nodpi")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")

FONT_CANDIDATES = [
    r"C:\Windows\Fonts\msyhbd.ttc",
    r"C:\Windows\Fonts\msyh.ttc",
    r"C:\Windows\Fonts\simhei.ttf",
    r"C:\Windows\Fonts\simsun.ttc",
]


def font(size):
    for p in FONT_CANDIDATES:
        if os.path.exists(p):
            try:
                return ImageFont.truetype(p, size)
            except Exception:
                pass
    return ImageFont.load_default()


def gradient(size, c1, c2):
    """竖直渐变"""
    w, h = size
    base = Image.new("RGB", size, c1)
    top = Image.new("RGB", size, c2)
    mask = Image.new("L", size)
    d = ImageDraw.Draw(mask)
    for y in range(h):
        d.line([(0, y), (w, y)], fill=int(255 * y / max(1, h - 1)))
    base.paste(top, (0, 0), mask)
    return base.convert("RGBA")


def rounded(img, radius):
    w, h = img.size
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, w - 1, h - 1], radius=radius, fill=255)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def text_at(draw, text, f, cx, cy, fill=(255, 255, 255)):
    box = draw.textbbox((0, 0), text, font=f)
    tw, th = box[2] - box[0], box[3] - box[1]
    draw.text((cx - tw / 2 - box[0], cy - th / 2 - box[1]), text, font=f, fill=fill)


def text_left(draw, text, f, x, cy, fill=(255, 255, 255)):
    box = draw.textbbox((0, 0), text, font=f)
    th = box[3] - box[1]
    draw.text((x - box[0], cy - th / 2 - box[1]), text, font=f, fill=fill)


# ---------------------------------------------------------------- 数据定义
SHOPS = [
    {
        "id": 1, "shopName": "肯德基宅急送", "score": 4.8, "saleNum": 1200,
        "offerPrice": 20, "distributionCost": 5, "feature": "满30减10", "time": "30分钟",
        "category": "快餐", "colors": ("#E8452B", "#F7A03B"),
        "foods": [
            ("香辣鸡腿堡", 18.5, "人气爆款"), ("新奥尔良烤翅", 12.0, "店长推荐"),
            ("劲爆鸡米花", 11.0, "热销"), ("黄金薯条(大)", 9.0, "经典小食"),
            ("冰镇可乐(中)", 7.0, "解腻首选"), ("芝士鸡肉卷", 14.5, "新品上市"),
        ],
    },
    {
        "id": 2, "shopName": "张亮麻辣烫", "score": 4.7, "saleNum": 3200,
        "offerPrice": 15, "distributionCost": 3, "feature": "满25减8", "time": "25分钟",
        "category": "麻辣烫", "colors": ("#C0392B", "#E97B3C"),
        "foods": [
            ("招牌麻辣烫", 22.0, "镇店招牌"), ("番茄麻辣烫", 24.0, "酸甜可口"),
            ("麻辣香锅", 38.0, "人气爆款"), ("手打牛肉丸", 12.0, "弹牙"),
            ("现炸小油条", 3.0, "搭配必点"), ("冰镇酸梅汤", 6.0, "解辣"),
        ],
    },
    {
        "id": 3, "shopName": "沙县小吃", "score": 4.5, "saleNum": 860,
        "offerPrice": 12, "distributionCost": 2, "feature": "满20减5", "time": "20分钟",
        "category": "小吃", "colors": ("#3E7C59", "#8DC26F"),
        "foods": [
            ("花生酱拌面", 8.0, "经典主食"), ("扁肉馄饨", 9.0, "皮薄馅大"),
            ("蒸饺(8只)", 10.0, "现包现蒸"), ("卤蛋", 2.5, "加料"),
            ("瓦罐炖汤", 12.0, "滋补"), ("花生汤", 6.5, "甜品"),
        ],
    },
    {
        "id": 4, "shopName": "蜜雪冰城", "score": 4.6, "saleNum": 5600,
        "offerPrice": 10, "distributionCost": 2, "feature": "第二杯半价", "time": "15分钟",
        "category": "饮品", "colors": ("#E4002B", "#FF7A8A"),
        "foods": [
            ("冰鲜柠檬水", 5.0, "销量第一"), ("珍珠奶茶", 8.0, "经典款"),
            ("香草圣代", 6.0, "冰爽甜品"), ("蜜桃四季春", 7.0, "果香清爽"),
            ("摩天脆脆", 8.0, "第二杯半价"), ("满杯百香果", 7.5, "酸甜"),
        ],
    },
    {
        "id": 5, "shopName": "川香居小炒", "score": 4.6, "saleNum": 940,
        "offerPrice": 25, "distributionCost": 4, "feature": "满50减15", "time": "35分钟",
        "category": "正餐", "colors": ("#B03A2E", "#D96A3B"),
        "foods": [
            ("宫保鸡丁", 28.0, "招牌川菜"), ("麻婆豆腐", 18.0, "麻辣鲜香"),
            ("水煮肉片", 32.0, "人气爆款"), ("鱼香肉丝", 26.0, "下饭神器"),
            ("干煸四季豆", 20.0, "素菜"), ("米饭", 2.0, "主食"),
        ],
    },
    {
        "id": 6, "shopName": "兰州牛肉拉面", "score": 4.7, "saleNum": 1800,
        "offerPrice": 18, "distributionCost": 3, "feature": "满30减6", "time": "28分钟",
        "category": "面食", "colors": ("#B9770E", "#E0A940"),
        "foods": [
            ("牛肉拉面", 15.0, "一清二白"), ("加肉拉面", 22.0, "肉量翻倍"),
            ("凉拌牛肉", 26.0, "下酒菜"), ("牛肉炒面", 18.0, "锅气十足"),
            ("大盘鸡拌面", 24.0, "店长推荐"), ("自制酸奶", 6.0, "解腻"),
        ],
    },
]

BANNERS = [
    ("新客立减20元", "首单专享 · 限时领取", ("#FF6A00", "#FF2D55")),
    ("午市套餐5折起", "11:00-13:00 限时秒杀", ("#2E7DF7", "#39C0FF")),
    ("满30减15", "全城免配送费 · 30分钟送达", ("#00A870", "#7ED957")),
]

FOOD_PALETTE = [
    ("#FF9A44", "#FC6076"), ("#43CBFF", "#9708CC"), ("#F6D365", "#FDA085"),
    ("#84FAB0", "#8FD3F4"), ("#FFD26F", "#3677FF"), ("#F093FB", "#F5576C"),
    ("#5EFCE8", "#736EFE"), ("#FAD961", "#F76B1C"), ("#A8EDEA", "#FED6E3"),
    ("#FF758C", "#FF7EB3"), ("#C2E9FB", "#A1C4FD"), ("#FBC2EB", "#A6C1EE"),
]


# ---------------------------------------------------------------- 生成函数
def gen_food(name, idx):
    w = h = 360
    c1, c2 = FOOD_PALETTE[idx % len(FOOD_PALETTE)]
    img = gradient((w, h), c1, c2)
    d = ImageDraw.Draw(img, "RGBA")
    # 装饰圆环
    d.ellipse([w * 0.52, -h * 0.15, w * 1.25, h * 0.62], fill=(255, 255, 255, 34))
    d.ellipse([-w * 0.22, h * 0.60, w * 0.45, h * 1.3], fill=(255, 255, 255, 24))
    # 中心白色圆底
    d.ellipse([w * 0.32, h * 0.16, w * 0.68, h * 0.52], fill=(255, 255, 255, 52))
    text_at(d, name[:2], font(64), w / 2, h * 0.34, (255, 255, 255))
    # 底部深色半透明条 + 菜名
    f_name = font(38)
    box = d.textbbox((0, 0), name, font=f_name)
    tw = box[2] - box[0]
    d.rounded_rectangle([w / 2 - tw / 2 - 22, h * 0.66, w / 2 + tw / 2 + 22, h * 0.66 + 62],
                        radius=31, fill=(0, 0, 0, 92))
    text_at(d, name, f_name, w / 2, h * 0.66 + 31, (255, 255, 255))
    img = rounded(img, 26)
    img.convert("RGB").save(os.path.join(DRAWABLE, "food%d.jpg" % idx), quality=88)


def gen_shop(shop, idx):
    w = h = 240
    c1, c2 = shop["colors"]
    img = gradient((w, h), c1, c2)
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse([w * 0.45, -h * 0.2, w * 1.3, h * 0.65], fill=(255, 255, 255, 40))
    d.ellipse([-w * 0.3, h * 0.55, w * 0.5, h * 1.35], fill=(255, 255, 255, 30))
    text_at(d, shop["shopName"][:2], font(72), w / 2, h / 2)
    img = rounded(img, 24)
    img.convert("RGB").save(os.path.join(DRAWABLE, "shop%d.jpg" % idx), quality=90)


def gen_banner(title, sub, colors, idx):
    w, h = 1080, 420
    img = gradient((w, h), colors[0], colors[1])
    d = ImageDraw.Draw(img, "RGBA")
    d.ellipse([w * 0.66, -h * 0.45, w * 1.25, h * 0.9], fill=(255, 255, 255, 36))
    d.ellipse([w * 0.70, h * 0.35, w * 1.4, h * 1.5], fill=(255, 255, 255, 26))
    text_left(d, title, font(96), 80, h * 0.40)
    text_left(d, sub, font(46), 84, h * 0.66, (255, 255, 255, 225))
    img.convert("RGB").save(os.path.join(DRAWABLE, "banner%d.jpg" % idx), quality=90)


def gen_qr():
    module = 12
    n = 40
    size = module * n
    img = Image.new("RGB", (size, size), (255, 255, 255))
    d = ImageDraw.Draw(img)
    rnd = random.Random(20260705)

    def finder(cx, cy):
        d.rectangle([cx, cy, cx + 7 * module - 1, cy + 7 * module - 1], fill=(0, 0, 0))
        d.rectangle([cx + module, cy + module, cx + 6 * module - 1, cy + 6 * module - 1], fill=(255, 255, 255))
        d.rectangle([cx + 2 * module, cy + 2 * module, cx + 5 * module - 1, cy + 5 * module - 1], fill=(0, 0, 0))

    for y in range(n):
        for x in range(n):
            if rnd.random() < 0.45:
                d.rectangle([x * module, y * module, (x + 1) * module - 1, (y + 1) * module - 1], fill=(0, 0, 0))
    for (fx, fy) in [(0, 0), (n - 8, 0), (0, n - 8)]:
        d.rectangle([fx * module - 4, fy * module - 4, (fx + 8) * module + 3, (fy + 8) * module + 3], fill=(255, 255, 255))
    finder(0, 0)
    finder((n - 7) * module, 0)
    finder(0, (n - 7) * module)

    c = 7 * module
    d.rectangle([size // 2 - c, size // 2 - c, size // 2 + c, size // 2 + c], fill=(255, 255, 255))
    d.rounded_rectangle([size // 2 - c + 8, size // 2 - c + 8, size // 2 + c - 8, size // 2 + c - 8],
                        radius=18, fill=(255, 125, 0))
    text_at(d, "美", font(88), size / 2, size / 2)
    img.save(os.path.join(DRAWABLE, "qr_pay.png"))


def gen_launcher():
    sizes = [("mdpi", 48), ("hdpi", 72), ("xhdpi", 96), ("xxhdpi", 144), ("xxxhdpi", 192)]
    for name, px in sizes:
        out_dir = os.path.join(RES, "mipmap-" + name)
        os.makedirs(out_dir, exist_ok=True)
        img = gradient((px, px), "#FF9A2E", "#FF6A00")
        d = ImageDraw.Draw(img, "RGBA")
        d.ellipse([px * 0.45, -px * 0.18, px * 1.3, px * 0.66], fill=(255, 255, 255, 42))
        text_at(d, "美", font(int(px * 0.62)), px / 2, px / 2)
        rounded(img, int(px * 0.24)).convert("RGB").save(os.path.join(out_dir, "ic_launcher.png"))
        # 圆形图标
        img2 = gradient((px, px), "#FF9A2E", "#FF6A00")
        d2 = ImageDraw.Draw(img2, "RGBA")
        d2.ellipse([px * 0.45, -px * 0.18, px * 1.3, px * 0.66], fill=(255, 255, 255, 42))
        text_at(d2, "美", font(int(px * 0.62)), px / 2, px / 2)
        mask = Image.new("L", (px, px), 0)
        ImageDraw.Draw(mask).ellipse([0, 0, px - 1, px - 1], fill=255)
        out = Image.new("RGB", (px, px), (255, 125, 0))
        out.paste(img2.convert("RGB"), (0, 0), mask)
        out.save(os.path.join(out_dir, "ic_launcher_round.png"))


def build_json():
    data = {
        "banners": ["banner1", "banner2", "banner3"],
        "shops": [],
    }
    food_no = 1
    for i, s in enumerate(SHOPS, start=1):
        foods = []
        for fname, price, pop in s["foods"]:
            foods.append({
                "foodId": i * 100 + len(foods) + 1,
                "foodName": fname,
                "popularity": pop,
                "saleNum": 60 + (food_no * 37) % 400,
                "price": price,
                "foodPic": "food%d" % food_no,
            })
            food_no += 1
        data["shops"].append({
            "id": s["id"],
            "shopName": s["shopName"],
            "score": s["score"],
            "saleNum": s["saleNum"],
            "offerPrice": s["offerPrice"],
            "distributionCost": s["distributionCost"],
            "feature": s["feature"],
            "time": s["time"],
            "category": s["category"],
            "banner": "shop%d" % i,
            "foodList": foods,
        })
    os.makedirs(ASSETS, exist_ok=True)
    with open(os.path.join(ASSETS, "shop.json"), "w", encoding="utf-8") as fp:
        json.dump(data, fp, ensure_ascii=False, indent=2)
    return food_no - 1, len(data["shops"])


def main():
    os.makedirs(DRAWABLE, exist_ok=True)
    food_no = 1
    for s in SHOPS:
        for fname, _, _ in s["foods"]:
            gen_food(fname, food_no)
            food_no += 1
    for i, s in enumerate(SHOPS, start=1):
        gen_shop(s, i)
    for i, (t, sub, c) in enumerate(BANNERS, start=1):
        gen_banner(t, sub, c, i)
    gen_qr()
    gen_launcher()
    n_food, n_shop = build_json()
    print("OK  food=%d  shop=%d  banner=%d  drawable=%s" % (n_food, n_shop, len(BANNERS), DRAWABLE))


if __name__ == "__main__":
    main()
