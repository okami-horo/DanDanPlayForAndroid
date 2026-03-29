#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
settings_file="$repo_root/settings.gradle.kts"

if [[ ! -f "$settings_file" ]]; then
    echo "settings.gradle.kts not found at: $settings_file" >&2
    exit 1
fi

mapfile -t module_paths < <(
    grep -o '":[^"]*"' "$settings_file" \
        | tr -d '"' \
        | sed 's/^://; s/:/\//g' \
        | grep -v '^repository/' \
        | awk 'NF && !seen[$0]++'
)

sources=()
tests=()

for module_path in "${module_paths[@]}"; do
    module_dir="$repo_root/$module_path"

    if [[ ! -d "$module_dir" ]]; then
        continue
    fi

    sources+=("$module_path")

    if [[ -d "$module_dir/src/test" ]]; then
        tests+=("$module_path/src/test")
    fi

    if [[ -d "$module_dir/src/androidTest" ]]; then
        tests+=("$module_path/src/androidTest")
    fi
done

if [[ -d "$repo_root/buildSrc/src/main/java" ]]; then
    sources+=("buildSrc/src/main/java")
fi

if [[ -d "$repo_root/scripts/sonarcloud" ]]; then
    sources+=("scripts/sonarcloud")
fi

if [[ -d "$repo_root/scripts/sonarcloud/tests" ]]; then
    tests+=("scripts/sonarcloud/tests")
fi

if [[ ${#sources[@]} -eq 0 ]]; then
    echo "No sonar source directories were resolved." >&2
    exit 1
fi

if [[ ${#tests[@]} -eq 0 ]]; then
    # Keep scanner input valid even for repos without test folders.
    tests=(".")
fi

mapfile -t unique_sources < <(printf '%s\n' "${sources[@]}" | awk 'NF && !seen[$0]++')
mapfile -t unique_tests < <(printf '%s\n' "${tests[@]}" | awk 'NF && !seen[$0]++')

sources_csv="$(IFS=,; echo "${unique_sources[*]}")"
tests_csv="$(IFS=,; echo "${unique_tests[*]}")"

echo "Resolved ${#unique_sources[@]} source directories and ${#unique_tests[@]} test directories."
echo "SONAR_SOURCES=$sources_csv"
echo "SONAR_TESTS=$tests_csv"

if [[ -n "${GITHUB_ENV:-}" ]]; then
    {
        echo "SONAR_SOURCES=$sources_csv"
        echo "SONAR_TESTS=$tests_csv"
    } >> "$GITHUB_ENV"
fi
