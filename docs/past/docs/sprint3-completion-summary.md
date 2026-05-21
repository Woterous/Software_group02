# Sprint 3 Completion Summary

## 1. Sprint Goal
Sprint 3 expands the Sprint 2 TA MVP into a connected three-role recruitment workflow covering TA, MO, and Admin.

## 2. Completed Scope
| Area | Completion Summary |
|---|---|
| TA status tracking | TA can view submitted applications and status updates from MO decisions. |
| MO job management | MO can create, list, and update own job postings. |
| MO applicant review | MO can view applicants for owned jobs and inspect profile/CV references. |
| MO decision flow | MO can select or reject applications; decisions are persisted. |
| Admin user monitoring | Admin can view and filter users across roles. |
| Admin application monitoring | Admin can view recruitment records globally. |
| Admin workload monitoring | Admin can calculate selected TA workload and risk levels. |
| Architecture cleanup | Code is aligned to Controller, Service, Entity, and Storage layers. |

## 3. Remaining Risk Before Final Sprint
| Risk | Impact | Recommended Action |
|---|---|---|
| Manual integration evidence is incomplete | Weak assessment evidence | Merge `docs/sprint3-test-evidence.md` and complete manual run results. |
| API docs may still contain Sprint 2 mock assumptions | Backend/frontend handoff confusion | Merge Sprint 3 API sync notes and update canonical API docs. |
| Workload rule must be validated in UI | Possible mismatch between backend and user feedback | Test selected/rejected transitions with high-hour assignments. |
| Report/backlog status may lag behind code | Assessment inconsistency | Update report and backlog after all Sprint 3 branches merge. |

## 4. Definition of Done Check
| Criterion | Status |
|---|---|
| Three-role workflow exists | Completed |
| File-based persistence is active | Completed |
| Four-layer architecture is documented and implemented | Completed |
| Service-level unit tests exist | Completed |
| Manual integration evidence exists | To complete |
| Report/backlog updated | To complete |

## 5. Suggested Report Text
Sprint 3 focused on role expansion and integration. The system now supports TA application tracking, MO job and applicant management, and Admin-level monitoring of users, applications, and workload. The backend was also refactored into a clearer four-layer architecture: Controller, Service, Entity, and Storage. This separation improves maintainability and makes business rules testable independently from HTTP routing and JSON file persistence.
