# Sprint 4 Technical Architecture Update

Owner: Xingyu-Tao

## Purpose
This document records the final Sprint 4 technical structure for the TA Recruitment System. It replaces the earlier informal three-layer explanation with the current four-part backend structure used by the Servlet/JSP application.

## Technology stack
- UI: JSP, HTML, CSS, JavaScript
- Server: Java Servlet API on Apache Tomcat 10.1
- Build: Maven WAR project
- Runtime data: project-local JSON files under `data/`
- AI provider: GLM-compatible chat completion endpoint configured through environment variables

## Final backend structure

```text
src/main/java/com/group02/tars
├── controller
├── entity
├── service
├── storage
└── util
```

## Layer responsibilities

### 1. Controller layer
Package: `com.group02.tars.controller`

Responsibilities:
- Receive HTTP requests from JSP/JavaScript frontend.
- Check session and role permissions.
- Parse query parameters, JSON bodies, and multipart CV uploads.
- Call the correct service method.
- Return JSON responses using the standard response envelope.

Examples:
- `AuthApiServlet`
- `TaApiServlet`
- `MoApiServlet`
- `AdminApiServlet`
- `AiApiServlet`
- `CvFileServlet`

### 2. Entity layer
Package: `com.group02.tars.entity`

Responsibilities:
- Define the main business objects used across the backend.
- Keep object fields aligned with JSON storage schema.
- Avoid storing HTTP-specific logic or UI-specific behavior.

Entities:
- `User`
- `Job`
- `Application`

### 3. Service layer
Package: `com.group02.tars.service`

Responsibilities:
- Implement business rules.
- Validate application operations.
- Enforce workflow rules such as duplicate application prevention, MO ownership checks, workload constraints, and CV path validation.
- Prepare structured data for frontend rendering.
- Call the AI provider adapter when AI analysis is requested.

Examples:
- `UserServiceImpl`
- `JobServiceImpl`
- `ApplicationServiceImpl`
- `MoServiceImpl`
- `AdminServiceImpl`
- `AiAssistantServiceImpl`
- `CvAccessServiceImpl`

### 4. Storage layer
Package: `com.group02.tars.storage`

Responsibilities:
- Load and save JSON data files.
- Hide file I/O details from service logic.
- Keep `users.json`, `jobs.json`, and `applications.json` persistence consistent.
- Provide local file storage for uploaded CV files.

Examples:
- `FileStorage`
- `JsonFileStorage`

### Utility package
Package: `com.group02.tars.util`

Responsibilities:
- Shared infrastructure helpers that do not belong to a business layer.
- Resolve local data directory.
- Write JSON responses.
- Handle common servlet response formatting.

Examples:
- `DataDirectoryResolver`
- `JsonResponse`

## Request flow example: TA applies for a job

1. Frontend calls `POST /api/v1/ta/applications`.
2. `TaApiServlet` verifies the current session is a TA.
3. Controller extracts `jobId` from the request body.
4. `ApplicationServiceImpl.createApplication()` validates:
   - current user exists;
   - user role is TA;
   - job exists;
   - application is not duplicated.
5. Service creates a new `Application` entity with `pending` status.
6. Storage saves the updated `applications.json`.
7. Controller returns a success response to the frontend.

## Request flow example: MO reviews a candidate

1. Frontend calls `GET /api/v1/mo/review/{applicationId}`.
2. `MoApiServlet` verifies the current session is MO.
3. `MoServiceImpl.reviewApplication()` loads the application, job, and user.
4. Service checks that the job belongs to this MO.
5. Service returns candidate profile, application status, required skills, and CV path.
6. Frontend displays candidate data and enables CV preview and AI summary.

## Request flow example: AI candidate summary

1. Frontend calls `POST /api/v1/ai/mo/candidate-summary`.
2. `AiApiServlet` verifies the current user is MO.
3. `AiAssistantServiceImpl.summarizeCandidateForMo()` builds structured context:
   - candidate profile;
   - job requirement;
   - application status;
   - CV availability;
   - matched and missing skills.
4. Service calls `ZaiAiProvider` if provider configuration is available.
5. The model is instructed to return strict JSON.
6. Service parses JSON into `modelView`.
7. Frontend renders `modelView` as designed cards instead of raw text.

## Design constraints
- Business logic should not be implemented in JSP pages.
- Controllers should not directly manipulate JSON files.
- Storage should not enforce business rules.
- Entity classes should remain simple data structures.
- AI provider configuration must not be committed to Git.
