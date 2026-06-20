/*
SPDX-FileCopyrightText: © 2026 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.datahandler.db;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Lightweight client for the Velocify v2 JSON:API.
 */
public class VelocifyConnector {

    private static final Logger log = Logger.getLogger(VelocifyConnector.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String JSON_API_MEDIA_TYPE = "application/vnd.api+json";
    private static final String JSON_API_CURSOR_PROFILE = "https://jsonapi.org/profiles/ethanresnick/cursor-pagination/";
    private static final String DEFAULT_API_ROOT_PATH = "/api/v2";
    private static final String PROVIDER_SVM = "svm";
    private static final String PROVIDER_VELOCIFY = "velocify";
    private static final int MAX_PAGE_SIZE = 200;

    private final String baseUrl;
    private final String rootPath;
    private final String token;

    public VelocifyConnector() {
        Map<String, String> props = loadProperties();
        this.baseUrl = trimToEmpty(props.getOrDefault(SW360ConfigKeys.VELOCIFY_API_BASE_URL, ""));
        this.rootPath = normalizePath(props.getOrDefault(SW360ConfigKeys.VELOCIFY_API_ROOT_PATH, DEFAULT_API_ROOT_PATH));
        this.token = trimToEmpty(props.getOrDefault(SW360ConfigKeys.VELOCIFY_API_TOKEN, ""));

        logMethodCall("<init>", "baseUrlPresent=" + StringUtils.isNotBlank(baseUrl)
                + ", rootPath=" + rootPath + ", tokenPresent=" + StringUtils.isNotBlank(token));
        validateExclusiveProvider();
    }

    /**
     * Package-private constructor for unit tests. Allows tests to point the
     * connector at a local HTTP server without reading {@code sw360.properties}.
     * Exclusive-provider validation is intentionally skipped so tests run
     * regardless of the {@code sync.integration.provider} setting.
     */
    @VisibleForTesting
    VelocifyConnector(String baseUrl, String rootPath, String token) {
        this.baseUrl = trimToEmpty(baseUrl);
        this.rootPath = normalizePath(rootPath != null ? rootPath : DEFAULT_API_ROOT_PATH);
        this.token = trimToEmpty(token);

        logMethodCall("<init>", "baseUrlPresent=" + StringUtils.isNotBlank(this.baseUrl)
                + ", rootPath=" + this.rootPath + ", tokenPresent=" + StringUtils.isNotBlank(this.token)
                + ", testingConstructor=true");
    }

    public boolean isConfigured() {
        // Also disabled when the mutual-exclusivity switch selects SVM instead
        String provider = SW360Constants.SYNC_INTEGRATION_PROVIDER;
        logMethodCall("isConfigured", "provider=" + provider + ", baseUrlPresent=" + StringUtils.isNotBlank(baseUrl)
                + ", tokenPresent=" + StringUtils.isNotBlank(token));
        if (StringUtils.isNotBlank(provider) && PROVIDER_SVM.equalsIgnoreCase(provider.trim())) {
            return false;
        }
        return StringUtils.isNotBlank(baseUrl) && StringUtils.isNotBlank(token);
    }

    private String resolveCaller() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (VelocifyConnector.class.getName().equals(element.getClassName())) {
                continue;
            }
            return element.getClassName() + "." + element.getMethodName() + ":" + element.getLineNumber();
        }
        return "unknown";
    }

    public RequestStatus sendProjectExportForMonitoringLists(String jsonString) throws IOException, SW360Exception {
        logMethodCall("sendProjectExportForMonitoringLists", "payloadLength=" + (jsonString == null ? 0 : jsonString.length()));
        if (isUnavailable(jsonString)) {
            return RequestStatus.SUCCESS;
        }

        JsonNode projects = MAPPER.readTree(jsonString);
        if (!projects.isArray()) {
            throw new SW360Exception("Invalid project export payload: expected JSON array");
        }

        Map<String, String> listIdsByApplicationId = new HashMap<>();

        // Pass 1: create/update monitoring lists and remember list IDs by project application_id.
        for (JsonNode projectNode : projects) {
            String applicationId = readText(projectNode, "application_id", null);
            if (StringUtils.isBlank(applicationId)) {
                continue;
            }

            String listName = buildMonitoringListName(projectNode, applicationId);
            String listComment = buildMonitoringListComment(projectNode, applicationId);

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

        // Pass 2: update list relationships once all list IDs are known.
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

        log.info("Velocify monitoring list synchronization completed for " + listIdsByApplicationId.size()
                + " projects");
        return RequestStatus.SUCCESS;
    }

    private Optional<String> findMonitoringListIdForProject(String applicationId, String listName)
            throws IOException, SW360Exception {
        if (StringUtils.isBlank(applicationId)) {
            return Optional.empty();
        }

        JsonNode page = getJson("/monitoringLists", Map.of("page[size]", String.valueOf(MAX_PAGE_SIZE)));
        while (true) {
            JsonNode data = page.path("data");
            if (data.isArray()) {
                for (JsonNode entry : data) {
                    JsonNode attributes = entry.path("attributes");
                    String existingName = readText(attributes, "name", null);
                    String existingComment = readText(attributes, "comment", null);
                    boolean matchesByName = StringUtils.isNotBlank(listName) && StringUtils.equals(existingName, listName);
                    boolean matchesByCommentMarker = StringUtils.contains(existingComment,
                            "SW360 project id: " + applicationId);
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

    private String buildMonitoringListName(JsonNode projectNode, String applicationId) {
        String applicationName = readText(projectNode, "application_name", "SW360 Project");
        String applicationVersion = readText(projectNode, "application_version", "");
        if (StringUtils.isBlank(applicationVersion)) {
            return applicationName + " [SW360:" + applicationId + "]";
        }
        return applicationName + " (" + applicationVersion + ") [SW360:" + applicationId + "]";
    }

    private String buildMonitoringListComment(JsonNode projectNode, String applicationId) {
        logMethodCall("buildMonitoringListComment", "applicationId=" + applicationId);
        String businessUnit = readText(projectNode, "business_unit", null);
        if (StringUtils.isBlank(businessUnit)) {
            return "SW360 project id: " + applicationId;
        }
        return "SW360 project id: " + applicationId + ", business unit: " + businessUnit;
    }

    private List<String> extractStringArray(JsonNode arrayNode) {
        logMethodCall("extractStringArray", null);
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
        logMethodCall("resolveMembershipRelationshipIds", "valueCount=" + (rawValues == null ? 0 : rawValues.size()));
        if (rawValues == null || rawValues.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> membershipIds = new ArrayList<>();
        for (String rawValue : rawValues) {
            if (StringUtils.isBlank(rawValue)) {
                continue;
            }

            // The export payload currently contains user GIDs; keep them as-is.
            // If emails are provided, resolve/create memberships first.
            if (!StringUtils.contains(rawValue, "@")) {
                membershipIds.add(rawValue);
                continue;
            }

            Optional<String> membershipId = getMembershipIdByEmail(rawValue);
            if (membershipId.isEmpty()) {
                membershipId = createMembership(rawValue);
            }
            membershipId.ifPresent(membershipIds::add);
        }
        return membershipIds;
    }

    private List<String> extractComponentIds(JsonNode componentsArray) {
        logMethodCall("extractComponentIds", null);
        if (!componentsArray.isArray()) {
            return Collections.emptyList();
        }

        List<String> componentIds = new ArrayList<>();
        for (JsonNode component : componentsArray) {
            if (component == null || component.isNull()) {
                continue;
            }
            String componentId = readText(component, "svm_component_id", null);
            if (StringUtils.isBlank(componentId)) {
                continue;
            }
            componentIds.add(componentId.trim());
        }
        return componentIds;
    }

    private List<String> resolveChildMonitoringListIds(JsonNode childApplicationIds,
                                                        JsonNode existingMonitoringListIds,
                                                        Map<String, String> listIdsByApplicationId) {
        logMethodCall("resolveChildMonitoringListIds", "childApplicationIdsPresent=" + childApplicationIds.isArray()
            + ", existingMonitoringListIdsPresent=" + existingMonitoringListIds.isArray());
        Set<String> childIds = new HashSet<>();

        // Prefer resolving child app IDs to monitoring list IDs created/found in this sync run.
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

        // Keep already known external list IDs if provided in export payload.
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

    public Optional<String> findComponentIdByRelease(Release release) throws IOException, SW360Exception {
        logMethodCall("findComponentIdByRelease", release == null ? "release=null" : "releaseId=" + release.getId() + ", releaseName=" + release.getName() + ", releaseVersion=" + release.getVersion());
        if (!isConfigured()) {
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
        logMethodCall("findMatchingComponentId", "packageUrl=" + packageUrl + ", releaseName=" + releaseName + ", releaseVersion=" + releaseVersion);
        JsonNode data = response.path("data");
        if (!data.isArray() || data.size() == 0) {
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
        logMethodCall("createComponentRequest", release == null ? "release=null" : "releaseId=" + release.getId() + ", releaseName=" + release.getName() + ", releaseVersion=" + release.getVersion());
        if (isUnavailable()) {
            return Optional.empty();
        }

        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "componentRequests");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("name", release.getName());
        attributes.put("version", release.getVersion());
        // Velocify requires at least one of 'vendor' or 'securityUrl'.
        // Add the release vendor fullname when available.
        if (release.isSetVendor() && StringUtils.isNotBlank(release.getVendor().getFullname())) {
            attributes.put("vendor", release.getVendor().getFullname());
        }
        data.put("attributes", attributes);
        payload.put("data", data);

        JsonNode response = postJson("/componentRequests", payload);
        return extractResourceId(response);
    }

    public Optional<String> resolveMappedComponentId(String requestId) throws IOException, SW360Exception {
        logMethodCall("resolveMappedComponentId", "requestId=" + requestId);
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
        return componentId.isMissingNode() || componentId.isNull() ? Optional.empty() : Optional.of(componentId.asText());
    }

    /**
     * Check the status of a componentRequest and return the component ID if mapped.
     *
     * <p>This method is intentionally non-destructive: it does not delete the
     * componentRequest. Deletion is performed by the caller only after SW360
     * persistence succeeds.
     *
     * @param requestId The componentRequest ID
     * @return Optional component ID if the request is mapped, empty otherwise
     * @throws IOException if HTTP communication fails
     * @throws SW360Exception if Velocify API returns an error
     */
    public Optional<String> checkAndResolveComponentRequest(String requestId) throws IOException, SW360Exception {
        logMethodCall("checkAndResolveComponentRequest", "requestId=" + requestId);
        if (isUnavailable(requestId)) {
            return Optional.empty();
        }

        // GET /api/v2/componentRequests/{id} to check status
        JsonNode response = getJson("/componentRequests/" + requestId, Collections.emptyMap());
        JsonNode data = response.path("data");
        
        // Check if state is "mapped"
        JsonNode attributes = data.path("attributes");
        String state = attributes.path("state").asText();
        
        if ("mapped".equalsIgnoreCase(state)) {
            log.info("ComponentRequest " + requestId + " is mapped, extracting component ID");
            
            // Extract component ID from relationship
            JsonNode relationships = data.path("relationships");
            JsonNode component = relationships.path("component").path("data");
            
            if (component.isMissingNode() || component.isNull()) {
                log.warn("ComponentRequest " + requestId + " is mapped but no component relationship found");
                return Optional.empty();
            }
            
            JsonNode componentIdNode = component.path("id");
            if (componentIdNode.isMissingNode() || componentIdNode.isNull()) {
                log.warn("ComponentRequest " + requestId + " is mapped but component ID is null");
                return Optional.empty();
            }

            return Optional.of(componentIdNode.asText());
        } else {
            log.debug("ComponentRequest " + requestId + " is in state: " + state + ", not yet mapped");
            return Optional.empty();
        }
    }

    /**
     * Delete a componentRequest from Velocify.
     * 
     * @param requestId The componentRequest ID to delete
     * @return true if deletion was successful, false otherwise
     * @throws IOException if HTTP communication fails
     * @throws SW360Exception if Velocify API returns an error
     */
    public boolean deleteComponentRequest(String requestId) throws IOException, SW360Exception {
        logMethodCall("deleteComponentRequest", "requestId=" + requestId);
        if (isUnavailable(requestId)) {
            return false;
        }

        String path = "/componentRequests/" + requestId;
        URI uri = buildUri(path, Collections.emptyMap());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpDelete request = new HttpDelete(uri);
            addDefaultHeaders(request);
            
            try (CloseableHttpResponse response = client.execute(request)) {
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                
                // 204 No Content is the expected success response for DELETE
                if (statusCode == 204 || statusCode == 200) {
                    log.info("Successfully deleted componentRequest: " + requestId);
                    return true;
                } else if (statusCode == 404) {
                    log.warn("ComponentRequest " + requestId + " not found (404), may already be deleted");
                    return true; // Consider 404 as success since the resource is gone
                } else {
                    log.error("Failed to delete componentRequest " + requestId + ": " + statusCode + " " + statusLine.getReasonPhrase());
                    return false;
                }
            }
        }
    }

    public Optional<String> getMembershipIdByEmail(String email) throws IOException, SW360Exception {
        logMethodCall("getMembershipIdByEmail", "email=" + email);
        if (isUnavailable(email)) {
            return Optional.empty();
        }
        return firstResourceId("/memberships", Map.of("filter[email]", email));
    }

    public Optional<String> createMembership(String email) throws IOException, SW360Exception {
        logMethodCall("createMembership", "email=" + email);
        if (isUnavailable(email)) {
            return Optional.empty();
        }
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("type", "memberships");
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        data.put("attributes", attributes);
        payload.put("data", data);

        JsonNode response = postJson("/memberships", payload);
        return extractResourceId(response);
    }

    public List<String> getComponentNotificationIds(String componentId) throws IOException, SW360Exception {
        logMethodCall("getComponentNotificationIds", "componentId=" + componentId);
        if (isUnavailable(componentId)) {
            return Collections.emptyList();
        }
        return getRelationshipIdsWithPagination("/components/" + componentId + "/relationships/notifications");
    }

    public List<String> getNotificationVulnerabilityIds(String notificationId) throws IOException, SW360Exception {
        logMethodCall("getNotificationVulnerabilityIds", "notificationId=" + notificationId);
        if (isUnavailable(notificationId)) {
            return Collections.emptyList();
        }
        return getRelationshipIdsWithPagination("/notifications/" + notificationId + "/relationships/vulnerabilities");
    }

    public Optional<Vulnerability> getVulnerabilityAsSw360(String vulnerabilityId, String componentId)
            throws IOException, SW360Exception {
        logMethodCall("getVulnerabilityAsSw360", "vulnerabilityId=" + vulnerabilityId + ", componentId=" + componentId);
        if (isUnavailable(vulnerabilityId)) {
            return Optional.empty();
        }

        // Use 404-tolerant fetch: a deleted or unknown vulnerability should not be an error
        Optional<JsonNode> responseOpt = getJsonAllowNotFound("/vulnerabilities/" + vulnerabilityId,
                Collections.emptyMap());
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

        // Skip vulnerabilities that have been soft-deleted on the Vilocify side
        if (attributes.path("deleted").asBoolean(false)) {
            return Optional.empty();
        }

        Vulnerability vulnerability = new Vulnerability();

        // 'cve' is the primary identifier; fall back to the numeric resource id
        String cve = attributes.path("cve").asText(null);
        String externalId = StringUtils.isNotBlank(cve) ? cve : data.path("id").asText();
        vulnerability.setExternalId(externalId);
        // API has no separate title field; use the CVE id as the title
        vulnerability.setTitle(cve);
        vulnerability.setDescription(readText(attributes, "description", null));
        vulnerability.setCwe(readText(attributes, "cwe", null));

        // 'mitigatingFactor' is the Vilocify field closest to SW360's 'action'
        vulnerability.setAction(readText(attributes, "mitigatingFactor", null));
        // 'note' is the Vilocify field closest to SW360's 'legalNotice'
        vulnerability.setLegalNotice(readText(attributes, "note", null));

        // 'cvss' is an array of scoring entries; map the highest base_score to SW360's cvss field
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

    /**
     * Creates a new monitoringList in Vilocify.
     *
     * @param name    required name for the list (max 255 chars)
     * @param comment optional description, or {@code null}
     * @return the UUID of the created monitoringList, or empty if not configured / name blank
     */
    public Optional<String> createMonitoringList(String name, String comment)
            throws IOException, SW360Exception {
        logMethodCall("createMonitoringList", "name=" + name);
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

    /**
     * Updates the attributes (name / comment) of an existing monitoringList.
     * Sends {@code PATCH /api/v2/monitoringLists/{id}}.
     *
     * @param listId  UUID of the monitoringList to update
     * @param name    new name, or {@code null} / blank to leave unchanged
     * @param comment new comment value; pass {@code null} to leave unchanged,
     *                pass an empty string to clear it
     */
    public void updateMonitoringList(String listId, String name, String comment)
            throws IOException, SW360Exception {
        logMethodCall("updateMonitoringList", "listId=" + listId + ", name=" + name);
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

    /**
     * Replaces the full set of user (membership) relationships on a monitoringList.
     * Sends {@code PATCH /api/v2/monitoringLists/{id}/relationships/users}.
     * Passing an empty list clears all users from the list.
     *
     * @param listId        UUID of the monitoringList
     * @param membershipIds membership UUIDs to set as the complete user set
     */
    public void replaceMonitoringListUsers(String listId, List<String> membershipIds)
            throws IOException, SW360Exception {
        logMethodCall("replaceMonitoringListUsers", "listId=" + listId + ", membershipCount=" + (membershipIds == null ? 0 : membershipIds.size()));
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/users",
                buildToManyRelationshipPayload("memberships", membershipIds));
    }

    /**
     * Replaces the full set of component relationships on a monitoringList.
     * Sends {@code PATCH /api/v2/monitoringLists/{id}/relationships/components}.
     * Passing an empty list clears all components from the list.
     *
     * @param listId       UUID of the monitoringList
     * @param componentIds Vilocify component IDs to set as the complete component set
     */
    public void replaceMonitoringListComponents(String listId, List<String> componentIds)
            throws IOException, SW360Exception {
        logMethodCall("replaceMonitoringListComponents", "listId=" + listId + ", componentCount=" + (componentIds == null ? 0 : componentIds.size()));
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/components",
                buildToManyRelationshipPayload("components", componentIds));
    }

    /**
     * Replaces the full set of child monitoringList relationships.
     * Sends {@code PATCH /api/v2/monitoringLists/{id}/relationships/children}.
     * Passing an empty list removes all sub-projects from the list.
     *
     * @param listId   UUID of the parent monitoringList
     * @param childIds UUIDs of child monitoringLists to set as the complete children set
     */
    public void replaceMonitoringListChildren(String listId, List<String> childIds)
            throws IOException, SW360Exception {
        logMethodCall("replaceMonitoringListChildren", "listId=" + listId + ", childCount=" + (childIds == null ? 0 : childIds.size()));
        if (isUnavailable(listId)) {
            return;
        }
        patchJson("/monitoringLists/" + listId + "/relationships/children",
                buildToManyRelationshipPayload("monitoringLists", childIds));
    }

    /**
     * Fetches a monitoringList by ID, optionally requesting related resources in a
     * single call via the JSON:API {@code include} parameter (reduces round-trips).
     *
     * @param listId        UUID of the monitoringList
     * @param includeParams comma-separated relationship names to include
     *                      (e.g. {@code "components,children"}), or {@code null}
     * @return the raw JSON:API response node, or a missing node when not configured
     */
    public JsonNode getMonitoringList(String listId, String includeParams)
            throws IOException, SW360Exception {
        logMethodCall("getMonitoringList", "listId=" + listId + ", include=" + includeParams);
        if (isUnavailable(listId)) {
            return MAPPER.missingNode();
        }
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(includeParams)) {
            params.put("include", includeParams.trim());
        }
        return getJson("/monitoringLists/" + listId, params);
    }

    /**
     * Returns all notification IDs whose {@code updatedAt} timestamp is after
     * the given value (incremental fetch). All pages are traversed via cursor
     * pagination.
     *
     * @param afterTimestamp ISO-8601 datetime string (e.g. {@code "2024-05-01T00:00:00Z"});
     *                       pass {@code null} or blank to fetch all notifications
     * @return list of notification resource IDs
     */
    public List<String> getNotificationIdsAfter(String afterTimestamp) throws IOException, SW360Exception {
        logMethodCall("getNotificationIdsAfter", "afterTimestamp=" + afterTimestamp);
        if (isUnavailable()) {
            return Collections.emptyList();
        }
        Map<String, String> params = new HashMap<>();
        if (StringUtils.isNotBlank(afterTimestamp)) {
            params.put("filter[updatedAt][after]", afterTimestamp.trim());
        }
        return collectAllIds("/notifications", params);
    }

    /**
     * Returns all non-deleted vulnerability IDs whose {@code updatedAt} timestamp is after
     * the given value (incremental fetch). All pages are traversed via cursor pagination
     * (max {@value #MAX_PAGE_SIZE} per page).
     *
     * <p>Uses {@code filter[deleted][eq]=false} so that soft-deleted vulnerabilities are
     * excluded at the API level, avoiding unnecessary quota consumption on records that
     * would be skipped anyway during individual fetch in {@link #getVulnerabilityAsSw360}.
     *
     * @param afterTimestamp ISO-8601 datetime string (e.g. {@code "2024-05-01T00:00:00Z"});
     *                       pass {@code null} or blank to fetch all non-deleted vulnerabilities
     * @return list of vulnerability resource IDs
     */
    public List<String> getVulnerabilityIdsAfter(String afterTimestamp) throws IOException, SW360Exception {
        logMethodCall("getVulnerabilityIdsAfter", "afterTimestamp=" + afterTimestamp);
        Map<String, String> queryParams = new HashMap<>();
        if (StringUtils.isNotBlank(afterTimestamp)) {
            queryParams.put("filter[updatedAt][after]", afterTimestamp.trim());
        }
        return getVulnerabilityIds(queryParams);
    }

    /**
     * Returns vulnerability IDs using the provided query parameters as-is.
     *
     * <p>Use JSON:API filter/sort field names as keys, for example:
     * <ul>
     *     <li>{@code filter[createdAt][after]}</li>
     *     <li>{@code filter[updatedAt][after]}</li>
     *     <li>{@code filter[id][eq]}</li>
     *     <li>{@code filter[id][in]}</li>
     *     <li>{@code filter[notifications.id][any]}</li>
     *     <li>{@code filter[deleted][eq]}</li>
     *     <li>{@code sort}</li>
     *     <li>{@code fields[vulnerabilities]}</li>
     * </ul>
     *
     * <p>If {@code filter[deleted][eq]} is not provided, it defaults to {@code false}
     * so soft-deleted vulnerabilities are excluded.
     */
    public List<String> getVulnerabilityIds(Map<String, String> queryParams) throws IOException, SW360Exception {
        logMethodCall("getVulnerabilityIds", "queryKeys=" + (queryParams == null ? 0 : queryParams.size()));
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
        return idNode == null || idNode.isMissingNode() || idNode.isNull() ? Optional.empty() : Optional.of(idNode.asText());
    }

    private Optional<String> firstResourceId(String path, Map<String, String> queryParams) throws IOException, SW360Exception {
        logMethodCall("firstResourceId", "path=" + path + ", queryKeys=" + (queryParams == null ? 0 : queryParams.size()));
        JsonNode response = getJson(path, queryParams);
        return extractResourceId(response);
    }

    private List<String> getRelationshipIdsWithPagination(String relativePath) throws IOException, SW360Exception {
        logMethodCall("getRelationshipIdsWithPagination", "relativePath=" + relativePath);
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
        logMethodCall("appendIds", "currentCount=" + (ids == null ? 0 : ids.size()));
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
        logMethodCall("getNextLink", null);
        JsonNode next = response.path("links").path("next");
        if (next.isMissingNode() || next.isNull()) {
            return null;
        }
        String nextUrl = next.asText();
        return StringUtils.isBlank(nextUrl) || "null".equalsIgnoreCase(nextUrl) ? null : nextUrl;
    }

    private JsonNode getJsonByAbsoluteUrl(String url) throws IOException, SW360Exception {
        logMethodCall("getJsonByAbsoluteUrl", "url=" + url);
        // Velocify's pagination next-links are returned as relative paths (e.g. "/api/v2/...");
        // prepend the configured base URL so the request targets the correct host.
        if (url.startsWith("/")) {
            url = normalizeBaseUrl() + url;
        }
        HttpGet request = new HttpGet(url);
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
        logMethodCall("postJson", "path=" + path + ", payloadKeys=" + (payload == null ? 0 : payload.size()));
        HttpPost request = new HttpPost(buildUri(path, Collections.emptyMap()));
        addDefaultHeaders(request);
        // Use the JSON:API content type directly on the entity so it is not overridden
        // by HttpClient's default Content-Type handling.
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(payload),
                ContentType.create(JSON_API_MEDIA_TYPE, StandardCharsets.UTF_8)));
        return executeJson(request);
    }

    private JsonNode patchJson(String path, Map<String, Object> payload) throws IOException, SW360Exception {
        logMethodCall("patchJson", "path=" + path + ", payloadKeys=" + (payload == null ? 0 : payload.size()));
        HttpPatch request = new HttpPatch(buildUri(path, Collections.emptyMap()));
        addDefaultHeaders(request);
        request.setEntity(new StringEntity(MAPPER.writeValueAsString(payload),
                ContentType.create(JSON_API_MEDIA_TYPE, StandardCharsets.UTF_8)));
        return executeJson(request);
    }

    /**
     * Builds a JSON:API to-many relationship payload, e.g.:
     * {@code {"data": [{"type": "components", "id": "123"}, ...]}}
     */
    private Map<String, Object> buildToManyRelationshipPayload(String type, List<String> ids) {
        logMethodCall("buildToManyRelationshipPayload", "type=" + type + ", idCount=" + (ids == null ? 0 : ids.size()));
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

    /** Fetches all pages for a path+query and collects resource IDs. */
    private List<String> collectAllIds(String path, Map<String, String> params)
            throws IOException, SW360Exception {
        logMethodCall("collectAllIds", "path=" + path + ", queryKeys=" + (params == null ? 0 : params.size()));
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
        logMethodCall("getJson", "path=" + path + ", queryKeys=" + (queryParams == null ? 0 : queryParams.size()));
        HttpGet request = new HttpGet(buildUri(path, queryParams));
        addDefaultHeaders(request);
        return executeJson(request);
    }

    /**
     * Like {@link #getJson} but returns {@link Optional#empty()} for HTTP 404 (resource not found)
     * instead of throwing, and still propagates {@link SW360Exception} for all other non-2xx codes
     * (401 unauthorized, 402 quota exceeded, 403 forbidden, 415 media type, 422 validation, 500 server error).
     */
    private Optional<JsonNode> getJsonAllowNotFound(String path, Map<String, String> queryParams)
            throws IOException, SW360Exception {
        logMethodCall("getJsonAllowNotFound", "path=" + path + ", queryKeys=" + (queryParams == null ? 0 : queryParams.size()));
        HttpGet request = new HttpGet(buildUri(path, queryParams));
        addDefaultHeaders(request);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = httpClient.execute(request)) {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode == 404) {
                return Optional.empty();
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw new SW360Exception("GET failed for Velocify endpoint: HTTP " + statusCode + " "
                        + httpResponse.getStatusLine().getReasonPhrase());
            }
            JsonNode body = httpResponse.getEntity() == null
                    ? MAPPER.missingNode()
                    : MAPPER.readTree(httpResponse.getEntity().getContent());
            return Optional.of(body);
        }
    }

    private JsonNode executeJson(HttpRequestBase request) throws IOException, SW360Exception {
        logMethodCall("executeJson", request == null ? "request=null" : "method=" + request.getMethod());
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse httpResponse = httpClient.execute(request)) {
            StatusLine statusLine = httpResponse.getStatusLine();
            requireSuccess(statusLine, request.getMethod() + " failed for Velocify endpoint");
            return MAPPER.readTree(httpResponse.getEntity().getContent());
        }
    }

    private void addDefaultHeaders(HttpRequestBase request) {
        logMethodCall("addDefaultHeaders", request == null ? "request=null" : "method=" + request.getMethod());
        request.addHeader(new BasicHeader(HttpHeaders.ACCEPT, JSON_API_MEDIA_TYPE));
        request.addHeader(new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private boolean isUnavailable() {
        return !isConfigured();
    }

    private boolean isUnavailable(String requiredValue) {
        return !isConfigured() || StringUtils.isBlank(requiredValue);
    }

    private void requireSuccess(StatusLine statusLine, String errorMessage) throws SW360Exception {
        logMethodCall("requireSuccess", statusLine == null ? "statusLine=null" : "statusCode=" + statusLine.getStatusCode());
        int statusCode = statusLine.getStatusCode();
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }
        throw new SW360Exception(errorMessage + ": HTTP " + statusCode + " " + statusLine.getReasonPhrase());
    }

    private URI buildUri(String path, Map<String, String> queryParams) {
        logMethodCall("buildUri", "path=" + path + ", queryKeys=" + (queryParams == null ? 0 : queryParams.size()));
        try {
            URIBuilder builder = new URIBuilder(normalizeBaseUrl() + normalizePath(rootPath) + normalizePath(path));
            // Only inject the default page size when the caller has not supplied one,
            // to avoid duplicate page[size] parameters in the query string.
            boolean pageSizeSupplied = queryParams.containsKey("page[size]");
            if (!pageSizeSupplied) {
                builder.addParameter("page[size]", String.valueOf(MAX_PAGE_SIZE));
            }
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    builder.addParameter(entry.getKey(), entry.getValue());
                }
            }
            return builder.build();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid Velocify URI", e);
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

    private Map<String, String> loadProperties() {
        logMethodCall("loadProperties", null);
        Map<String, String> values = new HashMap<>();
        java.util.Properties props = CommonUtils.loadProperties(VelocifyConnector.class, PROPERTIES_FILE_PATH);
        for (String name : props.stringPropertyNames()) {
            values.put(name, props.getProperty(name));
        }
        return values;
    }

    private void validateExclusiveProvider() {
        String provider = SW360Constants.SYNC_INTEGRATION_PROVIDER;
        logMethodCall("validateExclusiveProvider", "provider=" + provider);
        if (StringUtils.isBlank(provider)) {
            return;
        }
        String normalizedProvider = provider.trim().toLowerCase();
        if (!Objects.equals(normalizedProvider, PROVIDER_SVM)
                && !Objects.equals(normalizedProvider, PROVIDER_VELOCIFY)) {
            throw new IllegalStateException(
                "sync.integration.provider must be either '" + PROVIDER_SVM + "' or '"
                        + PROVIDER_VELOCIFY + "', got: '" + provider + "'");
        }
        if (Objects.equals(normalizedProvider, PROVIDER_SVM)) {
            log.info("sync.integration.provider=svm: Velocify integration is disabled in favour of SVM");
        }
    }

    private void logMethodCall(String methodName, String context) {
        if (log.isDebugEnabled()) {
            String message = context != null ? methodName + ": " + context : methodName;
            log.debug(message);
        }
    }
}