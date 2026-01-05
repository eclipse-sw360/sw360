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
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ObligationService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public List<Obligation> getObligations() {
        List<Obligation> obligations;
        try {
            LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
            obligations = sw360LicenseClient.getObligations();
        } catch (TException e) {
            throw new RuntimeException(e);
        }
        if (obligations != null) {
            return obligations.stream().map(o -> o.setNode(null)).toList();
        }
        return null;
    }

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
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/licenses/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new LicenseService.Client(protocol);
    }

    public Obligation updateObligation(Obligation obligation, User sw360User) {
        if (CommonUtils.isNotNullEmptyOrWhitespace(obligation.getTitle())
                || CommonUtils.isNotNullEmptyOrWhitespace(obligation.getText())) {
            try {
                LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
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
}
