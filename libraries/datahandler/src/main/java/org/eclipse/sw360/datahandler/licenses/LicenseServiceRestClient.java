/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.licenses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.eclipse.sw360.datahandler.services.common.CustomProperties;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.licenses.LicenseType;
import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.datahandler.services.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.services.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.services.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP implementation of {@link LicenseClient}.
 *
 * Maps to {@code LicenseController} under {@code /licenses/api/licenses}.
 */
public class LicenseServiceRestClient implements LicenseClient {

    private static final String BASE = "/licenses/api/licenses";

    private static final ParameterizedTypeReference<List<License>> LICENSE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<LicenseType>> LICENSE_TYPE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Obligation>> OBLIGATION_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ObligationElement>> OBLIGATION_ELEMENT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ObligationNode>> OBLIGATION_NODE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CustomProperties>> CUSTOM_PROP_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PaginatedResult<Obligation>> OBLIGATION_PAGE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;

    public LicenseServiceRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static <T> T call(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body,
                    e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new SW360Exception(e.getMessage(), 503, e);
        }
    }

    private static void addUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        headers.set("X-User-Email", user.getEmail());
        if (user.getDepartment() != null) {
            headers.set("X-User-Department", user.getDepartment());
        }
        headers.set("X-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    private static void addRequestingUser(HttpHeaders headers, User user) {
        if (user == null) {
            return;
        }
        headers.set("X-Requesting-User-Email", user.getEmail());
        if (user.getDepartment() != null) {
            headers.set("X-Requesting-User-Department", user.getDepartment());
        }
        headers.set("X-Requesting-User-Group", user.getUserGroup() != null ? user.getUserGroup().name() : "");
    }

    @Override
    public License getByID(String id, String organisation) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}").queryParam("organisation", organisation).build(id))
                .retrieve()
                .body(License.class));
    }

    @Override
    public License getByIDWithOwnModerationRequests(String id, String organisation, User user) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/{id}/with-moderation").queryParam("organisation", organisation).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(License.class));
    }

    @Override
    public List<License> getByIds(Set<String> ids, String organisation) {
        List<License> list = call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/by-ids").queryParam("organisation", organisation).build())
                .body(ids)
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<License> getLicenseSummary() {
        List<License> list = call(() -> restClient.get()
                .uri(BASE + "/summary")
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<License> getLicenseSummaryForExport() {
        List<License> list = call(() -> restClient.get()
                .uri(BASE + "/summary/export")
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<License> getDetailedLicenseSummaryForExport(String organisation) {
        List<License> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/detailed-summary/export").queryParam("organisation", organisation).build())
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<License> getDetailedLicenseSummary(String organisation, List<String> identifiers) {
        List<License> list = call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/detailed-summary").queryParam("organisation", organisation).build())
                .body(identifiers)
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<License> getLicenses() {
        List<License> list = call(() -> restClient.get()
                .uri(BASE)
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<License> addLicenses(List<License> licenses, User user) {
        List<License> list = call(() -> restClient.post()
                .uri(BASE + "/bulk")
                .headers(h -> addUser(h, user))
                .body(licenses)
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<License> addOrOverwriteLicenses(List<License> licenses, User user) {
        List<License> list = call(() -> restClient.post()
                .uri(BASE + "/bulk/overwrite")
                .headers(h -> addUser(h, user))
                .body(licenses)
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public RequestStatus updateLicense(License license, User user, User requestingUser) {
        return call(() -> restClient.put()
                .uri(BASE)
                .headers(h -> {
                    addUser(h, user);
                    addRequestingUser(h, requestingUser);
                })
                .body(license)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus updateLicenseFromModerationRequest(License additions, License deletions, User user,
            User requestingUser) {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", additions);
        body.put("deletions", deletions);
        return call(() -> restClient.put()
                .uri(BASE + "/from-moderation")
                .headers(h -> {
                    addUser(h, user);
                    addRequestingUser(h, requestingUser);
                })
                .body(body)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus updateWhitelist(String licenseId, Set<String> obligationsDatabaseIds, User user) {
        return call(() -> restClient.put()
                .uri(b -> b.path(BASE + "/{id}/whitelist").build(licenseId))
                .headers(h -> addUser(h, user))
                .body(obligationsDatabaseIds)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestStatus deleteLicense(String licenseId, User user) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}").build(licenseId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public RequestSummary deleteAllLicenseInformation(User user) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(BASE + "/all")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestSummary.class));
    }

    @Override
    public RequestSummary importAllSpdxLicenses(User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/import-spdx")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestSummary.class));
    }

    @Override
    public RequestSummary importAllOSADLLicenses(User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/import-osadl")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestSummary.class));
    }

    @Override
    public List<License> searchLicense(String searchText) {
        List<License> list = call(() -> restClient.get()
                .uri(b -> {
                    var ub = b.path(BASE + "/search");
                    if (searchText != null) {
                        ub.queryParam("searchText", searchText);
                    }
                    return ub.build();
                })
                .retrieve()
                .body(LICENSE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public byte[] downloadExcel(String token) {
        byte[] bytes = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/download-excel").queryParam("token", token).build())
                .retrieve()
                .body(byte[].class));
        return bytes == null ? new byte[0] : bytes;
    }

    @Override
    public byte[] getLicenseReportDataStream() {
        byte[] bytes = call(() -> restClient.get()
                .uri(BASE + "/report-stream")
                .retrieve()
                .body(byte[].class));
        return bytes == null ? new byte[0] : bytes;
    }

    @Override
    public RequestStatus addLicenseType(LicenseType licenseType, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/types")
                .headers(h -> addUser(h, user))
                .body(licenseType)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user) {
        List<LicenseType> list = call(() -> restClient.post()
                .uri(BASE + "/types/bulk")
                .headers(h -> addUser(h, user))
                .body(licenseTypes)
                .retrieve()
                .body(LICENSE_TYPE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<LicenseType> getLicenseTypes() {
        List<LicenseType> list = call(() -> restClient.get()
                .uri(BASE + "/types")
                .retrieve()
                .body(LICENSE_TYPE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<LicenseType> getLicenseTypesByIds(List<String> ids) {
        List<LicenseType> list = call(() -> restClient.post()
                .uri(BASE + "/types/by-ids")
                .body(ids)
                .retrieve()
                .body(LICENSE_TYPE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public LicenseType getLicenseTypeById(String id) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/types/{id}").build(id))
                .retrieve()
                .body(LicenseType.class));
    }

    @Override
    public RequestStatus deleteLicenseType(String id, User user) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/types/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public int checkLicenseTypeInUse(String id) {
        Integer count = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/types/{id}/in-use").build(id))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public List<LicenseType> searchByLicenseType(String licenseType) {
        List<LicenseType> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/types/search").queryParam("licenseType", licenseType).build())
                .retrieve()
                .body(LICENSE_TYPE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public String addObligations(Obligation obligations, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(obligations)
                .retrieve()
                .body(String.class));
    }

    @Override
    public String updateObligation(Obligation obligation, User user) {
        return call(() -> restClient.put()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(obligation)
                .retrieve()
                .body(String.class));
    }

    @Override
    public RequestStatus addObligationsToLicense(Set<Obligation> obligations, License license, User user) {
        Map<String, Object> body = new HashMap<>();
        body.put("obligations", obligations == null ? Set.of() : obligations);
        body.put("license", license);
        return call(() -> restClient.post()
                .uri(BASE + "/obligations/to-license")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public List<Obligation> addListOfObligations(List<Obligation> obligations, User user) {
        List<Obligation> list = call(() -> restClient.post()
                .uri(BASE + "/obligations/bulk")
                .headers(h -> addUser(h, user))
                .body(obligations)
                .retrieve()
                .body(OBLIGATION_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<Obligation> getObligations() {
        List<Obligation> list = call(() -> restClient.get()
                .uri(BASE + "/obligations")
                .retrieve()
                .body(OBLIGATION_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<Obligation> getObligationsByIds(List<String> ids) {
        List<Obligation> list = call(() -> restClient.post()
                .uri(BASE + "/obligations/by-ids")
                .body(ids)
                .retrieve()
                .body(OBLIGATION_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public List<Obligation> getObligationsByLicenseId(String id) {
        List<Obligation> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/obligations/by-license/{id}").build(id))
                .retrieve()
                .body(OBLIGATION_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public Obligation getObligationsById(String id) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/obligations/{id}").build(id))
                .retrieve()
                .body(Obligation.class));
    }

    @Override
    public RequestStatus deleteObligations(String id, User user) {
        return call(() -> restClient.method(HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/obligations/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(RequestStatus.class));
    }

    @Override
    public String convertTextToNode(Obligation obligation, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/obligations/convert-text")
                .headers(h -> addUser(h, user))
                .body(obligation)
                .retrieve()
                .body(String.class));
    }

    @Override
    public Obligation getWithTextNodes(Obligation obligation, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/obligations/with-text-nodes")
                .headers(h -> addUser(h, user))
                .body(obligation)
                .retrieve()
                .body(Obligation.class));
    }

    @Override
    public PaginatedResult<Obligation> searchObligationTextPaginated(String searchText, ObligationLevel obligationLevel,
            PaginationData pageData) {
        return call(() -> restClient.get()
                .uri(b -> {
                    var ub = b.path(BASE + "/obligations/search/paginated");
                    if (searchText != null) {
                        ub.queryParam("searchText", searchText);
                    }
                    if (obligationLevel != null) {
                        ub.queryParam("obligationLevel", obligationLevel.name());
                    }
                    if (pageData != null) {
                        ub.queryParam("displayStart", pageData.getDisplayStart());
                        ub.queryParam("rowsPerPage", pageData.getRowsPerPage());
                        ub.queryParam("ascending", pageData.getAscending());
                        ub.queryParam("sortColumnNumber", pageData.getSortColumnNumber());
                    }
                    return ub.build();
                })
                .retrieve()
                .body(OBLIGATION_PAGE));
    }

    @Override
    public String addObligationElements(ObligationElement obligationElement, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/obligation-elements")
                .headers(h -> addUser(h, user))
                .body(obligationElement)
                .retrieve()
                .body(String.class));
    }

    @Override
    public List<ObligationElement> getObligationElements() {
        List<ObligationElement> list = call(() -> restClient.get()
                .uri(BASE + "/obligation-elements")
                .retrieve()
                .body(OBLIGATION_ELEMENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public ObligationElement getObligationElementById(String id) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/obligation-elements/{id}").build(id))
                .retrieve()
                .body(ObligationElement.class));
    }

    @Override
    public List<ObligationElement> searchObligationElement(String text) {
        List<ObligationElement> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/obligation-elements/search").queryParam("text", text).build())
                .retrieve()
                .body(OBLIGATION_ELEMENT_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public String addObligationNodes(ObligationNode obligationNode, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/obligation-nodes")
                .headers(h -> addUser(h, user))
                .body(obligationNode)
                .retrieve()
                .body(String.class));
    }

    @Override
    public List<ObligationNode> getObligationNodes() {
        List<ObligationNode> list = call(() -> restClient.get()
                .uri(BASE + "/obligation-nodes")
                .retrieve()
                .body(OBLIGATION_NODE_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public ObligationNode getObligationNodeById(String id) {
        return call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/obligation-nodes/{id}").build(id))
                .retrieve()
                .body(ObligationNode.class));
    }

    @Override
    public String addNodes(String jsonString, User user) {
        return call(() -> restClient.post()
                .uri(BASE + "/obligation-nodes/add")
                .headers(h -> addUser(h, user))
                .body(jsonString)
                .retrieve()
                .body(String.class));
    }

    @Override
    public String buildObligationText(String nodes, String level) {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/obligation-nodes/build-text")
                        .queryParam("nodes", nodes)
                        .queryParam("level", level)
                        .build())
                .retrieve()
                .body(String.class));
    }

    @Override
    public List<CustomProperties> getCustomProperties(String documentType) {
        List<CustomProperties> list = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/custom-properties").queryParam("documentType", documentType).build())
                .retrieve()
                .body(CUSTOM_PROP_LIST));
        return list == null ? List.of() : list;
    }

    @Override
    public RequestStatus updateCustomProperties(CustomProperties customProperties, User user) {
        return call(() -> restClient.put()
                .uri(BASE + "/custom-properties")
                .headers(h -> addUser(h, user))
                .body(customProperties)
                .retrieve()
                .body(RequestStatus.class));
    }
}
