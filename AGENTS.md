## Skills

### Available skills
- `jina4j-openapi-release`: End-to-end workflow for this repository to refresh from `https://api.jina.ai/openapi.json`, update only `openapi/jina-openapi.json`, regenerate the Java client, run build + embedding/multi-vector/reranking example validations (with user-provided API key), and optionally commit/push/tag/release with `gh`. (file: `.codex/skills/jina4j-openapi-release/SKILL.md`)

## Trigger rules
- Use `jina4j-openapi-release` when the request mentions refreshing/updating the OpenAPI spec, regenerating client code, running embedding/multi-vector/reranking examples, or cutting/publishing/tagging a release.
- If the skill cannot be used (missing files/tools), continue with best-effort manual workflow and explain the gap.

## Usage notes
- Read `SKILL.md` first, then run `.codex/skills/jina4j-openapi-release/scripts/run_workflow.py` with the appropriate flags.
- Prefer the script over retyping manual command sequences.
- Ask for API key when example validation is requested and no key is available.
- Always return the generated detailed summary to the user.
