# Sprint 4 Service Layer Regression Tests

Owner: Kunpeng-Lin

## Scope
This test package strengthens Sprint 4 service-layer confidence without changing production code.

## Added test class
- `src/test/java/com/group02/tars/service/impl/Sprint4ServiceRegressionTest.java`

## Coverage
- Duplicate TA application submission returns `APPLICATION_DUPLICATE`.
- Module owner cannot read applicants for jobs owned by another module owner.
- Selection is blocked when projected TA workload exceeds the safe limit.
- Invalid CV file extensions are rejected by profile/CV service rules.
- TA AI job recommendations still return structured fallback output when the provider is unavailable.
- MO candidate AI summaries still return structured fallback output when the provider is unavailable.

## How to run
```powershell
mvn test -Dtest=Sprint4ServiceRegressionTest
```

For full regression:
```powershell
mvn clean test
```

## Expected result
All tests should pass. These tests do not require Tomcat, browser access, or an AI API key.
