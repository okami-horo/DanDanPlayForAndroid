#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from __future__ import annotations

import argparse
import sys
from pathlib import Path
from typing import Iterable

from audit_docs_lib import (
    MODULE_STATUS_BEGIN,
    MODULE_STATUS_END,
    extract_existing_owner_status,
    module_to_dir,
    module_to_filename,
    parse_module_groups,
    parse_module_id_prefixes,
    parse_settings_modules,
    read_module_status_block_lines,
    read_text,
    repo_root_from_script,
    write_text,
)


def _render_module_status_table(
    modules: Iterable[str],
    module_to_group: dict[str, str],
    group_order: dict[str, int],
    module_to_prefix: dict[str, str],
    existing_owner_status: dict[str, tuple[str, str]],
) -> str:
    rows: list[tuple[int, str, str, str, str, str, str]] = []
    for module in modules:
        group = module_to_group.get(module, "UNASSIGNED")
        order = group_order.get(group, 999)
        prefix = module_to_prefix.get(module, "UNKNOWN")
        owner, status = existing_owner_status.get(module, ("", "Todo"))
        report_path = f"document/code_quality_audit/modules/{module_to_filename(module)}"
        rows.append((order, module, group, prefix, owner, status, report_path))

    rows.sort(key=lambda r: (r[0], r[1]))

    lines: list[str] = []
    lines.append("| 模块 | 分组 | ID 前缀 | Owner | Status | 报告 |")
    lines.append("|---|---|---|---|---|---|")
    for _, module, group, prefix, owner, status, report_path in rows:
        lines.append(f"| {module} | {group} | {prefix} | {owner} | {status} | {report_path} |")
    return "\n".join(lines) + "\n"


def _update_module_status_file(
    path: Path,
    table_markdown: str,
) -> None:
    if not path.exists():
        content = "\n".join(
            [
                "# 模块覆盖率 / 状态跟踪表",
                "",
                "本表用于跟踪“纳入构建范围”的模块排查覆盖率与推进状态，便于分工与复核。",
                "",
                MODULE_STATUS_BEGIN,
                "",
                table_markdown.rstrip(),
                "",
                MODULE_STATUS_END,
                "",
            ]
        )
        write_text(path, content + "\n")
        return

    text = read_text(path)
    if MODULE_STATUS_BEGIN not in text or MODULE_STATUS_END not in text:
        raise ValueError(
            f"module_status.md missing markers: {MODULE_STATUS_BEGIN} / {MODULE_STATUS_END}"
        )

    before, rest = text.split(MODULE_STATUS_BEGIN, 1)
    _, after = rest.split(MODULE_STATUS_END, 1)
    new_block = "\n" + table_markdown.rstrip() + "\n"
    new_text = before + MODULE_STATUS_BEGIN + new_block + MODULE_STATUS_END + after
    write_text(path, new_text)


def _ensure_module_reports(
    template_path: Path,
    modules_dir: Path,
    modules: Iterable[str],
    module_to_prefix: dict[str, str],
) -> tuple[int, int]:
    """
    Returns: (created_count, skipped_existing_count)
    """
    template = read_text(template_path)
    created = 0
    skipped = 0
    for module in modules:
        out_path = modules_dir / module_to_filename(module)
        if out_path.exists():
            skipped += 1
            continue

        prefix = module_to_prefix.get(module, "UNKNOWN")
        content = (
            template.replace("<modulePath>", module)
            .replace("<owner>", "")
            .replace("<moduleDir 或关键包名>", module_to_dir(module))
            .replace("<PREFIX>", prefix)
        )
        write_text(out_path, content.rstrip() + "\n")
        created += 1
    return created, skipped


def _validate_modules_covered(
    modules: list[str],
    module_to_group: dict[str, str],
    module_to_prefix: dict[str, str],
) -> None:
    missing_group = [m for m in modules if m not in module_to_group]
    missing_prefix = [m for m in modules if m not in module_to_prefix]

    if missing_group or missing_prefix:
        parts: list[str] = []
        if missing_group:
            parts.append("Missing group for: " + ", ".join(missing_group))
        if missing_prefix:
            parts.append("Missing prefix for: " + ", ".join(missing_prefix))
        raise ValueError("; ".join(parts))


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Init code quality audit docs.")
    parser.add_argument(
        "--repo-root",
        type=str,
        default="",
        help="Repository root path (auto-detected if omitted).",
    )
    args = parser.parse_args(argv)

    repo_root = Path(args.repo_root).resolve() if args.repo_root else repo_root_from_script(Path(__file__))
    settings_gradle = repo_root / "settings.gradle.kts"

    config_groups = repo_root / "document" / "code_quality_audit" / "config" / "module_groups.yaml"
    config_prefixes = repo_root / "document" / "code_quality_audit" / "config" / "module_id_prefixes.yaml"
    template_module_report = (
        repo_root / "document" / "code_quality_audit" / "templates" / "module_report.md"
    )

    modules_dir = repo_root / "document" / "code_quality_audit" / "modules"
    global_dir = repo_root / "document" / "code_quality_audit" / "global"
    module_status_path = global_dir / "module_status.md"

    modules = parse_settings_modules(settings_gradle)
    if not modules:
        print(f"ERROR: no modules found in {settings_gradle}", file=sys.stderr)
        return 1

    groups = parse_module_groups(config_groups)
    module_to_group: dict[str, str] = {}
    group_order: dict[str, int] = {}
    for group in groups:
        group_order[group.name] = group.order
        for module in group.modules:
            if module in module_to_group:
                raise ValueError(f"Duplicate module in groups config: {module}")
            module_to_group[module] = group.name

    module_to_prefix = parse_module_id_prefixes(config_prefixes)
    _validate_modules_covered(modules, module_to_group, module_to_prefix)

    existing_block_lines = read_module_status_block_lines(module_status_path)
    existing_owner_status = extract_existing_owner_status(existing_block_lines)

    created_reports, skipped_reports = _ensure_module_reports(
        template_path=template_module_report,
        modules_dir=modules_dir,
        modules=modules,
        module_to_prefix=module_to_prefix,
    )

    table = _render_module_status_table(
        modules=modules,
        module_to_group=module_to_group,
        group_order=group_order,
        module_to_prefix=module_to_prefix,
        existing_owner_status=existing_owner_status,
    )
    _update_module_status_file(module_status_path, table)

    print("Init audit docs done.")
    print(f"- Repo root: {repo_root}")
    print(f"- Modules (from settings.gradle.kts): {len(modules)}")
    print(f"- Module reports: created {created_reports}, existing {skipped_reports}")
    print(f"- Module status updated: {module_status_path.relative_to(repo_root)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
