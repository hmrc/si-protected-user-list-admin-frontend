# SI Protected User List Admin Frontend

Admin frontend microservice to manage restricted users.

## Running and Testing

### Run local version

Start the service with ```sbt run``` in the terminal. By default, it runs on port `8508`.

### Run with Service Manager

```bash
sm2 --start SI_PROTECTED_USER_LIST_ADMIN_FRONTEND
```

### Unit & Integration Tests with coverage report

Before integration testing, ensure backend service is running first (by default, on port `8507`). You can do this either
by cloning [the repo](http://github.com/hmrc/si-protected-user-list-admin) and executing `sbt run` or invoking Service
Manager:

```bash
sm2 --start SI_PROTECTED_USER_LIST_ADMIN
```

You can then execute all tests with SCoverage in the usual manner by:

```bash
sbt clean compile coverage test it:test coverageReport
```

or solely the integration tests with:

```bash
sbt it:test
```
