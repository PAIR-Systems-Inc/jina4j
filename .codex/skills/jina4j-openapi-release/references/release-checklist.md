# Jina4j OpenAPI Release Checklist

## Preconditions
- Work from repository root.
- Ensure tools are installed: `curl`, `jq`, `git`, `gh`, `./gradlew`.
- Ask user for `JINA_API_KEY` before running examples when key is not already provided.
- Ensure git auth and GitHub CLI auth are configured.

## Manual Fallback Commands
Use these if the workflow script cannot be used.

```bash
curl -fsSL https://api.jina.ai/openapi.json -o openapi/jina-openapi.json
jq . openapi/jina-openapi.json > /tmp/jina-openapi-formatted.json
mv /tmp/jina-openapi-formatted.json openapi/jina-openapi.json

./gradlew clean --console=plain
./gradlew :openApiGenerate :fixGeneratedCode --rerun-tasks --console=plain
./gradlew build --console=plain

JINA_API_KEY="$JINA_API_KEY" ./gradlew :examples:runEmbeddingExample --console=plain
JINA_API_KEY="$JINA_API_KEY" ./gradlew :examples:runMultiVectorExample --console=plain
JINA_API_KEY="$JINA_API_KEY" ./gradlew :examples:runRerankingExample --console=plain
```

## Release Checklist
- Ensure `build.gradle.kts` version matches intended release tag.
- Commit includes OpenAPI refresh and generation-related updates.
- Push branch before creating release.
- Create tag/release with `gh release create <tag> ...`.
- Verify release URL and remote tag visibility.

## User Summary Checklist
Include all of the following in the final response:
- OpenAPI source and old/new schema versions.
- Confirmation that `openapi/jina-openapi.json` was refreshed.
- Exact commands executed and pass/fail status.
- Example run highlights (`Model`, `Usage`/`Tokens`, and result lines).
- Commit hash and push result.
- Release tag and URL when applicable.
- Final working tree state.
