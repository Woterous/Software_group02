# Sprint 3 Test Evidence

## 1. Purpose
This document records Sprint 3 integration evidence for the TA Recruitment System. Sprint 3 focuses on role expansion: TA status tracking, MO applicant decisions, and Admin global monitoring.

## 2. Environment
- Branch baseline: `sprint3-4`
- Technology: Servlet/JSP + HTML/CSS/JS + Maven WAR
- Data storage: local JSON files under `data/`
- Test command: `mvn test`
- Manual runtime: Tomcat 10.1.x with Java 21 running Java 17-targeted bytecode

## 3. Automated Test Evidence
Run:

```powershell
mvn test
```

Expected result:

```text
BUILD SUCCESS
Tests run: 20 or more
Failures: 0
Errors: 0
Skipped: 0
```

Minimum service areas covered:
- User registration, login, profile update, CV path update
- TA job listing and application creation
- TA application dashboard/status listing
- MO applicant listing and application decision permissions
- Admin user/application/workload aggregation

## 4. Manual Integration Scenario A: MO Posts Job, TA Applies
Precondition:
- At least one MO account exists.
- At least one TA account exists.

Steps:
1. Log in as MO.
2. Open Job Management.
3. Create a new job with valid title, module, skills, deadline, description, and weekly hours.
4. Log out.
5. Log in as TA.
6. Open Job Board.
7. Confirm the newly created job is visible.
8. Open job detail.
9. Submit application.
10. Open Applications page.

Expected result:
- Job appears in TA Job Board.
- Application is created once only.
- Application status is `pending`.
- Duplicate application attempt is blocked.

## 5. Manual Integration Scenario B: MO Reviews and Decides
Steps:
1. Log in as the MO who created the job.
2. Open Applicant List for the job.
3. Confirm the TA application appears.
4. Open applicant review.
5. Review applicant profile, skills, and CV reference.
6. Select or reject the applicant.
7. Log out and log in as the TA.
8. Open Applications page.

Expected result:
- MO only sees applicants for jobs posted by that MO.
- Decision is persisted to `data/applications.json`.
- TA-facing status updates consistently to `selected` or `rejected`.

## 6. Manual Integration Scenario C: Admin Monitoring
Steps:
1. Log in as Admin.
2. Open User List.
3. Filter by role.
4. Open Application Monitor.
5. Filter by status/module/job if available.
6. Open Workload Overview.

Expected result:
- Admin can view all users without editing them.
- Admin can view all applications across roles.
- Workload is calculated from selected applications.
- Risk level reflects total assigned weekly hours.

## 7. Defect Log Template
| ID | Area | Steps to Reproduce | Expected | Actual | Severity | Status |
|---|---|---|---|---|---|---|
| D-S3-001 | Example | Example | Example | Example | Medium | Open |

## 8. Sprint 3 Exit Check
| Criterion | Evidence | Status |
|---|---|---|
| TA can track status | TA Applications page and service tests | Pass when verified |
| MO can post/manage jobs | MO Job Management and service tests | Pass when verified |
| MO can review applicants | Applicant Review page and service tests | Pass when verified |
| MO can select/reject | Decision action and TA status refresh | Pass when verified |
| Admin can monitor users/applications | Admin pages and service tests | Pass when verified |
| Admin can monitor workload | Workload Overview and service tests | Pass when verified |
