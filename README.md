# TA Recruitment System

EBU6304 Software Engineering Group 02 project. The system supports teaching assistant recruitment with role-based workflows for Teaching Assistants, Module Organisers, and Administrators.

## Project Scope

The project replaces a form-and-spreadsheet recruitment workflow with a lightweight Java web application.

Main capabilities:
- TA: register/login, maintain profile, upload/view CV, browse jobs, submit applications, track application status, request AI job recommendations.
- MO: publish and edit jobs, inspect applicants, view candidate CVs, request AI candidate summaries, select or reject applicants.
- Admin: inspect users and applications, monitor workload risk, request AI risk analysis.

## Technology Stack

- Java Servlet/JSP
- HTML/CSS/JavaScript frontend
- Maven WAR packaging
- Tomcat 10.1.x deployment
- JSON file persistence only, no database
- JUnit 5 service-layer tests
- Optional GLM/Z.AI model integration for AI assistant features

## Repository Structure

| Path | Purpose |
|---|---|
| `src/main/java/com/group02/tars/controller/` | Web/API servlet layer. Receives requests and returns JSP/API responses. |
| `src/main/java/com/group02/tars/service/` | Business workflow layer. Handles validation, permissions, status changes, and AI orchestration. |
| `src/main/java/com/group02/tars/entity/` | Domain entities such as users, jobs, and applications. |
| `src/main/java/com/group02/tars/storage/` | JSON file persistence layer. |
| `src/main/java/com/group02/tars/util/` | Shared utilities such as data directory resolution and JSON responses. |
| `src/main/webapp/` | JSP pages and static frontend assets. |
| `src/test/` | Automated JUnit tests. |
| `data/` | Baseline local JSON data and uploaded CV files for demonstration. Runtime changes may modify these files. |
| `docs/` | Final-assessment engineering documentation. |
| `archive/docs/` | Historical sprint/process notes moved out of the main documentation set. |
| `report/` | Formal report/backlog artefacts. |

## Active Documentation

- Requirements: `docs/requirement-analysis.md`
- User stories: `docs/user-stories.md`
- System architecture: `docs/system-architecture.md`
- Data model: `docs/data-model.md`
- Iteration plan: `docs/iteration-plan.md`
- Traceability matrix: `docs/traceability-matrix.md`
- AI assistant design: `docs/ai-assistant-design.md`
- API contracts: `docs/api/INDEX.md`
- Test documentation: `docs/testing/service-tests.md`, `docs/testing/api-test-plan.md`
- Final manual acceptance checklist: `docs/testing/final-manual-acceptance-checklist.md`

## Prerequisites

Install these before running the project:
- JDK 21 is acceptable. The Maven compiler target is Java 17.
- Apache Maven 3.9.x.
- Apache Tomcat 10.1.x.

Verify:

```powershell
java -version
mvn -version
```

## Build

From the repository root:

```powershell
mvn clean package
```

The WAR file is generated at:

```text
target/ta-recruitment-system.war
```

## Deploy To Tomcat

Example for a local Tomcat installation at `D:\CodeSoft\apache-tomcat-10.1.49`:

```powershell
$tomcat = "D:\CodeSoft\apache-tomcat-10.1.49"
Copy-Item .\target\ta-recruitment-system.war "$tomcat\webapps\" -Force
& "$tomcat\bin\startup.bat"
```

Open:

```text
http://localhost:8080/ta-recruitment-system/pages/login
```

To stop Tomcat:

```powershell
$tomcat = "D:\CodeSoft\apache-tomcat-10.1.49"
& "$tomcat\bin\shutdown.bat"
```

## Data Storage

The application stores data in local JSON files under `data/` by default:

```text
data/users.json
data/jobs.json
data/applications.json
data/uploads/
```

The resolver uses this priority:

1. JVM system property `tars.data.dir`
2. Environment variable `TARS_DATA_DIR`
3. Servlet context parameter `tars.data.dir`
4. Default project-local `data/`

To force a specific data directory when running Tomcat, set an environment variable before startup:

```powershell
$env:TARS_DATA_DIR = "E:\A_study_resource\Grade3\homework\Software_Engineering\Software_group02\data"
& "D:\CodeSoft\apache-tomcat-10.1.49\bin\startup.bat"
```

Only TA users need CV files. MO and Admin users do not require CV uploads.

## AI Assistant Setup

AI features work in two modes:
- Without an API key: deterministic local fallback output is shown.
- With an API key: the system calls the configured GLM/Z.AI model.

Do not commit API keys into Git. Configure them locally through environment variables.

Required variable:

```powershell
$env:TARS_AI_API_KEY = "your-api-key"
```

Optional variables:

```powershell
$env:TARS_AI_MODEL = "glm-4.6v"
$env:TARS_AI_BASE_URL = "https://api.z.ai/api/paas/v4"
```

Then restart Tomcat. The AI provider status can be checked through:

```text
http://localhost:8080/ta-recruitment-system/api/v1/ai/status
```

For a permanent Windows user-level setting:

```powershell
[Environment]::SetEnvironmentVariable("TARS_AI_API_KEY", "your-api-key", "User")
[Environment]::SetEnvironmentVariable("TARS_AI_MODEL", "glm-4.6v", "User")
```

Open a new terminal after setting permanent environment variables.

## Run Tests

```powershell
mvn test
```

The tests focus on service-layer behavior: authentication, role permissions, application duplication, MO access control, workload rules, CV rules, storage behavior, and AI fallback behavior.

## Demo Accounts

The committed baseline data includes demonstration users in `data/users.json`. Typical examples:

| Role | Example Email | Password |
|---|---|---|
| TA | `james@school.edu` | `Password123!` |
| MO | `kevin.zhao@school.edu` | `Password123!` |
| Admin | `admin.chen@school.edu` | `Password123!` |

If local runtime data changes, inspect `data/users.json` for the current demo accounts.

## Final Delivery Notes

Before final submission:

```powershell
mvn test
mvn clean package
```

Package source code, tests, documentation, report artefacts, and setup instructions according to the coursework handout. Do not package local secret values or personal API keys.
