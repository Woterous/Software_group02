# AI Assistant Design

## 1. Purpose
The AI assistant layer supports three role-specific decisions without changing the existing Servlet/JSP architecture:

- TA: identify suitable TA roles from profile skills, deadlines, and application history.
- MO: summarize a candidate application, profile, skill overlap, and CV availability.
- Admin: surface workload and role-level operational risks.

The current implementation is provider-ready but does not hardcode any API key. When no model provider is configured, endpoints return deterministic tool output so the system remains usable and testable.

## 2. Provider Configuration
Environment variables:

- `TARS_AI_API_KEY` or `AI_API_KEY`
- `TARS_AI_MODEL` or `AI_MODEL`

Current behavior:

- If no key is configured, `mode = tool-only`.
- If a key and model are configured, `mode = provider-ready`.
- `ZaiAiProvider` calls the Z.AI OpenAI-compatible `chat/completions` endpoint through Java `HttpClient`.
- The deterministic tool output remains available as structured context and fallback evidence.

## 3. CV Handling
CV files are stored under project-local `data/uploads`.

Secured file access:

- Endpoint: `GET /api/v1/files/cv/{fileName}`
- TA can view only their own CV.
- MO can view CVs only for applicants to their own jobs.
- Admin can view CVs for audit and risk monitoring.
- File names are sanitized and path traversal is rejected.

Native multimodal model path:

1. MO opens an application in Review Center.
2. Frontend calls `POST /api/v1/ai/mo/candidate-summary`.
3. Backend returns profile facts, skill match facts, and CV file reference.
4. Future provider adapter reads the local CV file from `data/uploads` and submits it to the selected multimodal model together with the structured tool context.

## 4. Implemented Tool Endpoints
| Role | Endpoint | Purpose |
|---|---|---|
| Any logged-in role | `GET /api/v1/ai/status` | Report provider readiness and current assistant mode |
| TA | `POST /api/v1/ai/ta/job-recommendations` | Rank suitable open/closing jobs |
| MO | `POST /api/v1/ai/mo/candidate-summary` | Build candidate review brief and CV readiness summary |
| Admin | `POST /api/v1/ai/admin/risk-analysis` | Detect workload and role-level risks |
| Any logged-in role | `POST /api/v1/ai/chat` | Global workspace assistant for page-level questions |

## 5. Frontend Entry Points
| Page | Entry |
|---|---|
| TA Job Board | `Generate Matches` in the AI match assistant panel |
| MO Review Center | `Generate Summary` in the candidate summary panel |
| Admin Workload | `Analyze Risk` in the workload risk analysis panel |
| All authenticated pages | `AI Assistant` button in the top navigation |

## 6. Real Model Call Flow
1. Frontend calls a role-specific AI endpoint or `/api/v1/ai/chat`.
2. Backend gathers local tool context from JSON storage.
3. Backend sends a guarded prompt plus structured JSON context to GLM through `ZaiAiProvider`.
4. Backend returns model text, model name, finish reason, and usage metadata.
5. If the provider call fails, the endpoint returns local deterministic output plus `modelError`.

## 7. Testing Scope
Backend unit tests cover:

- TA recommendation ranking by skill match.
- MO access control for candidate summaries.
- MO CV reference generation.
- Admin overload detection.
- Secured CV access authorization and path validation.

Run:

```powershell
mvn test
```
