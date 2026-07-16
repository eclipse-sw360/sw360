/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.license;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.converter.common.CustomPropertiesConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.licenses.LicenseConverter;
import org.eclipse.sw360.common.utils.converter.licenses.LicenseTypeConverter;
import org.eclipse.sw360.common.utils.converter.licenses.ObligationConverter;
import org.eclipse.sw360.common.utils.converter.licenses.ObligationElementConverter;
import org.eclipse.sw360.common.utils.converter.licenses.ObligationNodeConverter;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.LicenseImportExportGateway;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thrift {@link LicenseService.Iface} adapter that delegates to the licenses REST backend
 * ({@code /licenses/api/licenses}). Keeps the Thrift contract intact for existing resource-server
 * callers (including {@code LicsExporter}/{@code LicsImporter}) while removing the Thrift transport.
 */
@Component
public class LicenseServiceRestAdapter implements LicenseService.Iface, LicenseImportExportGateway {

    private static final String BASE = "/licenses/api/licenses";

    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.licenses.License>> LICENSE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.licenses.LicenseType>> LICENSE_TYPE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.licenses.Obligation>> OBLIGATION_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.licenses.ObligationElement>> OBLIGATION_ELEMENT_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.licenses.ObligationNode>> OBLIGATION_NODE_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<org.eclipse.sw360.datahandler.services.common.CustomProperties>> CUSTOM_PROP_LIST =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.licenses.Obligation>> OBLIGATION_PAGE =
            new ParameterizedTypeReference<org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.licenses.Obligation>>() {};

    private final RestClient restClient;

    public LicenseServiceRestAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    // ---- Licenses -------------------------------------------------------------------------------

    @Override
    public License getByID(String id, String organisation) throws TException {
        return call(() -> LicenseConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/{id}").queryParam("organisation", organisation).build(id))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.licenses.License.class)));
    }

    @Override
    public License getByIDWithOwnModerationRequests(String id, String organisation, User user) throws TException {
        return call(() -> LicenseConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/{id}/with-moderation").queryParam("organisation", organisation).build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.licenses.License.class)));
    }

    @Override
    public List<License> getByIds(Set<String> ids, String organisation) throws TException {
        return call(() -> toThriftLicenses(restClient.post()
                .uri(b -> b.path(BASE + "/by-ids").queryParam("organisation", organisation).build())
                .body(ids)
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public List<License> getLicenseSummary() throws TException {
        return call(() -> toThriftLicenses(restClient.get()
                .uri(BASE + "/summary")
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public List<License> getLicenseSummaryForExport() throws TException {
        return call(() -> toThriftLicenses(restClient.get()
                .uri(BASE + "/summary/export")
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public List<License> getDetailedLicenseSummaryForExport(String organisation) throws TException {
        return call(() -> toThriftLicenses(restClient.get()
                .uri(b -> b.path(BASE + "/detailed-summary/export").queryParam("organisation", organisation).build())
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public List<License> getDetailedLicenseSummary(String organisation, List<String> identifiers) throws TException {
        return call(() -> toThriftLicenses(restClient.post()
                .uri(b -> b.path(BASE + "/detailed-summary").queryParam("organisation", organisation).build())
                .body(identifiers)
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public List<License> getLicenses() throws TException {
        return call(() -> toThriftLicenses(restClient.get()
                .uri(BASE)
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public List<License> addLicenses(List<License> licenses, User user) throws TException {
        return call(() -> toThriftLicenses(restClient.post()
                .uri(BASE + "/bulk")
                .headers(h -> addUser(h, user))
                .body(toPojoLicenses(licenses))
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public List<License> addOrOverwriteLicenses(List<License> licenses, User user) throws TException {
        return call(() -> toThriftLicenses(restClient.post()
                .uri(BASE + "/bulk/overwrite")
                .headers(h -> addUser(h, user))
                .body(toPojoLicenses(licenses))
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public RequestStatus updateLicense(License license, User user, User requestingUser) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE)
                .headers(h -> {
                    addUser(h, user);
                    addRequestingUser(h, requestingUser);
                })
                .body(LicenseConverter.fromThrift(license))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateLicenseFromModerationRequest(License additions, License deletions, User user,
            User requestingUser) throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("additions", LicenseConverter.fromThrift(additions));
        body.put("deletions", LicenseConverter.fromThrift(deletions));
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/from-moderation")
                .headers(h -> {
                    addUser(h, user);
                    addRequestingUser(h, requestingUser);
                })
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus updateWhitelist(String licenseId, Set<String> obligationsDatabaseIds, User user)
            throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(b -> b.path(BASE + "/{id}/whitelist").build(licenseId))
                .headers(h -> addUser(h, user))
                .body(obligationsDatabaseIds)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestStatus deleteLicense(String licenseId, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/{id}").build(licenseId))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public RequestSummary deleteAllLicenseInformation(User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(BASE + "/all")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestSummary importAllSpdxLicenses(User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.post()
                .uri(BASE + "/import-spdx")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public RequestSummary importAllOSADLLicenses(User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(restClient.post()
                .uri(BASE + "/import-osadl")
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestSummary.class)));
    }

    @Override
    public List<License> searchLicense(String searchText) throws TException {
        return call(() -> toThriftLicenses(restClient.get()
                .uri(b -> {
                    var ub = b.path(BASE + "/search");
                    if (searchText != null) {
                        ub.queryParam("searchText", searchText);
                    }
                    return ub.build();
                })
                .retrieve()
                .body(LICENSE_LIST)));
    }

    @Override
    public ByteBuffer downloadExcel(String token) throws TException {
        return call(() -> toByteBuffer(restClient.get()
                .uri(b -> b.path(BASE + "/download-excel").queryParam("token", token).build())
                .retrieve()
                .body(byte[].class)));
    }

    @Override
    public ByteBuffer getLicenseReportDataStream() throws TException {
        return call(() -> toByteBuffer(restClient.get()
                .uri(BASE + "/report-stream")
                .retrieve()
                .body(byte[].class)));
    }

    // ---- License types --------------------------------------------------------------------------

    @Override
    public RequestStatus addLicenseType(LicenseType licenseType, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(BASE + "/types")
                .headers(h -> addUser(h, user))
                .body(LicenseTypeConverter.fromThrift(licenseType))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user) throws TException {
        return call(() -> toThriftLicenseTypes(restClient.post()
                .uri(BASE + "/types/bulk")
                .headers(h -> addUser(h, user))
                .body(toPojoLicenseTypes(licenseTypes))
                .retrieve()
                .body(LICENSE_TYPE_LIST)));
    }

    @Override
    public List<LicenseType> getLicenseTypes() throws TException {
        return call(() -> toThriftLicenseTypes(restClient.get()
                .uri(BASE + "/types")
                .retrieve()
                .body(LICENSE_TYPE_LIST)));
    }

    @Override
    public List<LicenseType> getLicenseTypesByIds(List<String> ids) throws TException {
        return call(() -> toThriftLicenseTypes(restClient.post()
                .uri(BASE + "/types/by-ids")
                .body(ids)
                .retrieve()
                .body(LICENSE_TYPE_LIST)));
    }

    @Override
    public LicenseType getLicenseTypeById(String id) throws TException {
        return call(() -> LicenseTypeConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/types/{id}").build(id))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.licenses.LicenseType.class)));
    }

    @Override
    public RequestStatus deleteLicenseType(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/types/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public int checkLicenseTypeInUse(String id) throws TException {
        Integer count = call(() -> restClient.get()
                .uri(b -> b.path(BASE + "/types/{id}/in-use").build(id))
                .retrieve()
                .body(Integer.class));
        return count != null ? count : 0;
    }

    @Override
    public List<LicenseType> searchByLicenseType(String licenseType) throws TException {
        return call(() -> toThriftLicenseTypes(restClient.get()
                .uri(b -> b.path(BASE + "/types/search").queryParam("licenseType", licenseType).build())
                .retrieve()
                .body(LICENSE_TYPE_LIST)));
    }

    // ---- Obligations ----------------------------------------------------------------------------

    @Override
    public String addObligations(Obligation obligations, User user) throws TException {
        return call(() -> restClient.post()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(ObligationConverter.fromThrift(obligations))
                .retrieve()
                .body(String.class));
    }

    @Override
    public String updateObligation(Obligation obligation, User user) throws TException {
        return call(() -> restClient.put()
                .uri(BASE + "/obligations")
                .headers(h -> addUser(h, user))
                .body(ObligationConverter.fromThrift(obligation))
                .retrieve()
                .body(String.class));
    }

    @Override
    public RequestStatus addObligationsToLicense(Set<Obligation> obligations, License license, User user)
            throws TException {
        Map<String, Object> body = new HashMap<>();
        body.put("obligations", obligations == null ? Set.of()
                : obligations.stream().map(ObligationConverter::fromThrift).collect(Collectors.toSet()));
        body.put("license", LicenseConverter.fromThrift(license));
        return call(() -> RequestStatusConverter.toThrift(restClient.post()
                .uri(BASE + "/obligations/to-license")
                .headers(h -> addUser(h, user))
                .body(body)
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public List<Obligation> addListOfObligations(List<Obligation> obligations, User user) throws TException {
        return call(() -> toThriftObligations(restClient.post()
                .uri(BASE + "/obligations/bulk")
                .headers(h -> addUser(h, user))
                .body(toPojoObligations(obligations))
                .retrieve()
                .body(OBLIGATION_LIST)));
    }

    @Override
    public List<Obligation> getObligations() throws TException {
        return call(() -> toThriftObligations(restClient.get()
                .uri(BASE + "/obligations")
                .retrieve()
                .body(OBLIGATION_LIST)));
    }

    @Override
    public List<Obligation> getObligationsByIds(List<String> ids) throws TException {
        return call(() -> toThriftObligations(restClient.post()
                .uri(BASE + "/obligations/by-ids")
                .body(ids)
                .retrieve()
                .body(OBLIGATION_LIST)));
    }

    @Override
    public List<Obligation> getObligationsByLicenseId(String id) throws TException {
        return call(() -> toThriftObligations(restClient.get()
                .uri(b -> b.path(BASE + "/obligations/by-license/{id}").build(id))
                .retrieve()
                .body(OBLIGATION_LIST)));
    }

    @Override
    public Obligation getObligationsById(String id) throws TException {
        return call(() -> ObligationConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/obligations/{id}").build(id))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.licenses.Obligation.class)));
    }

    @Override
    public RequestStatus deleteObligations(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.method(org.springframework.http.HttpMethod.DELETE)
                .uri(b -> b.path(BASE + "/obligations/{id}").build(id))
                .headers(h -> addUser(h, user))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    @Override
    public String convertTextToNode(Obligation obligation, User user) throws TException {
        return call(() -> restClient.post()
                .uri(BASE + "/obligations/convert-text")
                .headers(h -> addUser(h, user))
                .body(ObligationConverter.fromThrift(obligation))
                .retrieve()
                .body(String.class));
    }

    @Override
    public Obligation getWithTextNodes(Obligation obligation, User user) throws TException {
        return call(() -> ObligationConverter.toThrift(restClient.post()
                .uri(BASE + "/obligations/with-text-nodes")
                .headers(h -> addUser(h, user))
                .body(ObligationConverter.fromThrift(obligation))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.licenses.Obligation.class)));
    }

    @Override
    public Map<PaginationData, List<Obligation>> searchObligationTextPaginated(String searchText,
            ObligationLevel obligationLevel, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.common.PaginatedResult<org.eclipse.sw360.datahandler.services.licenses.Obligation> result =
                call(() -> restClient.get()
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
                                ub.queryParam("ascending", pageData.isAscending());
                                ub.queryParam("sortColumnNumber", pageData.getSortColumnNumber());
                            }
                            return ub.build();
                        })
                        .retrieve()
                        .body(OBLIGATION_PAGE));
        Map<PaginationData, List<Obligation>> map = new HashMap<>();
        if (result != null) {
            PaginationData thriftPage = result.getPaginationData() != null
                    ? PaginationDataConverter.toThrift(result.getPaginationData())
                    : (pageData != null ? pageData : new PaginationData());
            map.put(thriftPage, toThriftObligations(result.getData()));
        }
        return map;
    }

    // ---- Obligation elements --------------------------------------------------------------------

    @Override
    public String addObligationElements(ObligationElement obligationElement, User user) throws TException {
        return call(() -> restClient.post()
                .uri(BASE + "/obligation-elements")
                .headers(h -> addUser(h, user))
                .body(ObligationElementConverter.fromThrift(obligationElement))
                .retrieve()
                .body(String.class));
    }

    @Override
    public List<ObligationElement> getObligationElements() throws TException {
        return call(() -> toThriftObligationElements(restClient.get()
                .uri(BASE + "/obligation-elements")
                .retrieve()
                .body(OBLIGATION_ELEMENT_LIST)));
    }

    @Override
    public ObligationElement getObligationElementById(String id) throws TException {
        return call(() -> ObligationElementConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/obligation-elements/{id}").build(id))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.licenses.ObligationElement.class)));
    }

    @Override
    public List<ObligationElement> searchObligationElement(String text) throws TException {
        return call(() -> toThriftObligationElements(restClient.get()
                .uri(b -> b.path(BASE + "/obligation-elements/search").queryParam("text", text).build())
                .retrieve()
                .body(OBLIGATION_ELEMENT_LIST)));
    }

    // ---- Obligation nodes -----------------------------------------------------------------------

    @Override
    public String addObligationNodes(ObligationNode obligationNode, User user) throws TException {
        return call(() -> restClient.post()
                .uri(BASE + "/obligation-nodes")
                .headers(h -> addUser(h, user))
                .body(ObligationNodeConverter.fromThrift(obligationNode))
                .retrieve()
                .body(String.class));
    }

    @Override
    public List<ObligationNode> getObligationNodes() throws TException {
        return call(() -> toThriftObligationNodes(restClient.get()
                .uri(BASE + "/obligation-nodes")
                .retrieve()
                .body(OBLIGATION_NODE_LIST)));
    }

    @Override
    public ObligationNode getObligationNodeById(String id) throws TException {
        return call(() -> ObligationNodeConverter.toThrift(restClient.get()
                .uri(b -> b.path(BASE + "/obligation-nodes/{id}").build(id))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.licenses.ObligationNode.class)));
    }

    @Override
    public String addNodes(String jsonString, User user) throws TException {
        return call(() -> restClient.post()
                .uri(BASE + "/obligation-nodes/add")
                .headers(h -> addUser(h, user))
                .body(jsonString)
                .retrieve()
                .body(String.class));
    }

    @Override
    public String buildObligationText(String nodes, String level) throws TException {
        return call(() -> restClient.post()
                .uri(b -> b.path(BASE + "/obligation-nodes/build-text")
                        .queryParam("nodes", nodes)
                        .queryParam("level", level)
                        .build())
                .retrieve()
                .body(String.class));
    }

    // ---- Custom properties ----------------------------------------------------------------------

    @Override
    public List<CustomProperties> getCustomProperties(String documentType) throws TException {
        return call(() -> {
            List<org.eclipse.sw360.datahandler.services.common.CustomProperties> pojos = restClient.get()
                    .uri(b -> b.path(BASE + "/custom-properties").queryParam("documentType", documentType).build())
                    .retrieve()
                    .body(CUSTOM_PROP_LIST);
            if (pojos == null) {
                return new ArrayList<>();
            }
            return pojos.stream().map(CustomPropertiesConverter::toThrift).collect(Collectors.toList());
        });
    }

    @Override
    public RequestStatus updateCustomProperties(CustomProperties customProperties, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(restClient.put()
                .uri(BASE + "/custom-properties")
                .headers(h -> addUser(h, user))
                .body(CustomPropertiesConverter.fromThrift(customProperties))
                .retrieve()
                .body(org.eclipse.sw360.datahandler.services.common.RequestStatus.class)));
    }

    // ---- Helpers --------------------------------------------------------------------------------

    private static <T> T call(Supplier<T> supplier) throws TException {
        try {
            return supplier.get();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            throw new SW360Exception(body == null || body.isEmpty() ? e.getMessage() : body)
                    .setErrorCode(e.getStatusCode().value());
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

    private static ByteBuffer toByteBuffer(byte[] bytes) {
        return ByteBuffer.wrap(bytes == null ? new byte[0] : bytes);
    }

    private static List<License> toThriftLicenses(List<org.eclipse.sw360.datahandler.services.licenses.License> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(LicenseConverter::toThrift).collect(Collectors.toList());
    }

    private static List<org.eclipse.sw360.datahandler.services.licenses.License> toPojoLicenses(List<License> thrifts) {
        if (thrifts == null) {
            return new ArrayList<>();
        }
        return thrifts.stream().map(LicenseConverter::fromThrift).collect(Collectors.toList());
    }

    private static List<LicenseType> toThriftLicenseTypes(
            List<org.eclipse.sw360.datahandler.services.licenses.LicenseType> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(LicenseTypeConverter::toThrift).collect(Collectors.toList());
    }

    private static List<org.eclipse.sw360.datahandler.services.licenses.LicenseType> toPojoLicenseTypes(
            List<LicenseType> thrifts) {
        if (thrifts == null) {
            return new ArrayList<>();
        }
        return thrifts.stream().map(LicenseTypeConverter::fromThrift).collect(Collectors.toList());
    }

    private static List<Obligation> toThriftObligations(
            List<org.eclipse.sw360.datahandler.services.licenses.Obligation> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ObligationConverter::toThrift).collect(Collectors.toList());
    }

    private static List<org.eclipse.sw360.datahandler.services.licenses.Obligation> toPojoObligations(
            List<Obligation> thrifts) {
        if (thrifts == null) {
            return new ArrayList<>();
        }
        return thrifts.stream().map(ObligationConverter::fromThrift).collect(Collectors.toList());
    }

    private static List<ObligationElement> toThriftObligationElements(
            List<org.eclipse.sw360.datahandler.services.licenses.ObligationElement> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ObligationElementConverter::toThrift).collect(Collectors.toList());
    }

    private static List<ObligationNode> toThriftObligationNodes(
            List<org.eclipse.sw360.datahandler.services.licenses.ObligationNode> pojos) {
        if (pojos == null) {
            return new ArrayList<>();
        }
        return pojos.stream().map(ObligationNodeConverter::toThrift).collect(Collectors.toList());
    }
}
