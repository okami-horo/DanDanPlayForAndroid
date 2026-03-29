#!/usr/bin/env python3
"""Export SonarCloud baseline snapshot + high-risk catalog for remediation tracking."""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class SeverityRank:
    order: dict[str, int]

    def value(self, raw: Any, default: int = 99) -> int:
        if not isinstance(raw, str):
            return default
        return self.order.get(raw.upper(), default)


ISSUE_SEVERITY_RANK = SeverityRank(
    {
        "BLOCKER": 0,
        "CRITICAL": 1,
        "MAJOR": 2,
        "MINOR": 3,
        "INFO": 4,
    }
)

HOTSPOT_PROBABILITY_RANK = SeverityRank(
    {
        "HIGH": 0,
        "MEDIUM": 1,
        "LOW": 2,
    }
)


DEFAULT_OUTPUT_SUBDIR = Path("specs/001-fix-sonarcloud-issues/evidence/baseline")


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def default_report() -> Path:
    return repo_root() / ".sonarcloud-report" / "sonarcloud-report.json"


def default_output_dir() -> Path:
    return repo_root() / DEFAULT_OUTPUT_SUBDIR


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Export baseline quality snapshot and high-risk lists.",
    )
    parser.add_argument(
        "--report",
        type=Path,
        default=default_report(),
        help="Path to SonarCloud consolidated report JSON.",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=default_output_dir(),
        help="Directory for quality-baseline.json / quality-baseline.md.",
    )
    parser.add_argument(
        "--analysis-key",
        type=str,
        default="",
        help="Optional baseline analysis key override.",
    )
    parser.add_argument(
        "--top-files-limit",
        type=int,
        default=10,
        help="Top files count in output (default: 10).",
    )
    parser.add_argument(
        "--high-risk-limit",
        type=int,
        default=30,
        help="Max rows per high-risk section in markdown output (default: 30).",
    )
    parser.add_argument(
        "--write-json-only",
        action="store_true",
        help="Only write quality-baseline.json.",
    )
    return parser.parse_args(argv)


def read_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"Report JSON not found: {path}")

    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError("Report root must be a JSON object")
    return payload


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list_of_dict(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        return []
    return [item for item in value if isinstance(item, dict)]


def to_number(value: Any) -> float | None:
    if value is None:
        return None
    if isinstance(value, (int, float)):
        return float(value)
    if isinstance(value, str):
        try:
            return float(value)
        except ValueError:
            return None
    return None


def file_path_of(item: dict[str, Any]) -> str:
    path = item.get("path")
    if isinstance(path, str) and path:
        return path

    component = item.get("component")
    if isinstance(component, str) and component:
        return component.split(":", 1)[-1]

    return "(unknown)"


def extract_top_issue_files(
    summary: dict[str, Any],
    issues: list[dict[str, Any]],
    limit: int,
) -> list[dict[str, Any]]:
    raw_top = summary.get("topIssueFiles")
    if isinstance(raw_top, list):
        normalized: list[dict[str, Any]] = []
        for raw in raw_top:
            if not isinstance(raw, dict):
                continue
            count = raw.get("count")
            normalized.append(
                {
                    "path": str(raw.get("path") or raw.get("component") or "(unknown)"),
                    "count": int(count) if isinstance(count, (int, float)) else 0,
                }
            )
        if normalized:
            return normalized[:limit]

    counter: Counter[str] = Counter(file_path_of(issue) for issue in issues)
    return [{"path": path, "count": count} for path, count in counter.most_common(limit)]


def normalize_issue(issue: dict[str, Any]) -> dict[str, Any]:
    return {
        "key": issue.get("key") or "",
        "ruleKey": issue.get("ruleKey") or "",
        "type": issue.get("type") or "",
        "severity": issue.get("severity") or "",
        "impactSeverity": issue.get("impactSeverity") or "",
        "status": issue.get("status") or "",
        "path": file_path_of(issue),
        "line": issue.get("line"),
        "message": issue.get("message") or "",
    }


def normalize_hotspot(hotspot: dict[str, Any]) -> dict[str, Any]:
    return {
        "key": hotspot.get("key") or "",
        "ruleKey": hotspot.get("ruleKey") or "",
        "status": hotspot.get("status") or "",
        "vulnerabilityProbability": hotspot.get("vulnerabilityProbability") or "",
        "path": file_path_of(hotspot),
        "line": hotspot.get("line"),
        "message": hotspot.get("message") or "",
    }


def sort_issues(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return sorted(
        items,
        key=lambda item: (
            ISSUE_SEVERITY_RANK.value(item.get("severity")),
            ISSUE_SEVERITY_RANK.value(item.get("impactSeverity")),
            str(item.get("path") or ""),
            int(item.get("line") or 0),
        ),
    )


def sort_hotspots(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    return sorted(
        items,
        key=lambda item: (
            HOTSPOT_PROBABILITY_RANK.value(item.get("vulnerabilityProbability")),
            str(item.get("path") or ""),
            int(item.get("line") or 0),
        ),
    )


def extract_measures(report: dict[str, Any]) -> dict[str, float | None]:
    measures = as_dict(report.get("measures"))

    def quality_gate_metric(name: str) -> float | None:
        quality_gate = as_dict(report.get("qualityGate"))
        for condition in as_list_of_dict(quality_gate.get("conditions")):
            if condition.get("metricKey") == name:
                return to_number(condition.get("actualValue"))
        return None

    def metric(name: str) -> float | None:
        node = as_dict(measures.get(name))
        value = to_number(node.get("value"))
        return value if value is not None else quality_gate_metric(name)

    return {
        "new_coverage": metric("new_coverage"),
        "new_duplicated_lines_density": metric("new_duplicated_lines_density"),
        "new_security_hotspots_reviewed": metric("new_security_hotspots_reviewed"),
        "vulnerabilities": metric("vulnerabilities"),
        "new_vulnerabilities": metric("new_vulnerabilities"),
    }


def extract_quality_gate(report: dict[str, Any]) -> dict[str, Any]:
    quality_gate = as_dict(report.get("qualityGate"))
    conditions = as_list_of_dict(quality_gate.get("conditions"))

    normalized_conditions = []
    for condition in conditions:
        normalized_conditions.append(
            {
                "metricKey": condition.get("metricKey") or "",
                "status": condition.get("status") or "",
                "actualValue": condition.get("actualValue"),
                "threshold": condition.get("errorThreshold"),
                "comparator": condition.get("comparator"),
            }
        )

    return {
        "status": quality_gate.get("status") or "UNKNOWN",
        "conditions": normalized_conditions,
    }


def select_high_risk(
    issues: list[dict[str, Any]],
    hotspots: list[dict[str, Any]],
) -> dict[str, list[dict[str, Any]]]:
    normalized_issues = [normalize_issue(issue) for issue in issues]
    normalized_hotspots = [normalize_hotspot(hotspot) for hotspot in hotspots]

    vulnerabilities = [item for item in normalized_issues if item.get("type") == "VULNERABILITY"]
    high_impact = [item for item in normalized_issues if item.get("impactSeverity") == "HIGH"]
    high_probability_hotspots = [
        item
        for item in normalized_hotspots
        if str(item.get("vulnerabilityProbability", "")).upper() == "HIGH"
    ]

    return {
        "vulnerabilityIssues": sort_issues(vulnerabilities),
        "highImpactIssues": sort_issues(high_impact),
        "highRiskHotspots": sort_hotspots(high_probability_hotspots),
    }


def derive_analysis(report: dict[str, Any], override_analysis_key: str) -> dict[str, str]:
    project = as_dict(report.get("project"))
    latest_analysis = as_dict(project.get("latestAnalysis"))

    analysis_key = override_analysis_key.strip() or str(latest_analysis.get("key") or "")
    return {
        "projectKey": str(project.get("key") or ""),
        "organization": str(project.get("organization") or ""),
        "branch": str(project.get("branch") or ""),
        "pullRequest": str(project.get("pullRequest") or ""),
        "baselineAnalysisKey": analysis_key,
    }


def build_snapshot(report: dict[str, Any], args: argparse.Namespace) -> dict[str, Any]:
    summary = as_dict(report.get("summary"))
    issues = as_list_of_dict(report.get("issues"))
    hotspots = as_list_of_dict(report.get("hotspots"))

    issues_total = int(summary.get("issuesTotal") or len(issues))
    issues_fetched = int(summary.get("issuesFetched") or len(issues))
    hotspots_total = int(summary.get("hotspotsTotal") or len(hotspots))
    hotspots_fetched = int(summary.get("hotspotsFetched") or len(hotspots))

    facets = as_dict(summary.get("issuesByFacet"))
    types = as_dict(facets.get("types"))
    impact = as_dict(facets.get("impactSeverities"))

    high_risk = select_high_risk(issues, hotspots)

    snapshot = {
        "meta": {
            "exportedAt": datetime.now(timezone.utc).isoformat(),
            "reportPath": str(args.report.resolve()),
            "reportGeneratedAt": report.get("generatedAt") or "",
            "sampling": {
                "issuesTotal": issues_total,
                "issuesFetched": issues_fetched,
                "issuesTruncated": issues_fetched < issues_total,
                "hotspotsTotal": hotspots_total,
                "hotspotsFetched": hotspots_fetched,
                "hotspotsTruncated": hotspots_fetched < hotspots_total,
            },
        },
        "analysis": derive_analysis(report, args.analysis_key),
        "qualityGate": extract_quality_gate(report),
        "measures": extract_measures(report),
        "summary": {
            "issuesTotal": issues_total,
            "hotspotsTotal": hotspots_total,
            "vulnerabilitiesTotal": int(types.get("VULNERABILITY") or 0),
            "highImpactTotal": int(impact.get("HIGH") or 0),
            "codeSmellsTotal": int(types.get("CODE_SMELL") or 0),
        },
        "topIssueFiles": extract_top_issue_files(summary, issues, max(1, args.top_files_limit)),
        "highRisk": high_risk,
    }
    return snapshot


def markdown_issue_table(items: list[dict[str, Any]], limit: int) -> list[str]:
    lines = ["| # | Key | Rule | Severity | Path:Line |", "|---|-----|------|----------|-----------|"]
    if not items:
        lines.append("| 1 | (none) | - | - | - |")
        return lines

    for idx, item in enumerate(items[:limit], start=1):
        path = str(item.get("path") or "(unknown)")
        line = item.get("line")
        path_line = f"{path}:{line}" if line else path
        lines.append(
            "| {idx} | `{key}` | `{rule}` | {severity} | `{path_line}` |".format(
                idx=idx,
                key=item.get("key") or "",
                rule=item.get("ruleKey") or "",
                severity=item.get("impactSeverity") or item.get("severity") or "",
                path_line=path_line,
            )
        )
    return lines


def markdown_hotspot_table(items: list[dict[str, Any]], limit: int) -> list[str]:
    lines = ["| # | Key | Rule | Probability | Status | Path:Line |", "|---|-----|------|-------------|--------|-----------|"]
    if not items:
        lines.append("| 1 | (none) | - | - | - | - |")
        return lines

    for idx, item in enumerate(items[:limit], start=1):
        path = str(item.get("path") or "(unknown)")
        line = item.get("line")
        path_line = f"{path}:{line}" if line else path
        lines.append(
            "| {idx} | `{key}` | `{rule}` | {probability} | {status} | `{path_line}` |".format(
                idx=idx,
                key=item.get("key") or "",
                rule=item.get("ruleKey") or "",
                probability=item.get("vulnerabilityProbability") or "",
                status=item.get("status") or "",
                path_line=path_line,
            )
        )
    return lines


def render_markdown(snapshot: dict[str, Any], high_risk_limit: int) -> str:
    meta = as_dict(snapshot.get("meta"))
    analysis = as_dict(snapshot.get("analysis"))
    quality_gate = as_dict(snapshot.get("qualityGate"))
    measures = as_dict(snapshot.get("measures"))
    summary = as_dict(snapshot.get("summary"))
    sampling = as_dict(meta.get("sampling"))
    high_risk = as_dict(snapshot.get("highRisk"))

    lines: list[str] = []
    lines.append("# 质量基线快照（Phase 2）")
    lines.append("")
    lines.append(f"- 导出时间：{meta.get('exportedAt', '')}")
    lines.append(f"- 报告生成时间：{meta.get('reportGeneratedAt', '')}")
    lines.append(f"- 项目：`{analysis.get('projectKey', '')}`")
    lines.append(f"- 分支：`{analysis.get('branch', '')}`")
    lines.append(f"- 基线分析线：`{analysis.get('baselineAnalysisKey', '')}`")
    lines.append("")
    lines.append("## 数据完整性")
    lines.append("")
    lines.append(f"- Issues：{sampling.get('issuesFetched', 0)} / {sampling.get('issuesTotal', 0)}")
    lines.append(f"- Hotspots：{sampling.get('hotspotsFetched', 0)} / {sampling.get('hotspotsTotal', 0)}")
    lines.append(f"- Issues 是否截断：{sampling.get('issuesTruncated', False)}")
    lines.append(f"- Hotspots 是否截断：{sampling.get('hotspotsTruncated', False)}")
    lines.append("")
    lines.append("## 关键指标概览")
    lines.append("")
    lines.append(f"- Quality Gate：`{quality_gate.get('status', 'UNKNOWN')}`")
    lines.append(f"- 总问题数：{summary.get('issuesTotal', 0)}")
    lines.append(f"- 漏洞总数：{summary.get('vulnerabilitiesTotal', 0)}")
    lines.append(f"- 高影响问题总数：{summary.get('highImpactTotal', 0)}")
    lines.append(f"- Hotspot 总数：{summary.get('hotspotsTotal', 0)}")
    lines.append(f"- new_coverage：{measures.get('new_coverage')}")
    lines.append(f"- new_duplicated_lines_density：{measures.get('new_duplicated_lines_density')}")
    lines.append(f"- new_security_hotspots_reviewed：{measures.get('new_security_hotspots_reviewed')}")
    lines.append("")
    lines.append("## 质量门条件")
    lines.append("")
    lines.append("| Metric | Status | Actual | Threshold |")
    lines.append("|--------|--------|--------|-----------|")
    conditions = as_list_of_dict(quality_gate.get("conditions"))
    if conditions:
        for condition in conditions:
            lines.append(
                "| `{metric}` | `{status}` | `{actual}` | `{threshold}` |".format(
                    metric=condition.get("metricKey") or "",
                    status=condition.get("status") or "",
                    actual=condition.get("actualValue") or "",
                    threshold=condition.get("threshold") or "",
                )
            )
    else:
        lines.append("| `(none)` | - | - | - |")

    lines.append("")
    lines.append("## Top 问题文件")
    lines.append("")
    lines.append("| Rank | File | Issues |")
    lines.append("|------|------|--------|")

    top_files = as_list_of_dict(snapshot.get("topIssueFiles"))
    if top_files:
        for idx, item in enumerate(top_files, start=1):
            lines.append(
                "| {idx} | `{path}` | {count} |".format(
                    idx=idx,
                    path=item.get("path") or "(unknown)",
                    count=item.get("count") or 0,
                )
            )
    else:
        lines.append("| 1 | `(none)` | 0 |")

    lines.append("")
    lines.append("## 高风险清单")
    lines.append("")

    vulnerabilities = as_list_of_dict(high_risk.get("vulnerabilityIssues"))
    lines.append(f"### 漏洞问题（{len(vulnerabilities)}）")
    lines.append("")
    lines.extend(markdown_issue_table(vulnerabilities, high_risk_limit))
    lines.append("")

    high_impact = as_list_of_dict(high_risk.get("highImpactIssues"))
    lines.append(f"### 高影响问题（抽样 {len(high_impact)}）")
    lines.append("")
    lines.extend(markdown_issue_table(high_impact, high_risk_limit))
    lines.append("")

    hotspots = as_list_of_dict(high_risk.get("highRiskHotspots"))
    lines.append(f"### 高风险热点（{len(hotspots)}）")
    lines.append("")
    lines.extend(markdown_hotspot_table(hotspots, high_risk_limit))
    lines.append("")

    lines.append("> 注：若 `issuesFetched < issuesTotal`，高影响问题清单仅代表已抓取样本，最终验收以分析线完整数据为准。")
    lines.append("")
    return "\n".join(lines)


def write_outputs(snapshot: dict[str, Any], output_dir: Path, write_json_only: bool, high_risk_limit: int) -> tuple[Path, Path | None]:
    output_dir.mkdir(parents=True, exist_ok=True)

    json_path = output_dir / "quality-baseline.json"
    md_path = output_dir / "quality-baseline.md"

    json_path.write_text(json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if write_json_only:
        return json_path, None

    md_path.write_text(render_markdown(snapshot, high_risk_limit), encoding="utf-8")
    return json_path, md_path


def main(argv: list[str]) -> int:
    args = parse_args(argv)

    if args.top_files_limit < 1:
        sys.stderr.write("[export_baseline] --top-files-limit must be >= 1\n")
        return 2
    if args.high_risk_limit < 1:
        sys.stderr.write("[export_baseline] --high-risk-limit must be >= 1\n")
        return 2

    try:
        report = read_json(args.report.resolve())
        snapshot = build_snapshot(report, args)
        json_path, md_path = write_outputs(
            snapshot,
            args.output_dir.resolve(),
            args.write_json_only,
            args.high_risk_limit,
        )
    except Exception as exc:
        sys.stderr.write(f"[export_baseline] failed: {exc}\n")
        return 1

    sys.stdout.write(f"[export_baseline] json: {json_path}\n")
    if md_path is not None:
        sys.stdout.write(f"[export_baseline] markdown: {md_path}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
