# ShopEasy – Digitalising the Returns Process

Project in the module "Digitalisierung von Geschäftsprozessen" (HKA, WS 2026/27, Prof. Dr. Heberle).
Goal: concept and prototypes for the digitalised returns process of ShopEasy GmbH.

**Language:** the Sprint 1 documents are in German (as handed in). From Sprint 2 on, all documentation, code comments and program output are in English.

## Sprints

| Sprint | Period | Content | Folder |
|---|---|---|---|
| 1 | 6.–20.10. | BPM: as-is/target BPMN, weakness analysis, KPI catalogue | `sprint1-bpm/` |
| 2 | 20.10.–3.11. | Integration: REST mock, gRPC, messaging | `sprint2-integration/` |
| 3 | 3.–17.11. | Process mining (Celonis) | `sprint3-mining/` |
| 4 | 17.11.–8.12. | Executable workflow (Camunda) | `sprint4-workflow/` |
| 5 | 8.12.–12.1. | RPA (UiPath) & AI agent (n8n) | `sprint5-rpa-ai/` |
| 6 | 12.–26.1. | Integration & documentation | `sprint6-abschluss/` |

## More documents

- [Project charter](docs/00-PROJECT-CHARTER.md)
- [Sprint 1 report](sprint1-bpm/SPRINT1-BERICHT.md) with diagrams: [as-is process](sprint1-bpm/ist-prozess.png), [target process](sprint1-bpm/soll-prozess.png)
- The models in `sprint1-bpm/` are maintained in the Camunda Modeler; the diagrams (PNG) are exported there (File → Export as Image).
- [Sprint 2: README](sprint2-integration/README.md), [task analysis](sprint2-integration/TASK-ANALYSIS.md), [demo script](sprint2-integration/DEMO-SCRIPT.md)
- [AI use (mandatory documentation)](AI-USAGE.md): overview, one `AI-USAGE.md` file per sprint folder

## Note

Every use of AI tools is documented in the `AI-USAGE.md` of the respective sprint (overview: `AI-USAGE.md`).
