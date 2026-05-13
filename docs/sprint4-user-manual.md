# Sprint 4 User Manual

Owner: David-Wang

## Purpose
This manual explains how to use the final Sprint 4 TA Recruitment System during local demonstration and marking.

## Supported roles
- Teaching Assistant (TA)
- Module Owner (MO)
- Administrator (Admin)

## Common login information
Use the Login page and choose the correct role before signing in.

Example accounts:

| Role | Email | Password |
|---|---|---|
| TA | `james@school.edu` | `Password123!` |
| MO | `kevin.zhao@school.edu` | `Password123!` |
| Admin | `admin.chen@school.edu` | `Password123!` |

## TA user guide

### 1. View dashboard
After login, the TA dashboard shows:
- open job count;
- submitted application count;
- pending review count;
- selected application count;
- latest applications;
- recommended jobs.

### 2. Manage profile
Open `My Profile`.

Available actions:
- edit name, email, skills, major, and contact;
- upload CV file;
- view uploaded CV;
- remove current CV.

CV requirements:
- PDF, DOC, or DOCX;
- maximum 5MB;
- a valid session is required to open CV files.

### 3. Browse jobs
Open `Job Board`.

Available actions:
- search by keyword;
- filter by module and status;
- view job details;
- quick apply;
- generate AI job matches.

### 4. Apply for a job
The TA can apply from either:
- job card quick apply;
- job detail page.

After submission, the application appears in `Applications` with `pending` status.

### 5. Track applications
Open `Applications`.

Available filters:
- all status;
- pending;
- selected;
- rejected;
- keyword search.

### 6. Use AI assistant
TA AI features:
- page-level job match assistant on Job Board;
- global AI assistant from the top bar.

The AI output is advisory and should be checked against job details before applying.

## MO user guide

### 1. View dashboard
The MO dashboard shows:
- active jobs;
- total applicants;
- pending reviews;
- selected count;
- near deadline jobs.

### 2. Manage job postings
Open `Job Management`.

Available actions:
- create a new job;
- edit existing job;
- filter jobs by keyword and status;
- open applicant list for a job.

### 3. Review applicants
Open `Applicants` or use the `Applicants` button from Job Management.

Available actions:
- filter by job;
- filter by status;
- search candidates;
- view CV;
- open review page.

### 4. Candidate review
Open `Review Center` or a specific application review page.

Available actions:
- read candidate profile and application details;
- view candidate CV;
- generate AI candidate summary;
- write review note;
- select candidate;
- reject candidate.

Important rule:
The system blocks selection if the TA would exceed the workload limit.

## Admin user guide

### 1. View dashboard
Admin dashboard shows:
- total users;
- open jobs;
- total applications;
- overload count;
- recent applications;
- workload alerts.

### 2. Manage user visibility
Open `Users`.

Available actions:
- filter by role;
- search users;
- inspect profile fields.

### 3. Monitor applications
Open `Applications`.

Available actions:
- filter by status;
- filter by module;
- keyword search.

### 4. Monitor workload
Open `Workload`.

Available actions:
- filter risk level;
- inspect selected module count;
- inspect total weekly hours;
- generate AI risk analysis.

## Global AI assistant
The top bar contains `AI Assistant` for logged-in users.

Usage:
1. Click `AI Assistant`.
2. Ask a role-specific question.
3. Read the structured answer card.
4. Follow suggested next actions manually.

Example questions:
- TA: `Which jobs should I apply for first?`
- MO: `Which candidate evidence should I verify?`
- Admin: `Who has workload risk?`

## Known limitations
- Data is stored in local JSON files, not a production database.
- AI output is advisory and should not replace human review.
- The system is desktop-first for Sprint 4 demonstration.
