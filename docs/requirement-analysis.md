# Requirement Analysis

## 1. Purpose
This document defines the Sprint 1 requirement baseline for the TA Recruitment System and provides the foundation for backlog planning, architecture design, and later sprint implementation.

## 2. Problem Statement
The current TA recruitment process depends on disconnected forms, emails, and spreadsheets. This causes:
- duplicated records and manual reconciliation effort
- low transparency of application status for TAs
- inefficient screening for MOs
- weak visibility of TA workload distribution for Admins

## 3. Stakeholders and Needs
### Teaching Assistant (TA)
- needs a single place to manage profile and CV
- needs to discover open jobs and apply efficiently
- needs clear application status updates

### Module Organiser (MO)
- needs to publish positions quickly
- needs structured applicant comparison
- needs controlled candidate selection flow

### Administrator (Admin)
- needs global oversight of recruitment records
- needs workload monitoring to prevent TA overload

## 4. Scope Baseline
### In Scope
- role-based recruitment workflow (TA, MO, Admin)
- file-based data persistence
- application lifecycle status management
- workload statistics for TA allocation visibility

### Out of Scope (Current Coursework Scope)
- relational database integration
- external notification gateways (email/SMS)
- advanced AI automation as mandatory functionality

### Optional Extension (Bonus)
- skill matching assistance
- skill gap analysis
- workload balancing recommendation

## 5. Functional Requirements
| ID | Requirement | Priority | Target Sprint |
|---|---|---|---|
| FR-01 | TA can register an account | Must | Sprint 2 |
| FR-02 | TA can log in with role validation | Must | Sprint 2 |
| FR-03 | TA can create and edit profile | Must | Sprint 2 |
| FR-04 | TA can upload and manage CV file path | Must | Sprint 2 |
| FR-05 | TA can browse available jobs | Must | Sprint 2 |
| FR-06 | TA can view job details | Must | Sprint 2 |
| FR-07 | TA can submit job applications | Must | Sprint 2 |
| FR-08 | TA can track application status | Should | Sprint 3 |
| FR-09 | MO can create and manage job postings | Must | Sprint 3 |
| FR-10 | MO can view applicants by posting | Must | Sprint 3 |
| FR-11 | MO can review applicant skills/CV | Should | Sprint 3 |
| FR-12 | MO can select or reject applicants | Must | Sprint 3 |
| FR-13 | Admin can view all users and applications | Should | Sprint 3 |
| FR-14 | Admin can monitor TA workload summary | Must | Sprint 3 |

## 6. Non-Functional Requirements
| ID | Category | Requirement |
|---|---|---|
| NFR-01 | Compliance | Implementation must be Java-based and follow sprint delivery model |
| NFR-02 | Storage | Data must be stored in text-based files (`.txt`, `.csv`, `.json`, `.xml`) |
| NFR-03 | Usability | Core workflow should be learnable without training for first-time users |
| NFR-04 | Reliability | Invalid data input must be validated and blocked |
| NFR-05 | Maintainability | Architecture must remain modular with clear controller, service, entity, and storage separation |
| NFR-06 | Traceability | Requirements must map to user stories and backlog entries |

## 7. Constraints and Assumptions
### Constraints
- no relational database is allowed
- team output must show Agile evidence (planning, iteration, reflection)
- sprint deliverables must match assessment checkpoints

### Assumptions
- role definitions (TA/MO/Admin) remain stable during Sprint 1 and Sprint 2
- file-based storage volume is sufficient for coursework-scale data
- prototype feedback will be available before Sprint 2 implementation lock

## 8. Prioritisation Logic
MoSCoW is used to sequence delivery:
- Must: complete minimum recruit-to-select workflow
- Should: improve transparency and governance
- Could: enhancements that are valuable but non-blocking
- Won't (for current stage): integrations that violate coursework constraints

## 9. Acceptance Baseline for Sprint 1
Sprint 1 requirement work is complete only if:
- all in-scope requirements are documented with IDs
- every requirement maps to user stories and backlog entries
- priorities and target sprints are explicitly assigned
- scope boundary and constraints are unambiguous
