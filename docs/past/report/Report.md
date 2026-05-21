# Sprint 1 Brief Report (Group 02)
## TA Recruitment System - Requirement Analysis and Iteration Planning

## 1. Report Objective
This report records Sprint 1 engineering decisions and provides assessment evidence for:
- requirement clarity
- prioritisation quality
- iteration feasibility
- traceable planning outputs

This file is intentionally different from `README.md`:
- `README.md`: repository orientation
- `Report.md`: assessment-facing rationale and evidence

## 2. Context and Problem Definition
BUPT International School currently relies on forms, emails, and spreadsheets for TA recruitment. The process lacks consistency, transparency, and workload visibility.

Main operational problems:
- fragmented records and manual data reconciliation
- low status visibility for applicants
- inefficient MO screening flow
- weak workload governance for Admin

Project goal:
- deliver a modular TA recruitment system using Java and file-based storage, aligned with coursework constraints.

## 3. Fact-Finding and Requirement Elicitation
Sprint 1 requirement work used three evidence channels:

1. Existing artefact review:
- current forms and spreadsheet structures were reviewed
- required data fields were extracted into entity candidates

2. Stakeholder-oriented scenario interviews:
- TA, MO, and Admin needs were translated into role-specific capabilities
- pain points were mapped to functional requirements

3. Internal validation workshop:
- conflicting or non-compliant ideas were removed (for example DB-first design)
- scope was constrained to sprint-feasible, handout-compliant features

Output:
- requirement baseline with explicit in-scope/out-of-scope boundaries

## 4. Prioritisation and Estimation Strategy
### 4.1 Prioritisation Method: MoSCoW
- Must: minimum closed recruitment flow
- Should: operational transparency and governance enhancement
- Could: optional improvements and AI-assisted features
- Won't (current stage): non-compliant or schedule-risk features

### 4.2 Estimation Method: Planning Poker
- scale baseline: Fibonacci sequence
- objective: reduce anchoring bias and improve consensus quality
- adjustment rule: re-estimate at sprint boundaries if scope changes

## 5. Key Trade-Off Decisions
### Decision A: File Storage over Database
- Reason: direct compliance with coursework constraint
- Benefit: lower setup complexity and faster sprint onboarding
- Cost: weaker query efficiency and stricter validation burden

### Decision B: Core Workflow First
- Reason: maximize early demonstrable value and reduce integration risk
- Benefit: Sprint 2 can show end-to-end TA path
- Cost: some governance features deferred to Sprint 3

### Decision C: Strict Scope Gate for Sprint 1
- Reason: avoid planning inflation and pseudo-completeness
- Benefit: tighter quality of artefacts and clearer handover to implementation
- Cost: less breadth in non-essential narrative content

## 6. Iteration Plan and Assessment Alignment
| Stage | Date | Focus | Assessment |
|---|---|---|---|
| Sprint 1 | By March 22, 2026 | Requirements, backlog, architecture, prototype direction | 30% |
| Sprint 2 | By April 12, 2026 | MVP implementation and demonstration checkpoint | 20% |
| Sprint 3 | April 13, 2026 - May 3, 2026 | Role expansion and workflow closure | Internal gate |
| Sprint 4 | May 4, 2026 - May 17, 2026 | Testing, quality, and final packaging | Internal gate |
| Final | By May 24, 2026 | Full submission + report + video | 50% |

## 7. Sprint 1 Deliverable Evidence
| Deliverable | Evidence File |
|---|---|
| Requirement analysis | `docs/requirement-analysis.md` |
| User stories and AC | `docs/user-stories.md` |
| Product backlog | `report/ProductBacklog_group2.xlsx` |
| Architecture baseline | `docs/system-architecture.md` |
| Data model baseline | `docs/data-model.md` |
| Iteration plan | `docs/iteration-plan.md` |
| Requirement traceability | `docs/traceability-matrix.md` |

## 8. Risks and Controls
### Risk: Requirement inconsistency across files
- Control: traceability matrix and document synchronization rule

### Risk: Scope creep before Sprint 2
- Control: Sprint 2 accepts Must items only for baseline MVP

### Risk: Weak evidence for viva and marking
- Control: every major decision includes rationale and trade-off statement

### Risk: Insufficient Git contribution evidence
- Control: enforce branch-per-member workflow, PR-based merge, and issue-linked commits

## 9. Sprint 1 Exit Statement
Sprint 1 is accepted only when:
- requirements are explicit, testable, and role-complete
- stories, backlog, and plan are internally consistent
- architecture and data model are implementation-ready
- artefacts are organized for direct assessment review

## 10. Next-Step Entry Criteria (Sprint 2)
Sprint 2 starts only after:
- backlog priority conflicts are resolved
- prototype-to-story mapping is reviewed
- implementation scope freeze is confirmed for MVP
