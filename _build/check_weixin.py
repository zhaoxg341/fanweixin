# -*- coding: utf-8 -*-
"""WeiXinDemo 静态引用校验：
1. Java 里的 R.xxx.yyy 是否都有对应资源；
2. XML 里的 @drawable/@color/@string/@style/@dimen 是否都已定义；
3. XML 里 @+id/xxx 与 Java 里 R.id.xxx 是否互相对应；
4. assets/weixin.json 里引用的图片是否都存在于 drawable-nodpi；
5. Java 内部：括号配对、package 声明、跨类静态调用是否存在。
"""
import os
import re
import json
import xml.etree.ElementTree as ET

ROOT = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\WeiXinDemo\app\src\main"
RES = os.path.join(ROOT, "res")
JAVA = os.path.join(ROOT, "java")

errors = []

# ---------- 1. 收集资源 ----------
res = {"layout": set(), "drawable": set(), "color": set(), "string": set(),
       "style": set(), "mipmap": set(), "dimen": set()}

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
            if ext != ".xml":
                continue
            try:
                tree = ET.parse(os.path.join(dirpath, f))
            except ET.ParseError as e:
                errors.append("values 解析失败 %s: %s" % (f, e))
                continue
            for child in tree.getroot():
                nm = child.get("name")
                if not nm:
                    continue
                tag = child.tag
                if tag == "color":
                    res["color"].add(nm)
                elif tag == "string":
                    res["string"].add(nm)
                elif tag == "style":
                    res["style"].add(nm)
                elif tag == "dimen":
                    res["dimen"].add(nm)

# ---------- 2. Java 中的 R.xxx.yyy ----------
java_files = []
for dirpath, _dirs, files in os.walk(JAVA):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(dirpath, f))

java_ids = set()
r_ref_re = re.compile(r"R\.(layout|id|drawable|string|color|style|mipmap|dimen)\.([A-Za-z_][A-Za-z0-9_]*)")

for path in java_files:
    with open(path, encoding="utf-8") as fp:
        text = fp.read()
    for m in r_ref_re.finditer(text):
        kind, name = m.group(1), m.group(2)
        if kind == "id":
            java_ids.add(name)
        elif kind in res and name not in res[kind]:
            errors.append("%s: R.%s.%s 未找到" % (os.path.basename(path), kind, name))

# ---------- 3. XML 里的资源引用 ----------
xml_id_defs = set()
xml_ref_re = re.compile(r"@(android:)?(drawable|color|string|style|mipmap|layout|dimen)/([A-Za-z_.][A-Za-z0-9_.]*)")
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
        if kind in res and name not in res[kind]:
            errors.append("%s: @%s/%s 未找到" % (os.path.basename(path), kind, name))
    for m in xml_id_re.finditer(text):
        xml_id_defs.add(m.group(1))

for jid in sorted(java_ids):
    if jid not in xml_id_defs:
        errors.append("Java 引用了 R.id.%s，但没有任何布局定义 @+id/%s" % (jid, jid))

# ---------- 4. weixin.json 里引用的图片 ----------
assets_json = os.path.join(ROOT, "assets", "weixin.json")
with open(assets_json, encoding="utf-8") as fp:
    data = json.load(fp)


def check_image(name):
    """JSON 里是 avatar1.jpg，资源名是 avatar1"""
    if not name:
        return
    key = name[:-4] if name.lower().endswith(".jpg") else name
    if key not in res["drawable"]:
        errors.append("weixin.json 引用了不存在的图片: %s" % name)


check_image(data.get("user", {}).get("avatar"))
for chat in data.get("chats", []):
    check_image(chat.get("avatar"))
for contact in data.get("contacts", []):
    check_image(contact.get("avatar"))
for moment in data.get("moments", []):
    check_image(moment.get("avatar"))
    for img in moment.get("images", []):
        check_image(img)

# ---------- 5. Java 内部交叉校验 ----------
classes = {}
simple = {}
for path in java_files:
    with open(path, encoding="utf-8") as fp:
        text = fp.read()
    base = os.path.basename(path)[:-5]
    pkg_m = re.search(r"^\s*package\s+([\w.]+)\s*;", text, re.M)
    if not pkg_m:
        errors.append("%s: 缺少 package 声明" % base)
        continue
    if base not in text:
        errors.append("%s: 文件内没有出现与文件名同名的类" % base)
    fqn = pkg_m.group(1) + "." + base
    methods = set(re.findall(
        r"(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\],\s.?]+\s+(\w+)\s*\(", text))
    classes[fqn] = {"file": path, "methods": methods, "text": text}
    simple[base] = fqn

    if text.count("{") != text.count("}"):
        errors.append("%s: 大括号不配对 {=%d }=%d" % (base, text.count("{"), text.count("}")))
    if text.count("(") != text.count(")"):
        errors.append("%s: 圆括号不配对 (=%d )=%d" % (base, text.count("("), text.count(")")))

for fqn, info in classes.items():
    text = info["text"]
    for m in re.finditer(r"^\s*import\s+(cn\.itcast\.weixin\.[\w.]+)\s*;", text, re.M):
        target = m.group(1)
        if target.endswith(".R"):
            continue
        if target not in classes:
            errors.append("%s: import %s 指向的类不存在" % (fqn.split(".")[-1], target))

KNOWN = dict(simple)
for fqn, info in classes.items():
    text = info["text"]
    me = fqn.split(".")[-1]
    for m in re.finditer(r"\b([A-Z]\w+)\.(\w+)\s*\(", text):
        cls, method = m.group(1), m.group(2)
        if cls == me or cls not in KNOWN:
            continue
        if method[0].isupper():
            continue
        target = classes[KNOWN[cls]]
        if method not in target["methods"]:
            errors.append("%s: 调用 %s.%s(...)，但 %s 里没有该方法" % (me, cls, method, cls))

# ---------- 输出 ----------
print("资源统计: layout=%d drawable=%d color=%d string=%d style=%d dimen=%d mipmap=%d"
      % (len(res["layout"]), len(res["drawable"]), len(res["color"]), len(res["string"]),
         len(res["style"]), len(res["dimen"]), len(res["mipmap"])))
print("Java 文件数=%d，XML 文件数=%d" % (len(java_files), len(xml_files)))
print("布局定义的 id 数量=%d，Java 使用的 id 数量=%d" % (len(xml_id_defs), len(java_ids)))
print("-" * 60)
if errors:
    print("发现 %d 个问题:" % len(errors))
    for e in errors:
        print("  [X] " + e)
else:
    print("全部引用校验通过，没有发现缺失的资源或 id。")
