# -*- coding: utf-8 -*-
"""把 _build/genimg 里新生成的真人风图片裁掉底部水印、裁剪压缩，
覆盖到 WeiXinDemo 的 drawable-nodpi（avatar*.jpg / moment*.jpg / moments_cover.jpg）"""
import os
from PIL import Image

SRC = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\_build\genimg"
DST = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\WeiXinDemo\app\src\main\res\drawable-nodpi"

# 按生成顺序映射：17 个好友头像 + 我的头像
AVATARS = [
    ("真人手机自拍照_25岁中国年轻女性", "avatar1"),
    ("真人手机自拍照_28岁中国年轻男性", "avatar2"),
    ("真人照片_45岁中国中年女性", "avatar3"),
    ("真人照片_50岁中国中年男性", "avatar4"),
    ("真人手机自拍照_22岁中国女大学生", "avatar5"),
    ("真人手机自拍照_26岁中国男性_运动装", "avatar6"),
    ("真人手机自拍照_30岁中国女性_中长发微卷", "avatar7"),
    ("真人照片_35岁中国男性工程师", "avatar8"),
    ("真人手机自拍照_20岁中国男大学生_篮球服", "avatar9"),
    ("真人照片_60岁中国老奶奶", "avatar10"),
    ("真人照片_62岁中国老爷爷", "avatar11"),
    ("真人手机自拍照_24岁中国女性_长发披肩", "avatar12"),
    ("真人手机自拍照_23岁中国男性程序员", "avatar13"),
    ("真人照片_32岁中国女教师", "avatar14"),
    ("真人手机自拍照_27岁中国男性外卖员", "avatar15"),
    ("真人照片_38岁中国女性医生", "avatar16"),
    ("真人手机自拍照_19岁中国女高中生", "avatar17"),
    ("真人手机自拍照_21岁中国男大学生_我的头像", "avatar_me"),
]

MOMENTS = [
    ("手机实拍生活照_一杯奶茶和一块草莓蛋糕", "moment1"),
    ("手机实拍旅行照_云南泸沽湖清晨", "moment2"),
    ("手机实拍宠物照_一只橘猫", "moment3"),
    ("手机实拍美食照_重庆火锅沸腾红汤", "moment4"),
    ("手机实拍夜景_重庆洪崖洞夜景", "moment5"),
    ("手机实拍运动照_清晨公园跑道", "moment6"),
]

COVER_PREFIX = "朋友圈封面横幅图"


def load_cropped(path):
    img = Image.open(path).convert("RGB")
    w, h = img.size
    # 裁掉底部 9%（AI 生成水印在右下角）
    img = img.crop((0, 0, w, int(h * 0.91)))
    return img


def center_square(img, side):
    w, h = img.size
    left = (w - side) // 2
    top = (h - side) // 2
    return img.crop((left, top, left + side, top + side))


def main():
    files = os.listdir(SRC)
    used = set()
    count = 0

    for prefix, name in AVATARS:
        src = next((f for f in files if f.startswith(prefix) and f.endswith(".png")), None)
        if src is None:
            print("!! 未找到:", prefix)
            continue
        used.add(src)
        img = load_cropped(os.path.join(SRC, src))
        w, h = img.size
        img = center_square(img, min(w, h)).resize((400, 400), Image.LANCZOS)
        img.save(os.path.join(DST, name + ".jpg"), "JPEG", quality=85)
        count += 1
        print("头像", name + ".jpg", "<-", src)

    for prefix, name in MOMENTS:
        src = next((f for f in files if f.startswith(prefix) and f.endswith(".png")), None)
        if src is None:
            print("!! 未找到:", prefix)
            continue
        used.add(src)
        img = load_cropped(os.path.join(SRC, src))
        w, h = img.size
        img = center_square(img, min(w, h)).resize((800, 800), Image.LANCZOS)
        img.save(os.path.join(DST, name + ".jpg"), "JPEG", quality=85)
        count += 1
        print("配图", name + ".jpg", "<-", src)

    src = next((f for f in files if f.startswith(COVER_PREFIX) and f.endswith(".png")), None)
    if src is None:
        print("!! 未找到封面")
    else:
        used.add(src)
        img = load_cropped(os.path.join(SRC, src))
        w, h = img.size
        # 16:9 横幅居中裁剪
        th = int(w * 9 / 16)
        if th <= h:
            top = (h - th) // 2
            img = img.crop((0, top, w, top + th))
        img = img.resize((1200, 675), Image.LANCZOS)
        img.save(os.path.join(DST, "moments_cover.jpg"), "JPEG", quality=85)
        count += 1
        print("封面 moments_cover.jpg <-", src)

    print("共输出", count, "张")
    leftover = [f for f in files if f.endswith(".png") and f not in used]
    if leftover:
        print("未使用:", leftover)


if __name__ == "__main__":
    main()
