#!/usr/bin/env python3
"""Compare baseline and current Sonar quality snapshots with gate thresholds."""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class Thresholds:
    new_coverage: float
    new_duplicated_lines_density: float
    new_security_hotspots_reviewed: float
    vulnerabilities: int


def repo_root() -> Path:
    return Path(__file__).resolve().parents[2]


def default_baseline() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/baseline/quality-baseline.json"


def default_current() -> Path:
    return repo_root() / ".sonarcloud-report/sonarcloud-report.json"


def default_output_json() -> Path:
    return repo_root() / "specs/001-fix-sonarcloud-issues/evidence/baseline/quality-compare.json"


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare baseline and current quality snapshots.",
    )
    parser.add_argument("--baseline", type=Path, default=default_baseline(), help="Baseline snapshot or Sonar report JSON.")
    parser.add_argument("--current", type=Path, default=default_current(), help="Current snapshot or Sonar report JSON.")
    parser.add_argument("--output", type=Path, default=default_output_json(), help="Comparison JSON output path.")
    parser.add_argument("--markdown", type=Path, default=None, help="Optional markdown output path.")
    parser.add_argument("--threshold-new-coverage", type=float, default=80.0)
    parser.add_argument("--threshold-new-dup", type=float, default=3.0)
    parser.add_argument("--threshold-hotspots-reviewed", type=float, default=100.0)
    parser.add_argument("--threshold-vulnerabilities", type=int, default=0)
    parser.add_argument(
        "--fail-on-gate-fail",
        action="store_true",
        help="Exit with code 1 when gateStatus=FAIL.",
    )
    return parser.parse_args(argv)


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def as_list_of_dict(value: Any) -> list[dict[str, Any]]:
    if not isinstance(value, list):
        return []
    return [item for item in value if isinstance(item, dict)]


def to_float(value: Any) -> float | None:
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


def read_json(path: Path) -> dict[str, Any]:
    if not path.exists():
        raise FileNotFoundError(f"JSON file not found: {path}")
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"JSON root must be object: {path}")
    return payload


def file_count_map(top_files: list[dict[str, Any]]) -> dict[str, int]:
    result: dict[str, int] = {}
    for item in top_files:
        path = str(item.get("path") or item.get("component") or "(unknown)")
        count = item.get("count")
        result[path] = int(count) if isinstance(count, (int, float)) else 0
    return result


def normalized_gate_status(raw: Any) -> str:
    if not isinstance(raw, str):
        return "UNKNOWN"
    value = raw.upper()
    if value in {"OK", "PASS"}:
        return "PASS"
    if value in {"ERROR", "FAIL"}:
        return "FAIL"
    return value


def parse_exported_snapshot(payload: dict[str, Any]) -> dict[str, Any]:
    analysis = as_dict(payload.get("analysis"))
    summary = as_dict(payload.get("summary"))
    measures = as_dict(payload.get("measures"))
    quality_gate = as_dict(payload.get("qualityGate"))

    return {
        "analysisKey": str(analysis.get("baselineAnalysisKey") or analysis.get("analysisKey") or ""),
        "branch": str(analysis.get("branch") or ""),
        "gateStatus": normalized_gate_status(quality_gate.get("status")),
        "issuesTotal": int(summary.get("issuesTotal") or 0),
        "highImpactTotal": int(summary.get("highImpactTotal") or 0),
        "vulnerabilitiesTotal": int(summary.get("vulnerabilitiesTotal") or 0),
        "hotspotsTotal": int(summary.get("hotspotsTotal") or 0),
        "newCoverage": to_float(measures.get("new_coverage")),
        "newDuplicatedLinesDensity": to_float(measures.get("new_duplicated_lines_density")),
        "newSecurityHotspotsReviewed": to_float(measures.get("new_security_hotspots_reviewed")),
        "topIssueFiles": as_list_of_dict(payload.get("topIssueFiles")),
        "raw": payload,
    }


def parse_sonar_report(payload: dict[str, Any]) -> dict[str, Any]:
    project = as_dict(payload.get("project"))
    latest_analysis = as_dict(project.get("latestAnalysis"))
    summary = as_dict(payload.get("summary"))
    quality_gate = as_dict(payload.get("qualityGate"))
    measures = as_dict(payload.get("measures"))

    facets = as_dict(summary.get("issuesByFacet"))
    types = as_dict(facets.get("types"))
    impacts = as_dict(facets.get("impactSeverities"))

    def metric(name: str) -> float | None:
        node = as_dict(measures.get(name))
        value = to_float(node.get("value"))
        if value is not None:
            return value

        for condition in as_list_of_dict(quality_gate.get("conditions")):
            if condition.get("metricKey") == name:
                return to_float(condition.get("actualValue"))
        return None

    return {
        "analysisKey": str(latest_analysis.get("key") or ""),
        "branch": str(project.get("branch") or ""),
        "gateStatus": normalized_gate_status(quality_gate.get("status")),
        "issuesTotal": int(summary.get("issuesTotal") or 0),
        "highImpactTotal": int(impacts.get("HIGH") or 0),
        "vulnerabilitiesTotal": int(types.get("VULNERABILITY") or metric("vulnerabilities") or 0),
        "hotspotsTotal": int(summary.get("hotspotsTotal") or 0),
        "newCoverage": metric("new_coverage"),
        "newDuplicatedLinesDensity": metric("new_duplicated_lines_density"),
        "newSecurityHotspotsReviewed": metric("new_security_hotspots_reviewed"),
        "topIssueFiles": as_list_of_dict(summary.get("topIssueFiles")),
        "raw": payload,
    }


def load_snapshot(path: Path) -> dict[str, Any]:
    payload = read_json(path)
    if "meta" in payload and "analysis" in payload and "summary" in payload:
        return parse_exported_snapshot(payload)
    return parse_sonar_report(payload)


def build_top10_delta(baseline: dict[str, Any], current: dict[str, Any], top_n: int = 10) -> list[dict[str, Any]]:
    baseline_counts = file_count_map(as_list_of_dict(baseline.get("topIssueFiles")))
    current_counts = file_count_map(as_list_of_dict(current.get("topIssueFiles")))

    paths = sorted(set(baseline_counts.keys()) | set(current_counts.keys()))
    rows = [
        {
            "path": path,
            "baselineCount": baseline_counts.get(path, 0),
            "currentCount": current_counts.get(path, 0),
            "delta": current_counts.get(path, 0) - baseline_counts.get(path, 0),
        }
        for path in paths
    ]
    rows.sort(key=lambda row: (-abs(int(row["delta"])), -int(row["currentCount"]), row["path"]))
    return rows[:top_n]


def check_thresholds(current: dict[str, Any], thresholds: Thresholds) -> list[dict[str, Any]]:
    checks: list[dict[str, Any]] = []

    coverage = to_float(current.get("newCoverage"))
    checks.append(
        {
            "metric": "new_coverage",
            "actual": coverage,
            "threshold": thresholds.new_coverage,
            "comparator": ">=",
            "pass": coverage is not None and coverage >= thresholds.new_coverage,
        }
    )

    dup = to_float(current.get("newDuplicatedLinesDensity"))
    checks.append(
        {
            "metric": "new_duplicated_lines_density",
            "actual": dup,
            "threshold": thresholds.new_duplicated_lines_density,
            "comparator": "<=",
            "pass": dup is not None and dup <= thresholds.new_duplicated_lines_density,
        }
    )

    hotspots = to_float(current.get("newSecurityHotspotsReviewed"))
    checks.append(
        {
            "metric": "new_security_hotspots_reviewed",
            "actual": hotspots,
            "threshold": thresholds.new_security_hotspots_reviewed,
            "comparator": ">=",
            "pass": hotspots is not None and hotspots >= thresholds.new_security_hotspots_reviewed,
        }
    )

    vulnerabilities = int(current.get("vulnerabilitiesTotal") or 0)
    checks.append(
        {
            "metric": "vulnerabilities",
            "actual": vulnerabilities,
            "threshold": thresholds.vulnerabilities,
            "comparator": "<=",
            "pass": vulnerabilities <= thresholds.vulnerabilities,
        }
    )

    return checks


def build_snapshot_id(baseline_key: str, target_key: str) -> str:
    raw = f"{baseline_key}->{target_key}".encode("utf-8")
    return hashlib.sha256(raw).hexdigest()[:12]


def build_comparison(
    baseline: dict[str, Any],
    current: dict[str, Any],
    thresholds: Thresholds,
) -> dict[str, Any]:
    threshold_checks = check_thresholds(current, thresholds)
    gate_status = "PASS" if all(bool(item.get("pass")) for item in threshold_checks) else "FAIL"

    baseline_key = str(baseline.get("analysisKey") or "")
    current_key = str(current.get("analysisKey") or "")

    top10_delta = build_top10_delta(baseline, current, top_n=10)

    return {
        "snapshotId": build_snapshot_id(baseline_key, current_key),
        "baselineAnalysisKey": baseline_key,
        "targetAnalysisKey": current_key,
        "gateStatus": gate_status,
        "baselineGateStatus": baseline.get("gateStatus"),
        "targetGateStatus": current.get("gateStatus"),
        "totalIssuesBaseline": int(baseline.get("issuesTotal") or 0),
        "totalIssuesCurrent": int(current.get("issuesTotal") or 0),
        "highImpactBaseline": int(baseline.get("highImpactTotal") or 0),
        "highImpactCurrent": int(current.get("highImpactTotal") or 0),
        "vulnerabilitiesBaseline": int(baseline.get("vulnerabilitiesTotal") or 0),
        "vulnerabilitiesCurrent": int(current.get("vulnerabilitiesTotal") or 0),
        "hotspotsReviewedRate": to_float(current.get("newSecurityHotspotsReviewed")),
        "newCoverage": to_float(current.get("newCoverage")),
        "newDuplicatedLinesDensity": to_float(current.get("newDuplicatedLinesDensity")),
        "top10FilesDelta": top10_delta,
        "thresholds": {
            "new_coverage": thresholds.new_coverage,
            "new_duplicated_lines_density": thresholds.new_duplicated_lines_density,
            "new_security_hotspots_reviewed": thresholds.new_security_hotspots_reviewed,
            "vulnerabilities": thresholds.vulnerabilities,
        },
        "thresholdChecks": threshold_checks,
        "meta": {
            "baselineBranch": baseline.get("branch"),
            "targetBranch": current.get("branch"),
            "generatedAt": datetime.now(timezone.utc).isoformat(),
        },
    }


def render_markdown(comparison: dict[str, Any]) -> str:
    lines: list[str] = []
    lines.append("# 质量快照对比")
    lines.append("")
    lines.append(f"- 生成时间：{as_dict(comparison.get('meta')).get('generatedAt', '')}")
    lines.append(f"- 基线分析线：`{comparison.get('baselineAnalysisKey', '')}`")
    lines.append(f"- 当前分析线：`{comparison.get('targetAnalysisKey', '')}`")
    lines.append(f"- 门禁结论：`{comparison.get('gateStatus', '')}`")
    lines.append("")

    lines.append("## 指标对比")
    lines.append("")
    lines.append("| 指标 | Baseline | Current | Delta |")
    lines.append("|------|----------|---------|-------|")

    def row(name: str, baseline_key: str, current_key: str) -> None:
        baseline_value = comparison.get(baseline_key)
        current_value = comparison.get(current_key)
        delta = None
        if isinstance(baseline_value, (int, float)) and isinstance(current_value, (int, float)):
            delta = current_value - baseline_value
        lines.append(f"| {name} | {baseline_value} | {current_value} | {delta} |")

    row("总问题数", "totalIssuesBaseline", "totalIssuesCurrent")
    row("高影响问题", "highImpactBaseline", "highImpactCurrent")
    row("漏洞数", "vulnerabilitiesBaseline", "vulnerabilitiesCurrent")

    lines.append("")
    lines.append("## 阈值检查")
    lines.append("")
    lines.append("| Metric | Comparator | Actual | Threshold | Result |")
    lines.append("|--------|------------|--------|-----------|--------|")

    for item in as_list_of_dict(comparison.get("thresholdChecks")):
        lines.append(
            "| `{metric}` | `{comparator}` | `{actual}` | `{threshold}` | `{result}` |".format(
                metric=item.get("metric") or "",
                comparator=item.get("comparator") or "",
                actual=item.get("actual"),
                threshold=item.get("threshold"),
                result="PASS" if item.get("pass") else "FAIL",
            )
        )

    lines.append("")
    lines.append("## Top10 文件问题变化")
    lines.append("")
    lines.append("| # | File | Baseline | Current | Delta |")
    lines.append("|---|------|----------|---------|-------|")

    top_delta = as_list_of_dict(comparison.get("top10FilesDelta"))
    if top_delta:
        for idx, item in enumerate(top_delta, start=1):
            lines.append(
                "| {idx} | `{path}` | {baseline} | {current} | {delta} |".format(
                    idx=idx,
                    path=item.get("path") or "",
                    baseline=item.get("baselineCount") or 0,
                    current=item.get("currentCount") or 0,
                    delta=item.get("delta") or 0,
                )
            )
    else:
        lines.append("| 1 | `(none)` | 0 | 0 | 0 |")

    lines.append("")
    return "\n".join(lines)


def main(argv: list[str]) -> int:
    args = parse_args(argv)

    thresholds = Thresholds(
        new_coverage=args.threshold_new_coverage,
        new_duplicated_lines_density=args.threshold_new_dup,
        new_security_hotspots_reviewed=args.threshold_hotspots_reviewed,
        vulnerabilities=args.threshold_vulnerabilities,
    )

    try:
        baseline = load_snapshot(args.baseline.resolve())
        current = load_snapshot(args.current.resolve())
        comparison = build_comparison(baseline, current, thresholds)
    except Exception as exc:
        sys.stderr.write(f"[compare_quality_snapshot] failed: {exc}\n")
        return 1

    output_json = args.output.resolve()
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(comparison, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    sys.stdout.write(f"[compare_quality_snapshot] json: {output_json}\n")

    output_md: Path | None = args.markdown.resolve() if args.markdown else output_json.with_suffix(".md")
    if output_md is not None:
        output_md.write_text(render_markdown(comparison), encoding="utf-8")
        sys.stdout.write(f"[compare_quality_snapshot] markdown: {output_md}\n")

    if args.fail_on_gate_fail and comparison.get("gateStatus") != "PASS":
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
