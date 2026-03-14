<!--
  ~ Copyright Contributors to the SW360 Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  -->

# LicenseDB Integration — Developer Guide

## Module Location

```
rest/resource-server/src/main/java/org/eclipse/sw360/rest/resourceserver/licensedb/
├── client/
│   └── LicenseDBClient.java
├── transformer/
│   ├── LicenseTransformer.java
│   └── ObligationTransformer.java
├── resolver/
│   └── ConflictResolver.java
└── scheduler/
    └── LicenseDBSyncScheduler.java

libraries/datahandler/src/main/java/org/eclipse/sw360/datahandler/common/
└── SW360Constants.java          ← config constant keys

libraries/datahandler/src/main/resources/
└── sw360.properties             ← default property values
```

## Extending Transformers

Transformers convert LicenseDB data objects into SW360 internal model objects.
Each transformer implements a single `transform()` method.

### LicenseTransformer

```java
/*
 * Copyright Contributors to the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
public class LicenseTransformer {

    public License transform(LicenseDBLicense source) {
        License license = new License();
        license.setShortname(source.getShortname());
        license.setFullname(source.getFullname());
        license.setText(source.getText());
        license.setLicenseType(source.getLicenseType());
        license.setOsiApproved(source.isOsiApproved());
        return license;
    }
}
```

### ObligationTransformer

```java
/*
 * Copyright Contributors to the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
public class ObligationTransformer {

    public Obligation transform(LicenseDBObligation source) {
        Obligation obligation = new Obligation();
        obligation.setText(source.getText());
        obligation.setTitle(source.getTitle());
        obligation.setObligationType(source.getType());
        obligation.setObligationLevel(source.getLevel());
        return obligation;
    }
}
```

### Adding a new field mapping

1. Add the new field to the LicenseDB model class (if not already present)
2. Add the corresponding setter call in the relevant transformer
3. Add a unit test in `LicenseTransformerTest` or `ObligationTransformerTest`

Example — adding `deprecatedVersion` field:

```java
// In LicenseTransformer.transform():
license.setDeprecated(source.isDeprecated());
```

## Adding Custom Conflict Resolution Strategies

The `ConflictResolver` uses a strategy pattern. To add a new strategy:

### Step 1: Implement the strategy interface

```java
/*
 * Copyright Contributors to the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
public class MergeConflictStrategy implements ConflictResolutionStrategy {

    @Override
    public License resolve(License existing, License incoming) {
        // Custom merge logic here
        // Example: keep SW360 text, use LicenseDB metadata
        existing.setLicenseType(incoming.getLicenseType());
        existing.setOsiApproved(incoming.isOsiApproved());
        return existing;
    }

    @Override
    public String getName() {
        return "MERGE";
    }
}
```

### Step 2: Register the strategy

Register it as a Spring `@Bean` or add it to the strategy registry in `ConflictResolver`.

### Step 3: Use it via API

```http
POST /api/licensedb/sync/conflicts/Apache-2.0/resolve
Content-Type: application/json

{ "strategy": "MERGE" }
```

## Testing Guide

### Running unit tests for the LicenseDB module

```bash
mvn test -pl libraries/datahandler -Dtest=LicenseDBConstantsTest
mvn test -pl rest/resource-server -Dtest=LicenseTransformerTest,ObligationTransformerTest
```

### Running all tests (requires Docker + CouchDB)

```bash
docker run -d -p 5984:5984 couchdb:3
mvn test -pl rest/resource-server
```

### Writing a transformer test

```java
/*
 * Copyright Contributors to the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
public class LicenseTransformerTest {

    private LicenseTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new LicenseTransformer();
    }

    @Test
    void testTransformMapsShortname() {
        LicenseDBLicense source = new LicenseDBLicense();
        source.setShortname("Apache-2.0");

        License result = transformer.transform(source);

        assertEquals("Apache-2.0", result.getShortname());
    }

    @Test
    void testTransformHandlesNullFields() {
        LicenseDBLicense source = new LicenseDBLicense();
        License result = transformer.transform(source);
        assertNotNull(result);
    }
}
```

### Writing a constants test

Follow the pattern in `LicenseDBConstantsTest.java` — verify constant name,
value, and prefix for every new constant added to `SW360Constants.java`.

## Adding a New Configuration Property

1. Add the constant key to `SW360Constants.java`:

```java
public static final String LICENSEDB_YOUR_PROPERTY = "licensedb.your.property";
```

2. Add the default value to all three `sw360.properties` files:

```properties
licensedb.your.property=defaultValue
```

3. Add a test in `LicenseDBConstantsTest.java`

4. Document the property in [configuration.md](configuration.md)

## License Header Requirement

All new Java files must include the EPL-2.0 header:

```java
/*
 * Copyright Contributors to the SW360 Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
```

## Commit Message Format

Follow SW360's semantic commit style:

```
feat(licensedb): add custom conflict resolution strategy
fix(licensedb): handle null obligation level in transformer
docs(licensedb): update configuration guide for batch size
test(licensedb): add edge case tests for LicenseTransformer
```
