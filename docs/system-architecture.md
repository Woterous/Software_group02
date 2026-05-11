# System Architecture

## 1. Architecture Goal
The architecture is designed to satisfy three constraints from the coursework:
- simple enough for iterative delivery
- modular enough for team parallel work
- extensible enough for Sprint 3/4 feature growth

## 2. Layered Design
The system uses a 4-layer architecture. The entity layer is documented explicitly because
the core data objects are shared by the controller, service, and storage layers.

### Layer 1: Controller Layer
Responsibilities:
- route JSP pages and API endpoints
- parse request parameters, JSON bodies, and multipart uploads
- enforce session and role access checks
- invoke service operations and return JSP or JSON responses

Representative modules:
- `PageRouterServlet`: page routing for public, TA, MO, and Admin screens
- `AuthApiServlet`: authentication and registration endpoints
- `TaApiServlet`, `MoApiServlet`, `AdminApiServlet`: role-specific API endpoints

### Layer 2: Application Service Layer
Responsibilities:
- enforce business rules
- orchestrate cross-entity operations
- keep role workflows out of controller code
- expose use-case-level methods to the controller layer

Core services:
- `UserService`: register, authenticate, profile operations
- `JobService`: TA-facing job list and detail retrieval
- `ApplicationService`: TA application submission, status visibility, and dashboard data
- `MoService`: MO job posting, applicant review, and decision workflow
- `AdminService`: global user/application views and workload statistics

### Layer 3: Entity Layer
Responsibilities:
- define the core business data structures
- provide shared data contracts between service and storage
- keep entity fields aligned with JSON persistence files and API schemas

Core entities:
- `User`
- `Job`
- `Application`

### Layer 4: File Storage Layer
Responsibilities:
- read and write structured files
- maintain entity-level persistence consistency
- provide storage abstraction to service layer

Default storage baseline:
- `users.json`
- `jobs.json`
- `applications.json`

## 3. Technology Decision
Implementation option selected for the current baseline:
- Java Web application using Servlet/JSP + HTML/CSS/JS
- Maven WAR packaging for Tomcat deployment
- JSON file persistence through the storage layer

Non-negotiable constraint:
- no relational database

## 4. Key Design Trade-Offs
### Trade-Off A: Simplicity vs Query Efficiency
- Choice: file-based persistence
- Benefit: low setup cost, high compliance with coursework constraints
- Cost: weaker query performance than database-backed systems

### Trade-Off B: Fast UI Iteration vs Full Service Completion
- Choice: role-first UI decomposition for Sprint progression
- Benefit: visible sprint outputs and stakeholder feedback earlier
- Cost: requires strict service contract discipline to avoid UI-business coupling

### Trade-Off C: Flexibility vs Strict Schema
- Choice: text-based files with controlled field definitions
- Benefit: easy portability and inspection
- Cost: additional validation logic is needed in services

## 5. Extension Direction
The architecture reserves extension points for optional features:
- skill matching module
- skill gap analysis
- workload balancing recommendation

These are add-on modules and must not break the core 4-layer separation.
