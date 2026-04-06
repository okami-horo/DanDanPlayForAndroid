import xml.etree.ElementTree as ET

tree = ET.parse("build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
root = tree.getroot()

counters = {}
for c in root.findall("counter"):
    t = c.get("type")
    m = int(c.get("missed", 0))
    v = int(c.get("covered", 0))
    total = m + v
    pct = v / total * 100 if total > 0 else 0.0
    counters[t] = (v, m, total, pct)

print("====== 全项目汇总覆盖率 ======")
for t, (v, m, total, pct) in sorted(counters.items()):
    print("  %-15s covered=%6d  missed=%6d  total=%6d  %6.2f%%" % (t, v, m, total, pct))

print("\n====== 各 Package 行覆盖率（前 30，按覆盖率降序） ======")
pkgs = []
for pkg in root.findall("package"):
    name = pkg.get("name", "?")
    for c in pkg.findall("counter"):
        if c.get("type") == "LINE":
            m = int(c.get("missed", 0))
            v = int(c.get("covered", 0))
            total = m + v
            pct = v / total * 100 if total > 0 else 0.0
            pkgs.append((pct, v, total, name))

pkgs.sort(reverse=True)
for pct, v, total, name in pkgs[:30]:
    print("  %6.2f%%  %5d/%-5d  %s" % (pct, v, total, name))
if len(pkgs) > 30:
    print("  ... (共 %d 个 package，仅显示前 30)" % len(pkgs))
