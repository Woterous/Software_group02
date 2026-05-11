# TA Recruitment System

Repository for EBU6304 Software Engineering Group Project (Group 02).

## 1. Project Positioning
This project delivers a role-based TA recruitment system for BUPT International School to replace the current form + spreadsheet workflow.

Core roles:
- Teaching Assistant (TA): profile, CV, job browsing, application, status tracking
- Module Organiser (MO): job posting, applicant review, candidate selection
- Administrator (Admin): workload monitoring and recruitment oversight

## 2. Assessment Context
This repository follows the coursework handout constraints:
- Agile development with 4 sprints (iterations)
- Java-based implementation
- File-based persistence only (`.txt` / `.csv` / `.json` / `.xml`)
- No relational database

Key milestones:
- Sprint 1 assessment deadline: **March 22, 2026**
- Sprint 2 checkpoint: **April 12, 2026**
- Final assessment: **May 24, 2026**

## 3. Repository Structure
| Path | Purpose |
|---|---|
| `docs/` | Requirement, architecture, model, plan, and traceability docs |
| `prototype/` | UI prototype source images |
| `report/` | Formal report documents and Sprint backlog artefact |
| `src/main/java/com/group02/tars/controller/` | Servlet page/API routing layer |
| `src/main/java/com/group02/tars/service/` | Business workflow and validation layer |
| `src/main/java/com/group02/tars/entity/` | Core entity definitions shared across layers |
| `src/main/java/com/group02/tars/storage/` | JSON file persistence layer |
| `src/main/webapp/` | JSP pages and static frontend assets |
| `src/test/` | JUnit service-layer tests |

## 4. Documentation Index
- Requirements analysis: [docs/requirement-analysis.md](docs/requirement-analysis.md)
- User stories and acceptance criteria: [docs/user-stories.md](docs/user-stories.md)
- System architecture: [docs/system-architecture.md](docs/system-architecture.md)
- Data model: [docs/data-model.md](docs/data-model.md)
- Sprint plan: [docs/iteration-plan.md](docs/iteration-plan.md)
- Traceability matrix: [docs/traceability-matrix.md](docs/traceability-matrix.md)
- API integration docs: [docs/api/INDEX.md](docs/api/INDEX.md)
- Sprint 1 report: [report/Report.md](report/Report.md)
- Product backlog: [report/ProductBacklog_group2.xlsx](report/ProductBacklog_group2.xlsx)

## 5. Document Governance
To avoid duplicated content and conflicting statements:
- `README.md` is the navigation and project-orientation document
- `docs/` stores source-of-truth engineering documents
- `report/` stores formal assessment-facing narrative

## 6. Current Status
Current focus is Sprint 3-4 finalization:
- role workflows across TA, MO, and Admin
- controller/service/entity/storage architecture consistency
- cross-role data integrity and permission checks
- test evidence, documentation, and final delivery readiness

## 7. Delivery Principle
The project is managed as a software engineering process, not only a coding task:
- decision rationale must be explicit
- trade-offs must be documented
- sprint outputs must be auditable
