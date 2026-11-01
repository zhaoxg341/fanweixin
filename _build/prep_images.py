# -*- coding: utf-8 -*-
"""把 _build/img 里 AI 生成的图片裁掉底部水印、压缩并输出到 MilkTeaDemo drawable-nodpi"""
import os
from PIL import Image

SRC = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\_build\img"
DST = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\MilkTeaDemo\app\src\main\res\drawable-nodpi"

MAPPING = {
    "商业产品摄影_一杯招牌珍珠奶茶": "drink1",
    "商业产品摄影_一杯黑糖脏脏鲜奶": "drink2",
    "商业产品摄影_一杯芝士奶盖绿茶": "drink3",
    "商业产品摄影_一杯多肉葡萄果茶": "drink4",
    "商业产品摄影_一杯杨枝甘露": "drink5",
    "商业产品摄影_一杯满杯百香果果茶": "drink6",
    "商业产品摄影_一杯生椰拿铁": "drink7",
    "商业产品摄影_一杯抹茶奶绿": "drink8",
    "商业产品摄影_一杯布丁奶茶": "drink9",
    "商业产品摄影_一杯芋泥波波奶茶": "drink10",
    "商业美食摄影_三个金黄色的葡式蛋挞": "snack1",
    "奶茶店促销横幅摄影_三杯不同的奶茶饮品": "banner1",
}

os.makedirs(DST, exist_ok=True)

for f in os.listdir(SRC):
    if not f.lower().endswith(".png"):
        continue
    target = None
    for prefix, name in MAPPING.items():
        if f.startswith(prefix):
            target = name
            break
    if target is None:
        print("跳过未匹配文件:", f)
        continue

    img = Image.open(os.path.join(SRC, f)).convert("RGB")
    w, h = img.size
    # 裁掉底部 9%（去掉 AI 生成水印）
    img = img.crop((0, 0, w, int(h * 0.91)))
    w, h = img.size

    if target == "banner1":
        # 横幅：按 2:1 居中裁剪后缩放到 1200x600
        th = w // 2
        top = (h - th) // 2
        img = img.crop((0, top, w, top + th)).resize((1200, 600), Image.LANCZOS)
        img.save(os.path.join(DST, target + ".jpg"), "JPEG", quality=85)
    else:
        # 商品图：居中裁正方形，缩放 600x600
        side = min(w, h)
        left = (w - side) // 2
        top = (h - side) // 2
        img = img.crop((left, top, left + side, top + side)).resize((600, 600), Image.LANCZOS)
        img.save(os.path.join(DST, target + ".jpg"), "JPEG", quality=82)
    print("输出", target + ".jpg", "原始", f)
print("完成")
