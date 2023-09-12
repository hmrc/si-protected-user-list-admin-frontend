# SI Protected User List Admin Frontend

Admin frontend microservice to manage restricted users.

## Running and Testing
### Run local version
Start the service with ```sbt run``` in the terminal. By default, it runs on port `8508`.

### Run with Service Manager
Standalone:
```bash
sm2 --start SI_PROTECTED_USER_LIST_ADMIN_FRONTEND
```
With dependent services:
```bash
sm2 --start SI_PROTECTED_USER_LIST_ADMIN_UI
```
### Unit & Integration Tests with coverage report
```bash
sbt clean compile coverage test it:test coverageReport
```

### Integration Tests Only
```bash
sbt it:test
```

Please see backend [si-protected-user-list-admin](https://github.com/hmrc/si-protected-user-list-admin#readme) for usage instructions