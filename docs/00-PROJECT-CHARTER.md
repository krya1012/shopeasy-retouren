# Project Charter – ShopEasy Returns Process (DRAFT)

> State 19.10.2026 (translated to English on 24.10.2026). The communication rules are taken over as a proposal and must be confirmed by the team. The team fills in the expectations (fields marked with `_…_`).

## 1. Project name and description
**ShopEasy Returns Digital.** The partly manual returns process of ShopEasy GmbH, with media breaks (return rate 25–35 %, refund after 8–12 days), is designed as a digital concept and the central building blocks are implemented as prototypes (BPMN, REST/gRPC/messaging, process mining, Camunda workflow, RPA, AI agent).

## 2. Project goals (SMART)
1. By 20.10.: as-is and target process in BPMN, weakness analysis and KPI catalogue are available and agreed in the team.
2. By 3.11.: REST mock, gRPC service and messaging status updates run locally and can be demonstrated with tests or video.
3. By 8.12.: the target process runs as a Camunda workflow, uses the Sprint 2 services and has at least 3 demonstrable test runs (standard, rejection, error case).
4. By 12.1.: UiPath bot and n8n AI agent are implemented and tested; the agent is integrated in Camunda.
5. By 26.1.: overall documentation and final presentation are finished; code is on GitHub.

## 3. Scope
**In scope:** returns process from registration to refund/reporting, all three input channels, prototypes of the building blocks with mock systems.
**Not in scope:** connection to real Amazon/Zalando/ERP systems, production operation, real payment processing, detailed legal check (right of withdrawal).

## 4. Roles and responsibilities
The course provides two roles (agile lecture, slide "Team and roles"): product owner and team members. We are three people and work on every sprint together.

| Role | Person | Task |
|---|---|---|
| Product owner | Eduard Ede Moser | maintain the product backlog, contact to the lecturer, keep an eye on the deadlines |
| Team members | Eduard Ede Moser, Christian Sticker, Yaraslau Krautsou | work on all sprints together |

The product owner works like the others as a team member. The roles change per sprint. Sprint 1: product owner Eduard Ede Moser; order for the following sprints: _fill in_.
Contributions stay traceable through commits under one's own GitHub account.

## 5. Time frame and milestones
Hand-ins are always due at 11:30 in ILIAS: 20.10. (S1), 3.11. (S2), 17.11. (S3), 8.12. (S4), 12.1. (S5), 26.1. (S6).

## 6. Communication rules
- Meeting rhythm: once per week, 30 minutes
- Tools: GitHub (code/documentation), WhatsApp (chat)
- Response time: within 24 hours
- Short minutes per meeting in the chat or in the repo
- Everyone commits under their own GitHub account, so contributions are traceable.

## 7. Decision rules
Consensus; on disagreement a majority decision, on a tie the product owner decides. Changes to agreed artifacts only with information to the team.

## 8. Risks and assumptions
**Top 3 risks:** (1) uneven distribution of work; (2) tool problems (Camunda, UiPath, Celonis access); (3) time pressure because of parallel exams.
**Assumptions:** the event log for Sprint 3 is provided by the lecturer; all tools are free/licensed.

## Team expectations
Everyone enters their own details (project rule: clarify goals and expectations together).

| Person | Target grade | Hours per week | Special circumstances (e.g. exam periods) |
|---|---|---|---|
| Eduard Ede Moser | _fill in_ | _fill in_ | _fill in_ |
| Christian Sticker | _fill in_ | _fill in_ | _fill in_ |
| Yaraslau Krautsou | _fill in_ | _fill in_ | _fill in_ |

## Rules for AI use
Every use of AI is documented in the `AI-USAGE.md` of the respective sprint (overview: `AI-USAGE.md`). The personal summary in Sprint 6 is written without AI.
