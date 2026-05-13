# Sprint 4 Final Demo Script

Owner: David-Wang

## Demo objective
Show that the project is a complete TA recruitment workflow with real backend persistence, role-based access, CV handling, workload control, and AI-assisted review.

## Preparation before demo
1. Start Tomcat.
2. Deploy `target/ta-recruitment-system.war` if needed.
3. Confirm local data exists under `data/`.
4. Confirm AI key is configured in Tomcat `setenv.bat` if AI live call is required.
5. Open browser at:

```text
http://localhost:8080/ta-recruitment-system/pages/login
```

## Demo path 1: TA workflow

Login:
- Email: `james@school.edu`
- Password: `Password123!`
- Role: TA

Steps:
1. Show TA dashboard metrics and recommended jobs.
2. Open `My Profile`.
3. Show profile fields and CV management area.
4. Click `View CV` to prove CV access works.
5. Open `Job Board`.
6. Use filters/search briefly.
7. Click `Generate Matches`.
8. Explain AI returns structured recommendation cards.
9. Open one job detail page.
10. Apply or explain duplicate application protection if already applied.
11. Open `Applications` and show application statuses.

Talking points:
- TA can manage profile and CV.
- TA can browse and apply for roles.
- AI helps prioritise jobs but does not submit automatically.

## Demo path 2: MO workflow

Login:
- Email: `kevin.zhao@school.edu`
- Password: `Password123!`
- Role: MO

Steps:
1. Show MO dashboard metrics.
2. Open `Job Management`.
3. Show job list and edit/create controls.
4. Click `Applicants` for one job.
5. Open one candidate review page.
6. Click `View CV`.
7. Click `Generate Summary`.
8. Explain AI candidate summary sections: evidence, gaps, questions.
9. Add a review note.
10. Select or reject candidate if appropriate.

Talking points:
- MO can only see applicants for owned jobs.
- The system checks workload before selection.
- CV and AI summary support faster candidate assessment.

## Demo path 3: Admin workflow

Login:
- Email: `admin.chen@school.edu`
- Password: `Password123!`
- Role: Admin

Steps:
1. Show Admin dashboard.
2. Open `Users` and show role filtering.
3. Open `Applications` and show application monitoring.
4. Open `Workload`.
5. Filter warning or overload if available.
6. Click `Generate Risk Analysis`.
7. Explain AI workload risk cards.

Talking points:
- Admin can monitor system-level activity.
- Workload risk is calculated from selected applications and weekly hours.
- AI helps identify operational risks.

## Demo path 4: Global AI assistant

Steps:
1. Click top bar `AI Assistant`.
2. Ask a role-specific question.
3. Show the structured answer and next actions.

Recommended prompt:

```text
Which page should I use to review workload risk?
```

## Closing statement
The system now supports a complete role-based TA recruitment workflow:
- TA applies and manages CV.
- MO posts jobs and reviews applicants.
- Admin monitors users, applications, and workload.
- AI assists but does not replace user decisions.
- Data persists locally under project `data/`.
