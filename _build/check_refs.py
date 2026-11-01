# -*- coding: utf-8 -*-
"""对 Android 工程做静态引用校验：
1. Java 里的 R.xxx.yyy 是否都有对应资源；
2. XML 里的 @drawable/@color/@string/@style 是否都已定义；
3. XML 里 @+id/xxx 与 Java 里 R.id.xxx 是否互相对应；
4. shop.json 中引用的图片名是否都存在于 drawable-nodpi。
"""
import os
import re
import json
import xml.etree.ElementTree as ET

ROOT = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\MeiTuanWaiMai\app\src\main"
RES = os.path.join(ROOT, "res")
JAVA = os.path.join(ROOT, "java")

errors = []
warnings = []

# ---------- 1. 收集资源 ----------
res = {"layout": set(), "drawable": set(), "color": set(), "string": set(), "style": set(), "mipmap": set()}

for dirpath, _dirs, files in os.walk(RES):
    folder = os.path.basename(dirpath)
    if folder.startswith("layout"):
        key = "layout"
    elif folder == "values":
        key = "values"
    elif folder.startswith("drawable"):
        key = "drawable"
    elif folder.startswith("mipmap"):
        key = "mipmap"
    else:
        continue
    for f in files:
        name, ext = os.path.splitext(f)
        if key == "layout":
            res["layout"].add(name)
        elif key == "drawable":
            res["drawable"].add(name)
        elif key == "mipmap":
            res["mipmap"].add(name)
        elif key == "values":
            if ext == ".xml":
                try:
                    tree = ET.parse(os.path.join(dirpath, f))
                except ET.ParseError as e:
                    errors.append("values 解析失败 %s: %s" % (f, e))
                    continue
                for child in tree.getroot():
                    tag = child.tag
                    nm = child.get("name")
                    if not nm:
                        continue
                    if tag == "color":
                        res["color"].add(nm)
                    elif tag == "string":
                        res["string"].add(nm)
                    elif tag == "style":
                        res["style"].add(nm)

# ---------- 2. Java 中的 R.xxx.yyy ----------
java_files = []
for dirpath, _dirs, files in os.walk(JAVA):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(dirpath, f))

java_ids = set()
r_ref_re = re.compile(r"R\.(layout|id|drawable|string|color|style|mipmap)\.([A-Za-z_][A-Za-z0-9_]*)")

for path in java_files:
    with open(path, encoding="utf-8") as fp:
        text = fp.read()
    for m in r_ref_re.finditer(text):
        kind, name = m.group(1), m.group(2)
        if kind == "id":
            java_ids.add(name)
        else:
            if kind not in res:
                continue
            if name not in res[kind]:
                errors.append("%s: R.%s.%s 未找到" % (os.path.basename(path), kind, name))

# ---------- 3. XML 中的 @drawable/@color/@string/@style ----------
xml_id_defs = set()
# 样式名可能带点（如 Theme.Order）；带 android: 前缀的是框架自带资源，不校验
xml_ref_re = re.compile(r"@(android:)?(drawable|color|string|style|mipmap|layout)/([A-Za-z_.][A-Za-z0-9_.]*)")
xml_id_re = re.compile(r"@\+id/([A-Za-z_][A-Za-z0-9_]*)")

xml_files = []
for dirpath, _dirs, files in os.walk(RES):
    if os.path.basename(dirpath) == "values":
        continue
    for f in files:
        if f.endswith(".xml"):
            xml_files.append(os.path.join(dirpath, f))
xml_files.append(os.path.join(ROOT, "AndroidManifest.xml"))

for path in xml_files:
    with open(path, encoding="utf-8") as fp:
        text = fp.read()
    for m in xml_ref_re.finditer(text):
        is_framework, kind, name = m.group(1), m.group(2), m.group(3)
        if is_framework:
            continue
        if kind not in res:
            continue
        if name not in res[kind]:
            errors.append("%s: @%s/%s 未找到" % (os.path.basename(path), kind, name))
    for m in xml_id_re.finditer(text):
        xml_id_defs.add(m.group(1))

# Java 里用到的 id 必须在某个布局中定义过
for jid in sorted(java_ids):
    if jid not in xml_id_defs:
        errors.append("Java 引用了 R.id.%s，但没有任何布局定义 @+id/%s" % (jid, jid))

# ---------- 4. shop.json 图片名 ----------
assets_json = os.path.join(ROOT, "assets", "shop.json")
with open(assets_json, encoding="utf-8") as fp:
    data = json.load(fp)

for b in data.get("banners", []):
    if b not in res["drawable"]:
        errors.append("shop.json banners 引用了不存在的图片: %s" % b)
for shop in data.get("shops", []):
    if shop.get("banner") not in res["drawable"]:
        errors.append("shop.json 店铺 banner 不存在: %s" % shop.get("banner"))
    for food in shop.get("foodList", []):
        if food.get("foodPic") not in res["drawable"]:
            errors.append("shop.json 菜品图不存在: %s" % food.get("foodPic"))

# ---------- 输出 ----------
print("资源统计: layout=%d drawable=%d color=%d string=%d style=%d mipmap=%d"
      % (len(res["layout"]), len(res["drawable"]), len(res["color"]),
         len(res["string"]), len(res["style"]), len(res["mipmap"])))
print("Java 文件数=%d，XML 文件数=%d" % (len(java_files), len(xml_files)))
print("布局定义的 id 数量=%d，Java 使用的 id 数量=%d" % (len(xml_id_defs), len(java_ids)))
print("-" * 60)
if errors:
    print("发现 %d 个问题:" % len(errors))
    for e in errors:
        print("  [X] " + e)
else:
    print("全部引用校验通过，没有发现缺失的资源或 id。")
if warnings:
    print("提示:")
    for w in warnings:
        print("  [!] " + w)
