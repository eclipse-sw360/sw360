/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.nouveau.NouveauResult;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.eclipse.sw360.datahandler.common.SearchUtils.EMIT_EDGE_N_GRAM_INDEX;
import static org.eclipse.sw360.datahandler.common.SearchUtils.INDEX_DATE_AS_DOUBLE;
import static org.eclipse.sw360.datahandler.common.SearchUtils.OBJ_ARRAY_TO_STRING_INDEX;
import static org.eclipse.sw360.datahandler.common.SearchUtils.OBJ_TO_DEFAULT_INDEX;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.convertToFreeSearch;
import static org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector.sanitizeLuceneString;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.SCORE_SORTING_FIELD;

/**
 * Base class to provide helpers and other infrastructure which will help in
 * preparing and performing Nouveau search.
 *
 * <h2>Field spec DSL</h2>
 * Subclasses declare their index schema using {@link IndexField} factory methods and pass the
 * resulting {@link BuiltIndexDefinition} to the constructor.
 * Metadata is derived automatically from those fields for query routing in {@link #baseSearch}.
 *
 * <h2>Quick reference – field categories</h2>
 * <table border="1">
 *   <tr><th>Category</th><th>Example fields</th><th>Index suffixes</th><th>Auto analyzers</th></tr>
 *   <tr><td>{@link IndexField#standard}</td><td>name, version</td>
 *       <td>_exact (text), _ngram (edge n-gram), _sort (string)</td>
 *       <td>_ngram->whitespace, _sort->keyword</td></tr>
 *   <tr><td>{@link IndexField#simple}</td><td>projectType, state, clearingState</td>
 *       <td>base (text), _sort (string)</td>
 *       <td>_sort->keyword; base override via {@code simple(field, analyzer)}</td></tr>
 *   <tr><td>{@link IndexField#emptyAware}</td><td>businessUnit, tag</td>
 *       <td>same as standard, but empty docs get a sentinel token</td>
 *       <td>_ngram->whitespace, _sort->keyword</td></tr>
 *   <tr><td>{@link IndexField#date}</td><td>createdOn</td>
 *       <td>double (yyyyMMdd via {@code indexDateAsDouble})</td><td>–</td></tr>
 *   <tr><td>{@link IndexField#doubleField}</td><td>nothing</td>
 *       <td>Nothing special</td></tr>
 *   <tr><td>{@link IndexField#string}</td><td>nothing</td>
 *       <td>keyword</td></tr>
 *   <tr><td>{@link IndexField#defaultIndex}</td><td>nothing</td>
 *       <td>standard</td></tr>
 * </table>
 */
public abstract class BaseNouveauSearchHandler<T> {

    /**
     * Default maximum edge n-gram length for search indexing. Controls prefix
     * matching coverage: higher values allow longer prefix matches at the cost
     * of slightly larger indexes. Used for fields like name, version, tag.
     */
    public static final int EDGE_NGRAM_MAX_LENGTH = 50;

    /**
     * Short n-gram max for constrained fields (e.g., businessUnit) where values
     * are short and index bloat should be minimized.
     */
    public static final int EDGE_NGRAM_SHORT_MAX_LENGTH = 10;

    protected static final Joiner AND = Joiner.on(" AND ");
    protected static final Joiner OR = Joiner.on(" OR ");
    protected static final Joiner SPACE = Joiner.on(" ");

    /**
     * Built index definition that bundles the generated index function with derived
     * field-metadata required for query routing.
     */
    public static final class BuiltIndexDefinition {
        private final NouveauIndexFunction indexFunction;
        private final Set<String> tieredFields;
        private final Set<String> emptyAwareFields;
        private final Set<String> dateFields;
        private final Set<String> defaultFields;

        private BuiltIndexDefinition(
                NouveauIndexFunction indexFunction,
                Set<String> tieredFields,
                Set<String> emptyAwareFields,
                Set<String> dateFields,
                Set<String> defaultFields
        ) {
            this.indexFunction = indexFunction;
            this.tieredFields = Set.copyOf(tieredFields);
            this.emptyAwareFields = Set.copyOf(emptyAwareFields);
            this.dateFields = Set.copyOf(dateFields);
            this.defaultFields = Set.copyOf(defaultFields);
        }

        public NouveauIndexFunction getIndexFunction() {
            return indexFunction;
        }
    }

    // -------------------------------------------------------------------------
    //  IndexField: public DSL for declaring index schema
    // -------------------------------------------------------------------------

    /**
     * Describes how a single CouchDB document field should be indexed and queried.
     * Use the static factory methods ({@link #standard}, {@link #simple},
     * {@link #emptyAware}, {@link #date}) to create instances.
     */
    public static final class IndexField {

        /** Supported indexing categories. */
        public enum Category {
            /**
             * Full-text field with prefix search support.
             * Generates: {@code <field>_exact} (text), {@code <field>_ngram} (edge n-gram),
             * {@code <field>_sort} (string).
             * Auto-analyzers: {@code _ngram->whitespace, _sort->keyword}.
             */
            STANDARD,

            /**
             * Controlled-value or enum field (no n-gram).
             * Generates: {@code <field>} (text), {@code <field>_sort} (string).
             * Auto-analyzers: {@code _sort->keyword};
             * base field defaults to the index default unless overridden via
             * {@link #simple(String, String)}.
             */
            SIMPLE,

            /**
             * Same as {@link #STANDARD} but substitutes an empty-token sentinel
             * ({@link SW360Constants#PROJECT_SEARCH_EMPTY_TOKEN}) when the field is
             * absent/empty, enabling "no value" filter queries.
             */
            EMPTY_AWARE,

            /**
             * Date field stored as a sortable {@code double} (yyyyMMdd integer).
             * Uses the {@code indexDateAsDouble} JS helper.
             */
            DATE,

            /**
             * Create index for numbers as {@code double}.
             */
            DOUBLE,

            /**
             * Create simple index of type {@code string}.
             */
            STRING,

            /**
             * Create {@code default} index of entire object as {@code text}.
             */
            DEFAULT
        }

        private final String fieldName;
        private final Category category;
        @Nullable private final String baseAnalyzerOverride;
        private final int ngramMin;
        private final int ngramMax;

        private IndexField(
                String fieldName, Category category,
                @Nullable String baseAnalyzerOverride, int ngramMin,
                int ngramMax
        ) {
            this.fieldName = fieldName;
            this.category = category;
            this.baseAnalyzerOverride = baseAnalyzerOverride;
            this.ngramMin = ngramMin;
            this.ngramMax = ngramMax;
        }

        // --- Factory methods -------------------------------------------------

        /**
         * Standard text field with prefix search (n-gram min=2, max={@link #EDGE_NGRAM_MAX_LENGTH}).
         * Generates {@code _exact}, {@code _ngram} and {@code _sort} index entries.
         */
        public static @NonNull IndexField standard(String fieldName) {
            return new IndexField(fieldName, Category.STANDARD, null, 2, EDGE_NGRAM_MAX_LENGTH);
        }

        /** Standard text field with a custom n-gram range. */
        public static @NonNull IndexField standard(String fieldName, int ngramMin, int ngramMax) {
            return new IndexField(fieldName, Category.STANDARD, null, ngramMin, ngramMax);
        }

        /**
         * Simple field (base text + {@code _sort} only).
         * The base text field uses the default index analyzer.
         */
        public static IndexField simple(String fieldName) {
            return new IndexField(fieldName, Category.SIMPLE, null, 0, 0);
        }

        /**
         * Simple field with an explicit analyzer for the base text index.
         * Common values: {@code "email"} for email addresses, {@code "keyword"} for enum fields.
         */
        public static IndexField simple(String fieldName, String baseAnalyzer) {
            return new IndexField(fieldName, Category.SIMPLE, baseAnalyzer, 0, 0);
        }

        /**
         * Empty-aware standard field (n-gram min=2, max=50).
         * Documents with a missing/empty field value are indexed under the sentinel token
         * ({@link SW360Constants#PROJECT_SEARCH_EMPTY_TOKEN}) so they can be found via
         * an "empty" filter.
         */
        public static IndexField emptyAware(String fieldName) {
            return new IndexField(fieldName, Category.EMPTY_AWARE, null, 2, 50);
        }

        /** Empty-aware standard field with a custom n-gram range. */
        public static IndexField emptyAware(String fieldName, int ngramMin, int ngramMax) {
            return new IndexField(fieldName, Category.EMPTY_AWARE, null, ngramMin, ngramMax);
        }

        /**
         * Date field stored as a sortable {@code double} (yyyyMMdd integer).
         * Supports range queries like {@code [20240101 TO 20241231]}.
         */
        public static IndexField date(String fieldName) {
            return new IndexField(fieldName, Category.DATE, null, 0, 0);
        }

        /** Number fields index as Nouveau field type {@code double}. */
        public static IndexField doubleField(String fieldName) {
            return new IndexField(fieldName, Category.DOUBLE, null, 0, 0);
        }

        /** String fields index with no extra work. */
        public static IndexField string(String fieldName) {
            return new IndexField(fieldName, Category.STRING, "keyword", 0, 0);
        }

        /** Text index of entire CouchDB Object for generic search. */
        public static IndexField defaultIndex() {
            return new IndexField("default", Category.DEFAULT, null, 0, 0);
        }

        // --- Accessors -------------------------------------------------------

        public String getFieldName() {
            return fieldName;
        }

        public Category getCategory() {
            return category;
        }

        // --- JS snippet generation -------------------------------------------

        /**
         * Returns the JavaScript indexing snippet for this field, ready to be embedded
         * in the CouchDB design-document index function body.
         *
         * @param emptyToken Sentinel value used for absent fields in
         *                   {@link Category#EMPTY_AWARE} (e.g. {@code "__EMPTY__"}).
         */
        public @NonNull String toJsSnippet(String emptyToken) {
            return switch (category) {
                case STANDARD -> String.format(
                    "    if(doc.%1$s !== undefined && doc.%1$s != null && typeof(doc.%1$s) == 'string' && doc.%1$s.length > 0) {" +
                    "      index('text', '%1$s_exact', doc.%1$s);" +
                    "      emitEdgeNGrams('%1$s_ngram', doc.%1$s, %2$d, %3$d);" +
                    "      index('string', '%1$s_sort', doc.%1$s.toLowerCase());" +
                    "    }",
                    fieldName, ngramMin, ngramMax);
                case SIMPLE -> String.format(
                    "    if(doc.%1$s !== undefined && doc.%1$s != null && typeof(doc.%1$s) == 'string' && doc.%1$s.length > 0) {" +
                    "      index('text', '%1$s', doc.%1$s);" +
                    "      index('string', '%1$s_sort', doc.%1$s.toLowerCase());" +
                    "    }",
                    fieldName);
                case EMPTY_AWARE -> String.format(
                    "    var %1$s = '%4$s';" +
                    "    if(doc.%1$s !== undefined && doc.%1$s != null && typeof(doc.%1$s) == 'string' && doc.%1$s.length > 0) {" +
                    "      %1$s = doc.%1$s;" +
                    "    }" +
                    "    index('text', '%1$s_exact', %1$s);" +
                    "    emitEdgeNGrams('%1$s_ngram', %1$s, %2$d, %3$d);" +
                    "    index('string', '%1$s_sort', %1$s.toLowerCase());",
                    fieldName, ngramMin, ngramMax, emptyToken);
                case DATE -> String.format(
                    "    if(doc.%1$s) {" +
                    "      indexDateAsDouble('%1$s', doc.%1$s);" +
                    "    }",
                    fieldName);
                case DOUBLE -> String.format(
                    "    if(doc.%1$s !== undefined && doc.%1$s != null && (typeof(doc.%1$s) == 'number' || typeof(doc.%1$s) == 'bigint')) {" +
                    "      index('double', '%1$s', doc.%1$s);" +
                    "    }",
                    fieldName);
                case STRING -> String.format(
                    "    if(doc.%1$s !== undefined && doc.%1$s != null && typeof(doc.%1$s) == 'string' && doc.%1$s.length > 0) {" +
                    "      index('string', '%1$s', doc.%1$s);" +
                    "    }",
                    fieldName);
                case DEFAULT ->
                    "    var objString = getObjAsString(doc);" +
                    "    if (objString && objString.length > 0) {" +
                    "      index('text', 'default', objString);" +
                    "    }";
            };
        }

        // --- Analyzer contributions ------------------------------------------

        /**
         * Writes analyzer overrides for this field into {@code map}.
         *
         * <ul>
         *   <li>{@link Category#STANDARD} / {@link Category#EMPTY_AWARE}:
         *       {@code _ngram->whitespace, _sort->keyword}</li>
         *   <li>{@link Category#SIMPLE}: {@code _sort->keyword};
         *       base field entry added only when a {@code baseAnalyzerOverride} was given.</li>
         *   <li>{@link Category#STRING}: {@code _sort->keyword};
         *       base field entry added as {@code keyword}.</li>
         *   <li>{@link Category#DATE}, {@link Category#DOUBLE}, {@link Category#DEFAULT}:
         *       no entries (double fields need no text analyzer).</li>
         * </ul>
         */
        public void contributeAnalyzers(Map<String, String> map) {
            switch (category) {
                case STANDARD, EMPTY_AWARE -> {
                    map.put(fieldName + "_ngram", "whitespace");
                    map.put(fieldName + "_sort", "keyword");
                }
                case SIMPLE -> {
                    if (baseAnalyzerOverride != null) {
                        map.put(fieldName, baseAnalyzerOverride);
                    }
                    map.put(fieldName + "_sort", "keyword");
                }
                case STRING -> {
                    map.put(fieldName, "keyword");
                }
                case DATE, DOUBLE, DEFAULT -> { /* double fields need no Lucene text analyzers */ }
            }
        }
    }

    // -------------------------------------------------------------------------
    //  Static builder
    // -------------------------------------------------------------------------

    /**
     * Build a {@link NouveauIndexFunction} from the given field specs plus optional custom JS.
     *
     * <p>The preamble always includes the three JS helper functions
     * ({@code emitEdgeNGrams}, {@code arrayToStringIndex}, {@code indexDateAsDouble}) so that
     * {@code customJs} can call any of them freely. {@code getObjAsString} is added only when
     * one of the {@code fields} is an {@link IndexField#defaultIndex()}.</p>
     *
     * <p>Analyzer priority (highest wins):
     * {@code customAnalyzers} &gt; auto-generated from field specs.</p>
     *
     * @param docType         CouchDB {@code doc.type} value used as an early-exit guard
     *                        (e.g. {@code "project"}). Set to null to skip check.
     * @param emptyToken      Sentinel string for empty-aware fields
     *                        (use {@link SW360Constants#PROJECT_SEARCH_EMPTY_TOKEN}).
     * @param fields          Ordered list of field specs.
     * @param customJs        Raw JS appended after the generated per-field snippets
     *                        (may be {@code null}).
     * @param customAnalyzers Additional per-field analyzer entries; override auto-generated ones.
     * @param defaultAnalyzer Default Lucene analyzer name (e.g. {@code "standard"}).
     * @return A built definition containing the index function and derived query metadata.
     */
    protected static @NonNull BuiltIndexDefinition buildIndexFunction(
            @Nullable String docType,
            String emptyToken,
            @NonNull List<IndexField> fields,
            @Nullable String customJs,
            Map<String, String> customAnalyzers,
            String defaultAnalyzer
    ) {
        StringBuilder js = new StringBuilder("function(doc) {");
        js.append(EMIT_EDGE_N_GRAM_INDEX);
        js.append(OBJ_ARRAY_TO_STRING_INDEX);
        js.append(INDEX_DATE_AS_DOUBLE);
        // `getObjAsString` is only pulled in when a `default` index is requested to avoid
        // changing (and thus re-building) the index functions of handlers not using it.
        if (fields.stream().anyMatch(f -> f.getCategory() == IndexField.Category.DEFAULT)) {
            js.append(OBJ_TO_DEFAULT_INDEX);
        }
        if (docType != null) {
            js.append("  if(!doc.type || doc.type != '").append(docType).append("') return;");
        } else {
            js.append("  if(!doc.type) return;");
        }
        for (IndexField field : fields) {
            js.append(field.toJsSnippet(emptyToken));
        }
        if (customJs != null && !customJs.isEmpty()) {
            js.append(customJs);
        }
        js.append("}");

        // Collect analyzers: field specs first, then custom overrides take precedence
        Map<String, String> analyzers = new LinkedHashMap<>();
        for (IndexField f : fields) {
            f.contributeAnalyzers(analyzers);
        }
        analyzers.putAll(customAnalyzers);

        Set<String> tieredFields = new HashSet<>();
        Set<String> emptyAwareFields = new HashSet<>();
        Set<String> dateFields = new HashSet<>();
        Set<String> defaultFields = new HashSet<>();
        for (IndexField field : fields) {
            switch (field.getCategory()) {
                case STANDARD -> tieredFields.add(field.getFieldName());
                case EMPTY_AWARE -> {
                    tieredFields.add(field.getFieldName());
                    emptyAwareFields.add(field.getFieldName());
                }
                case DATE -> dateFields.add(field.getFieldName());
                case DEFAULT -> defaultFields.add(field.getFieldName());
                case SIMPLE, DOUBLE -> { /* no special query routing required */ }
            }
        }

        NouveauIndexFunction indexFunction = new NouveauIndexFunction(js.toString())
                .setFieldAnalyzer(analyzers)
                .setDefaultAnalyzer(defaultAnalyzer);

        return new BuiltIndexDefinition(indexFunction, tieredFields, emptyAwareFields, dateFields, defaultFields);
    }

    // -------------------------------------------------------------------------
    //  Instance state
    // -------------------------------------------------------------------------

    protected static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "lucene";

    private final Class<T> clazz;
    private final NouveauIndexDesignDocument luceneSearchView;

    /** Set of base field names that have {@code _exact} and {@code _ngram} index variants. */
    private final Set<String> tieredFields;
    /** Subset of tiered fields that use the empty-token substitution. */
    private final Set<String> emptyAwareFields;
    /** Set of field names indexed as sortable double dates. */
    private final Set<String> dateFields;
    /** Created as a default index, so no field name should be used. */
    private final Set<String> defaultFields;

    // -------------------------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------------------------

    /**
     * @param clazz            The type of objects this handler returns.
     * @param indexName        The index name under `_design/lucene`.
     * @param builtIndex       Built index definition from {@link #buildIndexFunction}.
     */
    protected BaseNouveauSearchHandler(
            Class<T> clazz, String indexName, @NonNull BuiltIndexDefinition builtIndex
    ) {
        this.clazz = clazz;
        this.luceneSearchView = new NouveauIndexDesignDocument(indexName, builtIndex.getIndexFunction());
        this.tieredFields = builtIndex.tieredFields;
        this.emptyAwareFields = builtIndex.emptyAwareFields;
        this.dateFields = builtIndex.dateFields;
        this.defaultFields = builtIndex.defaultFields;
    }

    // -------------------------------------------------------------------------
    //  Setup
    // -------------------------------------------------------------------------

    /**
     * Register the CouchDB design document and initialise the connector limit.
     * Call this once in the subclass constructor, passing the same {@code fields} list
     * that was used in {@link #buildIndexFunction}.
     *
     * @param connector Nouveau-aware connector for this handler.
     * @param db        Cloudant database connector for Gson access.
     */
    public NouveauDesignDocument setup(
            @NonNull NouveauLuceneAwareDatabaseConnector connector,
            @NonNull DatabaseConnectorCloudant db
    ) throws IOException {
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
        return searchView;
    }

    // -------------------------------------------------------------------------
    //  Search
    // -------------------------------------------------------------------------

    /**
     * Perform a base search for a given type with the provided field restrictions and pagination.
     *
     * <p>Query routing is metadata-driven from the {@link BuiltIndexDefinition} passed to
     * the constructor.</p>
     *
     * @param connector            Nouveau Aware DB Connector to use.
     * @param subQueryRestrictions Map of field names to their accepted values.
     * @param queryGenerator The query generator function to use for joining the query
     * @param pageData             Pagination information.
     * @return Paginated search results.
     */
    private final @NonNull @Unmodifiable Map<PaginationData, List<T>> genericBaseSearch(
            NouveauLuceneAwareDatabaseConnector connector,
            final @NonNull Map<String, Set<String>> subQueryRestrictions,
            Function<Map<String, Set<String>>, String> queryGenerator,
            PaginationData pageData
    ) {
        List<String> sortColumns = getSortColumns(pageData);

        String query = queryGenerator.apply(subQueryRestrictions);
        Map<PaginationData, List<T>> result = connector.searchView(
                clazz, luceneSearchView.getIndexName(), query, pageData, sortColumns);
        PaginationData respPageData = result.keySet().iterator().next();
        List<T> items = result.values().iterator().next();
        return Collections.singletonMap(respPageData, items);
    }

    /**
     * Perform a AND'd base search for a given type with the provided field restrictions and pagination.
     *
     * <p>Query routing is metadata-driven from the {@link BuiltIndexDefinition} passed to
     * the constructor.</p>
     *
     * @param connector            Nouveau Aware DB Connector to use.
     * @param subQueryRestrictions Map of field names to their accepted values.
     * @param pageData             Pagination information.
     * @return Paginated search results.
     */
    protected final @NonNull @Unmodifiable Map<PaginationData, List<T>> baseSearch(
            NouveauLuceneAwareDatabaseConnector connector,
            final @NonNull Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData
    ) {
        return genericBaseSearch(connector, subQueryRestrictions, this::buildQueryFromRestrictionsWithAnd, pageData);
    }

    /**
     * Perform a AND'd base search with an additional pre-built Lucene query clause AND'd into
     * the final query. Use this overload when the caller has already constructed a query fragment
     * (e.g. a visibility/permission filter) that must be combined with the field restrictions.
     *
     * @param connector            Nouveau Aware DB Connector to use.
     * @param subQueryRestrictions Map of field names to their accepted values.
     * @param additionalQuery      A pre-built Lucene query string AND'd with the restriction query.
     *                             When {@code null} or empty, falls back to the plain
     *                             {@link #baseSearch(NouveauLuceneAwareDatabaseConnector, Map, PaginationData)}.
     * @param pageData             Pagination information.
     * @return Paginated search results.
     */
    protected final @NonNull @Unmodifiable Map<PaginationData, List<T>> baseSearch(
            NouveauLuceneAwareDatabaseConnector connector,
            final @NonNull Map<String, Set<String>> subQueryRestrictions,
            @Nullable String additionalQuery,
            PaginationData pageData
    ) {
        return genericBaseSearch(connector, subQueryRestrictions,
                restrictions -> appendAdditionalQuery(buildQueryFromRestrictionsWithAnd(restrictions), additionalQuery),
                pageData);
    }

    /**
     * Perform a OR'd base search for a given type with the provided field restrictions and pagination.
     *
     * <p>Query routing is metadata-driven from the {@link BuiltIndexDefinition} passed to
     * the constructor.</p>
     *
     * @param connector            Nouveau Aware DB Connector to use.
     * @param subQueryRestrictions Map of field names to their accepted values.
     * @param pageData             Pagination information.
     * @return Paginated search results.
     */
    protected final @NonNull @Unmodifiable Map<PaginationData, List<T>> baseSearchWithOr(
            NouveauLuceneAwareDatabaseConnector connector,
            final @NonNull Map<String, Set<String>> subQueryRestrictions,
            PaginationData pageData
    ) {
        return genericBaseSearch(connector, subQueryRestrictions, this::buildQueryFromRestrictionsWithOr, pageData);
    }

    /**
     * Perform a OR'd base search with an additional pre-built Lucene query clause AND'd into
     * the final query. Use this overload when the caller has already constructed a query fragment
     * (e.g. a visibility/permission filter) that must be combined with the OR'd field restrictions.
     *
     * @param connector            Nouveau Aware DB Connector to use.
     * @param subQueryRestrictions Map of field names to their accepted values.
     * @param additionalQuery      A pre-built Lucene query string AND'd with the OR restriction query.
     *                             When {@code null} or empty, falls back to the plain
     *                             {@link #baseSearchWithOr(NouveauLuceneAwareDatabaseConnector, Map, PaginationData)}.
     * @param pageData             Pagination information.
     * @return Paginated search results.
     */
    protected final @NonNull @Unmodifiable Map<PaginationData, List<T>> baseSearchWithOr(
            NouveauLuceneAwareDatabaseConnector connector,
            final @NonNull Map<String, Set<String>> subQueryRestrictions,
            @Nullable String additionalQuery,
            PaginationData pageData
    ) {
        return genericBaseSearch(connector, subQueryRestrictions,
                restrictions -> appendAdditionalQuery(buildQueryFromRestrictionsWithOr(restrictions), additionalQuery),
                pageData);
    }

    /**
     * AND an extra Lucene clause onto an existing query string.
     * If either part is absent the other is returned unchanged.
     */
    private static @NonNull String appendAdditionalQuery(
            @NonNull String baseQuery, @Nullable String additionalQuery
    ) {
        if (CommonUtils.isNullEmptyOrWhitespace(additionalQuery)) {
            return baseQuery;
        }
        if (baseQuery.isBlank()) {
            return additionalQuery;
        }
        return "(" + baseQuery + ") AND (" + additionalQuery + ")";
    }

    /**
     * Perform a complex search with the provided field restrictions and
     * pagination. The function helps generate results where query needs to
     * contain {@code AND} or {@code OR} joiners.
     *
     * <p>
     * Provide the restrictions in format
     * <pre>
     * {@code
     * {
     *   "OR": {
     *     "field1": "val1",
     *     "field2": "val1"
     *   },
     *   "AND": {
     *     "field3": "val2",
     *     "field4": "val3"
     *   }
     * }
     * }
     * </pre>
     * </p>
     *
     * <p>
     * If the {@code joiner} is {@code AND}, then final query sent to Nouveau
     * will be:
     * {@code (field1:"val1" OR field2:"val1") AND (field3:"val2" AND field4:"val3")}
     * </p>
     *
     * @param connector Nouveau Aware DB Connector to use.
     * @param complexQueryRestrictions Complex map of field names to their accepted values.
     * @param joiner    How to join the query parts?
     * @param pageData  Pagination information.
     * @return Paginated search results.
     */
    protected final @NonNull @Unmodifiable Map<PaginationData, List<T>> complexBaseSearch(
            @NonNull NouveauLuceneAwareDatabaseConnector connector,
            final @NonNull Map<String, Map<String, Set<String>>> complexQueryRestrictions,
            final @NonNull Joiner joiner,
            PaginationData pageData
    ) {
        return complexBaseSearch(connector, complexQueryRestrictions, joiner, null, pageData);
    }

    /**
     * Perform a complex search with an additional pre-built Lucene query clause (e.g. visibility filter)
     * AND'd into the final query.
     */
    protected final @NonNull @Unmodifiable Map<PaginationData, List<T>> complexBaseSearch(
            @NonNull NouveauLuceneAwareDatabaseConnector connector,
            final @NonNull Map<String, Map<String, Set<String>>> complexQueryRestrictions,
            final @NonNull Joiner joiner,
            @Nullable String additionalQuery,
            PaginationData pageData
    ) {
        List<String> sortColumns = getSortColumns(pageData);

        String query = appendAdditionalQuery(
                joiner.join(createComplexQuery(complexQueryRestrictions)),
                additionalQuery
        );

        Map<PaginationData, List<T>> result = connector.searchView(
                clazz, luceneSearchView.getIndexName(), query, pageData, sortColumns);
        PaginationData respPageData = result.keySet().iterator().next();
        List<T> items = result.values().iterator().next();
        return Collections.singletonMap(respPageData, items);
    }

    /**
     * Perform a search returning the raw Nouveau hits instead of deserialized documents.
     *
     * <p>Use this when the result set mixes document types (for example the global search) and
     * therefore cannot be deserialized into a single Thrift type. The documents are shipped
     * along with the hits, so no additional fetch is required.</p>
     *
     * @param connector Nouveau Aware DB Connector to use.
     * @param query     Lucene query, usually built with
     *                  {@link #buildQueryFromRestrictionsWithAnd} or
     *                  {@link #buildQueryFromRestrictionsWithOr}.
     * @param pageData  Pagination information, also used to determine the sort columns.
     * @return The raw Nouveau result or {@code null} if the query was empty or failed.
     */
    protected final @Nullable NouveauResult baseSearchRaw(
            @NonNull NouveauLuceneAwareDatabaseConnector connector,
            String query, @NonNull PaginationData pageData
    ) {
        return connector.searchView(
                luceneSearchView.getIndexName(), query, pageData, getSortColumns(pageData), true);
    }

    /**
     * Simple text search (no pagination) - sanitises the input before forwarding to the connector.
     * Avoid using this function. Use other paginated methods where possible.
     */
    @Deprecated
    public final List<T> search(
            @NonNull NouveauLuceneAwareDatabaseConnector connector, String searchText
    ) {
        PaginationData pageData = NouveauLuceneAwareDatabaseConnector.pageDataForAllRecords();
        Map<PaginationData, List<T>> result = connector.searchView(clazz,
                luceneSearchView.getIndexName(),
                sanitizeLuceneString(searchText), pageData, null);
        return NouveauLuceneAwareDatabaseConnector.convertPaginatorToList(result);
    }

    protected final String getIndexName() {
        return luceneSearchView.getIndexName();
    }

    // -------------------------------------------------------------------------
    //  Metadata-driven query building
    // -------------------------------------------------------------------------

    /**
     * Build a Lucene query parts which can be joined based on condition
     * to from the final query.
     *
     * @param restrictions Map of {@code fieldName -> filterValue} (multi-value sets should
     *                     already be joined into a single string by the caller).
     * @return A list of Lucene query parts which needs to be joined before
     *         they can be passed directly to
     *         {@link NouveauLuceneAwareDatabaseConnector#searchView}.
     */
    private @NonNull List<String> buildQueryFromRestrictions(
            @NonNull Map<String, Set<String>> restrictions
    ) {
        List<String> parts = new ArrayList<>();
        for (var entry : restrictions.entrySet()) {
            String fieldName = entry.getKey();
            Set<String> filterValue = entry.getValue();
            parts.add(createFieldQueryRestriction(fieldName, filterValue));
        }
        return parts;
    }

    /**
     * Create query for complex scenarios based on "OR" or "AND" keys for Nouveau queries.
     * For example input like bellow:<br />
     * <pre>
     * {@code
     * {
     *   "OR": {
     *     "field1": "val1",
     *     "field2": "val1"
     *   },
     *   "AND": {
     *     "field3": "val2"
     *   }
     * }
     * }
     * </pre>
     * You will get the output:
     * <pre>
     * {@code
     * List<String> query = [
     *   "(( field1:"val1" ) OR ( field2:"val1" ))",
     *   "( field3:"val2" )"
     * ]
     * }
     * </pre>
     * This can later be joined using another joiner like and to get final query:
     * {@code (( field1:"val1" ) OR ( field2:"val1" )) AND
     *  ( field3:"val2" )}
     * @param subQueryRestrictions Restrictions with joiner. See description for example.
     * @return List of queries
     */
    private @NonNull @Unmodifiable List<String> createComplexQuery(
            @NonNull Map<String, Map<String, Set<String>>> subQueryRestrictions
    ) {
        List<String> subQueries = new ArrayList<>();
        for (Map.Entry<String, Map<String, Set<String>>> restriction : subQueryRestrictions.entrySet()) {
            boolean isPlural = restriction.getValue().size() > 1;
            String query = switch (restriction.getKey()) {
                case "OR" -> buildQueryFromRestrictionsWithOr(restriction.getValue());
                case "AND" -> buildQueryFromRestrictionsWithAnd(restriction.getValue());
                default -> null;
            };
            if (query == null) {
                continue;
            }
            if (isPlural) {
                subQueries.add("(" + query + ")");
            } else {
                subQueries.add(query);
            }
        }
        return subQueries;
    }

    /**
     * Build a Lucene query using AND joiner from a simplified restriction map, using registered
     * field metadata for per-field routing.
     *
     * @param restrictions Map of {@code fieldName -> filterValue} (multi-value sets should
     *                     already be joined into a single string by the caller).
     * @return A Lucene query string that can be passed directly to
     *         {@link NouveauLuceneAwareDatabaseConnector#searchView}.
     */
    protected final @NonNull String buildQueryFromRestrictionsWithAnd(
            @NonNull Map<String, Set<String>> restrictions
    ) {
        return AND.join(buildQueryFromRestrictions(restrictions));
    }

    /**
     * Build a Lucene query string from a simplified restriction map OR'd, using registered
     * field metadata for per-field routing.
     *
     * @param restrictions Map of {@code fieldName -> filterValue} (multi-value sets should
     *                     already be joined into a single string by the caller).
     * @return A Lucene query string that can be passed directly to
     *         {@link NouveauLuceneAwareDatabaseConnector#searchView}.
     */
    protected final @NonNull String buildQueryFromRestrictionsWithOr(
            @NonNull Map<String, Set<String>> restrictions
    ) {
        return OR.join(buildQueryFromRestrictions(restrictions));
    }

    /**
     * Build a single Lucene restriction clause for one field-value pair.
     *
     * <p>Routing order:
     * <ol>
     *   <li>Empty-aware field with the empty-token sentinel -> exact {@code _exact} lookup.</li>
     *   <li>Tiered field -> n-gram / exact / sort query via
     *       {@link NouveauLuceneAwareDatabaseConnector#buildFieldQuery}.</li>
     *   <li>Date field -> value formatted as yyyyMMdd double via
     *       {@link NouveauLuceneAwareDatabaseConnector#formatDateNouveauFormat}.</li>
     *   <li>All other fields -> simple quoted term on the base field.</li>
     * </ol>
     * </p>
     */
    private String createFieldQueryRestriction(String fieldName, Set<String> filterValues) {
        List<String> queries = new ArrayList<>();
        for (String filterValue : filterValues) {
            // Empty-aware: must query the _exact sub-field (the base field is not indexed)
            if (emptyAwareFields.contains(fieldName)
                    && SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN.equals(filterValue)) {
                queries.add(fieldName + "_exact:\"" + SW360Constants.PROJECT_SEARCH_EMPTY_TOKEN + "\"");
                continue;
            }

            String sanitized = sanitizeLuceneString(filterValue);
            boolean quoted = NouveauLuceneAwareDatabaseConnector.isValidQuotedPhrase(filterValue);
            String baseSearch = "(" + fieldName + (quoted ? ":" : ":\"") + sanitized + (quoted ? "" : "\"") + ")";

            if (tieredFields.contains(fieldName)) {
                if (!quoted) {
                    queries.add("(" + NouveauLuceneAwareDatabaseConnector.buildFieldQuery(
                            fieldName + "_exact", fieldName + "_ngram", filterValue, true) + ")");
                    continue;
                }
                // Quoted input -> exact sort-field look-up (keyword field stores lowercased value)
                queries.add("(" + fieldName + "_sort:" + sanitized.toLowerCase(Locale.ROOT) + ")");
                continue;
            }

            if (dateFields.contains(fieldName)) {
                try {
                    queries.add("(" + fieldName + ":" + NouveauLuceneAwareDatabaseConnector
                            .formatDateNouveauFormat(filterValue.trim()) + ")");
                    continue;
                } catch (ParseException e) {
                    queries.add(baseSearch);
                    continue;
                }
            }

            if (defaultFields.contains(fieldName)) {
                queries.add("( " + convertToFreeSearch(filterValue) + " )");
                continue;
            }
            queries.add(baseSearch);
        }

        StringBuilder query = new StringBuilder();
        if (queries.size() > 1) {
            query.append("(");
            query.append(String.join(" OR ", queries));
            query.append(")");
        } else {
            query.append(queries.getFirst());
        }
        return query.toString();
    }

    // -------------------------------------------------------------------------
    //  Sorting
    // -------------------------------------------------------------------------

    /**
     * Return the sort column list for the given pagination state.
     *
     * @param pageData Pagination data from the request.
     * @return Sort column names with direction ({@code -} prefix = descending).
     */
    protected final @NonNull @Unmodifiable List<String> getSortColumns(@NonNull PaginationData pageData) {
        List<String> columns = mapSortColumn(pageData.getSortColumnNumber());
        return columns.stream().map(c -> {
            if (!pageData.isAscending()) {
                if (c.startsWith("-")) {
                    return c.substring(1);
                }
                return "-" + c;
            }
            return c;
        }).toList();
    }

    /**
     * Map a sort column number (from the UI/API) to a list of Lucene sort fields.
     *
     * <p>Return sort fields in priority order. Prefix a field with {@code "-"} to indicate
     * descending-by-default direction (the direction is flipped automatically by
     * {@link #getSortColumns} when the client requests ascending order). Do <em>not</em>
     * prefix {@link org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector#SCORE_SORTING_FIELD}.</p>
     *
     * <p>Example implementation:
     * <pre>{@code
     * return switch (ProjectSortColumn.findByValue(sortColumnNumber)) {
     *     case BY_NAME -> List.of("name_sort", "-version_sort", "-createdOn");
     *     case BY_CREATEDON -> List.of("createdOn");
     *     case null, default -> List.of(SCORE_SORTING_FIELD);
     * };
     * }</pre>
     * </p>
     */
    protected abstract @NonNull @Unmodifiable List<String> mapSortColumn(int sortColumnNumber);
}
