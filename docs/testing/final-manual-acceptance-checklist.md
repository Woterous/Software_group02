# Final Manual Acceptance Checklist

Use this checklist before the final demonstration. Record the tester name, date, browser, Tomcat version, and result for each item.

## 1. Environment

| ID | Check | Expected Result | Result |
|---|---|---|---|
| ENV-01 | Run `mvn clean package` | Build succeeds and WAR is generated. | |
| ENV-02 | Deploy WAR to Tomcat 10.1.x | Application starts without server errors. | |
| ENV-03 | Open `/pages/login` | Login page loads with CSS and JavaScript. | |
| ENV-04 | Open `/api/v1/ai/status` | JSON status response is returned. | |
| ENV-05 | Confirm `data/` location | JSON files and uploads are read from the expected project-local data directory. | |

## 2. Authentication And Registration

| ID | Check | Expected Result | Result |
|---|---|---|---|
| AUTH-01 | Login as TA | User lands on TA dashboard. | |
| AUTH-02 | Login as MO | User lands on MO dashboard. | |
| AUTH-03 | Login as Admin | User lands on Admin dashboard. | |
| AUTH-04 | Enter invalid password | Login is rejected with a clear error message. | |
| AUTH-05 | Register a TA with CV | Account is created and CV upload is stored. | |
| AUTH-06 | Register MO/Admin | CV upload area is hidden and account can be created without CV. | |
| AUTH-07 | Logout | Session ends and protected pages redirect to login. | |

## 3. TA Workflow

| ID | Check | Expected Result | Result |
|---|---|---|---|
| TA-01 | Open TA dashboard | Metrics, latest applications, and recommended jobs are visible. | |
| TA-02 | Edit profile fields | Profile data is saved and visible after refresh. | |
| TA-03 | Upload CV from profile | File uploads successfully and appears as current CV. | |
| TA-04 | View uploaded CV | Browser opens or displays the uploaded CV. | |
| TA-05 | Browse Job Board | Open and closing jobs are listed. | |
| TA-06 | Filter jobs | Results update according to keyword/status/module filters. | |
| TA-07 | View job details | Detail page shows module, deadline, skills, workload, and actions clearly. | |
| TA-08 | Apply for a job | Application is created and appears in My Applications. | |
| TA-09 | Duplicate application | System prevents duplicate submission. | |
| TA-10 | Generate AI job matches | Structured recommendation cards are displayed; if no key is configured, fallback output appears. | |

## 4. MO Workflow

| ID | Check | Expected Result | Result |
|---|---|---|---|
| MO-01 | Open MO dashboard | Workload signals and owned job summary are visible. | |
| MO-02 | Create job | New job is saved and appears in job management. | |
| MO-03 | Edit job | Updated fields persist after refresh. | |
| MO-04 | Filter job list | Job list updates according to filter inputs. | |
| MO-05 | Open applicants for owned job | Applicant list is visible only for jobs owned by the current MO. | |
| MO-06 | Try foreign job applicants URL | Access is rejected. | |
| MO-07 | Open candidate review | Candidate profile, application, job, and CV area are visible. | |
| MO-08 | View candidate CV | CV opens if the TA uploaded one. | |
| MO-09 | Generate AI candidate summary | Structured candidate summary is displayed. If configured, CV file is sent to the multimodal provider. | |
| MO-10 | Select candidate | Status changes to selected unless workload rule blocks it. | |
| MO-11 | Reject candidate | Status changes to rejected with review note. | |
| MO-12 | Overload protection | Selecting an over-assigned TA is blocked. | |

## 5. Admin Workflow

| ID | Check | Expected Result | Result |
|---|---|---|---|
| AD-01 | Open Admin dashboard | Recruitment overview metrics are visible. | |
| AD-02 | Open User List | TA, MO, and Admin accounts are listed and filterable. | |
| AD-03 | Open Application Monitor | Application records and statuses are visible. | |
| AD-04 | Open Workload Overview | TA workload rows and risk indicators are visible. | |
| AD-05 | Filter workload risk | Risk table updates correctly. | |
| AD-06 | Generate AI risk analysis | Structured risk findings are displayed. | |

## 6. Global AI Assistant

| ID | Check | Expected Result | Result |
|---|---|---|---|
| AI-01 | Open AI Assistant from TA page | Assistant panel opens and keeps role/page context. | |
| AI-02 | Ask TA job guidance question | Answer references TA workflow and available jobs/applications. | |
| AI-03 | Ask MO review question | Answer references MO-owned jobs/applicants. | |
| AI-04 | Ask Admin risk question | Answer references workload and risk data. | |
| AI-05 | Remove API key and retry | Fallback/no-provider message appears without breaking the page. | |

## 7. UI And Error Handling

| ID | Check | Expected Result | Result |
|---|---|---|---|
| UI-01 | Navigate across role menus | No clipped content, broken overlays, or hidden dropdown options. | |
| UI-02 | Scroll dashboard pages | Scrolling remains usable and main content is readable. | |
| UI-03 | Use custom select controls | Dropdown panels are not clipped by parent containers. | |
| UI-04 | Submit incomplete forms | Validation errors are explicit and non-blocking. | |
| UI-05 | Refresh protected pages | Session handling remains stable. | |
| UI-06 | Use Chrome/Edge desktop browser | Layout remains stable at common laptop/desktop widths. | |

## 8. Final Sign-Off

| Item | Owner | Result |
|---|---|---|
| Automated tests passed | | |
| Manual checklist completed | | |
| Final WAR deployed and verified | | |
| README run steps verified by another member | | |
| AI key configured only locally, not committed | | |
| Final report, Javadoc, user manual, and video prepared by assigned members | | |
