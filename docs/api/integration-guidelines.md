# Integration Guidelines

## 1. Authentication Contract
- Auth strategy: server-side session cookie.
- Login sets session state.
- Logout clears session state.
- All role APIs (`/ta/*`, `/mo/*`, `/admin/*`) require authenticated session.
- AI APIs (`/ai/*`) and secured file APIs (`/files/*`) also require authenticated session.
- Role mismatch should return `403` with `AUTH_FORBIDDEN_ROLE`.

## 2. Response Envelope Rules
All responses must follow:
- `success` boolean
- `data` object or `null`
- `meta` object or `null`
- `error` object or `null`

No custom envelope variants are allowed.

## 3. Pagination / Filtering / Sorting
### Pagination
- Query params: `page`, `size`
- Defaults: `page=1`, `size=8`
- Meta response:
  - `page`
  - `size`
  - `totalItems`
  - `totalPages`

### Filtering
- Common: `keyword`
- Domain fields:
  - applications: `status`, `module`
  - users: `role`
  - workload: `riskLevel`

### Sorting
- Params: `sort`, `order`
- `order` enum: `asc`, `desc`
- If omitted, backend default ordering is acceptable but must be deterministic.

## 4. Date/Time and ID conventions
- Date format: `YYYY-MM-DD`
- Date-time format (if used): ISO-8601 UTC string
- ID naming:
  - user IDs: `TA***`, `MO***`, `AD***`
  - jobs: `JOB***`
  - applications: `APP***`

## 5. Validation and business rules
- Registration requires unique `email`.
- Job create/update validates required fields and deadline format.
- Application create enforces unique `(userId, jobId)`.
- Application status transitions:
  - default: `pending`
  - update: `pending -> selected/rejected`

## 6. Mock-to-Real switch policy
Frontend data source switch location:
- `src/main/webapp/static/js/core/config.js`

Current mode:
- `dataSource: "api"`

Integration mode:
1. switch to `dataSource: "api"` (already active in current branch)
2. keep `ApiClient` method names unchanged
3. implement backend endpoints as documented
4. validate endpoint parity against `openapi.yaml`

## 7. AI provider integration policy
- Do not hardcode API keys in repository files.
- Read model credentials from `TARS_AI_API_KEY` or `AI_API_KEY`.
- Read model name from `TARS_AI_MODEL` or `AI_MODEL`.
- Keep deterministic tool output available even when no provider is configured.
- For multimodal CV analysis, the provider adapter should read the authorized local file from `data/uploads` and submit the file content using the selected model's native file/document input format.

## 8. Contract freeze rule
Before Sprint 2 demo freeze, do not change:
- endpoint paths
- request/response field names
- envelope structure

If change is unavoidable:
1. update `openapi.yaml`
2. update `contracts.md`
3. update `INDEX.md`
4. update frontend adapter and impacted pages in same commit

## 9. Backend handoff checklist
- [ ] all endpoints in `openapi.yaml` implemented
- [ ] status/error codes follow `errors.md`
- [ ] auth/session behavior validated
- [ ] pagination meta validated
- [ ] duplicate-application conflict validated (`409`)
- [ ] role-based access checks validated (`403`)
