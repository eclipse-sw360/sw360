/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Obligation.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.obligation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationSortColumn;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ObligationService {
    public Obligation getObligationById(String obligationId, User user) {
        LicenseService.Iface sw360LicenseClient = null;
        Obligation obligation = null;
        try {
            sw360LicenseClient = getThriftLicenseClient();
            obligation = sw360LicenseClient.getObligationsById(obligationId);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
        if (user != null) {
            try {
                obligation = sw360LicenseClient.getWithTextNodes(obligation, user);
            } catch (TException e) {
                throw new RuntimeException(e);
            }
        }
        return obligation;
    }

    public Obligation createObligation(Obligation obligation, User sw360User) {
        try {
            if (obligation.getTitle() != null && !obligation.getTitle().trim().isEmpty()
            && obligation.getText() != null && !obligation.getText().trim().isEmpty()
            && obligation.getObligationLevel() != null) {
                LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
                String obligationId = sw360LicenseClient.addObligations(obligation, sw360User);
                obligation.setId(obligationId);
                return obligation;
            } else {
                throw new BadRequestClientException("Obligation Title, Text, Level are required. Obligation Title, Text cannot contain only space character.");
            }
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public RequestStatus deleteObligation(String obligationId, User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        return sw360LicenseClient.deleteObligations(obligationId, sw360User);
    }

    private LicenseService.Iface getThriftLicenseClient() throws TTransportException {
        ThriftClients thriftClients = new ThriftClients();
        return thriftClients.makeLicenseClient();
    }

    public Obligation updateObligation(Obligation obligation, User sw360User) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(obligation.getTitle())
                || CommonUtils.isNotNullEmptyOrWhitespace(obligation.getText())) {
            try {
                LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
                String updatedNode = sw360LicenseClient.addNodes(obligation.getNode(), sw360User);
                obligation.setNode(updatedNode);
                sw360LicenseClient.updateObligation(obligation, sw360User);
                return obligation;
            } catch (TException e) {
                throw new RuntimeException("Error updating obligation", e);
            }
        } else {
            throw new BadRequestClientException("Obligation Title, Text are required. Obligation Title, Text cannot contain only space character.");
        }
    }

    public List<ObligationNode> getObligationNodes() {
        LicenseService.Iface sw360LicenseClient = null;
        List<ObligationNode> obligationNodes = null;
        try {
            sw360LicenseClient = getThriftLicenseClient();
            obligationNodes = sw360LicenseClient.getObligationNodes();
        } catch (TException e) {
            throw new RuntimeException(e);
        }
        return obligationNodes;
    }

    public List<ObligationElement> getObligationElements() {
        LicenseService.Iface sw360LicenseClient = null;
        List<ObligationElement> obligationElements = null;
        try {
            sw360LicenseClient = getThriftLicenseClient();
            obligationElements = sw360LicenseClient.getObligationElements();
        } catch (TException e) {
            throw new RuntimeException(e);
        }
        return obligationElements;
    }

    public Map<PaginationData, List<Obligation>> getObligationsFiltered(String searchText, String obligationLevel,
                                                                        Pageable pageable) throws SW360Exception {
        ObligationLevel level = null;
        try {
            if (!CommonUtils.isNullEmptyOrWhitespace(obligationLevel)) {
                level = ObligationLevel.valueOf(obligationLevel);
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestClientException("Illegal value for obligationLevel: " + obligationLevel, e);
        }
        try {
            LicenseService.Iface licenseClient = getThriftLicenseClient();
            PaginationData pageData = pageableToPaginationData(pageable,
                    ObligationSortColumn.BY_TITLE, true);
            return licenseClient.searchObligationTextPaginated(searchText, level, pageData);
        } catch (TException e) {
            throw new SW360Exception("Unable to fetch Obligations.");
        }
    }


    /**
     * Converts a Pageable object to a PaginationData object.
     *
     * @param pageable the Pageable object to convert
     * @return a PaginationData object representing the pagination information
     */
    private static PaginationData pageableToPaginationData(
            @NotNull Pageable pageable, ObligationSortColumn defaultSort, Boolean defaultAscending
    ) {
        ObligationSortColumn column = ObligationSortColumn.BY_TITLE;
        boolean ascending = true;

        if (pageable.getSort().isSorted()) {
            Sort.Order order = pageable.getSort().iterator().next();
            String property = order.getProperty();
            column = switch (property) {
                case "text" -> ObligationSortColumn.BY_TEXT;
                case "level" -> ObligationSortColumn.BY_LEVEL;
                default -> column; // Default to BY_NAME if no match
            };
            ascending = order.isAscending();
        } else {
            if (defaultSort != null) {
                column = defaultSort;
                if (defaultAscending != null) {
                    ascending = defaultAscending;
                }
            }
        }

        return new PaginationData().setDisplayStart((int) pageable.getOffset())
                .setRowsPerPage(pageable.getPageSize()).setSortColumnNumber(column.getValue()).setAscending(ascending);
    }
}
