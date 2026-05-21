# Sprint 3 API Synchronisation Notes

## 1. Purpose
This file records Sprint 3 API contract updates after the backend changed from mock-driven UI support to a real four-layer Servlet service implementation.

## 2. Confirmed Runtime Contract
- Base path: `/api/v1`
- Authentication: session cookie (`JSESSIONID`)
- Response shape: `success`, `data`, `meta`, `error`
- Storage: local JSON files under `data/`
- Uploads: local files under `data/uploads/`

## 3. Sprint 3 Endpoint Groups
### TA
| Endpoint | Method | Purpose |
|---|---|---|
| `/api/v1/ta/dashboard` | GET | TA metrics, latest applications, recommended jobs |
| `/api/v1/ta/profile` | GET/PUT | Read/update TA profile |
| `/api/v1/ta/profile/cv/upload` | POST | Multipart CV upload using field `cvFile` |
| `/api/v1/ta/profile/cv` | POST/DELETE | Save or remove CV reference |
| `/api/v1/ta/jobs` | GET | List jobs with pagination/filtering |
| `/api/v1/ta/jobs/{jobId}` | GET | Job detail |
| `/api/v1/ta/applications` | GET/POST | List own applications or apply for a job |

### MO
| Endpoint | Method | Purpose |
|---|---|---|
| `/api/v1/mo/dashboard` | GET | MO metrics and near-deadline jobs |
| `/api/v1/mo/jobs` | GET/POST | List own jobs or create a job |
| `/api/v1/mo/jobs/{jobId}` | PUT | Update own job |
| `/api/v1/mo/applicants` | GET | List applicants for owned jobs |
| `/api/v1/mo/review/{applicationId}` | GET | Review one owned application |
| `/api/v1/mo/applications/{applicationId}/status` | PUT | Select/reject an applicant |

### Admin
| Endpoint | Method | Purpose |
|---|---|---|
| `/api/v1/admin/dashboard` | GET | Global summary |
| `/api/v1/admin/users` | GET | User list with role/keyword pagination |
| `/api/v1/admin/applications` | GET | Application list with status/module/keyword/job filters |
| `/api/v1/admin/workload` | GET | TA workload summary and risk filter |

## 4. Updated Query Parameters
### Shared
- `page`: positive integer, default `1`
- `size`: positive integer, default `8`
- `keyword`: case-insensitive search text
- `status`: domain status filter

### Admin Applications
- `status`: `pending`, `selected`, or `rejected`
- `module`: module code/name
- `jobId`: exact job identifier such as `JOB001`
- `keyword`: applicant name or job title search

## 5. Updated Error Codes
| Code | Meaning |
|---|---|
| `JOB_DEADLINE_INVALID` | Job deadline format is invalid or earlier than today |
| `APPLICATION_OVER_ASSIGNMENT` | Selecting the applicant would exceed workload limits |
| `JOB_PERMISSION_DENIED` | MO attempted to access a job/application not owned by them |
| `APPLICATION_STATUS_INVALID` | Application status transition payload is invalid |
| `VALIDATION_FILE_TOO_LARGE` | Uploaded CV is larger than the allowed maximum |

## 6. Documentation Files to Check After Merge
After this note is merged, compare and update the following canonical files if they still contain older Sprint 2 mock assumptions:
- `docs/api/contracts.md`
- `docs/api/schemas.md`
- `docs/api/errors.md`
- `docs/api/openapi.yaml`
- `docs/api/integration-guidelines.md`
