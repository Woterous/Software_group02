# Sprint 3-4 Integration Notes

## Sprint 3 Integration Result
Sprint 3 delivers the first fully connected role workflow:

1. MO publishes a job.
2. TA browses and applies for the job.
3. MO reviews the applicant and records a decision.
4. TA sees the updated status.
5. Admin monitors users, applications, and workload.

## Evidence to Attach in Final Report
- Branch names and commit messages from each member.
- Screenshots of TA/MO/Admin flows.
- `mvn test` result showing all tests passing.
- Manual integration checklist from `docs/sprint3-test-evidence.md`.
- Notes explaining the four-layer architecture refactor.

## Known Limitations
- The project uses JSON file storage rather than a database because coursework constraints prohibit relational database integration.
- External notification services are out of scope.
- Workload balancing is rule-based rather than AI-assisted.

## Final Sprint Focus
- Complete UI polish and bug fixing.
- Freeze API contracts.
- Prepare final demo script.
- Confirm every backlog item maps to implemented or explicitly deferred scope.
