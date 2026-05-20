# Sprint 4 AI Integration Guide

Owner: Xingyu-Tao

## Purpose
This document explains how the AI assistant is integrated into the TA Recruitment System in Sprint 4.

## AI use cases

### TA
- Recommend suitable TA positions.
- Compare skill match, deadline risk, and application history.
- Return structured `modelView` for frontend card rendering.

### MO
- Summarise candidate profile and attached PDF CV content.
- Highlight matched skills, missing evidence, and questions to verify.
- Support candidate review without making an automatic final decision.

### Admin
- Analyse workload risk.
- Identify overloaded or warning-level TAs.
- Highlight role coverage and deadline risks.

### Global assistant
- Answer role-aware questions inside the system.
- Explain which page or action the user should use.
- Return structured `answerView` when the model provides valid JSON.

## Provider implementation
Main classes:
- `AiAssistantServiceImpl`
- `AiProvider`
- `ZaiAiProvider`
- `AiProviderResult`

## Environment variables
The API key must not be committed into Git.

Tomcat local configuration should use `bin/setenv.bat`:

```bat
set "TARS_AI_API_KEY=your-api-key"
set "TARS_AI_MODEL=glm-4.6v"
set "TARS_AI_BASE_URL=https://api.z.ai/api/paas/v4"
```

## Response strategy
The backend asks the model to return strict JSON for AI feature panels.

Examples:
- TA recommendation returns `modelView`.
- MO candidate summary returns `modelView`.
- Admin workload risk returns `modelView`.
- Global assistant returns `answerView`.

If the model fails, times out, or returns invalid JSON, the service keeps deterministic fallback fields so the page remains usable.

## PDF CV input
For MO candidate summaries, the service resolves the applicant CV from `data/uploads`.
When the CV is a PDF and the configured model is ready, the backend attaches the PDF to the provider request as an inline `data:application/pdf;base64,...` `file_url` item.

The response includes these diagnostic fields:
- `cvSentToModel`: `true` only when the PDF was attached to the model call.
- `cvInputMode`: `inline-data-url` when the PDF was sent inline.
- `cv.modelReadable`: `true` when the PDF was prepared successfully.

If the CV is missing, not a PDF, too large, or the provider is unavailable, the system falls back to structured profile/job/application analysis.

## Frontend rendering
AI content is not displayed as raw model text. The frontend renders structured fields into cards:
- headline
- priority section
- evidence/strengths
- risks/gaps
- next actions/questions

Shared rendering support is located in:

```text
src/main/webapp/static/js/core/ui.js
```

## Safety and limitations
- AI output is advisory only.
- Final selection and rejection remain human decisions by MO users.
- The system does not expose the API key to browser JavaScript.
- CV file access still requires a valid authenticated session.
- Only PDF CV files are attached to multimodal model calls; DOC/DOCX files are still stored and viewable, but are not sent to the model.
