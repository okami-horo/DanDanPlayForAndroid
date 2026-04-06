"""
parse_coverage.py — JaCoCo XML coverage summary tool

Usage:
    python parse_coverage.py [--xml <path>] [--hotspot-min-lines N] [--hotspot-top N]
"""

import argparse
import sys
import xml.etree.ElementTree as ET

# Ensure UTF-8 output on Windows terminals
if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

# ---------------------------------------------------------------------------
# 模块前缀映射：package 前缀 → 可读模块名
# 顺序很重要：更具体的前缀排在前，避免被通配前缀提前匹配。
# ---------------------------------------------------------------------------
MODULE_PREFIXES = [
    ("com/okamihoro/ddplaytv/app", "app"),
    ("com/okamihoro/ddplaytv", "app"),
    ("com/xyoye/anime_component", "anime_component"),
    ("com/xyoye/local_component", "local_component"),
    ("com/xyoye/player_component", "player_component"),
    ("com/xyoye/player", "player_component"),
    ("com/xyoye/open_cc", "player_component"),
    ("com/xyoye/storage_component", "storage_component"),
    ("com/xyoye/user_component", "user_component"),
    ("com/xyoye/common_component/bilibili", "bilibili_component"),
    ("com/xyoye/common_component/database", "core_database_component"),
    ("com/xyoye/common_component/log", "core_log_component"),
    ("com/xyoye/common_component/telemetry", "core_log_component"),
    ("com/xyoye/common_component/network", "core_network_component"),
    ("com/xyoye/common_component/storage", "core_storage_component"),
    ("com/xyoye/common_component/preference", "core_ui_component"),
    ("com/xyoye/common_component/adapter", "core_ui_component"),
    ("com/xyoye/common_component/base/app", "core_system_component"),
    ("com/xyoye/common_component/session", "core_system_component"),
    ("com/xyoye/common_component/notification", "core_system_component"),
    ("com/xyoye/common_component/resolver", "core_system_component"),
    ("com/xyoye/common_component/utils", "core_system_component"),
    ("com/xyoye/common_component", "core_*_component"),
    ("com/xyoye/data_component", "data_component"),
]


def classify_package(pkg_name):
    for prefix, module in MODULE_PREFIXES:
        if pkg_name.startswith(prefix):
            return module
    return "other"


def parse_args():
    p = argparse.ArgumentParser(description="JaCoCo XML coverage summary")
    p.add_argument(
        "--xml",
        default="build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml",
        help="Path to JaCoCo XML report",
    )
    p.add_argument("--hotspot-min-lines", type=int, default=20, metavar="N")
    p.add_argument("--hotspot-top", type=int, default=30, metavar="N")
    return p.parse_args()


def collect_counters(root, counter_type="LINE"):
    """Return (covered, missed, total, pct) per package for the given counter type."""
    result = []
    for pkg in root.findall("package"):
        name = pkg.get("name", "?")
        for c in pkg.findall("counter"):
            if c.get("type") == counter_type:
                missed = int(c.get("missed", 0))
                covered = int(c.get("covered", 0))
                total = missed + covered
                pct = covered / total * 100 if total > 0 else 0.0
                result.append((pct, covered, total, name))
    return result


def main():
    args = parse_args()

    try:
        tree = ET.parse(args.xml)
    except FileNotFoundError:
        print("ERROR: JaCoCo XML not found at '%s'" % args.xml, file=sys.stderr)
        print("  Run './gradlew jacocoTestReport' first.", file=sys.stderr)
        sys.exit(1)

    root = tree.getroot()

    # ------------------------------------------------------------------
    # 1. 全项目汇总
    # ------------------------------------------------------------------
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

    # ------------------------------------------------------------------
    # 2. 模块级行覆盖率（按覆盖率升序）
    # ------------------------------------------------------------------
    module_stats = {}
    for pkg in root.findall("package"):
        name = pkg.get("name", "?")
        module = classify_package(name)
        for c in pkg.findall("counter"):
            if c.get("type") == "LINE":
                missed = int(c.get("missed", 0))
                covered = int(c.get("covered", 0))
                total = missed + covered
                if module not in module_stats:
                    module_stats[module] = [0, 0]
                module_stats[module][0] += covered
                module_stats[module][1] += total

    print("\n====== 模块级行覆盖率（按覆盖率升序，低覆盖优先）======")
    sorted_modules = sorted(
        module_stats.items(),
        key=lambda x: (x[1][0] / x[1][1] if x[1][1] > 0 else 0.0),
    )
    for module, (covered, total) in sorted_modules:
        pct = covered / total * 100 if total > 0 else 0.0
        bar = "#" * int(pct / 5)
        print("  %-35s %5.1f%%  (%d/%d)  %s" % (module, pct, covered, total, bar))

    # ------------------------------------------------------------------
    # 3. 低覆盖 Package 热点清单（行数 >= min_lines，按覆盖率升序）
    # ------------------------------------------------------------------
    pkgs = collect_counters(root, "LINE")
    hotspots = [
        (pct, covered, total, name)
        for pct, covered, total, name in pkgs
        if total >= args.hotspot_min_lines
    ]
    hotspots.sort()

    print(
        "\n====== 低覆盖热点（代码行 >= %d，按覆盖率升序，前 %d）======"
        % (args.hotspot_min_lines, args.hotspot_top)
    )
    for pct, covered, total, name in hotspots[: args.hotspot_top]:
        module = classify_package(name)
        print("  %5.1f%%  %5d/%-5d  [%-25s]  %s" % (pct, covered, total, module, name))
    remaining = len(hotspots) - args.hotspot_top
    if remaining > 0:
        print("  ... (还有 %d 个热点未显示，可增大 --hotspot-top 查看)" % remaining)

    # ------------------------------------------------------------------
    # 4. 高覆盖 Package 参考（前 30，覆盖率降序）
    # ------------------------------------------------------------------
    pkgs.sort(reverse=True)
    print("\n====== 各 Package 行覆盖率（前 30，按覆盖率降序） ======")
    for pct, v, total, name in pkgs[:30]:
        print("  %6.2f%%  %5d/%-5d  %s" % (pct, v, total, name))
    if len(pkgs) > 30:
        print("  ... (共 %d 个 package，仅显示前 30)" % len(pkgs))


if __name__ == "__main__":
    main()
