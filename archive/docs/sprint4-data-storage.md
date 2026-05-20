# Sprint 4 Data Storage and Runtime Data Guide

Owner: Xingyu-Tao

## Storage model
The project uses project-local JSON storage for Sprint 4. This keeps the system simple, inspectable, and suitable for course demonstration without requiring a database server.

## Runtime data directory
Default location:

```text
data/
├── users.json
├── jobs.json
├── applications.json
└── uploads/
```

The application resolves the data directory through `DataDirectoryResolver`.

Resolution priority:
1. JVM system property: `tars.data.dir`
2. Environment variable: `TARS_DATA_DIR`
3. Servlet context parameter: `tars.data.dir`
4. Fallback: project root `data/`

## JSON files

### `users.json`
Stores all TA, MO, and Admin accounts.

Main fields:
- `userId`
- `name`
- `email`
- `password`
- `role`
- `skills`
- `major`
- `contact`
- `cvPath`

### `jobs.json`
Stores all job postings created by module owners.

Main fields:
- `jobId`
- `title`
- `moduleName`
- `requiredSkills`
- `deadline`
- `description`
- `status`
- `postedBy`
- `weeklyHours`
- `createdAt`

### `applications.json`
Stores TA job applications and review decisions.

Main fields:
- `applicationId`
- `userId`
- `jobId`
- `status`
- `reviewNote`
- `updatedAt`

## Uploaded CV files
CV files are stored under:

```text
data/uploads/
```

The `cvPath` saved in `users.json` uses a web-style path such as:

```text
/uploads/TA001_cv.pdf
```

The secured endpoint maps that path to a local file:

```text
GET /api/v1/files/cv/{fileName}
```

## Why JSON storage is acceptable for this project
- The project is a course prototype, not a production-scale deployment.
- JSON files are easy to inspect during marking.
- It avoids database setup problems during local demonstration.
- It supports persistence across Tomcat redeploys when the `data/` directory is outside `target/` and `webapps/`.

## Limitations
- No concurrent write locking beyond simple file write behavior.
- Not suitable for heavy multi-user production traffic.
- No query indexing.
- Manual backup is required if data is important.

## Suggested future improvement
For a production version, replace `JsonFileStorage` with a database-backed repository layer while keeping the service interfaces stable.
