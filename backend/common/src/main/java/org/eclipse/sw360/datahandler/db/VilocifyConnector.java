/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.google.common.annotations.VisibleForTesting;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VendorAdvisory;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Client for the Vilocify v2 JSON:API.
 *
 * <p>Uses a shared pooled {@link CloseableHttpClient} from {@link VilocifyHttpClientFactory},
 * supports dynamic configuration lookup from {@code sw360_config}, cursor pagination,
 * {@code include} compound documents, and automatic retry on HTTP 429 rate-limiting.
 */
public class VilocifyConnector {

    public static final String PROVIDER_SVM = "svm";
    public static final String PROVIDER_VILOCIFY = "vilocify";
    private static final Logger log = LogManager.getLogger(VilocifyConnector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JSON_API_MEDIA_TYPE = "application/vnd.api+json";
    private static final ContentType JSON_API_CONTENT_TYPE =
            ContentType.create(JSON_API_MEDIA_TYPE, StandardCharsets.UTF_8);
    private static final String DEFAULT_API_BASE_URL = "https://portal.vilocify.com";
    private static final String DEFAULT_API_ROOT_PATH = "/api/v2";
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_RATE_LIMIT_RETRIES = 5;
    private static final long RATE_LIMIT_BACKOFF_MS = 1000L;

    /**
     * PURL ecosystem type to Vilocify component name prefix mapping
     * (mirrors {@code vilocify-sdk-python/vilocify/match.py}).
     */
    private static final Map<String, String> PURL_TYPES = Map.ofEntries(
            Map.entry("cargo", "Rust Crate"),
            Map.entry("composer", "PHP Package"),
            Map.entry("cpan", "Perl Module"),
            Map.entry("conda", "Anaconda Package"),
            Map.entry("cran", "R Project Package"),
            Map.entry("gem", "RubyGem"),
            Map.entry("golang", "Go Package"),
            Map.entry("hackage", "Haskell Package"),
            Map.entry("npm", "Node.js Package"),
            Map.entry("nuget", "NuGet Package"),
            Map.entry("pub", "Dart Package"),
            Map.entry("pypi", "Python Package"),
            Map.entry("swift", "Swift Package"),
            Map.entry("vscode-extension", "Visual Studio Code Extension")
    );

    private final String baseUrl;
    private final String rootPath;
    private final String token;
    private final boolean testingMode;

    public VilocifyConnector() {
        this.baseUrl = trimToEmpty(readDynamicConfig(
                SW360ConfigKeys.VILOCIFY_API_BASE_URL, DEFAULT_API_BASE_URL));
        this.rootPath = normalizePath(readDynamicConfig(
                SW360ConfigKeys.VILOCIFY_API_ROOT_PATH, DEFAULT_API_ROOT_PATH));
        this.token = trimToEmpty(readDynamicConfig(
                SW360ConfigKeys.VILOCIFY_API_TOKEN, ""));
        this.testingMode = false;

        validateExclusiveProvider();
    }

    /**
     * Package-private constructor for unit tests. Allows tests to point the
     * connector at a local HTTP server without querying {@code sw360_config}.
     */
    @VisibleForTesting
    VilocifyConnector(String baseUrl, String rootPath, String token) {
        this.baseUrl = trimToEmpty(baseUrl);
        this.rootPath = normalizePath(rootPath != null ? rootPath : DEFAULT_API_ROOT_PATH);
        this.token = trimToEmpty(token);
        this.testingMode = true;
    }

    /**
     * Returns the currently configured sync provider ({@code "svm"} or {@code "vilocify"})
     * from {@code sw360_config}.
     */
    public static String getActiveProvider() {
        String value = readDynamicConfig(SW360ConfigKeys.SYNC_INTEGRATION_PROVIDER, PROVIDER_SVM);
        return trimToEmpty(value);
    }

    public static String getSchedulerEmail() {
        String value = readDynamicConfig(SW360ConfigKeys.VILOCIFY_SCHEDULER_EMAIL, "");
        return trimToEmpty(value);
    }

    public static boolean isVilocifyProviderActive() {
        return PROVIDER_VILOCIFY.equalsIgnoreCase(getActiveProvider());
    }

    public static String buildMonitoringListName(String projectName, String projectVersion, String projectId) {
        String safeName = CommonUtils.isNotNullEmptyOrWhitespace(projectName) ? projectName : "SW360 Project";
        if (CommonUtils.isNullEmptyOrWhitespace(projectVersion)) {
            return safeName + " [SW360:" + projectId + "]";
        }
        return safeName + " (" + projectVersion + ") [SW360:" + projectId + "]";
    }

    public static String buildMonitoringListComment(String businessUnit, String projectId) {
        if (CommonUtils.isNullEmptyOrWhitespace(businessUnit)) {
            return "SW360 project id: " + projectId;
        }
        return "SW360 project id: " + projectId + ", business unit: " + businessUnit;
    }

    /**
     * Extracts the PURL or URL from {@code release.externalIds} under {@code package-url} or {@code purl.id}.
     */
    public static String extractReleasePurlOrUrl(Release release) {
        if (release == null || !release.isSetExternalIds()) {
            return null;
        }
        String packageUrl = release.getExternalIds().get(SW360Constants.PACKAGE_URL);
        if (CommonUtils.isNullEmptyOrWhitespace(packageUrl)) {
            packageUrl = release.getExternalIds().get(SW360Constants.PURL_ID);
        }
        return CommonUtils.isNotNullEmptyOrWhitespace(packageUrl) ? packageUrl.trim() : null;
    }

    /**
     * Builds Vilocify {@code /components} query filter parameters from a PURL or URL string.
     */
    @VisibleForTesting
    static Map<String, String> buildComponentFilterParamsFromPurl(String purlOrUrl, String fallbackReleaseVersion) {
        if (CommonUtils.isNullEmptyOrWhitespace(purlOrUrl)) {
            return Collections.emptyMap();
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("filter[active][eq]", "true");

        if (purlOrUrl.startsWith("pkg:")) {
            try {
                PackageURL purl = new PackageURL(purlOrUrl);
                String type = purl.getType() != null ? purl.getType().toLowerCase() : "";
                String version = CommonUtils.isNotNullEmptyOrWhitespace(purl.getVersion())
                        ? purl.getVersion().replaceFirst("^[vV]", "")
                        : trimToEmpty(fallbackReleaseVersion);

                if ("github".equals(type) && CommonUtils.isNotNullEmptyOrWhitespace(purl.getNamespace())) {
                    params.put("filter[url][like]",
                            "https://github.com/" + purl.getNamespace() + "/" + purl.getName());
                    if (CommonUtils.isNotNullEmptyOrWhitespace(version)) {
                        params.put("filter[version][eq]", version);
                    }
                    return params;
                }

                String prefix = PURL_TYPES.get(type);
                if (CommonUtils.isNotNullEmptyOrWhitespace(prefix)) {
                    String ns = CommonUtils.isNotNullEmptyOrWhitespace(purl.getNamespace())
                            ? purl.getNamespace() + "/"
                            : "";
                    params.put("filter[name][eq]", prefix + ": " + ns + purl.getName());
                    if (CommonUtils.isNotNullEmptyOrWhitespace(version)) {
                        params.put("filter[version][eq]", version);
                    }
                    return params;
                }
            } catch (MalformedPackageURLException e) {
                log.debug("Could not parse PURL '{}', falling back to URL filter", purlOrUrl, e);
            }
        }

        // Fallback for non-standard PURL or direct URL
        params.put("filter[url][like]", purlOrUrl);
        return params;
    }

    @VisibleForTesting
    static Optional<String> extractResourceId(JsonNode response) {
        JsonNode data = response == null ? null : response.path("data");
        if (data == null || data.isMissingNode() || data.isNull()) {
            return Optional.empty();
        }
        if (data.isArray()) {
            for (JsonNode entry : data) {
                Optional<String> id = extractResourceIdFromObject(entry);
                if (id.isPresent()) {
                    return id;
                }
            }
            return Optional.empty();
        }
        return extractResourceIdFromObject(data);
    }

    private static Optional<String> extractResourceIdFromObject(JsonNode data) {
        JsonNode idNode = data == null ? null : data.path("id");
        return idNode == null || idNode.isMissingNode() || idNode.isNull()
                ? Optional.empty()
                : Optional.of(idNode.asText());
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static String normalizePath(String path) {
        if (CommonUtils.isNullEmptyOrWhitespace(path)) {
            return "";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static Iterable<JsonNode> dataIterable(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return Collections.emptyList();
        }
        List<JsonNode> list = new ArrayList<>();
        for (JsonNode n : arrayNode) {
            if (n != null && !n.isNull()) {
                list.add(n);
            }
        }
        return list;
    }

    private static String readDynamicConfig(String key, String fallbackValue) {
        try {
            String value = SW360Utils.readConfig(key, fallbackValue);
            return CommonUtils.isNotNullEmptyOrWhitespace(value) ? value : fallbackValue;
        } catch (Exception e) {
            return fallbackValue;
        }
    }

    public boolean isConfigured() {
        if (!testingMode) {
            String provider = getActiveProvider();
            if (CommonUtils.isNotNullEmptyOrWhitespace(provider) && PROVIDER_SVM.equalsIgnoreCase(provider)) {
                return false;
            }
        }
        return CommonUtils.isNotNullEmptyOrWhitespace(baseUrl)
                && CommonUtils.isNotNullEmptyOrWhitespace(token);
    }

    /**
     * Legacy JSON-based entrypoint retained for backwards-compatible callers;
     * prefer {@link ProjectDatabaseHandler#exportProjectsToVilocifyMonitoringLists()}
     * which syncs {@code Project} entities directly without intermediate JSON.
     */
    public RequestStatus sendProjectExportForMonitoringLists(String jsonString) throws IOException, SW360Exception {
        if (isUnavailable(jsonString)) {
            return RequestStatus.SUCCESS;
        }

        JsonNode projects = MAPPER.readTree(jsonString);
        if (!projects.isArray()) {
            throw new SW360Exception("Invalid project export payload: expected JSON array");
        }

        Map<String, String> listIdsByApplicationId = new HashMap<>();

        for (JsonNode projectNode : projects) {
            String applicationId = readText(projectNode, "application_id", null);
            if (CommonUtils.isNullEmptyOrWhitespace(applicationId)) {
                continue;
            }

            String listName = buildMonitoringListName(
                    readText(projectNode, "application_name", "SW360 Project"),
                    readText(projectNode, "application_version", ""),
                    applicationId);
            String businessUnit = readText(projectNode, "business_unit", null);
            String listComment = buildMonitoringListComment(businessUnit, applicationId);

            Optional<String> listId = findMonitoringListIdForProject(applicationId, listName);
            if (listId.isEmpty()) {
                listId = createMonitoringList(listName, listComment, businessUnit);
            }
            if (listId.isEmpty()) {
                continue;
            }

            String resolvedListId = listId.get();
            updateMonitoringList(resolvedListId, listName, listComment);
            listIdsByApplicationId.put(applicationId, resolvedListId);
        }

        for (JsonNode projectNode : projects) {
            String applicationId = readText(projectNode, "application_id", null);
            if (CommonUtils.isNullEmptyOrWhitespace(applicationId)) {
                continue;
            }

            String listId = listIdsByApplicationId.get(applicationId);
            if (CommonUtils.isNullEmptyOrWhitespace(listId)) {
                continue;
            }

            List<String> userRelationshipIds = resolveMembershipRelationshipIds(
                    extractStringArray(projectNode.path("user_gids")));
            replaceMonitoringListUsers(listId, userRelationshipIds);

            List<String> componentRelationshipIds = extractComponentIds(projectNode.path("components"));
            replaceMonitoringListComponents(listId, componentRelationshipIds);

            List<String> childRelationshipIds = resolveChildMonitoringListIds(
                    projectNode.path("children_application_ids"),
                    projectNode.path("svm_children_ml_ids"),
                    listIdsByApplicationId);
            replaceMonitoringListChildren(listId, childRelationshipIds);
        }

        log.info("Vilocify monitoring list synchronization completed for {} projects", listIdsByApplicationId.size());
        return RequestStatus.SUCCESS;
    }

    /**
     * Searches for an existing monitoringList by exact name using Vilocify's
     * server-side {@code filter[name][eq]} parameter. Does not paginate through
     * all monitoring lists.
     */
    public Optional<String> findMonitoringListIdForProject(String applicationId, String listName)
            throws IOException, SW360Exception {
        if (isUnavailable(applicationId) || CommonUtils.isNullEmptyOrWhitespace(listName)) {
            return Optional.empty();
        }
        return firstResourceId("/monitoringLists", Map.of("filter[name][eq]", listName.trim()));
    }

    private List<String> extractStringArray(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            if (node == null || node.isNull()) {
                continue;
            }
            String value = node.asText(null);
            if (CommonUtils.isNotNullEmptyOrWhitespace(value)) {
                values.add(value.trim());
            }
        }
        return values;
    }

    private List<String> resolveMembershipRelationshipIds(List<String> rawValues)
            throws IOException, SW360Exception {
        if (rawValues == null || rawValues.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> membershipIds = new ArrayList<>();
        for (String rawValue : rawValues) {
            if (CommonUtils.isNullEmptyOrWhitespace(rawValue)) {
                continue;
            }

            if (!rawValue.contains("@")) {
                membershipIds.add(rawValue);
                continue;
            }

            Optional<String> membershipId = getOrCreateMembershipIdByEmail(rawValue, null);
            membershipId.ifPresent(membershipIds::add);
        }
        return membershipIds;
    }

    /**
     * Resolves a Vilocify membership UUID for the given email address: searches via
     * {@code GET /api/v2/memberships} first and creates a new membership via
     * {@code POST /api/v2/memberships} if none exists.
     */
    public Optional<String> getOrCreateMembershipIdByEmail(String email, String userName)
            throws IOException, SW360Exception {
        if (isUnavailable(email)) {
            return Optional.empty();
        }
        Optional<String> existing = getMembershipIdByEmail(email);
        if (existing.isPresent()) {
            return existing;
        }
        return createMembership(email, userName);
    }

    private List<String> extractComponentIds(JsonNode componentsArray) {
        if (!componentsArray.isArray()) {
            return Collections.emptyList();
        }

        List<String> componentIds = new ArrayList<>();
        for (JsonNode component : dataIterable(componentsArray)) {
            String componentId = readText(component, "vilocify_component_id",
                    readText(component, "svm_component_id", null));
            if (CommonUtils.isNotNullEmptyOrWhitespace(componentId)) {
                componentIds.add(componentId.trim());
            }
        }
        return componentIds;
    }

    private List<String> resolveChildMonitoringListIds(JsonNode childApplicationIds,
                                                       JsonNode existingMonitoringListIds,
                                                       Map<String, String> listIdsByApplicationId) {
        Set<String> childIds = new LinkedHashSet<>();

        if (childApplicationIds.isArray()) {
            for (JsonNode appIdNode : childApplicationIds) {
                if (appIdNode == null || appIdNode.isNull()) {
                    continue;
                }
                String appId = appIdNode.asText(null);
                if (CommonUtils.isNullEmptyOrWhitespace(appId)) {
                    continue;
                }
                String listId = listIdsByApplicationId.get(appId.trim());
                if (CommonUtils.isNotNullEmptyOrWhitespace(listId)) {
                    childIds.add(listId);
                }
            }
        }

        if (existingMonitoringListIds.isArray()) {
            for (JsonNode listIdNode : existingMonitoringListIds) {
                if (listIdNode == null || listIdNode.isNull()) {
                    continue;
                }
                String listId = listIdNode.asText(null);
                if (CommonUtils.isNotNullEmptyOrWhitespace(listId)) {
                    childIds.add(listId.trim());
                }
            }
        }

        return new ArrayList<>(childIds);
    }

    /**
     * Resolves the Vilocify component ID for a given SW360 {@link Release} ONLY if the
     * release has a Package URL (PURL) or URL configured in {@code externalIds}.
     *
     * <p>Never paginates through unfiltered {@code /components}:
     * <ul>
     *   <li>If the release has no PURL/URL in {@code externalIds}, returns {@link Optional#empty()}
     *       immediately without making any HTTP request.</li>
     *   <li>If the value is a valid {@code pkg:...} PURL, translates it to Vilocify's
     *       {@code filter[name][eq]} / {@code filter[url][like]} + {@code filter[version][eq]}
     *       + {@code filter[active][eq]=true} parameters (per {@code vilocify-sdk-python/vilocify/match.py}).</li>
     *   <li>If the value is an HTTP/HTTPS URL, queries {@code /components} with
     *       {@code filter[url][like]=<url>} and {@code filter[active][eq]=true}.</li>
     * </ul>
     */
    public Optional<String> findComponentIdByRelease(Release release) throws IOException, SW360Exception {
        return findComponentAsVmComponentByRelease(release).map(VMComponent::getVmid);
    }

    /**
     * Resolves a Vilocify component for a given SW360 {@link Release} via PURL/URL filter
     * and maps the returned JSON:API resource directly to a {@link VMComponent}.
     */
    public Optional<VMComponent> findComponentAsVmComponentByRelease(Release release)
            throws IOException, SW360Exception {
        if (!isConfigured() || release == null) {
            return Optional.empty();
        }

        String packageUrl = extractReleasePurlOrUrl(release);
        if (CommonUtils.isNullEmptyOrWhitespace(packageUrl)) {
            return Optional.empty();
        }

        Map<String, String> filterParams = buildComponentFilterParamsFromPurl(packageUrl.trim(), release.getVersion());
        if (filterParams.isEmpty()) {
            return Optional.empty();
        }

        JsonNode response = getJson("/components", filterParams);
        JsonNode data = response.path("data");
        if (data.isArray() && !data.isEmpty()) {
            return mapComponentNodeToVmComponent(data.get(0));
        }
        return Optional.empty();
    }

    /**
     * Fetches a single Vilocify component by ID ({@code GET /api/v2/components/{id}})
     * and maps it to a {@link VMComponent}.
     */
    public Optional<VMComponent> getComponentAsVmComponent(String componentId)
            throws IOException, SW360Exception {
        if (isUnavailable(componentId)) {
            return Optional.empty();
        }
        Optional<JsonNode> responseOpt = getJsonAllowNotFound(
                "/components/" + componentId.trim(), Collections.emptyMap());
        if (responseOpt.isEmpty()) {
            return Optional.empty();
        }
        JsonNode data = responseOpt.get().path("data");
        return mapComponentNodeToVmComponent(data);
    }

    /**
     * Fetches multiple Vilocify components in batches using {@code filter[id][in]}
     * (and optionally {@code filter[updatedAt][after]} for delta sync) and maps them
     * to {@link VMComponent} objects.
     */
    public List<VMComponent> getComponentsAsVmComponentsByIds(
            java.util.Collection<String> componentIds, String updatedAfter)
            throws IOException, SW360Exception {
        if (isUnavailable() || componentIds == null || componentIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> validIds = componentIds.stream()
                .filter(CommonUtils::isNotNullEmptyOrWhitespace)
                .map(String::trim)
                .distinct()
                .toList();
        if (validIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<VMComponent> results = new ArrayList<>();
        int chunkSize = 100;
        for (int i = 0; i < validIds.size(); i += chunkSize) {
            List<String> chunk = validIds.subList(i, Math.min(i + chunkSize, validIds.size()));
            Map<String, String> params = new HashMap<>();
            params.put("filter[id][in]", String.join(",", chunk));
            if (CommonUtils.isNotNullEmptyOrWhitespace(updatedAfter)) {
                params.put("filter[updatedAt][after]", updatedAfter.trim());
            }
            JsonNode page = getJson("/components", params);
            while (true) {
                JsonNode data = page.path("data");
                if (data.isArray()) {
                    for (JsonNode compNode : data) {
                        mapComponentNodeToVmComponent(compNode).ifPresent(results::add);
                    }
                }
                String nextUrl = getNextLink(page);
                if (CommonUtils.isNullEmptyOrWhitespace(nextUrl)) {
                    break;
                }
                page = getJsonByAbsoluteUrl(nextUrl);
            }
        }
        return results;
    }

    private Optional<VMComponent> mapComponentNodeToVmComponent(JsonNode compNode) {
        if (compNode == null || compNode.isMissingNode() || compNode.isNull()) {
            return Optional.empty();
        }
        String vmid = readText(compNode, "id", null);
        if (CommonUtils.isNullEmptyOrWhitespace(vmid)) {
            return Optional.empty();
        }
        JsonNode attributes = compNode.path("attributes");
        VMComponent vmComponent = new VMComponent(SW360Utils.getCreatedOnTime(), vmid);
        vmComponent.setName(readText(attributes, "name", null));
        vmComponent.setVersion(readText(attributes, "version", null));
        vmComponent.setVendor(readText(attributes, "vendor", null));
        vmComponent.setUrl(readText(attributes, "url", null));
        vmComponent.setEolReached(attributes.path("endOfLife").asBoolean(false));
        vmComponent.setLastUpdateDate(readText(attributes, "updatedAt", SW360Utils.getCreatedOnTime()));
        return Optional.of(vmComponent);
    }

    /**
     * Creates a {@code componentRequest} in Vilocify via {@code POST /api/v2/componentRequests}.
     *
     * <p><b>Vilocify API Validation Requirements (avoids HTTP 422 Unprocessable Content):</b>
     * <ol>
     *   <li>{@code name} (required, non-blank string)</li>
     *   <li>{@code version} (required, non-blank string)</li>
     *   <li>At least one of {@code vendor}, {@code componentUrl}, or {@code securityUrl}
     *       must be non-blank.</li>
     * </ol>
     * These checks are performed locally before making any HTTP call. If a release does not
     * satisfy these preconditions, a warning is logged and {@link Optional#empty()} is returned.
     */
    public Optional<String> createComponentRequest(Release release) throws IOException, SW360Exception {
        if (isUnavailable() || release == null) {
            return Optional.empty();
        }

        String name = trimToEmpty(release.getName());
        String version = trimToEmpty(release.getVersion());
        String vendorName = (release.isSetVendor() && release.getVendor() != null)
                ? (CommonUtils.isNotNullEmptyOrWhitespace(release.getVendor().getFullname())
                ? release.getVendor().getFullname().trim()
                : trimToEmpty(release.getVendor().getShortname()))
                : "";
        String componentUrl = trimToEmpty(extractReleasePurlOrUrl(release));
        String securityUrl = CommonUtils.isNotNullEmptyOrWhitespace(release.getSourceCodeDownloadurl())
                ? release.getSourceCodeDownloadurl().trim()
                : trimToEmpty(release.getBinaryDownloadurl());

        List<String> missingFields = new ArrayList<>();
        if (CommonUtils.isNullEmptyOrWhitespace(name)) {
            missingFields.add("name");
        }
        if (CommonUtils.isNullEmptyOrWhitespace(version)) {
            missingFields.add("version");
        }
        if (CommonUtils.isNullEmptyOrWhitespace(vendorName)
                && CommonUtils.isNullEmptyOrWhitespace(componentUrl)
                && CommonUtils.isNullEmptyOrWhitespace(securityUrl)) {
            missingFields.add("vendor|componentUrl|securityUrl");
        }

        if (!missingFields.isEmpty()) {
            log.warn("Release {} cannot create componentRequest in Vilocify. Missing required fields: {}",
                    release.getId(), String.join(", ", missingFields));
            return Optional.empty();
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", name);
        attributes.put("version", version);
        if (CommonUtils.isNotNullEmptyOrWhitespace(vendorName)) {
            attributes.put("vendor", vendorName);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(componentUrl)) {
            attributes.put("componentUrl", componentUrl);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(securityUrl)) {
            attributes.put("securityUrl", securityUrl);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("type", "componentRequests");
        data.put("attributes", attributes);
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);

        JsonNode response = postJson("/componentRequests", payload);
        return extractResourceId(response);
    }

    public Optional<String> resolveMappedComponentId(String requestId) throws IOException, SW360Exception {
        if (isUnavailable(requestId)) {
            return Optional.empty();
        }
        JsonNode response = getSingleResourceJson("/componentRequests/" + requestId, Collections.emptyMap());
        JsonNode data = response.path("data");
        JsonNode relationships = data.path("relationships");
        JsonNode component = relationships.path("component").path("data");
        if (component.isMissingNode() || component.isNull()) {
            return Optional.empty();
        }
        JsonNode componentId = component.path("id");
        return componentId.isMissingNode() || componentId.isNull()
                ? Optional.empty()
                : Optional.of(componentId.asText());
    }

    /**
     * Check the status of a componentRequest and return the component ID if mapped.
     *
     * <p>This method is intentionally non-destructive: it does not delete the
     * componentRequest. Deletion is performed by the caller only after SW360
     * persistence succeeds.
     */
    public Optional<String> checkAndResolveComponentRequest(String requestId) throws IOException, SW360Exception {
        if (isUnavailable(requestId)) {
            return Optional.empty();
        }

        JsonNode response = getSingleResourceJson("/componentRequests/" + requestId, Collections.emptyMap());
        JsonNode data = response.path("data");
        JsonNode attributes = data.path("attributes");
        String state = attributes.path("state").asText();

        if ("mapped".equalsIgnoreCase(state)) {
            log.info("ComponentRequest {} is mapped, extracting component ID", requestId);
            JsonNode relationships = data.path("relationships");
            JsonNode component = relationships.path("component").path("data");

            if (component.isMissingNode() || component.isNull()) {
                log.warn("ComponentRequest {} is mapped but no component relationship found", requestId);
                return Optional.empty();
            }

            JsonNode componentIdNode = component.path("id");
            if (componentIdNode.isMissingNode() || componentIdNode.isNull()) {
                log.warn("ComponentRequest {} is mapped but component ID is null", requestId);
                return Optional.empty();
            }

            return Optional.of(componentIdNode.asText());
        } else {
            log.debug("ComponentRequest {} is in state: {}, not yet mapped", requestId, state);
            return Optional.empty();
        }
    }

    /**
     * Delete a componentRequest from Vilocify.
     */
    public boolean deleteComponentRequest(String requestId) throws IOException, SW360Exception {
        if (isUnavailable(requestId)) {
            return false;
        }

        String path = "/componentRequests/" + requestId;
        URI uri = buildUri(path, Collections.emptyMap(), false);
        HttpDelete request = new HttpDelete(uri);
        addDefaultHeaders(request);

        return executeStatusCode(request, (statusCode, reasonPhrase) -> {
            if (statusCode == 204 || statusCode == 200) {
                log.info("Successfully deleted componentRequest: {}", requestId);
                return true;
            } else if (statusCode == 404) {
                log.warn("ComponentRequest {} not found (404), may already be deleted", requestId);
                return true;
            } else {
                log.error("Failed to delete componentRequest {}: {} {}", requestId, statusCode, reasonPhrase);
                return false;
            }
        });
    }

    public Optional<String> getMembershipIdByEmail(String email) throws IOException, SW360Exception {
        if (isUnavailable(email)) {
            return Optional.empty();
        }
        return firstResourceId("/memberships", Map.of("filter[email]", email.trim()));
    }

    public Optional<String> createMembership(String email) throws IOException, SW360Exception {
        return createMembership(email, null);
    }

    public Optional<String> createMembership(String email, String userName) throws IOException, SW360Exception {
        if (isUnavailable(email)) {
            return Optional.empty();
        }
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "memberships");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email.trim());
        attributes.put("userEmail", email.trim());
        if (CommonUtils.isNotNullEmptyOrWhitespace(userName)) {
            attributes.put("userName", userName.trim());
        }
        attributes.put("role", "user");
        data.put("attributes", attributes);
        payload.put("data", data);

        JsonNode response = postJson("/memberships", payload);
        return extractResourceId(response);
    }

    public List<String> getComponentNotificationIds(String componentId) throws IOException, SW360Exception {
        if (isUnavailable(componentId)) {
            return Collections.emptyList();
        }
        return getRelationshipIdsWithPagination("/components/" + componentId + "/relationships/notifications");
    }

    public List<String> getNotificationVulnerabilityIds(String notificationId) throws IOException, SW360Exception {
        if (isUnavailable(notificationId)) {
            return Collections.emptyList();
        }
        return getRelationshipIdsWithPagination("/notifications/" + notificationId + "/relationships/vulnerabilities");
    }

    public List<String> getNotificationComponentIds(String notificationId) throws IOException, SW360Exception {
        if (isUnavailable(notificationId)) {
            return Collections.emptyList();
        }
        return getRelationshipIdsWithPagination("/notifications/" + notificationId + "/relationships/components");
    }

    /**
     * Fetches a single Vilocify notification by ID, enriches it with its linked
     * component IDs ({@code /notifications/{id}/relationships/components}) and
     * vulnerability CVSS/CWE/note details ({@code /vulnerabilities?filter[notifications.id][any]={id}}),
     * and maps it to an SW360 {@link Vulnerability} whose {@code externalId} is the notification ID.
     */
    public Optional<Vulnerability> getNotificationAsSw360Vulnerability(String notificationId)
            throws IOException, SW360Exception {
        if (isUnavailable(notificationId)) {
            return Optional.empty();
        }
        String trimmedId = notificationId.trim();
        Optional<JsonNode> responseOpt = getJsonAllowNotFound(
                "/notifications/" + trimmedId, Collections.emptyMap());
        if (responseOpt.isEmpty()) {
            return Optional.empty();
        }
        JsonNode response = responseOpt.get();
        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull() || !data.isObject()) {
            return Optional.empty();
        }
        Optional<Vulnerability> vulnOpt = mapNotificationNodeToSw360Vulnerability(data);
        if (vulnOpt.isPresent()) {
            Vulnerability vuln = vulnOpt.get();
            enrichNotificationVulnerabilityWithComponentsAndCvss(vuln);
            return Optional.of(vuln);
        }
        return Optional.empty();
    }

    /**
     * Enriches an already-mapped notification {@link Vulnerability} with:
     * <ol>
     *   <li>{@code assignedExtComponentIds} from
     *       {@code GET /api/v2/notifications/{id}/relationships/components}</li>
     *   <li>Maximum CVSS {@code base_score}, {@code cvssVector}, {@code cvssVersion}, {@code cwe},
     *       and {@code legalNotice} ({@code note}) from
     *       {@code GET /api/v2/vulnerabilities?filter[notifications.id][any]={id}}</li>
     * </ol>
     */
    public void enrichNotificationVulnerabilityWithComponentsAndCvss(Vulnerability vulnerability) {
        if (isUnavailable() || vulnerability == null
                || CommonUtils.isNullEmptyOrWhitespace(vulnerability.getExternalId())) {
            return;
        }
        String notifId = vulnerability.getExternalId().trim();
        try {
            List<String> componentIds = getNotificationComponentIds(notifId);
            if (!componentIds.isEmpty()) {
                vulnerability.setAssignedExtComponentIds(new LinkedHashSet<>(componentIds));
            }
        } catch (Exception e) {
            log.debug("Could not fetch component relationships for notification {}", notifId, e);
        }

        try {
            JsonNode vulnPage = getJson("/vulnerabilities", Map.of(
                    "filter[notifications.id][any]", notifId,
                    "filter[deleted][eq]", "false"));
            JsonNode data = vulnPage.path("data");
            if (data.isArray() && !data.isEmpty()) {
                applyVulnerabilityNodesToNotification(vulnerability, data);
            }
        } catch (Exception e) {
            log.debug("Could not fetch vulnerability CVSS details for notification {}", notifId, e);
        }
    }

    private void applyVulnerabilityNodesToNotification(Vulnerability notificationVuln, JsonNode vulnArray) {
        double maxScore = notificationVuln.isSetIsSetCvss() && notificationVuln.isIsSetCvss()
                ? notificationVuln.getCvss()
                : -1.0;
        String bestVector = null;
        String bestVersion = null;
        Set<CVEReference> cveRefs = notificationVuln.isSetCveReferences()
                ? new LinkedHashSet<>(notificationVuln.getCveReferences())
                : new LinkedHashSet<>();

        for (JsonNode vulnNode : vulnArray) {
            JsonNode attrs = vulnNode.path("attributes");
            if (attrs.path("deleted").asBoolean(false)) {
                continue;
            }
            parseCveReference(readText(attrs, "cve", null)).ifPresent(cveRefs::add);
            if (CommonUtils.isNullEmptyOrWhitespace(notificationVuln.getCwe())) {
                notificationVuln.setCwe(readText(attrs, "cwe", null));
            }
            if (CommonUtils.isNullEmptyOrWhitespace(notificationVuln.getLegalNotice())) {
                notificationVuln.setLegalNotice(readText(attrs, "note", null));
            }
            JsonNode cvssArray = attrs.path("cvss");
            if (cvssArray.isArray()) {
                for (JsonNode cvssEntry : cvssArray) {
                    double score = cvssEntry.path("base_score").asDouble(-1.0);
                    if (score > maxScore) {
                        maxScore = score;
                        bestVector = readText(cvssEntry, "vector", null);
                        bestVersion = readText(cvssEntry, "version", null);
                    }
                }
            }
        }

        if (maxScore >= 0.0) {
            notificationVuln.setCvss(maxScore);
            notificationVuln.setIsSetCvss(true);
        }
        if (!cveRefs.isEmpty()) {
            notificationVuln.setCveReferences(cveRefs);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(bestVector)
                || CommonUtils.isNotNullEmptyOrWhitespace(bestVersion)) {
            Map<String, Map<String, String>> metaPerSource = notificationVuln.isSetCveFurtherMetaDataPerSource()
                    ? new HashMap<>(notificationVuln.getCveFurtherMetaDataPerSource())
                    : new HashMap<>();
            Map<String, String> vilocifyMeta = new HashMap<>(
                    metaPerSource.getOrDefault("vilocify", Collections.emptyMap()));
            if (CommonUtils.isNotNullEmptyOrWhitespace(bestVector)) {
                vilocifyMeta.put("cvssVector", bestVector);
            }
            if (CommonUtils.isNotNullEmptyOrWhitespace(bestVersion)) {
                vilocifyMeta.put("cvssVersion", bestVersion);
            }
            metaPerSource.put("vilocify", vilocifyMeta);
            notificationVuln.setCveFurtherMetaDataPerSource(metaPerSource);
        }
    }

    /**
     * Streams notifications page-by-page (200 notifications per HTTP request, filtered by
     * {@code updatedAt[after]} for delta sync, or all notifications sorted by {@code -id}
     * when {@code afterTimestamp} is null/blank for full sync), invoking {@code pageConsumer}
     * immediately with the list of 200 mapped SW360 {@link Vulnerability} objects per page.
     *
     * <p>Makes <b>zero</b> per-notification HTTP requests: all fields ({@code title},
     * {@code description}, {@code priority}, {@code action}, {@code solution},
     * {@code thirdPartyPublishedOn}, {@code updatedAt}, {@code cves}, {@code references},
     * {@code advisories}, {@code cvss}, {@code attackVector}, {@code vendorAffectedComponents})
     * are extracted directly from each notification's {@code attributes} in the page response.
     */
    public void streamNotificationsAsSw360Vulnerabilities(
            String afterTimestamp,
            java.util.function.Consumer<List<Vulnerability>> pageConsumer)
            throws IOException, SW360Exception {
        if (isUnavailable() || pageConsumer == null) {
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("sort", "-id");
        if (CommonUtils.isNotNullEmptyOrWhitespace(afterTimestamp)) {
            params.put("filter[updatedAt][after]", afterTimestamp.trim());
        }

        JsonNode page = getJson("/notifications", params);
        while (true) {
            List<Vulnerability> pageVulnerabilities = new ArrayList<>();
            JsonNode data = page.path("data");
            if (data.isArray()) {
                for (JsonNode notifNode : data) {
                    mapNotificationNodeToSw360Vulnerability(notifNode)
                            .ifPresent(pageVulnerabilities::add);
                }
            }
            if (!pageVulnerabilities.isEmpty()) {
                enrichPageVulnerabilitiesInBatch(pageVulnerabilities);
                pageConsumer.accept(pageVulnerabilities);
            }

            String nextUrl = getNextLink(page);
            if (CommonUtils.isNullEmptyOrWhitespace(nextUrl)) {
                break;
            }
            page = getJsonByAbsoluteUrl(nextUrl);
        }
    }

    private static final java.util.concurrent.ExecutorService COMPONENT_FETCH_POOL =
            java.util.concurrent.Executors.newFixedThreadPool(20, r -> {
                Thread t = new Thread(r, "vilocify-notif-comp-fetcher");
                t.setDaemon(true);
                return t;
            });

    /**
     * Enriches a page of up to 200 notifications with:
     * <ol>
     *   <li>CVSS scores, vectors, CWE, and notes from {@code /api/v2/vulnerabilities}
     *       using batched {@code filter[notifications.id][any]} queries.</li>
     *   <li>{@code assignedExtComponentIds} from
     *       {@code /api/v2/notifications/{id}/relationships/components} fetched concurrently
     *       across a 20-thread pool (matching {@code SVMConstants.PROCESSING_CORE_POOL_SIZE}).</li>
     * </ol>
     */
    private void enrichPageVulnerabilitiesInBatch(List<Vulnerability> pageVulnerabilities) {
        Map<String, Vulnerability> byNotifId = new LinkedHashMap<>();
        for (Vulnerability v : pageVulnerabilities) {
            if (v != null && CommonUtils.isNotNullEmptyOrWhitespace(v.getExternalId())) {
                byNotifId.put(v.getExternalId().trim(), v);
            }
        }
        if (byNotifId.isEmpty()) {
            return;
        }

        // 1. Batch-fetch CVSS / CWE / legalNotice from /vulnerabilities?filter[notifications.id][any]=...
        List<String> notifIds = new ArrayList<>(byNotifId.keySet());
        int chunkSize = 50;
        for (int i = 0; i < notifIds.size(); i += chunkSize) {
            List<String> chunk = notifIds.subList(i, Math.min(i + chunkSize, notifIds.size()));
            try {
                Map<String, String> vulnParams = Map.of(
                        "filter[notifications.id][any]", String.join(",", chunk),
                        "filter[deleted][eq]", "false");
                JsonNode vulnPage = getJson("/vulnerabilities", vulnParams);
                while (true) {
                    JsonNode data = vulnPage.path("data");
                    if (data.isArray() && !data.isEmpty()) {
                        applyBatchVulnerabilityNodesToPage(byNotifId, data);
                    }
                    String nextUrl = getNextLink(vulnPage);
                    if (CommonUtils.isNullEmptyOrWhitespace(nextUrl)) {
                        break;
                    }
                    vulnPage = getJsonByAbsoluteUrl(nextUrl);
                }
            } catch (Exception e) {
                log.debug("Batch vulnerability enrichment skipped for chunk; falling back per notification", e);
            }
        }

        // 2. Concurrently fetch /notifications/{id}/relationships/components (and CVSS if still unset)
        List<java.util.concurrent.Future<?>> futures = new ArrayList<>(pageVulnerabilities.size());
        for (Vulnerability v : pageVulnerabilities) {
            futures.add(COMPONENT_FETCH_POOL.submit(() -> {
                String notifId = v.getExternalId();
                try {
                    List<String> compIds = getNotificationComponentIds(notifId);
                    if (!compIds.isEmpty()) {
                        v.setAssignedExtComponentIds(new LinkedHashSet<>(compIds));
                    }
                } catch (Exception e) {
                    log.debug("Failed fetching component IDs for notification {}", notifId, e);
                }
                if (!v.isSetIsSetCvss() || !v.isIsSetCvss()) {
                    try {
                        JsonNode vulnPage = getJson("/vulnerabilities", Map.of(
                                "filter[notifications.id][any]", notifId,
                                "filter[deleted][eq]", "false"));
                        JsonNode data = vulnPage.path("data");
                        if (data.isArray() && !data.isEmpty()) {
                            applyVulnerabilityNodesToNotification(v, data);
                        }
                    } catch (Exception e) {
                        log.debug("Failed fetching CVSS for notification {}", notifId, e);
                    }
                }
            }));
        }
        for (java.util.concurrent.Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception ignored) {
                // Individual errors already logged
            }
        }
    }

    private void applyBatchVulnerabilityNodesToPage(
            Map<String, Vulnerability> byNotifId, JsonNode vulnArray) {
        // Index notifications by CVE string ("CVE-YYYY-NNNN") so we can correlate returned vulnerabilities
        Map<String, List<Vulnerability>> notifsByCve = new HashMap<>();
        for (Vulnerability notif : byNotifId.values()) {
            if (notif.isSetCveReferences()) {
                for (CVEReference ref : notif.getCveReferences()) {
                    if (ref != null && CommonUtils.isNotNullEmptyOrWhitespace(ref.getYear())
                            && CommonUtils.isNotNullEmptyOrWhitespace(ref.getNumber())) {
                        String cveKey = "CVE-" + ref.getYear().trim() + "-" + ref.getNumber().trim();
                        notifsByCve.computeIfAbsent(cveKey.toUpperCase(), k -> new ArrayList<>()).add(notif);
                    }
                }
            }
        }

        for (JsonNode vulnNode : vulnArray) {
            String cve = readText(vulnNode.path("attributes"), "cve", null);
            if (CommonUtils.isNotNullEmptyOrWhitespace(cve)) {
                List<Vulnerability> matchingNotifs = notifsByCve.getOrDefault(
                        cve.trim().toUpperCase(), Collections.emptyList());
                JsonNode singleArray = MAPPER.createArrayNode().add(vulnNode);
                for (Vulnerability notif : matchingNotifs) {
                    applyVulnerabilityNodesToNotification(notif, singleArray);
                }
            }
        }
    }

    /**
     * Fetches notifications (filtered by {@code updatedAt[after]} for delta sync, or all
     * notifications when {@code afterTimestamp} is null/blank for full sync)
     * and maps each notification to an SW360 {@link Vulnerability}.
     */
    public List<Vulnerability> getNotificationsAsSw360Vulnerabilities(String afterTimestamp)
            throws IOException, SW360Exception {
        List<Vulnerability> all = new ArrayList<>();
        streamNotificationsAsSw360Vulnerabilities(afterTimestamp, all::addAll);
        return all;
    }

    private Optional<Vulnerability> mapNotificationNodeToSw360Vulnerability(JsonNode notifNode) {
        String notifId = readText(notifNode, "id", null);
        if (CommonUtils.isNullEmptyOrWhitespace(notifId)) {
            return Optional.empty();
        }

        JsonNode attributes = notifNode.path("attributes");
        Vulnerability vulnerability = new Vulnerability();
        vulnerability.setExternalId(notifId);
        vulnerability.setTitle(readText(attributes, "title", notifId));
        vulnerability.setDescription(readText(attributes, "description", null));
        vulnerability.setPriority(readText(attributes, "priority", null));
        vulnerability.setAction(readText(attributes, "action", null));
        vulnerability.setExtendedDescription(readText(attributes, "solution", null));
        vulnerability.setPublishDate(readText(attributes, "thirdPartyPublishedOn",
                readText(attributes, "createdAt", null)));
        vulnerability.setLastExternalUpdate(readText(attributes, "updatedAt",
                vulnerability.getPublishDate()));
        vulnerability.setLastUpdateDate(SW360Utils.getCreatedOnTime());

        // Map references
        Set<String> references = new LinkedHashSet<>();
        JsonNode refsArray = attributes.path("references");
        if (refsArray.isArray()) {
            for (JsonNode ref : refsArray) {
                if (ref != null && CommonUtils.isNotNullEmptyOrWhitespace(ref.asText(null))) {
                    references.add(ref.asText().trim());
                }
            }
        }
        if (!references.isEmpty()) {
            vulnerability.setReferences(references);
        }

        // Map advisories (each entry has "title" and "url")
        Set<VendorAdvisory> vendorAdvisories = new LinkedHashSet<>();
        JsonNode advisoriesArray = attributes.path("advisories");
        if (advisoriesArray.isArray()) {
            for (JsonNode adv : advisoriesArray) {
                if (adv != null && adv.isObject()) {
                    String vendor = readText(adv, "vendor", "Vilocify");
                    String name = readText(adv, "title", readText(adv, "name", ""));
                    String url = readText(adv, "url", "");
                    if (CommonUtils.isNotNullEmptyOrWhitespace(name) || CommonUtils.isNotNullEmptyOrWhitespace(url)) {
                        vendorAdvisories.add(new VendorAdvisory(vendor, name, url));
                    }
                }
            }
        }
        if (!vendorAdvisories.isEmpty()) {
            vulnerability.setVendorAdvisories(vendorAdvisories);
        }

        // Map CVEs to CVEReferences (format: CVE-YYYY-NNNN+)
        Set<CVEReference> cveReferences = new LinkedHashSet<>();
        JsonNode cvesArray = attributes.path("cves");
        if (cvesArray.isArray()) {
            for (JsonNode cveNode : cvesArray) {
                parseCveReference(cveNode != null ? cveNode.asText(null) : null)
                        .ifPresent(cveReferences::add);
            }
        }
        if (!cveReferences.isEmpty()) {
            vulnerability.setCveReferences(cveReferences);
        }

        // Map CVSS if present directly on notification attributes
        JsonNode cvssNode = attributes.path("cvss");
        if (!cvssNode.isMissingNode() && !cvssNode.isNull()) {
            double score = cvssNode.asDouble(-1.0);
            if (score >= 0.0) {
                vulnerability.setCvss(score);
                vulnerability.setIsSetCvss(true);
            }
        }

        // Store attackVector, tlp, and vendorAffectedComponents in cveFurtherMetaDataPerSource
        Map<String, String> vilocifyMeta = new HashMap<>();
        vilocifyMeta.put("resourceId", notifId);
        String attackVector = readText(attributes, "attackVector", null);
        if (CommonUtils.isNotNullEmptyOrWhitespace(attackVector)) {
            vilocifyMeta.put("attackVector", attackVector);
        }
        String vendorAffected = readText(attributes, "vendorAffectedComponents", null);
        if (CommonUtils.isNotNullEmptyOrWhitespace(vendorAffected)) {
            vilocifyMeta.put("vendorAffectedComponents", vendorAffected);
        }
        String tlp = readText(attributes, "tlp", null);
        if (CommonUtils.isNotNullEmptyOrWhitespace(tlp)) {
            vilocifyMeta.put("tlp", tlp);
        }
        vulnerability.setCveFurtherMetaDataPerSource(Map.of("vilocify", vilocifyMeta));

        return Optional.of(vulnerability);
    }

    private Optional<CVEReference> parseCveReference(String cveString) {
        if (CommonUtils.isNullEmptyOrWhitespace(cveString)) {
            return Optional.empty();
        }
        String normalized = cveString.trim().toUpperCase();
        if (normalized.startsWith("CVE-")) {
            String[] parts = normalized.split("-");
            if (parts.length >= 3) {
                return Optional.of(new CVEReference(parts[1], parts[2]));
            }
        }
        return Optional.empty();
    }

    private Set<String> extractRelationshipIdsFromResource(JsonNode resourceNode, String relationshipName) {
        Set<String> ids = new LinkedHashSet<>();
        JsonNode relData = resourceNode.path("relationships").path(relationshipName).path("data");
        if (relData.isArray()) {
            for (JsonNode item : relData) {
                String id = readText(item, "id", null);
                if (CommonUtils.isNotNullEmptyOrWhitespace(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public Optional<Vulnerability> getVulnerabilityAsSw360(String vulnerabilityId, String componentId)
            throws IOException, SW360Exception {
        if (isUnavailable(vulnerabilityId)) {
            return Optional.empty();
        }

        Optional<JsonNode> responseOpt = getJsonAllowNotFound(
                "/vulnerabilities/" + vulnerabilityId, Collections.emptyMap());
        if (responseOpt.isEmpty()) {
            return Optional.empty();
        }
        JsonNode response = responseOpt.get();
        JsonNode jsonapi = response.path("jsonapi");
        if (jsonapi.isMissingNode() || jsonapi.path("version").isMissingNode()) {
            throw new SW360Exception("Invalid JSON:API vulnerability response: missing jsonapi.version");
        }

        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull() || !data.isObject()) {
            throw new SW360Exception("Invalid JSON:API vulnerability response: missing data object");
        }

        String type = data.path("type").asText("");
        if (!"vulnerabilities".equals(type)) {
            throw new SW360Exception("Invalid JSON:API vulnerability response: unexpected data.type=" + type);
        }

        String resourceId = data.path("id").asText("");
        if (CommonUtils.isNullEmptyOrWhitespace(resourceId)) {
            throw new SW360Exception("Invalid JSON:API vulnerability response: missing data.id");
        }

        JsonNode attributes = data.path("attributes");

        if (attributes.path("deleted").asBoolean(false)) {
            return Optional.empty();
        }

        Vulnerability vulnerability = new Vulnerability();

        String cve = attributes.path("cve").asText(null);
        String externalId = CommonUtils.isNotNullEmptyOrWhitespace(cve) ? cve : data.path("id").asText();
        vulnerability.setExternalId(externalId);
        vulnerability.setTitle(cve);
        vulnerability.setDescription(readText(attributes, "description", null));
        vulnerability.setCwe(readText(attributes, "cwe", null));
        vulnerability.setAction(readText(attributes, "mitigatingFactor", null));
        vulnerability.setLegalNotice(readText(attributes, "note", null));

        JsonNode cvssArray = attributes.path("cvss");
        String bestVector = null;
        String bestVersion = null;
        if (cvssArray.isArray()) {
            double maxScore = -1.0;
            for (JsonNode cvssEntry : cvssArray) {
                double score = cvssEntry.path("base_score").asDouble(-1.0);
                if (score > maxScore) {
                    maxScore = score;
                    bestVector = readText(cvssEntry, "vector", null);
                    bestVersion = readText(cvssEntry, "version", null);
                }
            }
            if (maxScore >= 0.0) {
                vulnerability.setCvss(maxScore);
                vulnerability.setIsSetCvss(true);
            }
        }

        Map<String, String> vilocifyMeta = new HashMap<>();
        vilocifyMeta.put("resourceId", resourceId);
        vilocifyMeta.put("deleted", String.valueOf(attributes.path("deleted").asBoolean(false)));
        String selfLink = data.path("links").path("self").asText(null);
        if (CommonUtils.isNotNullEmptyOrWhitespace(selfLink)) {
            vilocifyMeta.put("selfLink", selfLink);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(bestVector)) {
            vilocifyMeta.put("cvssVector", bestVector);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(bestVersion)) {
            vilocifyMeta.put("cvssVersion", bestVersion);
        }
        if (!vilocifyMeta.isEmpty()) {
            Map<String, Map<String, String>> cveFurtherMetaDataPerSource = new HashMap<>();
            cveFurtherMetaDataPerSource.put("vilocify", vilocifyMeta);
            vulnerability.setCveFurtherMetaDataPerSource(cveFurtherMetaDataPerSource);
        }

        Set<String> assignedExtComponents = new HashSet<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(componentId)) {
            assignedExtComponents.add(componentId);
        }
        if (!assignedExtComponents.isEmpty()) {
            vulnerability.setAssignedExtComponentIds(assignedExtComponents);
        }

        return Optional.of(vulnerability);
    }

    /**
     * Creates a new monitoringList in Vilocify without a tenant group.
     */
    public Optional<String> createMonitoringList(String name, String comment)
            throws IOException, SW360Exception {
        return createMonitoringList(name, comment, null);
    }

    /**
     * Creates a new monitoringList in Vilocify.
     *
     * <p><b>Vilocify API Validation Requirements:</b>
     * <ul>
     *   <li>{@code name} (required, max 255 chars)</li>
     *   <li>{@code group} (required on tenants with organization group feature enabled,
     *       e.g., mapped from SW360 project {@code businessUnit})</li>
     *   <li>{@code comment} (optional description)</li>
     * </ul>
     */
    public Optional<String> createMonitoringList(String name, String comment, String group)
            throws IOException, SW360Exception {
        if (isUnavailable(name)) {
            return Optional.empty();
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", name.trim());
        if (CommonUtils.isNotNullEmptyOrWhitespace(comment)) {
            attributes.put("comment", comment.trim());
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(group)) {
            attributes.put("group", group.trim());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("type", "monitoringLists");
        data.put("attributes", attributes);
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);

        JsonNode response = postJson("/monitoringLists", payload);
        return extractResourceId(response);
    }

    public void updateMonitoringList(String listId, String name, String comment)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return;
        }
        Map<String, Object> attributes = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(name)) {
            attributes.put("name", name.trim());
        }
        if (comment != null) {
            attributes.put("comment", comment.trim());
        }
        if (attributes.isEmpty()) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("type", "monitoringLists");
        data.put("id", listId);
        data.put("attributes", attributes);
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", data);
        patchJson("/monitoringLists/" + listId, payload);
    }

    public void replaceMonitoringListUsers(String listId, List<String> membershipIds)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/users",
                buildToManyRelationshipPayload("memberships", membershipIds));
    }

    public void replaceMonitoringListComponents(String listId, List<String> componentIds)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/components",
                buildToManyRelationshipPayload("components", componentIds));
    }

    /**
     * Replaces the full set of componentRequest relationships on a monitoringList.
     * Sends {@code PATCH /api/v2/monitoringLists/{id}/relationships/componentRequests}.
     */
    public void replaceMonitoringListComponentRequests(String listId, List<String> componentRequestIds)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/componentRequests",
                buildToManyRelationshipPayload("componentRequests", componentRequestIds));
    }

    public void replaceMonitoringListChildren(String listId, List<String> childIds)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/children",
                buildToManyRelationshipPayload("monitoringLists", childIds));
    }

    /**
     * Deletes a monitoringList from Vilocify ({@code DELETE /api/v2/monitoringLists/{id}}).
     */
    public boolean deleteMonitoringList(String listId) throws IOException {
        if (isUnavailable(listId)) {
            return false;
        }
        URI uri = buildUri("/monitoringLists/" + listId.trim(), Collections.emptyMap(), false);
        HttpDelete request = new HttpDelete(uri);
        addDefaultHeaders(request);

        return executeStatusCode(request, (statusCode, reasonPhrase) -> {
            if (statusCode == 204 || statusCode == 200 || statusCode == 404) {
                log.info("Deleted Vilocify monitoringList {} (HTTP {})", listId, statusCode);
                return true;
            }
            log.error("Failed to delete Vilocify monitoringList {}: HTTP {} {}", listId, statusCode, reasonPhrase);
            return false;
        });
    }

    public JsonNode getMonitoringList(String listId, String includeParams)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return MAPPER.missingNode();
        }
        Map<String, String> params = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(includeParams)) {
            params.put("include", includeParams.trim());
        }
        return getSingleResourceJson("/monitoringLists/" + listId, params);
    }

    public List<String> getNotificationIdsAfter(String afterTimestamp) throws IOException, SW360Exception {
        if (isUnavailable()) {
            return Collections.emptyList();
        }
        Map<String, String> params = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(afterTimestamp)) {
            params.put("filter[updatedAt][after]", afterTimestamp.trim());
        }
        return collectAllIds("/notifications", params);
    }

    public List<String> getVulnerabilityIdsAfter(String afterTimestamp) throws IOException, SW360Exception {
        Map<String, String> queryParams = new HashMap<>();
        if (CommonUtils.isNotNullEmptyOrWhitespace(afterTimestamp)) {
            queryParams.put("filter[updatedAt][after]", afterTimestamp.trim());
        }
        return getVulnerabilityIds(queryParams);
    }

    public List<String> getVulnerabilityIds(Map<String, String> queryParams) throws IOException, SW360Exception {
        if (isUnavailable()) {
            return Collections.emptyList();
        }
        Map<String, String> params = new HashMap<>();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (CommonUtils.isNotNullEmptyOrWhitespace(entry.getValue())) {
                    params.put(entry.getKey(), entry.getValue().trim());
                }
            }
        }

        params.putIfAbsent("filter[deleted][eq]", "false");
        return collectAllIds("/vulnerabilities", params);
    }

    private Optional<String> firstResourceId(String path, Map<String, String> queryParams)
            throws IOException, SW360Exception {
        JsonNode response = getJson(path, queryParams);
        return extractResourceId(response);
    }

    private List<String> getRelationshipIdsWithPagination(String relativePath) throws IOException, SW360Exception {
        List<String> ids = new ArrayList<>();
        JsonNode page = getJson(relativePath, Collections.emptyMap());
        appendIds(page, ids);

        String nextUrl = getNextLink(page);
        while (CommonUtils.isNotNullEmptyOrWhitespace(nextUrl)) {
            page = getJsonByAbsoluteUrl(nextUrl);
            appendIds(page, ids);
            nextUrl = getNextLink(page);
        }
        return ids;
    }

    private void appendIds(JsonNode response, List<String> ids) {
        JsonNode data = response.path("data");
        if (!data.isArray()) {
            return;
        }
        for (JsonNode node : data) {
            JsonNode idNode = node.path("id");
            if (!idNode.isMissingNode() && !idNode.isNull()
                    && CommonUtils.isNotNullEmptyOrWhitespace(idNode.asText())) {
                ids.add(idNode.asText());
            }
        }
    }

    private String getNextLink(JsonNode response) {
        JsonNode next = response.path("links").path("next");
        if (next.isMissingNode() || next.isNull()) {
            return null;
        }
        String nextUrl = next.asText();
        return CommonUtils.isNullEmptyOrWhitespace(nextUrl) || "null".equalsIgnoreCase(nextUrl) ? null : nextUrl;
    }

    private JsonNode getJsonByAbsoluteUrl(String url) throws IOException, SW360Exception {
        if (url.startsWith("/")) {
            url = normalizeBaseUrl() + url;
        }
        HttpGet request = new HttpGet(URI.create(url));
        addDefaultHeaders(request);
        return executeJson(request);
    }

    private String readText(JsonNode attributes, String field, String fallback) {
        JsonNode node = attributes.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        String value = node.asText();
        return CommonUtils.isNullEmptyOrWhitespace(value) ? fallback : value;
    }

    private JsonNode postJson(String path, Map<String, Object> payload) throws IOException, SW360Exception {
        HttpPost request = new HttpPost(buildUri(path, Collections.emptyMap(), false));
        addDefaultHeaders(request);
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(payload), JSON_API_CONTENT_TYPE));
        return executeJson(request);
    }

    private JsonNode patchJson(String path, Map<String, Object> payload) throws IOException, SW360Exception {
        HttpPatch request = new HttpPatch(buildUri(path, Collections.emptyMap(), false));
        addDefaultHeaders(request);
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(payload), JSON_API_CONTENT_TYPE));
        return executeJson(request);
    }

    private Map<String, Object> buildToManyRelationshipPayload(String type, List<String> ids) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                if (CommonUtils.isNotNullEmptyOrWhitespace(id)) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("type", type);
                    entry.put("id", id);
                    dataList.add(entry);
                }
            }
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("data", dataList);
        return payload;
    }

    private List<String> collectAllIds(String path, Map<String, String> params)
            throws IOException, SW360Exception {
        List<String> ids = new ArrayList<>();
        JsonNode page = getJson(path, params);
        appendIds(page, ids);
        String nextUrl = getNextLink(page);
        while (CommonUtils.isNotNullEmptyOrWhitespace(nextUrl)) {
            page = getJsonByAbsoluteUrl(nextUrl);
            appendIds(page, ids);
            nextUrl = getNextLink(page);
        }
        return ids;
    }

    private JsonNode getJson(String path, Map<String, String> queryParams) throws IOException, SW360Exception {
        HttpGet request = new HttpGet(buildUri(path, queryParams, true));
        addDefaultHeaders(request);
        return executeJson(request);
    }

    private JsonNode getSingleResourceJson(String path, Map<String, String> queryParams)
            throws IOException, SW360Exception {
        HttpGet request = new HttpGet(buildUri(path, queryParams, false));
        addDefaultHeaders(request);
        return executeJson(request);
    }

    private Optional<JsonNode> getJsonAllowNotFound(String path, Map<String, String> queryParams)
            throws IOException, SW360Exception {
        HttpGet request = new HttpGet(buildUri(path, queryParams, false));
        addDefaultHeaders(request);
        return executeWithRateLimitRetry(request, true);
    }

    private JsonNode executeJson(HttpUriRequestBase request) throws IOException, SW360Exception {
        Optional<JsonNode> result = executeWithRateLimitRetry(request, false);
        return result.orElseGet(MAPPER::missingNode);
    }

    private Optional<JsonNode> executeWithRateLimitRetry(HttpUriRequestBase request, boolean allowNotFound)
            throws IOException, SW360Exception {
        CloseableHttpClient httpClient = VilocifyHttpClientFactory.getSharedClient();
        for (int attempt = 0; attempt <= MAX_RATE_LIMIT_RETRIES; attempt++) {
            int[] statusHolder = {0};
            String[] reasonHolder = {""};
            JsonNode[] bodyHolder = {MAPPER.missingNode()};

            httpClient.execute(request, response -> {
                statusHolder[0] = response.getCode();
                reasonHolder[0] = response.getReasonPhrase();
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    try (InputStream content = entity.getContent()) {
                        bodyHolder[0] = MAPPER.readTree(content);
                    } catch (Exception ignored) {
                        // Ignore non-JSON error bodies
                    }
                }
                return null;
            });

            int statusCode = statusHolder[0];
            if ((statusCode == 429 || (statusCode >= 500 && statusCode < 600))
                    && attempt < MAX_RATE_LIMIT_RETRIES) {
                log.warn("Vilocify API transient status ({}) on {} {}, retrying (attempt {}/{})",
                        statusCode, request.getMethod(), request.getRequestUri(), attempt + 1, MAX_RATE_LIMIT_RETRIES);
                sleepQuietly(RATE_LIMIT_BACKOFF_MS * (attempt + 1));
                continue;
            }
            if (allowNotFound && statusCode == 404) {
                return Optional.empty();
            }
            if (statusCode < 200 || statusCode >= 300) {
                String errorDetails = formatJsonApiErrors(bodyHolder[0]);
                String detailSuffix = CommonUtils.isNotNullEmptyOrWhitespace(errorDetails)
                        ? " - " + errorDetails
                        : "";
                throw new SW360Exception(request.getMethod() + " failed for Vilocify endpoint: HTTP "
                        + statusCode + " " + reasonHolder[0] + detailSuffix);
            }
            return Optional.of(bodyHolder[0]);
        }
        throw new SW360Exception(request.getMethod() + " failed for Vilocify endpoint: rate limit retries exhausted");
    }

    private String formatJsonApiErrors(JsonNode errorBody) {
        if (errorBody == null || errorBody.isMissingNode() || !errorBody.path("errors").isArray()) {
            return "";
        }
        List<String> messages = new ArrayList<>();
        for (JsonNode err : errorBody.path("errors")) {
            String title = readText(err, "title", "");
            String detail = readText(err, "detail", "");
            if (CommonUtils.isNotNullEmptyOrWhitespace(title) && CommonUtils.isNotNullEmptyOrWhitespace(detail)) {
                messages.add(title + ": " + detail);
            } else if (CommonUtils.isNotNullEmptyOrWhitespace(detail)) {
                messages.add(detail);
            } else if (CommonUtils.isNotNullEmptyOrWhitespace(title)) {
                messages.add(title);
            }
        }
        return String.join("; ", messages);
    }

    private <T> T executeStatusCode(HttpUriRequestBase request, StatusHandler<T> handler) throws IOException {
        CloseableHttpClient httpClient = VilocifyHttpClientFactory.getSharedClient();
        int[] statusHolder = {0};
        String[] reasonHolder = {""};
        httpClient.execute(request, response -> {
            statusHolder[0] = response.getCode();
            reasonHolder[0] = response.getReasonPhrase();
            return null;
        });
        return handler.handle(statusHolder[0], reasonHolder[0]);
    }

    private void addDefaultHeaders(HttpUriRequestBase request) {
        request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, JSON_API_MEDIA_TYPE));
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        request.addHeader(new BasicHeader(HttpHeaders.USER_AGENT, VilocifyHttpClientFactory.USER_AGENT));
    }

    private boolean isUnavailable() {
        return !isConfigured();
    }

    private boolean isUnavailable(String requiredValue) {
        return !isConfigured() || CommonUtils.isNullEmptyOrWhitespace(requiredValue);
    }

    private URI buildUri(String path, Map<String, String> queryParams, boolean includeDefaultPageSize) {
        try {
            URIBuilder builder = new URIBuilder(normalizeBaseUrl() + normalizePath(rootPath) + normalizePath(path));
            boolean pageSizeSupplied = queryParams != null && queryParams.containsKey("page[size]");
            if (includeDefaultPageSize && !pageSizeSupplied) {
                builder.addParameter("page[size]", String.valueOf(MAX_PAGE_SIZE));
            }
            if (queryParams != null) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    if (CommonUtils.isNotNullEmptyOrWhitespace(entry.getValue())) {
                        builder.addParameter(entry.getKey(), entry.getValue());
                    }
                }
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Vilocify URI", e);
        }
    }

    private String normalizeBaseUrl() {
        if (CommonUtils.isNullEmptyOrWhitespace(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private void validateExclusiveProvider() {
        String provider = getActiveProvider();
        if (CommonUtils.isNullEmptyOrWhitespace(provider)) {
            return;
        }
        String normalizedProvider = provider.trim().toLowerCase();
        if (!Objects.equals(normalizedProvider, PROVIDER_SVM)
                && !Objects.equals(normalizedProvider, PROVIDER_VILOCIFY)) {
            throw new IllegalStateException(
                    "sync.integration.provider must be either '" + PROVIDER_SVM + "' or '"
                            + PROVIDER_VILOCIFY + "', got: '" + provider + "'");
        }
        if (Objects.equals(normalizedProvider, PROVIDER_SVM)) {
            log.info("sync.integration.provider=svm: Vilocify integration is disabled in favour of SVM");
        }
    }

    @FunctionalInterface
    private interface StatusHandler<T> {
        T handle(int statusCode, String reasonPhrase);
    }

    /**
     * Represents a Vilocify notification along with its related component IDs and vulnerability IDs.
     */
    public record NotificationComponentsAndVulnerabilities(
            String notificationId,
            Set<String> componentIds,
            Set<String> vulnerabilityIds) {
    }
}
