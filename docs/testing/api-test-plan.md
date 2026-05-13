# Sprint 4 API Validation Plan

Owner: Wutong-Yan

## Purpose
This document validates the HTTP API layer used by the Servlet/JSP frontend. It focuses on session authentication, role authorization, response envelope consistency, CV file access, and AI endpoints.

## Environment
- Local URL: `http://localhost:8080/ta-recruitment-system`
- API base path: `/api/v1`
- Auth model: session cookie
- Required demo accounts:
  - TA: `james@school.edu / Password123!`
  - MO: `kevin.zhao@school.edu / Password123!`
  - Admin: `admin.chen@school.edu / Password123!`

## Response envelope standard
Every JSON API response must follow this shape:

```json
{
  "success": true,
  "data": {},
  "meta": null,
  "error": null
}
```

Failure responses must use:

```json
{
  "success": false,
  "data": null,
  "meta": { "path": "/request/path" },
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable message",
    "details": []
  }
}
```

## Critical API checks

| Area | Endpoint | Role | Expected result |
|---|---|---|---|
| Auth | `POST /auth/login` | Public | Valid credentials create a session. |
| Auth | `GET /auth/me` | TA/MO/Admin | Returns current session user. |
| TA | `GET /ta/profile` | TA | Returns TA profile and `cvPath`. |
| TA | `GET /ta/jobs` | TA | Returns paginated job list. |
| TA | `POST /ta/applications` | TA | Creates pending application or duplicate conflict. |
| MO | `GET /mo/jobs` | MO | Returns only jobs posted by this MO. |
| MO | `GET /mo/applicants` | MO | Returns only applicants for owned jobs. |
| MO | `PUT /mo/applications/{id}/status` | MO | Updates status or rejects overload. |
| Admin | `GET /admin/users` | Admin | Returns paginated users. |
| Admin | `GET /admin/workload` | Admin | Returns workload rows with risk level. |
| CV | `GET /files/cv/{fileName}` | Session required | Returns PDF bytes for accessible CV file. |
| AI | `GET /ai/status` | TA/MO/Admin | Returns provider readiness and model info. |
| AI | `POST /ai/ta/job-recommendations` | TA | Returns structured `modelView`. |
| AI | `POST /ai/mo/candidate-summary` | MO | Returns structured `modelView`. |
| AI | `POST /ai/admin/risk-analysis` | Admin | Returns structured `modelView`. |
| AI | `POST /ai/chat` | TA/MO/Admin | Returns structured `answerView` when model responds with JSON. |

## Authorization checks

| Scenario | Expected status/code |
|---|---|
| Not logged in and call protected API | `401 AUTH_NOT_LOGIN` |
| TA calls MO API | `403 AUTH_FORBIDDEN_ROLE` |
| MO calls Admin API | `403 AUTH_FORBIDDEN_ROLE` |
| MO opens applicant for job owned by another MO | `403 JOB_PERMISSION_DENIED` |
| Nonexistent CV file | `404 CV_FILE_NOT_FOUND` or equivalent file error code |

## Executable smoke script
Run this after Tomcat is started and the WAR is deployed:

```powershell
.\scripts\sprint4-api-smoke.ps1
```

The script logs in as TA, MO, and Admin, calls representative APIs, and fails fast if key response fields are missing.

## Acceptance criteria
- All smoke checks pass.
- Protected endpoints reject unauthenticated access.
- Role-restricted endpoints reject the wrong role.
- AI endpoints return structured fields instead of unformatted text.
- CV endpoint returns `application/pdf` for seeded CV files.
