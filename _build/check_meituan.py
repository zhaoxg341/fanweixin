import os, re, sys

root = r"C:\Users\lenovo\WorkBuddy\2026-10-12-14-18-05\MeiTuanWaiMai\app\src\main"
res = os.path.join(root, "res")
java_root = os.path.join(root, "java")

# 收集 layout
layouts = {}
for f in os.listdir(os.path.join(res, "layout")):
    if f.endswith(".xml"):
        layouts[f[:-4]] = os.path.join(res, "layout", f)

# 收集 id
ids = set()
for name, path in layouts.items():
    txt = open(path, encoding="utf-8").read()
    ids.update(re.findall(r'@\+id/(\w+)', txt))
    # include 引用
    ids.update([])

# 收集 drawable / color / string
drawables = set()
ddir = os.path.join(res, "drawable")
for f in os.listdir(ddir):
    drawables.add(f.rsplit(".", 1)[0])
for f in os.listdir(os.path.join(res, "drawable-nodpi")):
    drawables.add(f.rsplit(".", 1)[0])

colors = set(re.findall(r'<color name="(\w+)"', open(os.path.join(res, "values", "colors.xml"), encoding="utf-8").read()))
strings = set(re.findall(r'<string name="(\w+)"', open(os.path.join(res, "values", "strings.xml"), encoding="utf-8").read()))

# 收集 java
java_files = []
for dirpath, _, files in os.walk(java_root):
    for f in files:
        if f.endswith(".java"):
            java_files.append(os.path.join(dirpath, f))

errors = []
for path in java_files:
    txt = open(path, encoding="utf-8", errors="ignore").read()
    short = os.path.basename(path)
    for name in re.findall(r'R\.layout\.(\w+)', txt):
        if name not in layouts:
            errors.append(f"{short}: R.layout.{name} 不存在")
    for name in re.findall(r'R\.id\.(\w+)', txt):
        if name not in ids:
            errors.append(f"{short}: R.id.{name} 不存在")
    for name in re.findall(r'R\.drawable\.(\w+)', txt):
        if name not in drawables:
            errors.append(f"{short}: R.drawable.{name} 不存在")
    for name in re.findall(r'R\.color\.(\w+)', txt):
        if name not in colors:
            errors.append(f"{short}: R.color.{name} 不存在")
    for name in re.findall(r'R\.string\.(\w+)', txt):
        if name not in strings:
            errors.append(f"{short}: R.string.{name} 不存在")

# 检查 layout 内引用 @drawable 是否存在（排除系统）
for name, path in layouts.items():
    txt = open(path, encoding="utf-8").read()
    for d in re.findall(r'@drawable/(\w+)', txt):
        if d not in drawables:
            errors.append(f"layout/{name}.xml: @drawable/{d} 不存在")
    for c in re.findall(r'@color/(\w+)', txt):
        pass

print("JAVA FILES:", len(java_files))
print("IDS:", len(ids))
if errors:
    print("=== 问题 ===")
    for e in errors:
        print(e)
else:
    print("静态检查通过：未发现缺失资源")
