# Sprint 4 Final Presentation Outline

Owner: Yanzhen-Jiang

## Presentation goal
Present the TA Recruitment System as a complete software engineering project with clear requirements, architecture, implementation, testing, and project management evidence.

## Suggested structure

### 1. Project background
- Universities need a structured workflow for teaching assistant recruitment.
- The system supports TA, MO, and Admin users.
- The project focuses on role-based recruitment, CV handling, workload monitoring, and AI-assisted review.

### 2. Requirement summary
Main functional requirements:
- TA registration and login.
- TA profile and CV management.
- Job browsing and application submission.
- MO job posting and applicant review.
- Admin user/application/workload monitoring.
- AI support for recommendation, candidate summary, workload risk, and global assistant.

Main non-functional requirements:
- Clear role permissions.
- Consistent UI.
- Local persistence.
- Testable service layer.
- Deployable Servlet/JSP WAR project.

### 3. System architecture
Explain the four-part backend structure:
- Controller: HTTP request handling and session checks.
- Entity: User, Job, Application data objects.
- Service: business rules and workflow logic.
- Storage: JSON file persistence.

Mention utility package separately:
- JSON response formatting.
- Data directory resolution.

### 4. Data flow example
Use TA application as example:
1. TA clicks apply.
2. JavaScript calls `/api/v1/ta/applications`.
3. Servlet checks session role.
4. Service validates job and duplicate application.
5. Storage writes `applications.json`.
6. Frontend updates status.

### 5. AI integration
Explain AI is not a replacement for human decision making.

AI features:
- TA: job recommendation.
- MO: candidate review summary.
- Admin: workload risk analysis.
- Global: role-aware assistant.

Important implementation point:
- Backend asks model for JSON.
- Frontend renders structured cards.
- If model fails, deterministic fallback keeps the system usable.

### 6. Testing
Mention:
- Service unit tests.
- API smoke validation.
- Manual role-based regression testing.
- CV access checks.
- Permission checks.

### 7. Project management
Explain the sprint path:
- Sprint 1: requirements, user stories, project plan.
- Sprint 2: frontend-first Servlet/JSP structure and UI workflow.
- Sprint 3: backend, persistence, CV, AI, tests.
- Sprint 4: final integration, testing, documentation, and demo preparation.

### 8. Final demo
Recommended order:
1. TA workflow.
2. MO workflow.
3. Admin workflow.
4. Global AI assistant.
5. Testing and documentation evidence.

### 9. Limitations and future work
Current limitations:
- JSON storage is not production-scale.
- Desktop-first UI.
- AI depends on external provider availability.
- No real email/notification system.

Future work:
- Database migration.
- More automated API/controller tests.
- Better notification workflow.
- Deployment hardening.

## Closing message
The project demonstrates a complete and explainable TA recruitment workflow with clear role separation, persistent data, CV handling, workload control, and AI-assisted decision support.
