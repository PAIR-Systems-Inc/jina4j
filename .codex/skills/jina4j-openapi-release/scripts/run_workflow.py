#!/usr/bin/env python3
"""Run the Jina4j OpenAPI refresh, regeneration, validation, and optional release workflow."""

from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import subprocess
import sys
import time
from collections import deque
from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, List, Optional, Sequence

HTTP_METHODS = ("get", "put", "post", "delete", "options", "head", "patch", "trace")


def run_cmd(
    cmd: Sequence[str],
    cwd: Path,
    env: Optional[Dict[str, str]] = None,
    check: bool = True,
    highlights: Optional[Sequence[str]] = None,
    tail_size: int = 140,
) -> Dict[str, object]:
    """Run a command, stream output, keep tail, and collect highlight lines."""
    printable = " ".join(shlex.quote(part) for part in cmd)
    print(f"\n$ {printable}")

    start = time.monotonic()
    tail = deque(maxlen=tail_size)
    found: List[str] = []

    compiled = [re.compile(pattern) for pattern in (highlights or [])]

    process = subprocess.Popen(
        list(cmd),
        cwd=str(cwd),
        env=env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
    )

    assert process.stdout is not None
    for line in process.stdout:
        sys.stdout.write(line)
        stripped = line.rstrip("\n")
        tail.append(stripped)
        for pattern in compiled:
            if pattern.search(stripped):
                found.append(stripped)
                break

    exit_code = process.wait()
    duration = round(time.monotonic() - start, 2)
    result = {
        "command": printable,
        "exit_code": exit_code,
        "duration": duration,
        "tail": list(tail),
        "highlights": found,
    }

    if check and exit_code != 0:
        tail_text = "\n".join(result["tail"])
        raise RuntimeError(
            f"Command failed (exit {exit_code}): {printable}\n"
            f"--- command tail ---\n{tail_text}"
        )

    return result


def capture_cmd(cmd: Sequence[str], cwd: Path) -> str:
    result = subprocess.run(list(cmd), cwd=str(cwd), check=True, capture_output=True, text=True)
    return result.stdout.strip()


def extract_version(spec: Dict[str, object]) -> str:
    info = spec.get("info", {}) if isinstance(spec, dict) else {}
    if isinstance(info, dict):
        version = info.get("version")
        if isinstance(version, str):
            return version
    return "unknown"


def load_json(path: Path) -> Dict[str, object]:
    return json.loads(path.read_text(encoding="utf-8"))


def save_json(path: Path, content: Dict[str, object]) -> None:
    path.write_text(json.dumps(content, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def sanitize_path(path: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9]+", "_", path).strip("_")
    return cleaned or "root"


def dedupe_operation_ids(spec: Dict[str, object]) -> List[Dict[str, str]]:
    renames: List[Dict[str, str]] = []
    seen: set[str] = set()

    paths = spec.get("paths")
    if not isinstance(paths, dict):
        return renames

    for path, path_item in paths.items():
        if not isinstance(path_item, dict):
            continue
        for method in HTTP_METHODS:
            operation = path_item.get(method)
            if not isinstance(operation, dict):
                continue
            operation_id = operation.get("operationId")
            if not isinstance(operation_id, str) or not operation_id:
                continue

            if operation_id not in seen:
                seen.add(operation_id)
                continue

            preferred = re.sub(
                r"_(get|put|post|delete|options|head|patch|trace)$",
                f"_{method}",
                operation_id,
            )
            if preferred != operation_id and preferred not in seen:
                candidate = preferred
            else:
                base = f"{operation_id}_{method}_{sanitize_path(path)}"
                candidate = base
                suffix = 2
                while candidate in seen:
                    candidate = f"{base}_{suffix}"
                    suffix += 1

            operation["operationId"] = candidate
            seen.add(candidate)
            renames.append(
                {
                    "path": path,
                    "method": method.upper(),
                    "old": operation_id,
                    "new": candidate,
                }
            )

    return renames


def parse_project_version(build_gradle_path: Path) -> str:
    content = build_gradle_path.read_text(encoding="utf-8")
    match = re.search(r'^version\s*=\s*"([^"]+)"', content, flags=re.MULTILINE)
    if not match:
        raise RuntimeError(f"Could not find project version in {build_gradle_path}")
    return match.group(1)


def update_project_version(build_gradle_path: Path, new_version: str) -> bool:
    content = build_gradle_path.read_text(encoding="utf-8")
    updated = re.sub(
        r'^version\s*=\s*"([^"]+)"',
        f'version = "{new_version}"',
        content,
        count=1,
        flags=re.MULTILINE,
    )
    if updated == content:
        return False
    build_gradle_path.write_text(updated, encoding="utf-8")
    return True


def bump_patch(version: str) -> str:
    match = re.fullmatch(r"v?(\d+)\.(\d+)\.(\d+)", version)
    if not match:
        raise RuntimeError(
            f"Cannot auto-bump version '{version}'. Use --tag with an explicit semantic version like v0.0.4"
        )
    major = int(match.group(1))
    minor = int(match.group(2))
    patch = int(match.group(3)) + 1
    return f"v{major}.{minor}.{patch}"


def format_step_summary(step: Dict[str, object]) -> str:
    status = "PASS" if int(step["exit_code"]) == 0 else "FAIL"
    return f"- `{step['command']}` -> {status} ({step['duration']}s)"


def format_highlights(title: str, lines: Sequence[str]) -> List[str]:
    rendered = [f"- {title}:"]
    if not lines:
        rendered.append("  - (no matching highlight lines captured)")
        return rendered
    for line in lines:
        rendered.append(f"  - {line}")
    return rendered


def build_release_notes(tag: str, old_version: str, new_version: str, renames: Sequence[Dict[str, str]]) -> str:
    lines = [
        f"- Refreshed OpenAPI from https://api.jina.ai/openapi.json ({old_version} -> {new_version})",
        "- Regenerated client code and applied post-generation fixes via `fixGeneratedCode`",
        "- Ran clean build and embedding/multi-vector/reranking example validations",
    ]
    if renames:
        lines.append(f"- Auto-resolved {len(renames)} duplicate operationId entries before generation")
    if tag:
        lines.append(f"- Version/tag: {tag}")
    return "\n".join(lines)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Refresh Jina4j OpenAPI, regenerate, validate examples, and optionally release.",
    )
    parser.add_argument("--repo-root", default=".", help="Path to the jina4j repository root")
    parser.add_argument("--api-url", default="https://api.jina.ai/openapi.json", help="OpenAPI source URL")
    parser.add_argument(
        "--api-key",
        default=os.getenv("JINA_API_KEY", ""),
        help="Jina API key for running example tasks (defaults to JINA_API_KEY env var)",
    )
    parser.add_argument("--skip-examples", action="store_true", help="Skip running embedding/multi-vector/reranking examples")
    parser.add_argument("--commit", action="store_true", help="Stage and commit all changes")
    parser.add_argument("--push", action="store_true", help="Push current branch to origin (requires --commit)")
    parser.add_argument("--release", action="store_true", help="Create GitHub release/tag via gh (implies --commit and --push)")
    parser.add_argument("--tag", help="Release tag/version to use (e.g. v0.0.4)")
    parser.add_argument("--commit-message", help="Custom commit message")
    parser.add_argument("--summary-file", help="Optional markdown output file for the final summary")
    return parser.parse_args()


def main() -> int:
    args = parse_args()

    if args.release:
        args.commit = True
        args.push = True

    if args.push and not args.commit:
        raise RuntimeError("--push requires --commit")

    if args.tag and not re.fullmatch(r"v?\d+\.\d+\.\d+", args.tag):
        raise RuntimeError("--tag must be semantic version format, for example v0.0.4")

    repo = Path(args.repo_root).resolve()
    build_gradle = repo / "build.gradle.kts"
    openapi_dir = repo / "openapi"
    tracked_spec = openapi_dir / "jina-openapi.json"
    orphan_spec = openapi_dir / "openapi.json"

    for path in (build_gradle, tracked_spec):
        if not path.exists():
            raise RuntimeError(f"Required file not found: {path}")

    start_time = datetime.now(timezone.utc)
    steps: List[Dict[str, object]] = []
    example_summaries: Dict[str, List[str]] = {}
    release_url = ""
    version_updated = False
    created_tag = ""

    old_spec_version = extract_version(load_json(tracked_spec))
    steps.append(run_cmd(["curl", "-fsSL", args.api_url, "-o", str(tracked_spec)], cwd=repo))

    fetched_spec = load_json(tracked_spec)
    new_spec_version = extract_version(fetched_spec)
    renames = dedupe_operation_ids(fetched_spec)
    save_json(tracked_spec, fetched_spec)
    if orphan_spec.exists():
        orphan_spec.unlink()

    steps.append(run_cmd(["./gradlew", "clean", "--console=plain"], cwd=repo))
    steps.append(
        run_cmd(
            ["./gradlew", ":openApiGenerate", ":fixGeneratedCode", "--rerun-tasks", "--console=plain"],
            cwd=repo,
        )
    )
    steps.append(run_cmd(["./gradlew", "build", "--console=plain"], cwd=repo))

    if not args.skip_examples:
        if not args.api_key:
            raise RuntimeError(
                "Examples require API key. Provide --api-key or set JINA_API_KEY, or pass --skip-examples."
            )
        env = dict(os.environ)
        env["JINA_API_KEY"] = args.api_key

        embedding = run_cmd(
            ["./gradlew", ":examples:runEmbeddingExample", "--console=plain"],
            cwd=repo,
            env=env,
            highlights=(
                r"Model used:",
                r"Total tokens:",
                r"Number of embedding results:",
            ),
        )
        steps.append(embedding)
        example_summaries["Embedding"] = embedding["highlights"]  # type: ignore[index]

        multivector = run_cmd(
            ["./gradlew", ":examples:runMultiVectorExample", "--console=plain"],
            cwd=repo,
            env=env,
            highlights=(
                r"Model used:",
                r"Usage:",
            ),
        )
        steps.append(multivector)
        example_summaries["MultiVector"] = multivector["highlights"]  # type: ignore[index]

        rerank = run_cmd(
            ["./gradlew", ":examples:runRerankingExample", "--console=plain"],
            cwd=repo,
            env=env,
            highlights=(
                r"^Model:",
                r"^Usage:",
                r"^Index=",
            ),
        )
        steps.append(rerank)
        example_summaries["Reranking"] = rerank["highlights"]  # type: ignore[index]

    if args.commit:
        current_project_version = parse_project_version(build_gradle)
        target_tag = args.tag or ""

        if args.release and not target_tag:
            target_tag = bump_patch(current_project_version)

        if target_tag and current_project_version != target_tag:
            version_updated = update_project_version(build_gradle, target_tag)
            current_project_version = target_tag

        steps.append(run_cmd(["git", "add", "-A"], cwd=repo))

        commit_message = args.commit_message
        if not commit_message:
            if target_tag:
                commit_message = f"Regenerate client for latest Jina OpenAPI and bump to {target_tag}"
            else:
                commit_message = "Regenerate client for latest Jina OpenAPI"

        steps.append(run_cmd(["git", "commit", "-m", commit_message], cwd=repo))

        if args.push:
            branch = capture_cmd(["git", "branch", "--show-current"], cwd=repo)
            steps.append(run_cmd(["git", "push", "origin", branch], cwd=repo))

        if args.release:
            created_tag = target_tag
            notes = build_release_notes(created_tag, old_spec_version, new_spec_version, renames)
            release_step = run_cmd(
                [
                    "gh",
                    "release",
                    "create",
                    created_tag,
                    "--title",
                    f"jina4j {created_tag}",
                    "--notes",
                    notes,
                ],
                cwd=repo,
            )
            steps.append(release_step)
            release_lines = [line for line in release_step["tail"] if line.startswith("https://")]  # type: ignore[index]
            if release_lines:
                release_url = release_lines[-1]

    end_time = datetime.now(timezone.utc)
    git_status = capture_cmd(["git", "status", "--short", "--branch"], cwd=repo)

    summary_lines: List[str] = []
    summary_lines.append("## Jina4j OpenAPI Workflow Summary")
    summary_lines.append(f"- Started (UTC): {start_time.isoformat()}")
    summary_lines.append(f"- Finished (UTC): {end_time.isoformat()}")
    summary_lines.append(f"- Repository: {repo}")
    summary_lines.append(f"- OpenAPI source: {args.api_url}")
    summary_lines.append("- Updated file: `openapi/jina-openapi.json`")
    summary_lines.append(f"- Spec version: `{old_spec_version}` -> `{new_spec_version}`")

    if renames:
        summary_lines.append(f"- Duplicate operationId fixes: {len(renames)}")
        for rename in renames:
            summary_lines.append(
                f"  - `{rename['method']} {rename['path']}`: `{rename['old']}` -> `{rename['new']}`"
            )
    else:
        summary_lines.append("- Duplicate operationId fixes: none")

    summary_lines.append("\n### Command Results")
    summary_lines.extend(format_step_summary(step) for step in steps)

    if example_summaries:
        summary_lines.append("\n### Example Highlights")
        for name in ("Embedding", "MultiVector", "Reranking"):
            if name in example_summaries:
                summary_lines.extend(format_highlights(name, example_summaries[name]))

    if args.commit:
        head = capture_cmd(["git", "rev-parse", "HEAD"], cwd=repo)
        summary_lines.append("\n### Git/Release")
        summary_lines.append(f"- Commit: `{head}`")
        summary_lines.append(f"- Version file updated: {'yes' if version_updated else 'no'}")
        if created_tag:
            summary_lines.append(f"- Tag: `{created_tag}`")
        if release_url:
            summary_lines.append(f"- Release URL: {release_url}")

    summary_lines.append("\n### Working Tree")
    if git_status:
        summary_lines.append("```text")
        summary_lines.append(git_status)
        summary_lines.append("```")
    else:
        summary_lines.append("- clean")

    summary = "\n".join(summary_lines)
    print("\n" + summary)

    if args.summary_file:
        summary_path = Path(args.summary_file).resolve()
        summary_path.parent.mkdir(parents=True, exist_ok=True)
        summary_path.write_text(summary + "\n", encoding="utf-8")
        print(f"\nWrote summary to {summary_path}")

    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"\nERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
