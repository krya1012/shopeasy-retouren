# AI Use – Sprint 4 (Workflow (Camunda), 17.11.–8.12.2026)

According to the task, undocumented AI use leads to failing. Every entry: date, tool, co-author, purpose, review status. Overview of all sprints: [AI-USAGE.md](../AI-USAGE.md). GitHub repository: [https://github.com/krya1012/shopeasy-retouren](https://github.com/krya1012/shopeasy-retouren), this file: [sprint4-workflow/AI-USAGE.md](https://github.com/krya1012/shopeasy-retouren/blob/main/sprint4-workflow/AI-USAGE.md).

**Co-authorship:** entries with Claude Code are created in sessions of **Yaraslau Krautsou** (co-author of Claude Code). Other people are named per entry in the column "Co-author".
**Model:** Claude Code with the setting `opusplan` (Claude Opus 5.5 in planning mode, otherwise Claude Sonnet 5.5), unless entered differently.
**Language:** documentation, code comments and program output are in English (from Sprint 2 on).

Create entries immediately when AI is used, not only at hand-in.

| Date | Tool | Co-author | Purpose / artifact | Review status (team) |
|---|---|---|---|---|
| 24.11.2026 | Claude Code (planning: Claude Opus 5.5, implementation: Claude Sonnet 5.5) | Yaraslau Krautsou | Analysis of the Sprint 4 task (PDF) and of the Sprint 1 to 3 results; plan for architecture and model; `process/returns-process.bpmn` (generated from the Sprint 1 model by a one-off script, then linted), `process/registration-check.dmn`, `process/acceptance-decision.dmn`, 8 forms in `process/forms/`; Gradle build and `ReturnsProcessTest` (24 tests); `ARCHITECTURE.md`, `README.md`; CI job `workflow` | Drafted by AI. Run and shown: `c8ctl bpmn lint` clean, `dmnlint` clean, forms valid against the Camunda form schema, deployment to a local Camunda 8.10 cluster ok, 24 tests green. Not yet read in full by Yaraslau Krautsou; assumptions in `ARCHITECTURE.md` section 7 not yet confirmed |
