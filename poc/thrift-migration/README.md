# PoC: SW360 Thrift → Spring Direct Injection

**GSoC 2026 proposal** — [Remove Apache Thrift and Migrate to Direct Spring Service Calls](https://github.com/eclipse-sw360/sw360/issues/)

This module demonstrates the core migration mechanism for
[Phase 2 of the implementation plan](../../gsoc_implementation_plan.md): converting the Users
module from Apache Thrift IPC to direct Spring `@Service` injection.

---

## What this shows

SW360 currently routes every internal service call through Apache Thrift:

```
REST server (port 8091)
  Sw360UserService
    └─ getThriftUserClient()          ← creates new THttpClient per request
         └─ TCompactProtocol binary   ← serialize call
              └─ HTTP POST            ← network hop to port 8080
                   └─ UserServlet (TServlet)
                        └─ UserHandler.getAllUsers()   ← actual business logic
                             └─ UserDatabaseHandler → CouchDB
```

After migration:

```
REST server (port 8091)
  DirectInjectionUserService
    └─ userHandler.getAllUsers()      ← direct in-process call, zero overhead
         └─ UserHandlerBean (@Service)
              └─ UserDatabaseHandler (@Repository) → CouchDB
```

The external REST API (`/resource/api/...`) is **unchanged** — the migration is
entirely internal.

---

## Key files

| File | What it demonstrates |
|------|---------------------|
| [`handler/Sw360UserHandler.java`](src/main/java/org/eclipse/sw360/migration/poc/handler/Sw360UserHandler.java) | Plain Java interface replacing `UserService.Iface` (Thrift-generated) |
| [`handler/impl/UserHandlerBean.java`](src/main/java/org/eclipse/sw360/migration/poc/handler/impl/UserHandlerBean.java) | `UserHandler` after migration: `@Service` instead of Thrift servlet instance |
| [`service/DirectInjectionUserService.java`](src/main/java/org/eclipse/sw360/migration/poc/service/DirectInjectionUserService.java) | REST service layer calling `@Autowired Sw360UserHandler` directly |
| [`model/User.java`](src/main/java/org/eclipse/sw360/migration/poc/model/User.java) | Migrated data model: plain POJO replacing `TBase`-extending generated class |
| [`DirectInjectionTest.java`](src/test/java/org/eclipse/sw360/migration/poc/DirectInjectionTest.java) | Spring context test proving wiring works without Thrift |

The PoC uses an in-memory map instead of CouchDB so it runs without any
infrastructure. The real `UserDatabaseHandler` (Cloudant SDK calls) is unchanged
by the migration.

---

## How to run

**Prerequisites:** Java 21+, Maven 3.9+. No Thrift binary, no CouchDB, no Docker.

```bash
cd poc/thrift-migration

# Run tests (proves Spring wiring works without Thrift)
mvn test

# Build
mvn package
```

Expected output:
```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## The three-line diff

The entire handler migration is this change in `backend/users/UserHandler.java`:

```java
// Before
public class UserHandler implements UserService.Iface {
    public UserHandler() throws IOException {
        db = new UserDatabaseHandler(DatabaseSettings.getConfiguredClient(), ...);
    }

// After
@Service
public class UserHandler implements Sw360UserHandler {
    @Autowired
    public UserHandler(UserDatabaseHandler db) {
        this.db = db;
    }
```

And this change in `Sw360UserService.java` (REST layer):

```java
// Before
private UserService.Iface getThriftUserClient() throws TTransportException {
    THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
    TProtocol protocol = new TCompactProtocol(thriftClient);
    return new UserService.Client(protocol);
}

public List<User> getAllUsers() {
    try {
        return getThriftUserClient().getAllUsers();
    } catch (TException e) {
        throw new RuntimeException(e);
    }
}

// After
@Autowired
private Sw360UserHandler userHandler;

public List<User> getAllUsers() {
    return userHandler.getAllUsers();
}
```

All 23 service modules in SW360 follow this same pattern.
