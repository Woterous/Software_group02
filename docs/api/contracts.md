# API Contracts

## 1. Global Conventions
- Base path: `/api/v1`
- Content type: `application/json`
- Auth: Session Cookie
- Standard query params:
  - `page`, `size`, `sort`, `order`, `keyword`
  - domain filters: `status`, `module`, `role`, `dateFrom`, `dateTo`

---

## 2. Auth Endpoints

### POST `/auth/register`
Create a user account.

Request body:
```json
{
  "name": "Alex Morgan",
  "email": "alex@school.edu",
  "password": "Password123!",
  "role": "ta",
  "skills": "Java, SQL",
  "cvPath": "/uploads/alex_cv.pdf"
}
```

Success response (`201`):
```json
{
  "success": true,
  "data": {
    "user": {
      "userId": "TA011",
      "name": "Alex Morgan",
      "email": "alex@school.edu",
      "role": "ta"
    }
  },
  "meta": null,
  "error": null
}
```

### POST `/auth/login`
Create session.

Request body:
```json
{
  "email": "emma@school.edu",
  "password": "Password123!",
  "role": "ta"
}
```

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "user": {
      "userId": "TA001",
      "name": "Emma Thompson",
      "role": "ta"
    }
  },
  "meta": null,
  "error": null
}
```

### DELETE `/auth/logout`
Destroy session.

### GET `/auth/me`
Get current session user.

---

## 3. TA Endpoints

### GET `/ta/dashboard`
Return summary metrics and recent records.

Response data fields:
- `openJobs`
- `submitted`
- `pending`
- `selected`
- `latestApplications[]`
- `recommendedJobs[]`

### GET `/ta/profile`
Return profile for current TA.

### PUT `/ta/profile`
Update profile attributes.

Request body:
```json
{
  "name": "Emma Thompson",
  "email": "emma@school.edu",
  "skills": "Java, OOP, Tutoring",
  "major": "Software Engineering",
  "contact": "+86-13800010001"
}
```

### POST `/ta/profile/cv`
Upsert CV path reference.

Request body:
```json
{ "cvPath": "/uploads/emma_cv_v2.pdf" }
```

### POST `/ta/profile/cv/upload`
Upload CV file and persist managed file path.

Request type:
- `multipart/form-data`
- file field: `cvFile`
- allowed file types: `.pdf`, `.doc`, `.docx`
- max file size: `5MB`

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "cvPath": "/uploads/TA001_1711452645123_ab12cd34.pdf"
  },
  "meta": null,
  "error": null
}
```

### DELETE `/ta/profile/cv`
Remove CV path reference.

### GET `/files/cv/{fileName}`
Stream a stored CV file from the project-local `data/uploads` directory.

Access rules:
- TA can view only their own CV.
- MO can view a CV only when the TA has applied to one of the MO's jobs.
- Admin can view CV files for audit/risk monitoring.

Path rules:
- `fileName` must be a single file name, not a nested path.
- Allowed extensions: `.pdf`, `.doc`, `.docx`.
- Path traversal is rejected with `400 CV_INVALID_PATH`.

Success response:
- HTTP `200`
- Content type is inferred from the file extension.
- `Content-Disposition: inline`

### GET `/ta/jobs`
List jobs with filtering and pagination.

Query params:
- `page`, `size`
- `keyword`
- `module`
- `status`

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "jobs": [
      {
        "jobId": "JOB001",
        "title": "TA for Software Engineering",
        "moduleName": "EBU6304",
        "status": "open"
      }
    ]
  },
  "meta": {
    "page": 1,
    "size": 6,
    "totalItems": 24,
    "totalPages": 4
  },
  "error": null
}
```

### GET `/ta/jobs/{jobId}`
Get job detail.

### POST `/ta/applications`
Submit application.

Request body:
```json
{
  "jobId": "JOB001"
}
```

Success response (`201`):
```json
{
  "success": true,
  "data": {
    "application": {
      "applicationId": "APP010",
      "jobId": "JOB001",
      "status": "pending"
    }
  },
  "meta": null,
  "error": null
}
```

### GET `/ta/applications`
List current TA applications.

Query params:
- `status`
- `keyword`

---

## 4. MO Endpoints

### GET `/mo/dashboard`
Return MO summary metrics and near-deadline jobs.

### GET `/mo/jobs`
List MO-owned jobs.

Query params:
- `status`
- `keyword`

### POST `/mo/jobs`
Create job.

### PUT `/mo/jobs/{jobId}`
Update job.

### GET `/mo/applicants`
List applicants with joined TA/job data.

Query params:
- `jobId`
- `status`
- `keyword`

### GET `/mo/review/{applicationId}`
Load review context for one application.

### PUT `/mo/applications/{applicationId}/status`
Update application status.

Request body:
```json
{
  "status": "selected",
  "reviewNote": "Strong technical fit"
}
```

---

## 5. Admin Endpoints

### GET `/admin/dashboard`
Return global metrics, recent applications, workload alerts.

### GET `/admin/users`
List all users (read-only).

Query params:
- `page`, `size`
- `role`
- `keyword`

### GET `/admin/applications`
List all applications.

Query params:
- `status`
- `module`
- `keyword`

### GET `/admin/workload`
Return workload summary rows.

Query params:
- `riskLevel` (`normal`, `warning`, `overload`)

---

## 6. AI Assistant Endpoints

AI endpoints call the configured provider when model credentials are available. They still return deterministic tool output when no model provider is configured, or when the provider call fails.

Provider configuration:
- `TARS_AI_API_KEY` or `AI_API_KEY`
- `TARS_AI_MODEL` or `AI_MODEL`
- `TARS_AI_BASE_URL` or `AI_BASE_URL`

### GET `/ai/status`
Return provider readiness and current assistant mode.

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "provider": {
      "providerReady": false,
      "mode": "tool-only",
      "model": "",
      "multimodalCvReady": false,
      "message": "No AI API key is configured. The system is returning deterministic tool output only."
    }
  },
  "meta": null,
  "error": null
}
```

### POST `/ai/ta/job-recommendations`
Generate profile-based job recommendations for the current TA.

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "provider": { "mode": "tool-only" },
    "student": {
      "userId": "TA001",
      "name": "Emma Thompson",
      "skills": ["Java", "OOP", "Tutoring"]
    },
    "recommendations": [
      {
        "jobId": "JOB001",
        "title": "TA for Software Engineering",
        "moduleName": "EBU6304",
        "score": 100,
        "matchedSkills": ["java", "oop"],
        "missingSkills": [],
        "alreadyApplied": true,
        "rationale": "Matches java, oop. Deadline in 7 day(s)."
      }
    ],
    "guidance": "Review the highest-score jobs first, then confirm deadline and workload fit before applying."
  },
  "meta": null,
  "error": null
}
```

### POST `/ai/mo/candidate-summary`
Generate a candidate review brief for an MO-owned application.

Request body:
```json
{
  "applicationId": "APP003"
}
```

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "applicationId": "APP003",
    "candidate": {
      "userId": "TA002",
      "name": "James Wilson",
      "skills": ["Algorithms", "Python", "Data Structures"]
    },
    "cv": {
      "uploaded": true,
      "cvPath": "/uploads/james_cv.pdf",
      "fileName": "james_cv.pdf",
      "modelReadable": true,
      "modelInputMode": "inline-data-url",
      "modelInputReason": "PDF CV is attached to the multimodal model request."
    },
    "cvSentToModel": true,
    "cvInputMode": "inline-data-url",
    "cvInputFileName": "james_cv.pdf",
    "matchedSkills": ["data structures"],
    "missingSkills": ["algorithms"],
    "summary": "James Wilson is applying for TA for Data Structures...",
    "reviewQuestions": [
      "Does the CV confirm teaching, lab support, or assessment experience?"
    ]
  },
  "meta": null,
  "error": null
}
```

### POST `/ai/admin/risk-analysis`
Generate workload and role-level risk signals.

Request body:
```json
{
  "riskLevel": "overload"
}
```

### POST `/ai/chat`
Ask the global workspace assistant a page-level question.

Request body:
```json
{
  "page": "workload",
  "message": "Which TA is most risky right now and why?"
}
```

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "provider": {
      "providerReady": true,
      "mode": "provider-ready",
      "provider": "z.ai",
      "model": "glm-4.6v"
    },
    "role": "admin",
    "page": "workload",
    "modelCalled": true,
    "answer": "The highest risk is...",
    "model": "glm-4.6v",
    "finishReason": "stop",
    "usage": {
      "prompt_tokens": 300,
      "completion_tokens": 120
    }
  },
  "meta": null,
  "error": null
}
```

Success response (`200`):
```json
{
  "success": true,
  "data": {
    "riskPeople": [
      {
        "userId": "TA004",
        "name": "Michael Brown",
        "riskLevel": "overload",
        "selectedModules": 4,
        "totalHours": 30,
        "reason": "Selected workload is at or above 28 hours."
      }
    ],
    "roleSignals": [
      {
        "jobId": "JOB004",
        "moduleName": "EBU6302",
        "riskLevel": "deadline",
        "pendingApplications": 2,
        "reason": "Pending applications remain close to the deadline."
      }
    ],
    "summary": "Detected 1 overload TA(s), 0 warning TA(s), and 1 role-level risk signal(s)."
  },
  "meta": null,
  "error": null
}
```

---

## 7. Idempotency and Conflict Rules
- `POST /ta/applications` is not idempotent and returns `409` when duplicate.
- `PUT /mo/applications/{applicationId}/status` is idempotent for same status payload.
- `POST /auth/register` returns `409` when email already exists.

## 8. Frontend Adapter Alignment
`src/main/webapp/static/js/core/api-client.js` method names map 1:1 to this contract.
No backend endpoint should be renamed without updating:
- this file
- `openapi.yaml`
- `docs/api/INDEX.md`
