# Schemas and Field Dictionary

## 1. Envelope Schema
### Success Envelope
```json
{
  "success": true,
  "data": {},
  "meta": {},
  "error": null
}
```

### Error Envelope
```json
{
  "success": false,
  "data": null,
  "meta": {},
  "error": {
    "code": "VALIDATION_REQUIRED_FIELD",
    "message": "Field email is required.",
    "details": []
  }
}
```

## 2. Core Entities

### 2.1 User
| Field | Type | Required | Nullable | Description |
|---|---|---|---|---|
| `userId` | string | yes | no | Unique user ID (`TA***`, `MO***`, `AD***`) |
| `name` | string | yes | no | Display name |
| `email` | string | yes | no | Unique login email |
| `role` | string enum | yes | no | `ta`, `mo`, `admin` |
| `skills` | string[] | no | yes | Skill tags |
| `major` | string | no | yes | Major/department |
| `contact` | string | no | yes | Contact string |
| `cvPath` | string | no | yes | CV file path reference |

### 2.2 Job
| Field | Type | Required | Nullable | Description |
|---|---|---|---|---|
| `jobId` | string | yes | no | Unique job ID (`JOB***`) |
| `title` | string | yes | no | Job title |
| `moduleName` | string | yes | no | Module code/name |
| `requiredSkills` | string | yes | no | Required skills text |
| `deadline` | string (date) | yes | no | `YYYY-MM-DD` |
| `description` | string | yes | no | Job description |
| `status` | string enum | yes | no | `open`, `closing`, `closed` |
| `postedBy` | string | yes | no | MO user ID |
| `weeklyHours` | integer | no | yes | Estimated weekly hours |

### 2.3 Application
| Field | Type | Required | Nullable | Description |
|---|---|---|---|---|
| `applicationId` | string | yes | no | Unique application ID (`APP***`) |
| `userId` | string | yes | no | TA user ID |
| `jobId` | string | yes | no | Job ID |
| `status` | string enum | yes | no | `pending`, `selected`, `rejected` |
| `reviewNote` | string | no | yes | MO review note |
| `updatedAt` | string (date) | yes | no | Last status date (`YYYY-MM-DD`) |

### 2.4 WorkloadRow
| Field | Type | Required | Nullable | Description |
|---|---|---|---|---|
| `userId` | string | yes | no | TA ID |
| `name` | string | yes | no | TA name |
| `selectedModules` | integer | yes | no | Count of selected assignments |
| `totalHours` | integer | yes | no | Computed workload |
| `riskLevel` | string enum | yes | no | `normal`, `warning`, `overload` |

## 3. Request Schemas

### AuthRegisterRequest
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

### AuthLoginRequest
```json
{
  "email": "alex@school.edu",
  "password": "Password123!",
  "role": "ta"
}
```

### TaProfileUpdateRequest
```json
{
  "name": "Emma Thompson",
  "email": "emma@school.edu",
  "skills": "Java, OOP, Tutoring",
  "major": "Software Engineering",
  "contact": "+86-13800010001"
}
```

### CvUpdateRequest
```json
{
  "cvPath": "/uploads/emma_cv_v2.pdf"
}
```

### CvUploadRequest (multipart)
- content type: `multipart/form-data`
- file field: `cvFile`
- schema:
```text
cvFile: binary
```

### AiCandidateSummaryRequest
```json
{
  "applicationId": "APP003"
}
```

### AiRiskAnalysisRequest
```json
{
  "riskLevel": "overload"
}
```

### AiChatRequest
```json
{
  "page": "workload",
  "message": "Which workload issue should I handle first?"
}
```

### TaApplicationCreateRequest
```json
{
  "jobId": "JOB001"
}
```

### MoJobCreateRequest / MoJobUpdateRequest
```json
{
  "title": "TA for Software Engineering",
  "moduleName": "EBU6304",
  "requiredSkills": "Java, OOP, Teamwork",
  "deadline": "2026-04-06",
  "description": "Support labs and marking.",
  "status": "open"
}
```

### MoApplicationStatusUpdateRequest
```json
{
  "status": "selected",
  "reviewNote": "Strong communication and technical depth."
}
```

## 4. Meta Schema
### Pagination Meta
```json
{
  "page": 1,
  "size": 8,
  "totalItems": 52,
  "totalPages": 7
}
```

### Trace Meta
```json
{
  "path": "/api/v1/ta/jobs"
}
```

## 5. Validation Rules
- `email` must be unique for registration.
- `role` must be one of `ta`, `mo`, `admin`.
- `deadline` must match `YYYY-MM-DD` and should be current or future date.
- CV upload only accepts `.pdf`, `.doc`, `.docx`, and max size `5MB`.
- CV viewing only accepts a single sanitized file name from `data/uploads`.
- AI endpoints must not require a model key to return deterministic tool output.
- Application uniqueness key: `(userId, jobId)`.
- Status transition:
  - initial status: `pending`
  - update path: `pending -> selected` or `pending -> rejected`
