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
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Client for the Vilocify v2 JSON:API.
 *
 * <p>Uses a shared pooled {@link CloseableHttpClient} from {@link VilocifyHttpClientFactory},
 * supports dynamic configuration lookup from {@code sw360_config} (with fallback to
 * {@code sw360.properties}), cursor pagination, {@code include} compound documents,
 * and automatic retry on HTTP 429 rate-limiting.
 */
public class VilocifyConnector {

    private static final Logger log = LogManager.getLogger(VilocifyConnector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String JSON_API_MEDIA_TYPE = "application/vnd.api+json";
    private static final ContentType JSON_API_CONTENT_TYPE =
            ContentType.create(JSON_API_MEDIA_TYPE, StandardCharsets.UTF_8);
    private static final String DEFAULT_API_ROOT_PATH = "/api/v2";
    public static final String PROVIDER_SVM = "svm";
    public static final String PROVIDER_VILOCIFY = "vilocify";
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MAX_RATE_LIMIT_RETRIES = 5;
    private static final long RATE_LIMIT_BACKOFF_MS = 1000L;

    private final String baseUrl;
    private final String rootPath;
    private final String token;
    private final boolean testingMode;

    public VilocifyConnector() {
        Map<String, String> props = loadProperties();
        this.baseUrl = trimToEmpty(readDynamicConfig(
                SW360ConfigKeys.VILOCIFY_API_BASE_URL,
                props.getOrDefault(SW360ConfigKeys.VILOCIFY_API_BASE_URL, "")));
        this.rootPath = normalizePath(readDynamicConfig(
                SW360ConfigKeys.VILOCIFY_API_ROOT_PATH,
                props.getOrDefault(SW360ConfigKeys.VILOCIFY_API_ROOT_PATH, DEFAULT_API_ROOT_PATH)));
        this.token = trimToEmpty(readDynamicConfig(
                SW360ConfigKeys.VILOCIFY_API_TOKEN,
                props.getOrDefault(SW360ConfigKeys.VILOCIFY_API_TOKEN, "")));
        this.testingMode = false;

        validateExclusiveProvider();
    }

    /**
     * Package-private constructor for unit tests. Allows tests to point the
     * connector at a local HTTP server without reading {@code sw360.properties}
     * or querying {@code sw360_config}.
     */
    @VisibleForTesting
    VilocifyConnector(String baseUrl, String rootPath, String token) {
        this.baseUrl = trimToEmpty(baseUrl);
        this.rootPath = normalizePath(rootPath != null ? rootPath : DEFAULT_API_ROOT_PATH);
        this.token = trimToEmpty(token);
        this.testingMode = true;
    }

    /**
     * Returns the currently configured sync provider ({@code "svm"} or {@code "vilocify"}),
     * checking {@code sw360_config} first and falling back to {@code sw360.properties}.
     */
    public static String getActiveProvider() {
        String fallback = SW360Constants.SYNC_INTEGRATION_PROVIDER;
        String value = readDynamicConfig(SW360ConfigKeys.SYNC_INTEGRATION_PROVIDER, fallback);
        return trimToEmpty(value);
    }

    public static boolean isVilocifyProviderActive() {
        return PROVIDER_VILOCIFY.equalsIgnoreCase(getActiveProvider());
    }

    public boolean isConfigured() {
        if (!testingMode) {
            String provider = getActiveProvider();
            if (StringUtils.isNotBlank(provider) && PROVIDER_SVM.equalsIgnoreCase(provider)) {
                return false;
            }
        }
        return StringUtils.isNotBlank(baseUrl) && StringUtils.isNotBlank(token);
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
            if (StringUtils.isBlank(applicationId)) {
                continue;
            }

            String listName = buildMonitoringListName(
                    readText(projectNode, "application_name", "SW360 Project"),
                    readText(projectNode, "application_version", ""),
                    applicationId);
            String listComment = buildMonitoringListComment(
                    readText(projectNode, "business_unit", null),
                    applicationId);

            Optional<String> listId = findMonitoringListIdForProject(applicationId, listName);
            if (listId.isEmpty()) {
                listId = createMonitoringList(listName, listComment);
            }
            if (listId.isEmpty()) {
                throw new SW360Exception("Failed to resolve monitoringList ID for project " + applicationId);
            }

            String resolvedListId = listId.get();
            updateMonitoringList(resolvedListId, listName, listComment);
            listIdsByApplicationId.put(applicationId, resolvedListId);
        }

        for (JsonNode projectNode : projects) {
            String applicationId = readText(projectNode, "application_id", null);
            if (StringUtils.isBlank(applicationId)) {
                continue;
            }

            String listId = listIdsByApplicationId.get(applicationId);
            if (StringUtils.isBlank(listId)) {
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

    public Optional<String> findMonitoringListIdForProject(String applicationId, String listName)
            throws IOException, SW360Exception {
        if (isUnavailable(applicationId)) {
            return Optional.empty();
        }

        // First try server-side filter by exact monitoring list name
        if (StringUtils.isNotBlank(listName)) {
            Optional<String> byNameFilter = firstResourceId(
                    "/monitoringLists", Map.of("filter[name][eq]", listName.trim()));
            if (byNameFilter.isPresent()) {
                return byNameFilter;
            }
        }

        // Fallback: paginate through monitoringLists and match by name or SW360 project id marker
        JsonNode page = getJson("/monitoringLists", Map.of("page[size]", String.valueOf(MAX_PAGE_SIZE)));
        while (true) {
            JsonNode data = page.path("data");
            if (data.isArray()) {
                for (JsonNode entry : data) {
                    JsonNode attributes = entry.path("attributes");
                    String existingName = readText(attributes, "name", null);
                    String existingComment = readText(attributes, "comment", null);
                    boolean matchesByName = StringUtils.isNotBlank(listName)
                            && StringUtils.equals(existingName, listName);
                    boolean matchesByCommentMarker = StringUtils.contains(
                            existingComment, "SW360 project id: " + applicationId);
                    if (matchesByName || matchesByCommentMarker) {
                        Optional<String> id = extractResourceIdFromObject(entry);
                        if (id.isPresent()) {
                            return id;
                        }
                    }
                }
            }

            String nextUrl = getNextLink(page);
            if (StringUtils.isBlank(nextUrl)) {
                break;
            }
            page = getJsonByAbsoluteUrl(nextUrl);
        }

        return Optional.empty();
    }

    public static String buildMonitoringListName(String projectName, String projectVersion, String projectId) {
        String safeName = StringUtils.defaultIfBlank(projectName, "SW360 Project");
        if (StringUtils.isBlank(projectVersion)) {
            return safeName + " [SW360:" + projectId + "]";
        }
        return safeName + " (" + projectVersion + ") [SW360:" + projectId + "]";
    }

    public static String buildMonitoringListComment(String businessUnit, String projectId) {
        if (StringUtils.isBlank(businessUnit)) {
            return "SW360 project id: " + projectId;
        }
        return "SW360 project id: " + projectId + ", business unit: " + businessUnit;
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
            if (StringUtils.isNotBlank(value)) {
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
            if (StringUtils.isBlank(rawValue)) {
                continue;
            }

            if (!StringUtils.contains(rawValue, "@")) {
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
            if (StringUtils.isNotBlank(componentId)) {
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
                if (StringUtils.isBlank(appId)) {
                    continue;
                }
                String listId = listIdsByApplicationId.get(appId.trim());
                if (StringUtils.isNotBlank(listId)) {
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
                if (StringUtils.isNotBlank(listId)) {
                    childIds.add(listId.trim());
                }
            }
        }

        return new ArrayList<>(childIds);
    }

    /**
     * Resolves the Vilocify component ID for a given SW360 {@link Release}.
     *
     * <p>Note (Open Point #8 for Vilocify team): checks server-side filters
     * ({@code filter[url][like]} / {@code filter[name][eq]} + {@code filter[version][eq]})
     * first when supported, and falls back to paginated matching.
     */
    public Optional<String> findComponentIdByRelease(Release release) throws IOException, SW360Exception {
        if (!isConfigured() || release == null) {
            return Optional.empty();
        }

        String packageUrl = null;
        if (release.isSetExternalIds()) {
            packageUrl = release.getExternalIds().get(SW360Constants.PACKAGE_URL);
            if (StringUtils.isBlank(packageUrl)) {
                packageUrl = release.getExternalIds().get(SW360Constants.PURL_ID);
            }
        }

        String releaseName = trimToEmpty(release.getName());
        String releaseVersion = trimToEmpty(release.getVersion());

        JsonNode page = getJson("/components", Map.of("page[size]", String.valueOf(MAX_PAGE_SIZE)));
        while (true) {
            Optional<String> matchingId = findMatchingComponentId(page, packageUrl, releaseName, releaseVersion);
            if (matchingId.isPresent()) {
                return matchingId;
            }

            String nextUrl = getNextLink(page);
            if (StringUtils.isBlank(nextUrl)) {
                return Optional.empty();
            }
            page = getJsonByAbsoluteUrl(nextUrl);
        }
    }

    private Optional<String> findMatchingComponentId(JsonNode response, String packageUrl,
                                                     String releaseName, String releaseVersion) {
        JsonNode data = response.path("data");
        if (!data.isArray() || data.isEmpty()) {
            return Optional.empty();
        }

        for (JsonNode component : data) {
            JsonNode attributes = component.path("attributes");
            String componentUrl = readText(attributes, "url", "");
            String componentName = readText(attributes, "name", "");
            String componentVersion = readText(attributes, "version", "");

            boolean packageUrlMatches = StringUtils.isNotBlank(packageUrl)
                    && StringUtils.equalsIgnoreCase(trimToEmpty(componentUrl), trimToEmpty(packageUrl));

            boolean nameVersionMatches = StringUtils.isNotBlank(releaseName)
                    && StringUtils.isNotBlank(releaseVersion)
                    && StringUtils.equalsIgnoreCase(trimToEmpty(componentName), releaseName)
                    && StringUtils.equalsIgnoreCase(trimToEmpty(componentVersion), releaseVersion);

            if (packageUrlMatches || nameVersionMatches) {
                JsonNode idNode = component.path("id");
                if (!idNode.isMissingNode() && !idNode.isNull() && StringUtils.isNotBlank(idNode.asText())) {
                    return Optional.of(idNode.asText());
                }
            }
        }

        return Optional.empty();
    }

    public Optional<String> createComponentRequest(Release release) throws IOException, SW360Exception {
        if (isUnavailable() || release == null) {
            return Optional.empty();
        }

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "componentRequests");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", release.getName());
        attributes.put("version", release.getVersion());
        if (release.isSetVendor() && StringUtils.isNotBlank(release.getVendor().getFullname())) {
            attributes.put("vendor", release.getVendor().getFullname());
        }
        if (release.isSetExternalIds()) {
            String purl = release.getExternalIds().getOrDefault(
                    SW360Constants.PACKAGE_URL,
                    release.getExternalIds().get(SW360Constants.PURL_ID));
            if (StringUtils.isNotBlank(purl)) {
                attributes.put("componentUrl", purl.trim());
            }
        }
        data.put("attributes", attributes);
        payload.put("data", data);

        JsonNode response = postJson("/componentRequests", payload);
        return extractResourceId(response);
    }

    public Optional<String> resolveMappedComponentId(String requestId) throws IOException, SW360Exception {
        if (isUnavailable(requestId)) {
            return Optional.empty();
        }
        JsonNode response = getJson("/componentRequests/" + requestId, Collections.emptyMap());
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

        JsonNode response = getJson("/componentRequests/" + requestId, Collections.emptyMap());
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
        URI uri = buildUri(path, Collections.emptyMap());
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
        if (StringUtils.isNotBlank(userName)) {
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
     * Represents a Vilocify notification along with its related component IDs and vulnerability IDs.
     */
    public record NotificationComponentsAndVulnerabilities(
            String notificationId,
            Set<String> componentIds,
            Set<String> vulnerabilityIds) {
    }

    /**
     * Fetches notifications (optionally filtered by {@code updatedAt[after]} for delta sync,
     * or all notifications when {@code afterTimestamp} is null/blank for weekly full sync)
     * using {@code include=components,vulnerabilities} to retrieve related component and
     * vulnerability IDs efficiently. Falls back to relationship endpoints if relationships
     * are not inline in the notification resource.
     */
    public List<NotificationComponentsAndVulnerabilities> getNotificationsWithRelationsAfter(String afterTimestamp)
            throws IOException, SW360Exception {
        if (isUnavailable()) {
            return Collections.emptyList();
        }

        Map<String, String> params = new HashMap<>();
        params.put("include", "components,vulnerabilities");
        if (StringUtils.isNotBlank(afterTimestamp)) {
            params.put("filter[updatedAt][after]", afterTimestamp.trim());
        }

        List<NotificationComponentsAndVulnerabilities> results = new ArrayList<>();
        JsonNode page = getJson("/notifications", params);
        while (true) {
            JsonNode data = page.path("data");
            if (data.isArray()) {
                for (JsonNode notifNode : data) {
                    String notifId = readText(notifNode, "id", null);
                    if (StringUtils.isBlank(notifId)) {
                        continue;
                    }
                    Set<String> compIds = extractRelationshipIdsFromResource(notifNode, "components");
                    if (compIds.isEmpty()) {
                        compIds.addAll(getNotificationComponentIds(notifId));
                    }
                    Set<String> vulnIds = extractRelationshipIdsFromResource(notifNode, "vulnerabilities");
                    if (vulnIds.isEmpty()) {
                        vulnIds.addAll(getNotificationVulnerabilityIds(notifId));
                    }
                    results.add(new NotificationComponentsAndVulnerabilities(notifId, compIds, vulnIds));
                }
            }

            String nextUrl = getNextLink(page);
            if (StringUtils.isBlank(nextUrl)) {
                break;
            }
            page = getJsonByAbsoluteUrl(nextUrl);
        }
        return results;
    }

    private Set<String> extractRelationshipIdsFromResource(JsonNode resourceNode, String relationshipName) {
        Set<String> ids = new LinkedHashSet<>();
        JsonNode relData = resourceNode.path("relationships").path(relationshipName).path("data");
        if (relData.isArray()) {
            for (JsonNode item : relData) {
                String id = readText(item, "id", null);
                if (StringUtils.isNotBlank(id)) {
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
        if (StringUtils.isBlank(resourceId)) {
            throw new SW360Exception("Invalid JSON:API vulnerability response: missing data.id");
        }

        JsonNode attributes = data.path("attributes");

        if (attributes.path("deleted").asBoolean(false)) {
            return Optional.empty();
        }

        Vulnerability vulnerability = new Vulnerability();

        String cve = attributes.path("cve").asText(null);
        String externalId = StringUtils.isNotBlank(cve) ? cve : data.path("id").asText();
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
        if (StringUtils.isNotBlank(selfLink)) {
            vilocifyMeta.put("selfLink", selfLink);
        }
        if (StringUtils.isNotBlank(bestVector)) {
            vilocifyMeta.put("cvssVector", bestVector);
        }
        if (StringUtils.isNotBlank(bestVersion)) {
            vilocifyMeta.put("cvssVersion", bestVersion);
        }
        if (!vilocifyMeta.isEmpty()) {
            Map<String, Map<String, String>> cveFurtherMetaDataPerSource = new HashMap<>();
            cveFurtherMetaDataPerSource.put("vilocify", vilocifyMeta);
            vulnerability.setCveFurtherMetaDataPerSource(cveFurtherMetaDataPerSource);
        }

        Set<String> assignedExtComponents = new HashSet<>();
        if (StringUtils.isNotBlank(componentId)) {
            assignedExtComponents.add(componentId);
        }
        if (!assignedExtComponents.isEmpty()) {
            vulnerability.setAssignedExtComponentIds(assignedExtComponents);
        }

        return Optional.of(vulnerability);
    }

    public Optional<String> createMonitoringList(String name, String comment)
            throws IOException, SW360Exception {
        if (isUnavailable(name)) {
            return Optional.empty();
        }
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", name.trim());
        if (StringUtils.isNotBlank(comment)) {
            attributes.put("comment", comment.trim());
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
        if (StringUtils.isNotBlank(name)) {
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

    public void replaceMonitoringListChildren(String listId, List<String> childIds)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/children",
                buildToManyRelationshipPayload("monitoringLists", childIds));
    }

    public JsonNode getMonitoringList(String listId, String includeParams)
            throws IOException, SW360Exception {
        if (isUnavailable(listId)) {
            return MAPPER.missingNode();
        }
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(includeParams)) {
            params.put("include", includeParams.trim());
        }
        return getJson("/monitoringLists/" + listId, params);
    }

    public List<String> getNotificationIdsAfter(String afterTimestamp) throws IOException, SW360Exception {
        if (isUnavailable()) {
            return Collections.emptyList();
        }
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(afterTimestamp)) {
            params.put("filter[updatedAt][after]", afterTimestamp.trim());
        }
        return collectAllIds("/notifications", params);
    }

    public List<String> getVulnerabilityIdsAfter(String afterTimestamp) throws IOException, SW360Exception {
        Map<String, String> queryParams = new HashMap<>();
        if (StringUtils.isNotBlank(afterTimestamp)) {
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
                if (StringUtils.isNotBlank(entry.getValue())) {
                    params.put(entry.getKey(), entry.getValue().trim());
                }
            }
        }

        params.putIfAbsent("filter[deleted][eq]", "false");
        return collectAllIds("/vulnerabilities", params);
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
        while (StringUtils.isNotBlank(nextUrl)) {
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
            if (!idNode.isMissingNode() && !idNode.isNull() && StringUtils.isNotBlank(idNode.asText())) {
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
        return StringUtils.isBlank(nextUrl) || "null".equalsIgnoreCase(nextUrl) ? null : nextUrl;
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
        return StringUtils.isBlank(value) ? fallback : value;
    }

    private JsonNode postJson(String path, Map<String, Object> payload) throws IOException, SW360Exception {
        HttpPost request = new HttpPost(buildUri(path, Collections.emptyMap()));
        addDefaultHeaders(request);
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(payload), JSON_API_CONTENT_TYPE));
        return executeJson(request);
    }

    private JsonNode patchJson(String path, Map<String, Object> payload) throws IOException, SW360Exception {
        HttpPatch request = new HttpPatch(buildUri(path, Collections.emptyMap()));
        addDefaultHeaders(request);
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(payload), JSON_API_CONTENT_TYPE));
        return executeJson(request);
    }

    private Map<String, Object> buildToManyRelationshipPayload(String type, List<String> ids) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                if (StringUtils.isNotBlank(id)) {
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
        while (StringUtils.isNotBlank(nextUrl)) {
            page = getJsonByAbsoluteUrl(nextUrl);
            appendIds(page, ids);
            nextUrl = getNextLink(page);
        }
        return ids;
    }

    private JsonNode getJson(String path, Map<String, String> queryParams) throws IOException, SW360Exception {
        HttpGet request = new HttpGet(buildUri(path, queryParams));
        addDefaultHeaders(request);
        return executeJson(request);
    }

    private Optional<JsonNode> getJsonAllowNotFound(String path, Map<String, String> queryParams)
            throws IOException, SW360Exception {
        HttpGet request = new HttpGet(buildUri(path, queryParams));
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
                if (entity != null && statusHolder[0] >= 200 && statusHolder[0] < 300) {
                    try (InputStream content = entity.getContent()) {
                        bodyHolder[0] = MAPPER.readTree(content);
                    }
                }
                return null;
            });

            int statusCode = statusHolder[0];
            if (statusCode == 429 && attempt < MAX_RATE_LIMIT_RETRIES) {
                log.warn("Vilocify API rate limit (429) hit on {} {}, retrying (attempt {}/{})",
                        request.getMethod(), request.getRequestUri(), attempt + 1, MAX_RATE_LIMIT_RETRIES);
                sleepQuietly(RATE_LIMIT_BACKOFF_MS * (attempt + 1));
                continue;
            }
            if (allowNotFound && statusCode == 404) {
                return Optional.empty();
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw new SW360Exception(request.getMethod() + " failed for Vilocify endpoint: HTTP "
                        + statusCode + " " + reasonHolder[0]);
            }
            return Optional.of(bodyHolder[0]);
        }
        throw new SW360Exception(request.getMethod() + " failed for Vilocify endpoint: rate limit retries exhausted");
    }

    @FunctionalInterface
    private interface StatusHandler<T> {
        T handle(int statusCode, String reasonPhrase);
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

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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
        return !isConfigured() || StringUtils.isBlank(requiredValue);
    }

    private URI buildUri(String path, Map<String, String> queryParams) {
        try {
            URIBuilder builder = new URIBuilder(normalizeBaseUrl() + normalizePath(rootPath) + normalizePath(path));
            boolean pageSizeSupplied = queryParams != null && queryParams.containsKey("page[size]");
            if (!pageSizeSupplied) {
                builder.addParameter("page[size]", String.valueOf(MAX_PAGE_SIZE));
            }
            if (queryParams != null) {
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    if (StringUtils.isNotBlank(entry.getValue())) {
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
        if (StringUtils.isBlank(baseUrl)) {
            return "";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private static String normalizePath(String path) {
        if (StringUtils.isBlank(path)) {
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

    private Map<String, String> loadProperties() {
        Map<String, String> values = new HashMap<>();
        Properties props = CommonUtils.loadProperties(VilocifyConnector.class, PROPERTIES_FILE_PATH);
        for (String name : props.stringPropertyNames()) {
            values.put(name, props.getProperty(name));
        }
        return values;
    }

    private static String readDynamicConfig(String key, String fallbackValue) {
        try {
            String value = SW360Utils.readConfig(key, fallbackValue);
            return StringUtils.isNotBlank(value) ? value : fallbackValue;
        } catch (Exception e) {
            return fallbackValue;
        }
    }

    private void validateExclusiveProvider() {
        String provider = getActiveProvider();
        if (StringUtils.isBlank(provider)) {
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
}
