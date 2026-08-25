---
applyTo: "**/db/**,**/*Repository.java,**/*DatabaseHandler.java,**/*SearchHandler.java,**/cloudantclient/**,**/nouveau/**,**/couchdb/lucene/**"
---

# SW360 Database Layer Instructions

> **CouchDB via IBM Cloudant Java SDK.
> Repositories are the ONLY allowed CouchDB access point.
> Backend Handlers must never call CouchDB directly.**

---

## Core Database Classes

| Class | Purpose | Location |
|-------|---------|----------|
| `DatabaseConnectorCloudant` | Low-level CRUD, queries, attachments, connection pooling | `libraries/datahandler/.../cloudantclient/` |
| `DatabaseRepositoryCloudantClient<T>` | Base class for all repositories | `libraries/datahandler/.../cloudantclient/` |
| `DatabaseInstanceCloudant` | Manages CouchDB connection and DB instances | `libraries/datahandler/.../cloudantclient/` |

> **20.1.x:** the underlying Cloudant HTTP client is pooled inside
> `DatabaseSettings.getConfiguredClient()`. Obtaining a connector for a specific
> DB is still done per-Handler, but reuse the shared client — do not build a
> new `com.ibm.cloud.cloudant.v1.Cloudant` per request.

### Obtaining a connector (in a Handler)

```java
public class ComponentDatabaseHandler {

    private final DatabaseConnectorCloudant db;
    private final ComponentRepository repository;

    public ComponentDatabaseHandler(Cloudant client, String dbName) throws MalformedURLException {
        this.db = new DatabaseConnectorCloudant(client, dbName);   // client is pooled
        this.repository = new ComponentRepository(db);
    }
}
```

`Cloudant client` in the Handler constructor is always obtained from
`DatabaseSettings.getConfiguredClient()` at the caller site.

---

## Repository Pattern

All repositories **must** extend `DatabaseRepositoryCloudantClient<T>`:

```java
public class ComponentRepository extends DatabaseRepositoryCloudantClient<Component> {

    public ComponentRepository(DatabaseConnectorCloudant db) {
        super(db, Component.class);
        initStandardDesignDocument(getViews(), db);
        createPartialTypeIndex("component-partial-idx", "componentPartialIndex",
            "component", new String[]{"name", "createdOn"}, db);
    }

    public List<Component> getByName(String name) {
        return queryView("byName", name);
    }
}
```

---

## Query Operators

```java
import static org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant.*;

// NOTE: and()/or() take a List, not varargs
Map<String, Object> selector = and(List.of(
    eq("type", "component"),
    or(List.of(eq("name", "Apache"), eq("name", "Spring")))
));

// All operators (exact signatures — verified against DatabaseConnectorCloudant):
eq("field", "value")                                // $eq  (String value)
in("field", List.of("a", "b"))                      // $in  — match any
all("field", List.of("a", "b"))                     // $all — match all
exists("field", true)                               // $exists
elemMatch("field", "value")                         // $elemMatch (String form)
elemMatch("field", Map.of("$gt", 0))                // $elemMatch (Map form)
and(List.of(cond1, cond2))                          // $and
or(List.of(cond1, cond2))                           // $or
```

---

## Creating Views (Design Documents)

```java
private Map<String, DesignDocumentViewsMapReduce> getViews() {
    return Map.of(
        "all", createMapReduce(
            "function(doc) { if(doc.type == 'component') emit(doc._id, null); }",
            "_count"
        ),
        "byName", createMapReduce(
            "function(doc) { if(doc.type == 'component') emit(doc.name, null); }",
            null
        )
    );
}
```

---

## Creating Indexes

```java
// Simple index
createIndex("component-idx", "componentIndex",
    new String[]{"name", "type"}, db);

// Partial index with type filter (preferred — better performance)
createPartialTypeIndex("component-partial-idx", "componentPartialIndex",
    "component", new String[]{"name", "createdOn"}, db);
```

> **Prefer partial indexes** over full indexes — they filter by document type
> at index time, significantly reducing query cost on large databases.

---

## Pagination

```java
public Map<PaginationData, List<Component>> getComponentsWithPagination(
        PaginationData pageData) {
    // pageData: rowsPerPage, displayStart, sortColumnNumber, ascending
    List<Component> results = queryViewPaginated("all", pageData, /* isReduced */ false);
    pageData.setTotalRowCount(db.getDocumentCount(Component.class));
    return Map.of(pageData, results);
}
```

---

## Common Database Operations

```java
// Read
Component component = db.get(Component.class, componentId);
List<Component> byIds = db.get(Component.class, List.of(id1, id2));
List<Component> found = db.getQueryResult(postFindOptions, Component.class);

// Write
boolean added = db.add(component);                  // returns success flag; sets _id/_rev on the doc
db.update(component);                                // uses _id + _rev from the object
db.remove(componentId);                              // by id only — _rev looked up internally

// Bulk (preferred for multi-document writes — avoids N+1)
List<DocumentResult> results = db.executeBulk(listOfComponents);

// Count
int total = db.getDocumentCount(Component.class);    // typed, not string
```

---

## SW360Constants — Document Type Identifiers

```java
import static org.eclipse.sw360.datahandler.common.SW360Constants.*;

TYPE_COMPONENT   // "component"
TYPE_RELEASE     // "release"
TYPE_PROJECT     // "project"
TYPE_LICENSE     // "license"
TYPE_USER        // "user"
TYPE_VENDOR      // "vendor"
TYPE_ATTACHMENT  // "attachment"
```

Always filter views and indexes by document type using these constants.

---

## Nouveau / Lucene Full-Text Search

CouchDB **Mango selectors and views cannot do relevance-scored or fuzzy text
search.** For that SW360 uses **CouchDB Nouveau** (Lucene-backed indexes)
through the `nouveau-handler` library. Every domain that exposes text search
(`?luceneSearch=true` on the REST layer) has a matching `*SearchHandler`.

### Rule
- Text search / relevance sort → **new `*SearchHandler` extending
  `BaseNouveauSearchHandler<T>`** — never hand-roll a Nouveau HTTP call.
- Exact-match filters or ID lookups → stay in the repository/view layer.
- SearchHandlers live in `backend/common/.../datahandler/db/` (or the module's
  own `db/` package) and are wired into their Thrift `*Handler`.

### SearchHandler skeleton (matches `ComponentSearchHandler`)

```java
public class ComponentSearchHandler extends BaseNouveauSearchHandler<Component> {

    // 1. Field spec — analyzers per field
    private static final List<IndexField> COMPONENT_FIELDS = List.of(
            IndexField.standard("name"),                    // tokenized text + n-gram
            IndexField.simple("componentType", "keyword"),  // exact match
            IndexField.simple("createdBy", "email"),
            IndexField.date("createdOn")
    );

    // 2. Custom sort analyzers (for `<field>_sort` companion fields)
    private static final Map<String, String> COMPONENT_CUSTOM_ANALYZERS = Map.of(
            "categories_sort", "email",
            "id", "keyword"
    );

    // 3. Multi-valued fields — use arrayToStringIndex helper JS
    private static final String COMPONENT_CUSTOM_JS =
            "    arrayToStringIndex(doc.categories, 'categories');" +
            INDEX_ID_FIELD;

    // 4. Build the design-doc index function once, statically
    private static final BuiltIndexDefinition COMPONENT_INDEX_DEFINITION = buildIndexFunction(
            "component",                                    // document type filter
            SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN,
            COMPONENT_FIELDS,
            COMPONENT_CUSTOM_JS,
            COMPONENT_CUSTOM_ANALYZERS,
            "standard"                                      // default analyzer
    );

    private final NouveauLuceneAwareDatabaseConnector connector;

    public ComponentSearchHandler(Cloudant client, String dbName) throws IOException {
        super(Component.class, "components", COMPONENT_INDEX_DEFINITION);
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(client, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(
                db, DDOC_NAME, dbName, db.getInstance().getGson());
        setup(connector, db);                               // registers the design doc
    }

    /** Field-restricted AND search + post-query permission filter. */
    public Map<PaginationData, List<Component>> searchAccessibleComponents(
            Map<String, Set<String>> subQueryRestrictions,
            @Nullable User user,
            PaginationData pageData) {
        Map<PaginationData, List<Component>> result =
                baseSearch(connector, subQueryRestrictions, pageData);
        // Apply document-level permissions AFTER Lucene returns — Lucene has
        // no view of SW360 visibility rules.
        return filterByReadPermission(result, user);
    }

    /** Quick-filter across multiple fields joined with OR. */
    public Map<PaginationData, List<Component>> searchFilteredComponents(
            String searchText, User user, PaginationData pageData) {
        Map<String, Set<String>> restrictions = QUICK_FILTER_FIELDS.stream()
                .collect(Collectors.toMap(f -> f.getFieldName(),
                                          f -> Set.of(searchText)));
        return filterByReadPermission(
                baseSearchWithOr(connector, restrictions, pageData), user);
    }

    /** Sort-column mapping — `SCORE_SORTING_FIELD` = relevance ranking. */
    @Override
    protected @NonNull List<String> mapSortColumn(int sortColumnNumber) {
        return switch (ComponentSortColumn.findByValue(sortColumnNumber)) {
            case BY_NAME       -> List.of("name_sort", "-createdOn");
            case BY_CREATEDON  -> List.of("createdOn");
            case null, default -> List.of(SCORE_SORTING_FIELD);   // "relevance"
        };
    }
}
```

### `IndexField` factory cheat-sheet

| Factory | Use for | Notes |
|---------|---------|-------|
| `IndexField.standard(name)` | tokenized text | full-text + n-gram (default 3–20) |
| `IndexField.standard(name, min, max)` | text with custom n-gram | for short/long fields |
| `IndexField.simple(name, "keyword")` | exact-match (enums, IDs) | no tokenization |
| `IndexField.simple(name, "email")` | emails / dotted values | preserves `.` and `@` |
| `IndexField.emptyAware(name)` | text that may be blank | emits `_empty_` token |
| `IndexField.date(name)` | timestamp / date | range-queryable |
| `IndexField.doubleField(name)` | numeric | range-queryable |

### Query API on `BaseNouveauSearchHandler`

```java
baseSearch(connector, restrictions, pageData)       // AND across fields
baseSearchWithOr(connector, restrictions, pageData) // OR across fields
```

Both return `Map<PaginationData, List<T>>` where the response `PaginationData`
carries the total hit count.

### Unbounded queries and result unwrapping

```java
// Fetch every hit (use sparingly — no pagination)
PaginationData all = NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords();
Map<PaginationData, List<Project>> paginator = searchHandler.searchAll(all);

// Flatten to a plain List<T>
List<Project> projects =
    NouveauLuceneAwareDatabaseConnector.convertPaginatorToList(paginator);
```

### Nouveau DO's and DON'Ts

#### ✅ DO
- Extend `BaseNouveauSearchHandler<T>` — never call the Nouveau HTTP endpoint directly
- Declare `IndexField`s **statically** and pass them once to `buildIndexFunction(...)`
- Use `SCORE_SORTING_FIELD` (`"relevance"`) as the default sort key
- Apply `PermissionUtils.makePermission(doc, user).isActionAllowed(READ)`
  **after** the Lucene search returns — Nouveau does not know SW360 visibility
- Use `arrayToStringIndex(doc.field, 'field')` in `CUSTOM_JS` for `List<String>` fields
- Reuse the same `Cloudant` pooled client used by the Handler's `DatabaseConnectorCloudant`

#### ❌ DON'T
- Query `_find`/Mango for text-relevance ranking — it can't do it; use a SearchHandler
- Recreate `NouveauLuceneAwareDatabaseConnector` per request — hold it as a field
- Filter permissions inside the Lucene query — filter the returned list in Java
- Add `?luceneSearch=true` to a REST endpoint without a matching `*SearchHandler`
- Emit a `<field>_sort` companion without an entry in `CUSTOM_ANALYZERS`

---

## Database DO's and DON'Ts

### ✅ DO
- Always extend `DatabaseRepositoryCloudantClient<T>` for new repositories
- Use `createPartialTypeIndex` over plain `createIndex` for type-scoped queries
- Use bulk operations (`executeBulk`) for multi-document writes
- Use `PaginationData` for paginated queries — do not fetch all and slice in memory
- Reuse Cloudant connection pool — obtain from `DatabaseSettings.getConfiguredClient()`
- Filter views by document type to avoid cross-type collisions

### ❌ DON'T
- Access `DatabaseConnectorCloudant` from a Handler class — go through Repository
- Create new DB connections per request
- Fetch all documents and filter in Java (full-scan) — use views or indexes
- Use raw `_id` or `_rev` fields in REST API responses
- Write N+1 query patterns — use views, bulk fetches, or indexed queries
