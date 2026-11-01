# -*- coding: utf-8 -*-
"""对工程内自写的 Java 类做交叉校验：
1. 类名与文件名是否一致、是否有 package 声明；
2. import cn.itcast.order.* 指向的类是否真实存在；
3. 对其他自写类的静态调用（ClassName.method(...)）方法名是否存在；
4. 大括号/圆括号是否配对；
5. 实现了 R.id.xxx 之外的接口方法是否齐全（仅检查显式 implements 的接口方法名是否出现）。
"""
import os
import re

JAVA = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\MeiTuanWaiMai\app\src\main\java"

errors = []
files = []
for dirpath, _dirs, fs in os.walk(JAVA):
    for f in fs:
        if f.endswith(".java"):
            files.append(os.path.join(dirpath, f))

# 解析每个文件
classes = {}   # 全限定名 -> {"file":..., "methods":set(), "text":...}
simple = {}    # 简单类名 -> 全限定名

for path in files:
    with open(path, encoding="utf-8") as fp:
        text = fp.read()
    base = os.path.basename(path)[:-5]
    pkg_m = re.search(r"^\s*package\s+([\w.]+)\s*;", text, re.M)
    if not pkg_m:
        errors.append("%s: 缺少 package 声明" % base)
        continue
    pkg = pkg_m.group(1)
    if base not in text:
        errors.append("%s: 文件内没有出现与文件名同名的类" % base)
    fqn = pkg + "." + base
    methods = set(re.findall(r"(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\],\s.?]+\s+(\w+)\s*\(", text))
    classes[fqn] = {"file": path, "methods": methods, "text": text}
    simple[base] = fqn

    # 括号配对
    if text.count("{") != text.count("}"):
        errors.append("%s: 大括号不配对 {=%d }=%d" % (base, text.count("{"), text.count("}")))
    if text.count("(") != text.count(")"):
        errors.append("%s: 圆括号不配对 (=%d )=%d" % (base, text.count("("), text.count(")")))

# 校验 import cn.itcast.order.*
for fqn, info in classes.items():
    text = info["text"]
    for m in re.finditer(r"^\s*import\s+(cn\.itcast\.order\.[\w.]+)\s*;", text, re.M):
        target = m.group(1)
        if target.endswith(".R"):
            continue
        if target not in classes:
            errors.append("%s: import %s 指向的类不存在" % (fqn.split(".")[-1], target))

# 校验对其他自写类的静态调用
KNOWN = {n: fqn for n, fqn in simple.items()}
for fqn, info in classes.items():
    text = info["text"]
    me = fqn.split(".")[-1]
    for m in re.finditer(r"\b([A-Z]\w+)\.(\w+)\s*\(", text):
        cls, method = m.group(1), m.group(2)
        if cls == me or cls not in KNOWN:
            continue   # 只检查工程内自写类
        if method[0].isupper():
            continue   # 首字母大写说明是内部类/接口（如 OutClass.InnerClass），不是方法调用
        target = classes[KNOWN[cls]]
        if method not in target["methods"]:
            errors.append("%s: 调用 %s.%s(...)，但 %s 里没有该方法"
                          % (me, cls, method, cls))

# 校验接口实现：ShopDetail/ShopList 等 implements 的接口方法
iface_methods = {
    "MenuAdapter.OnMenuListener": ["onFoodClick", "onCartChanged"],
    "CartAdapter.OnCartChangedListener": ["onCartChanged"],
}
for fqn, info in classes.items():
    text = info["text"]
    for impl in re.findall(r"implements\s+([\w.,\s]+?)\s*\{", text):
        for part in impl.split(","):
            part = part.strip()
            if part in iface_methods:
                for meth in iface_methods[part]:
                    if not re.search(r"public\s+void\s+%s\s*\(" % meth, text):
                        errors.append("%s: 实现了 %s 但看不到 %s() 方法"
                                      % (fqn.split(".")[-1], part, meth))

print("自写 Java 类数量: %d" % len(classes))
for fqn in sorted(classes):
    print("  - " + fqn)
print("-" * 60)
if errors:
    print("发现 %d 个问题:" % len(errors))
    for e in errors:
        print("  [X] " + e)
else:
    print("Java 交叉校验通过：类名、导入、跨类静态调用、接口实现均正常。")
