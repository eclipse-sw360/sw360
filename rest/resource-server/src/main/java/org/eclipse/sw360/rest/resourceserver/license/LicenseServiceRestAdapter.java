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
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.licenses.LicenseClient;
import org.eclipse.sw360.datahandler.licenses.LicenseClients;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
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
import org.springframework.stereotype.Component;

/**
 * Thrift {@link LicenseService.Iface} adapter that delegates to the licenses REST backend
 * ({@code /licenses/api/licenses}). Keeps the Thrift contract intact for existing resource-server
 * callers while removing the Thrift transport. Archive import/export uses
 * {@link #asImportExportGateway()} which speaks service-api POJOs.
 */
@Component
public class LicenseServiceRestAdapter implements LicenseService.Iface {

    private LicenseClient client() {
        return LicenseClients.get();
    }

    @Override
    public License getByID(String id, String organisation) throws TException {
        return call(() -> LicenseConverter.toThrift(client().getByID(id, organisation)));
    }

    @Override
    public License getByIDWithOwnModerationRequests(String id, String organisation, User user) throws TException {
        return call(() -> LicenseConverter.toThrift(
                client().getByIDWithOwnModerationRequests(id, organisation, UserConverter.fromThrift(user))));
    }

    @Override
    public List<License> getByIds(Set<String> ids, String organisation) throws TException {
        return call(() -> toThriftLicenses(client().getByIds(ids, organisation)));
    }

    @Override
    public List<License> getLicenseSummary() throws TException {
        return call(() -> toThriftLicenses(client().getLicenseSummary()));
    }

    @Override
    public List<License> getLicenseSummaryForExport() throws TException {
        return call(() -> toThriftLicenses(client().getLicenseSummaryForExport()));
    }

    @Override
    public List<License> getDetailedLicenseSummaryForExport(String organisation) throws TException {
        return call(() -> toThriftLicenses(client().getDetailedLicenseSummaryForExport(organisation)));
    }

    @Override
    public List<License> getDetailedLicenseSummary(String organisation, List<String> identifiers) throws TException {
        return call(() -> toThriftLicenses(client().getDetailedLicenseSummary(organisation, identifiers)));
    }

    @Override
    public List<License> getLicenses() throws TException {
        return call(() -> toThriftLicenses(client().getLicenses()));
    }

    @Override
    public List<License> addLicenses(List<License> licenses, User user) throws TException {
        return call(() -> toThriftLicenses(
                client().addLicenses(toPojoLicenses(licenses), UserConverter.fromThrift(user))));
    }

    @Override
    public List<License> addOrOverwriteLicenses(List<License> licenses, User user) throws TException {
        return call(() -> toThriftLicenses(
                client().addOrOverwriteLicenses(toPojoLicenses(licenses), UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus updateLicense(License license, User user, User requestingUser) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateLicense(LicenseConverter.fromThrift(license),
                UserConverter.fromThrift(user), UserConverter.fromThrift(requestingUser))));
    }

    @Override
    public RequestStatus updateLicenseFromModerationRequest(License additions, License deletions, User user,
            User requestingUser) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateLicenseFromModerationRequest(
                LicenseConverter.fromThrift(additions), LicenseConverter.fromThrift(deletions),
                UserConverter.fromThrift(user), UserConverter.fromThrift(requestingUser))));
    }

    @Override
    public RequestStatus updateWhitelist(String licenseId, Set<String> obligationsDatabaseIds, User user)
            throws TException {
        return call(() -> RequestStatusConverter.toThrift(
                client().updateWhitelist(licenseId, obligationsDatabaseIds, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestStatus deleteLicense(String licenseId, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(
                client().deleteLicense(licenseId, UserConverter.fromThrift(user))));
    }

    @Override
    public RequestSummary deleteAllLicenseInformation(User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(
                client().deleteAllLicenseInformation(UserConverter.fromThrift(user))));
    }

    @Override
    public RequestSummary importAllSpdxLicenses(User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(
                client().importAllSpdxLicenses(UserConverter.fromThrift(user))));
    }

    @Override
    public RequestSummary importAllOSADLLicenses(User user) throws TException {
        return call(() -> RequestSummaryConverter.toThrift(
                client().importAllOSADLLicenses(UserConverter.fromThrift(user))));
    }

    @Override
    public List<License> searchLicense(String searchText) throws TException {
        return call(() -> toThriftLicenses(client().searchLicense(searchText)));
    }

    @Override
    public ByteBuffer downloadExcel(String token) throws TException {
        return call(() -> ByteBuffer.wrap(client().downloadExcel(token)));
    }

    @Override
    public ByteBuffer getLicenseReportDataStream() throws TException {
        return call(() -> ByteBuffer.wrap(client().getLicenseReportDataStream()));
    }

    @Override
    public RequestStatus addLicenseType(LicenseType licenseType, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().addLicenseType(
                LicenseTypeConverter.fromThrift(licenseType), UserConverter.fromThrift(user))));
    }

    @Override
    public List<LicenseType> addLicenseTypes(List<LicenseType> licenseTypes, User user) throws TException {
        return call(() -> toThriftLicenseTypes(
                client().addLicenseTypes(toPojoLicenseTypes(licenseTypes), UserConverter.fromThrift(user))));
    }

    @Override
    public List<LicenseType> getLicenseTypes() throws TException {
        return call(() -> toThriftLicenseTypes(client().getLicenseTypes()));
    }

    @Override
    public List<LicenseType> getLicenseTypesByIds(List<String> ids) throws TException {
        return call(() -> toThriftLicenseTypes(client().getLicenseTypesByIds(ids)));
    }

    @Override
    public LicenseType getLicenseTypeById(String id) throws TException {
        return call(() -> LicenseTypeConverter.toThrift(client().getLicenseTypeById(id)));
    }

    @Override
    public RequestStatus deleteLicenseType(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(
                client().deleteLicenseType(id, UserConverter.fromThrift(user))));
    }

    @Override
    public int checkLicenseTypeInUse(String id) throws TException {
        return call(() -> client().checkLicenseTypeInUse(id));
    }

    @Override
    public List<LicenseType> searchByLicenseType(String licenseType) throws TException {
        return call(() -> toThriftLicenseTypes(client().searchByLicenseType(licenseType)));
    }

    @Override
    public String addObligations(Obligation obligations, User user) throws TException {
        return call(() -> client().addObligations(ObligationConverter.fromThrift(obligations),
                UserConverter.fromThrift(user)));
    }

    @Override
    public String updateObligation(Obligation obligation, User user) throws TException {
        return call(() -> client().updateObligation(ObligationConverter.fromThrift(obligation),
                UserConverter.fromThrift(user)));
    }

    @Override
    public RequestStatus addObligationsToLicense(Set<Obligation> obligations, License license, User user)
            throws TException {
        Set<org.eclipse.sw360.datahandler.services.licenses.Obligation> pojoObligations = obligations == null
                ? Set.of()
                : obligations.stream().map(ObligationConverter::fromThrift).collect(Collectors.toSet());
        return call(() -> RequestStatusConverter.toThrift(client().addObligationsToLicense(pojoObligations,
                LicenseConverter.fromThrift(license), UserConverter.fromThrift(user))));
    }

    @Override
    public List<Obligation> addListOfObligations(List<Obligation> obligations, User user) throws TException {
        return call(() -> toThriftObligations(
                client().addListOfObligations(toPojoObligations(obligations), UserConverter.fromThrift(user))));
    }

    @Override
    public List<Obligation> getObligations() throws TException {
        return call(() -> toThriftObligations(client().getObligations()));
    }

    @Override
    public List<Obligation> getObligationsByIds(List<String> ids) throws TException {
        return call(() -> toThriftObligations(client().getObligationsByIds(ids)));
    }

    @Override
    public List<Obligation> getObligationsByLicenseId(String id) throws TException {
        return call(() -> toThriftObligations(client().getObligationsByLicenseId(id)));
    }

    @Override
    public Obligation getObligationsById(String id) throws TException {
        return call(() -> ObligationConverter.toThrift(client().getObligationsById(id)));
    }

    @Override
    public RequestStatus deleteObligations(String id, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(
                client().deleteObligations(id, UserConverter.fromThrift(user))));
    }

    @Override
    public String convertTextToNode(Obligation obligation, User user) throws TException {
        return call(() -> client().convertTextToNode(ObligationConverter.fromThrift(obligation),
                UserConverter.fromThrift(user)));
    }

    @Override
    public Obligation getWithTextNodes(Obligation obligation, User user) throws TException {
        return call(() -> ObligationConverter.toThrift(
                client().getWithTextNodes(ObligationConverter.fromThrift(obligation), UserConverter.fromThrift(user))));
    }

    @Override
    public Map<PaginationData, List<Obligation>> searchObligationTextPaginated(String searchText,
            ObligationLevel obligationLevel, PaginationData pageData) throws TException {
        org.eclipse.sw360.datahandler.services.licenses.ObligationLevel pojoLevel = obligationLevel == null ? null
                : org.eclipse.sw360.datahandler.services.licenses.ObligationLevel.valueOf(obligationLevel.name());
        PaginatedResult<org.eclipse.sw360.datahandler.services.licenses.Obligation> result = call(
                () -> client().searchObligationTextPaginated(searchText, pojoLevel,
                        pageData == null ? null : PaginationDataConverter.fromThrift(pageData)));
        Map<PaginationData, List<Obligation>> map = new HashMap<>();
        if (result != null) {
            PaginationData thriftPage = result.getPaginationData() != null
                    ? PaginationDataConverter.toThrift(result.getPaginationData())
                    : (pageData != null ? pageData : new PaginationData());
            map.put(thriftPage, toThriftObligations(result.getData()));
        }
        return map;
    }

    @Override
    public String addObligationElements(ObligationElement obligationElement, User user) throws TException {
        return call(() -> client().addObligationElements(ObligationElementConverter.fromThrift(obligationElement),
                UserConverter.fromThrift(user)));
    }

    @Override
    public List<ObligationElement> getObligationElements() throws TException {
        return call(() -> toThriftObligationElements(client().getObligationElements()));
    }

    @Override
    public ObligationElement getObligationElementById(String id) throws TException {
        return call(() -> ObligationElementConverter.toThrift(client().getObligationElementById(id)));
    }

    @Override
    public List<ObligationElement> searchObligationElement(String text) throws TException {
        return call(() -> toThriftObligationElements(client().searchObligationElement(text)));
    }

    @Override
    public String addObligationNodes(ObligationNode obligationNode, User user) throws TException {
        return call(() -> client().addObligationNodes(ObligationNodeConverter.fromThrift(obligationNode),
                UserConverter.fromThrift(user)));
    }

    @Override
    public List<ObligationNode> getObligationNodes() throws TException {
        return call(() -> toThriftObligationNodes(client().getObligationNodes()));
    }

    @Override
    public ObligationNode getObligationNodeById(String id) throws TException {
        return call(() -> ObligationNodeConverter.toThrift(client().getObligationNodeById(id)));
    }

    @Override
    public String addNodes(String jsonString, User user) throws TException {
        return call(() -> client().addNodes(jsonString, UserConverter.fromThrift(user)));
    }

    @Override
    public String buildObligationText(String nodes, String level) throws TException {
        return call(() -> client().buildObligationText(nodes, level));
    }

    @Override
    public List<CustomProperties> getCustomProperties(String documentType) throws TException {
        return call(() -> client().getCustomProperties(documentType).stream()
                .map(CustomPropertiesConverter::toThrift).collect(Collectors.toList()));
    }

    @Override
    public RequestStatus updateCustomProperties(CustomProperties customProperties, User user) throws TException {
        return call(() -> RequestStatusConverter.toThrift(client().updateCustomProperties(
                CustomPropertiesConverter.fromThrift(customProperties), UserConverter.fromThrift(user))));
    }

    /**
     * POJO-typed gateway for {@code LicsExporter}/{@code LicsImporter}. Converts thrift
     * {@link User} at the boundary; license types are already service-api POJOs on
     * {@link LicenseClient}.
     */
    public LicenseImportExportGateway asImportExportGateway() {
        return new LicenseImportExportGateway() {
            @Override
            public List<org.eclipse.sw360.datahandler.services.licenses.License> getLicenses() throws TException {
                return call(() -> client().getLicenses());
            }

            @Override
            public List<org.eclipse.sw360.datahandler.services.licenses.License> addOrOverwriteLicenses(
                    List<org.eclipse.sw360.datahandler.services.licenses.License> licenses, User user)
                    throws TException {
                return call(() -> client().addOrOverwriteLicenses(licenses, UserConverter.fromThrift(user)));
            }

            @Override
            public List<org.eclipse.sw360.datahandler.services.licenses.LicenseType> getLicenseTypes()
                    throws TException {
                return call(() -> client().getLicenseTypes());
            }

            @Override
            public List<org.eclipse.sw360.datahandler.services.licenses.LicenseType> addLicenseTypes(
                    List<org.eclipse.sw360.datahandler.services.licenses.LicenseType> licenseTypes, User user)
                    throws TException {
                return call(() -> client().addLicenseTypes(licenseTypes, UserConverter.fromThrift(user)));
            }

            @Override
            public List<org.eclipse.sw360.datahandler.services.licenses.Obligation> getObligations()
                    throws TException {
                return call(() -> client().getObligations());
            }

            @Override
            public List<org.eclipse.sw360.datahandler.services.licenses.Obligation> addListOfObligations(
                    List<org.eclipse.sw360.datahandler.services.licenses.Obligation> obligations, User user)
                    throws TException {
                return call(() -> client().addListOfObligations(obligations, UserConverter.fromThrift(user)));
            }

            @Override
            public List<org.eclipse.sw360.datahandler.services.common.CustomProperties> getCustomProperties(
                    String documentType) throws TException {
                return call(() -> client().getCustomProperties(documentType));
            }

            @Override
            public org.eclipse.sw360.datahandler.services.common.RequestStatus updateCustomProperties(
                    org.eclipse.sw360.datahandler.services.common.CustomProperties customProperties, User user)
                    throws TException {
                return call(() -> client().updateCustomProperties(customProperties, UserConverter.fromThrift(user)));
            }
        };
    }

    private static <T> T call(Supplier<T> supplier) throws TException {
        try {
            return supplier.get();
        } catch (org.eclipse.sw360.datahandler.services.common.SW360Exception e) {
            SW360Exception thriftEx = new SW360Exception(e.getMessage());
            if (e.getErrorCode() != null) {
                thriftEx.setErrorCode(e.getErrorCode());
            }
            throw thriftEx;
        }
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
