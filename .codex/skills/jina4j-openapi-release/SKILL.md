---
name: jina4j-openapi-release
description: Refresh the Jina4j Java client against the latest Jina API OpenAPI schema and optionally publish a release. Use when asked to fetch https://api.jina.ai/openapi.json directly into openapi/jina-openapi.json, regenerate via Gradle, run embedding/multi-vector/reranking examples with a user-provided API key, and optionally commit/push/tag/release with gh while producing a detailed action summary.
---

# Jina4j Openapi Release

## Overview
Use the workflow script to run the same end-to-end process used in this repository: refresh OpenAPI, regenerate the client, validate examples, and optionally perform release actions.

## Required Inputs
- `JINA_API_KEY` (or `--api-key`) is required unless examples are explicitly skipped.
- If the user requests example validation and has not provided a key, ask for the key before running the script.

## Execute Workflow
1. Change to repository root (the directory containing `build.gradle.kts`).
2. Obtain API key from the user when examples must be run.
3. Run the script with the right mode.

```bash
# Refresh + regenerate + build + run all 3 examples
.codex/skills/jina4j-openapi-release/scripts/run_workflow.py \
  --repo-root . \
  --api-key "$JINA_API_KEY"

# Full release flow: bump tag (or use --tag), commit, push, and create GitHub release
.codex/skills/jina4j-openapi-release/scripts/run_workflow.py \
  --repo-root . \
  --api-key "$JINA_API_KEY" \
  --commit --push --release

# Full release flow with explicit tag and summary file output
.codex/skills/jina4j-openapi-release/scripts/run_workflow.py \
  --repo-root . \
  --api-key "$JINA_API_KEY" \
  --commit --push --release \
  --tag v0.0.4 \
  --summary-file /tmp/jina4j-openapi-release-summary.md
```

## What The Script Does
1. Download `https://api.jina.ai/openapi.json` with `curl` directly into `openapi/jina-openapi.json`.
2. Normalize formatting in `openapi/jina-openapi.json`.
3. Detect duplicate `operationId` values and rename duplicates deterministically.
4. Run `./gradlew clean`.
5. Run `./gradlew :openApiGenerate :fixGeneratedCode --rerun-tasks`.
6. Run `./gradlew build`.
7. Run example validations unless `--skip-examples` is passed:
- `:examples:runEmbeddingExample`
- `:examples:runMultiVectorExample`
- `:examples:runRerankingExample`
8. Optionally commit/push/release:
- Bump `build.gradle.kts` version when needed.
- Commit all changes.
- Push current branch.
- Create GitHub release/tag via `gh`.
9. Print a detailed markdown summary.

## Project-Specific Notes
- The repo relies on `fixGeneratedCode` patches in `build.gradle.kts` for OpenAPI-generator quirks.
- Duplicate `operationId` conflicts can appear for classifier endpoints; the workflow handles this before generation.
- Example runs should be executed with the same user-provided API key used for manual verification.

## Report Back To User
Always include a detailed summary with:
- OpenAPI source URL and old/new schema version.
- Confirmation that only `openapi/jina-openapi.json` was written.
- Whether duplicate `operationId` fixes were applied.
- Pass/fail status for each major command.
- Example run highlights (`Model`, `Usage`/`Tokens`, and notable result lines).
- Commit hash, push status, tag, and release URL when release mode is used.
- Final `git status --short --branch` output.

## Resources
Use `references/release-checklist.md` for manual fallback commands and summary checklist.
