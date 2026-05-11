# API Integration Index

## 1. Purpose
This package defines the frontend-backend contract for Sprint 2 frontend-first delivery.

It is **decision-complete** for backend implementation:
- endpoint list
- request/response envelope
- schemas and field constraints
- auth/session model
- pagination/filtering conventions
- status/error code taxonomy

## 2. Document Map
| File | Purpose |
|---|---|
| `openapi.yaml` | Machine-readable API specification |
| `contracts.md` | Human-readable endpoint contracts and examples |
| `schemas.md` | Field dictionary and entity schema rules |
| `errors.md` | HTTP statuses and business error codes |
| `integration-guidelines.md` | Auth, filters, sorting, idempotency, and mock-to-real policy |
| `sprint3-api-sync.md` | Sprint 3 real-backend API synchronisation notes |
| `../ai-assistant-design.md` | AI assistant tool design, CV handling, and provider integration notes |

## 3. Base Conventions
- Base path: `/api/v1`
- Auth model: Session Cookie
- Response envelope:
  - success: `{ success: true, data, meta, error: null }`
  - failure: `{ success: false, data: null, meta, error: { code, message, details } }`

## 4. Endpoint Groups
- Auth: register, login, logout, me
- TA: profile, CV, jobs, apply, my applications
- MO: jobs, applicants, review, status update
- Admin: dashboard, users, applications, workload
- Files: secured CV viewing
- AI: role-specific assistant tools

## 5. Role-to-Endpoint Map
### TA
- `POST /auth/login`
- `GET /auth/me`
- `GET /ta/dashboard`
- `GET /ta/profile`
- `PUT /ta/profile`
- `POST /ta/profile/cv/upload`
- `POST /ta/profile/cv`
- `DELETE /ta/profile/cv`
- `GET /files/cv/{fileName}`
- `GET /ta/jobs`
- `GET /ta/jobs/{jobId}`
- `POST /ta/applications`
- `GET /ta/applications`
- `GET /ai/status`
- `POST /ai/ta/job-recommendations`

### MO
- `POST /auth/login`
- `GET /auth/me`
- `GET /mo/dashboard`
- `GET /mo/jobs`
- `POST /mo/jobs`
- `PUT /mo/jobs/{jobId}`
- `GET /mo/applicants`
- `GET /mo/review/{applicationId}`
- `GET /files/cv/{fileName}`
- `PUT /mo/applications/{applicationId}/status`
- `GET /ai/status`
- `POST /ai/mo/candidate-summary`

### Admin
- `POST /auth/login`
- `GET /auth/me`
- `GET /admin/dashboard`
- `GET /admin/users`
- `GET /admin/applications`
- `GET /admin/workload`
- `GET /files/cv/{fileName}`
- `GET /ai/status`
- `POST /ai/admin/risk-analysis`

## 6. Page-to-Endpoint Map
| Page Route | Primary Endpoints |
|---|---|
| `/pages/login` | `POST /auth/login` |
| `/pages/register` | `POST /auth/register` |
| `/pages/ta/dashboard` | `GET /ta/dashboard` |
| `/pages/ta/profile` | `GET/PUT /ta/profile`, `POST /ta/profile/cv/upload`, `DELETE /ta/profile/cv` |
| `/pages/ta/jobs` | `GET /ta/jobs`, `POST /ta/applications`, `POST /ai/ta/job-recommendations` |
| `/pages/ta/job-detail` | `GET /ta/jobs/{jobId}`, `POST /ta/applications` |
| `/pages/ta/applications` | `GET /ta/applications` |
| `/pages/mo/dashboard` | `GET /mo/dashboard` |
| `/pages/mo/jobs` | `GET/POST/PUT /mo/jobs` |
| `/pages/mo/applicants` | `GET /mo/applicants`, `GET /files/cv/{fileName}` |
| `/pages/mo/review` | `GET /mo/review/{applicationId}`, `GET /files/cv/{fileName}`, `POST /ai/mo/candidate-summary`, `PUT /mo/applications/{applicationId}/status` |
| `/pages/admin/dashboard` | `GET /admin/dashboard` |
| `/pages/admin/users` | `GET /admin/users` |
| `/pages/admin/applications` | `GET /admin/applications` |
| `/pages/admin/workload` | `GET /admin/workload`, `POST /ai/admin/risk-analysis` |



