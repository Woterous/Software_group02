# Sprint 4 Final Q&A Preparation

Owner: Yanzhen-Jiang

## Architecture questions

### Q1: What architecture does the project use?
The backend is organised into controller, entity, service, and storage parts. Controllers handle HTTP requests and sessions. Entities define core data objects. Services implement business rules. Storage handles JSON file persistence.

### Q2: Why add an entity layer?
The entity layer makes the data model explicit. `User`, `Job`, and `Application` are shared by service and storage code, so business logic no longer depends on loose maps or UI-specific structures.

### Q3: Why not use a database?
For this course project, local JSON storage is simpler to deploy and easier to inspect. It supports persistence across redeploys and avoids database setup risk during marking. A database would be the next step for production.

## Implementation questions

### Q4: How does TA application work?
The frontend sends `POST /api/v1/ta/applications` with `jobId`. The controller checks the TA session. `ApplicationServiceImpl` validates the user, job, and duplicate application. Then storage writes a new pending application to `applications.json`.

### Q5: How does MO permission work?
MO service methods check whether the target job is posted by the current MO. If not, the service throws `JOB_PERMISSION_DENIED`. This prevents one module owner from reviewing another module owner's applicants.

### Q6: How is workload risk calculated?
Admin workload uses selected applications. For each TA, the system sums weekly hours of selected jobs. It classifies the result into normal, warning, or overload.

### Q7: How does CV access work?
CV files are stored under `data/uploads`. The user record stores a `cvPath`. The browser requests `/api/v1/files/cv/{fileName}`. The servlet checks session access and streams the file as PDF/DOC/DOCX content.

## AI questions

### Q8: Does the AI really call a model?
Yes, when environment variables are configured. The provider status shows whether the model is ready. The response includes `modelCalled=true` when the model is used.

### Q9: Why does the AI return JSON?
Raw model text is hard to display consistently. The backend asks for strict JSON and parses it into fields such as headline, priority, evidence, risks, and actions. The frontend renders these fields as cards.

### Q9a: Does MO AI really read the uploaded CV?
For PDF CV files, yes. The backend resolves the applicant CV from `data/uploads`, converts the PDF to an inline data URL, and sends it to the multimodal GLM request together with the candidate, job, and application context. The response includes `cvSentToModel=true` when the PDF was attached successfully.

### Q10: What happens if AI fails?
The service keeps deterministic fallback output. The main system still works because AI is advisory, not required for core operations.

### Q11: Is the API key safe?
The key is configured through Tomcat environment variables, not committed to Git or exposed to browser JavaScript.

## Testing questions

### Q12: What do the unit tests cover?
They cover service logic such as login, job filtering, application creation, duplicate prevention, MO permissions, workload constraints, CV access, and AI fallback behavior.

### Q13: Why test service layer first?
Most business rules are implemented in service classes. Testing services catches workflow bugs without needing a browser or Tomcat.

### Q14: What is missing in testing?
More automated controller/API tests could be added. Sprint 4 includes an API smoke script and manual regression plan, but a production project should add more integration tests.

## Project management questions

### Q15: What changed from Sprint 2 to Sprint 3?
Sprint 2 focused on frontend-first UI and mock-driven flows. Sprint 3 added real backend services, local persistence, CV file handling, AI integration, and tests.

### Q16: What is Sprint 4 for?
Sprint 4 is final integration and delivery: regression testing, documentation, demo preparation, and stability polish.

### Q17: What would you improve next?
Move JSON storage to a database, add more API/controller automated tests, improve responsive design, and add notification support.
