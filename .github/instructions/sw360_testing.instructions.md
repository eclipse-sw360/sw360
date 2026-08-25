---
applyTo: "**/*Test.java,**/*SpecTest.java,**/test/**"
---

# SW360 Testing Instructions

> **JUnit 6 (Jupiter) is the project standard. JUnit 4 and JUnit 5 APIs are
> deprecated — never use them for new tests. Treat any existing JUnit 4/5
> usage as legacy to be migrated.**

---

## Test Technology Stack

| Library | Purpose |
|---------|---------|
| JUnit 6 (Jupiter) | Test framework — `@Test`, `@BeforeEach`, `@ExtendWith` |
| Mockito (latest) | Mocking — `@Mock`, `@InjectMocks`, `when()`, `verify()` |
| AssertJ (latest) | Fluent assertions — `assertThat()`, `assertThatThrownBy()` |
| MockMvc | HTTP-layer integration tests (Spring Boot 4.x) |
| TestRestTemplate | Full-stack integration tests |
| ArchUnit | Build-time test-coverage enforcement |

---

## ArchUnit Enforcement Rules

SW360 enforces test coverage at **build time** via
`TestCoverageCompletenessRulesTest.java`. Violations **fail the build**.

### Rule 1 — Every Controller must have a test class
`FooController` → test class name must contain `Foo` and end with `Test` or `SpecTest`
(e.g., `FooTest.java`, `FooSpecTest.java`)

### Rule 2 — Every Service must have a test class
`Sw360FooService` / `SW360FooService` → same matching after stripping the prefix.

### Rule 3 — Every endpoint must have an HTTP-exercising test
- Counts `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`,
  `@DeleteMapping`, and `@RequestMapping` methods per Controller
- Counts `@Test` methods that **actually invoke** `MockMvc.perform(...)` or a
  `TestRestTemplate` method (`exchange`, `getForEntity`, `postForEntity`, …).
  Detected via bytecode method-call analysis on test classes.
- If HTTP-test count < endpoint count → **build fails**
- Trivial tests (`assertTrue(true)`, empty bodies) do **not** count

> **When does this fail?** The ArchUnit rule runs during `mvn test` (Surefire),
> **not** during `mvn compile`. `mvn spotless:apply && mvn compile` will not
> catch the coverage gap — you must run at least `mvn test -pl rest/resource-server`
> to see it.

### Test file locations
```
rest/resource-server/src/test/.../integration/<Domain>Test.java     # HTTP integration tests
rest/resource-server/src/test/.../restdocs/<Domain>SpecTest.java    # REST docs spec tests
```

### Pre-existing gaps
Tracked via `EXCLUDED_CLASSES` and `ENDPOINT_RATIO_EXCLUDED` in
`TestCoverageCompletenessRulesTest.java`. Class names in those sets are the
**simple** class name (e.g., `"FossologyAdminController"`).
Do **not** add new entries without a documented reason in a code comment.

---

## Unit Test Pattern (JUnit 6)

```java
@ExtendWith(MockitoExtension.class)
class Sw360ComponentServiceTest {

    @Mock private ThriftClients thriftClients;
    @Mock private ComponentService.Iface componentClient;
    @InjectMocks private Sw360ComponentService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User().setEmail("test@example.com").setUserGroup(UserGroup.USER);
    }

    @Test
    void shouldReturnComponent_whenComponentExists() throws TException {
        // given
        String id = "comp123";
        Component expected = new Component().setId(id).setName("Apache Commons");
        when(thriftClients.makeComponentClient()).thenReturn(componentClient);
        when(componentClient.getComponentById(id, testUser)).thenReturn(expected);

        // when
        Component result = service.getComponentById(id, testUser);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        verify(componentClient).getComponentById(id, testUser);
    }

    @Test
    void shouldThrowNotFoundException_whenComponentDoesNotExist() throws TException {
        // given
        when(thriftClients.makeComponentClient()).thenReturn(componentClient);
        when(componentClient.getComponentById("missing", testUser)).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> service.getComponentById("missing", testUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
```

---

## HTTP Integration Test Pattern (MockMvc — JUnit 6)

> Prefer Spring Security test helpers over raw `Authorization` headers —
> hand-crafted bearer tokens do not clear the Spring Security 7 filter chain.

```java
@SpringBootTest
@AutoConfigureMockMvc
class ComponentTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void shouldReturnOkForGetComponent() throws Exception {
        mockMvc.perform(get("/api/components/comp123")
                .with(jwt().authorities(new SimpleGrantedAuthority("READ"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").exists());
    }

    @Test
    void shouldReturn404ForMissingComponent() throws Exception {
        mockMvc.perform(get("/api/components/nonexistent")
                .with(jwt().authorities(new SimpleGrantedAuthority("READ"))))
                .andExpect(status().isNotFound());
    }
}
```

---

## REST Docs Spec Test Pattern

```java
@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@SpringBootTest
class ComponentSpecTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider docs) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(docs))
                .build();
    }

    @Test
    void shouldDocumentGetComponent() throws Exception {
        mockMvc.perform(get("/api/components/comp123"))
                .andExpect(status().isOk())
                .andDo(document("get-component",
                        responseFields(
                                fieldWithPath("name").description("Component name"),
                                fieldWithPath("componentType").description("OSS, COTS, etc.")
                        )));
    }
}
```

---

## Test Naming Convention

```
shouldReturn<Result>_when<Condition>()
shouldThrow<Exception>_when<Condition>()
shouldCreate<Entity>_when<ValidInput>()
```

---

## CouchDB Tests

```bash
# Start CouchDB before running tests that hit the database
./scripts/startCouchdbForTests.sh

# Run with CouchDB
mvn test

# Module-specific
mvn test -pl backend/components
```

---

## What NOT to Do in Tests

- ❌ Use `@RunWith` (JUnit 4) or `@ExtendWith(SpringRunner.class)` (JUnit 4 bridge) — use `@ExtendWith(MockitoExtension.class)` or `@SpringBootTest`
- ❌ Use `org.junit.Test` (JUnit 4) — use `org.junit.jupiter.api.Test`
- ❌ Use `org.junit.Assert.*` — use AssertJ `assertThat()`
- ❌ Write trivial tests (`assertTrue(true)`) that don't exercise real logic
- ❌ Skip HTTP-exercising tests for new endpoints — ArchUnit will fail the build
