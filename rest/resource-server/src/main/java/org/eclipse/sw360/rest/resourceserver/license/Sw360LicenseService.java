/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.license;

import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360LicenseService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public List<License> getLicenses() throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        return sw360LicenseClient.getLicenseSummary();
    }

    public License getLicenseById(String licenseId) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        // TODO Kai Tödter 2017-01-26
        // What is the semantics of the second parameter (organization)?
        License license = null;
        try {
            license = sw360LicenseClient.getByID(licenseId, "?");
        } catch (SW360Exception exp) {
            if (exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException(exp.getWhy());
            } else {
                throw new RuntimeException(exp.getWhy());
            }
        }
        return license;
    }

    public void deleteLicenseById(String licenseId, User user) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        RequestStatus deleteLicenseStatus = sw360LicenseClient.deleteLicense(licenseId, user);

        if (deleteLicenseStatus == RequestStatus.IN_USE) {
            throw new HttpMessageNotReadableException("Unable to delete license. License is in Use");
        } else if (deleteLicenseStatus == RequestStatus.FAILURE) {
            throw new RuntimeException("Unable to delete License");
        }
    }

    public void deleteAllLicenseInfo(User user) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            RequestSummary deleteLicenseStatus = sw360LicenseClient.deleteAllLicenseInformation(user);
        } else {
            throw new HttpMessageNotReadableException("Unable to delete license. User is not admin");
        }
    }

    public License createLicense(License license, User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        license.setId(license.getShortname());
        List<License> licenses = sw360LicenseClient.addLicenses(Collections.singletonList(license), sw360User);
        for (License newLicense : licenses) {
            if (license.getFullname().equals(newLicense.getFullname())) {
                return newLicense;
            }
        }
        return null;
    }

    public RequestStatus updateLicenseToDB(License license, Set<String> obligationIds, User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (obligationIds.isEmpty()){
            license.unsetObligations();
        } else {
            List<String> obligationIdList = new ArrayList<>(obligationIds);
            List<Obligation> obligations = sw360LicenseClient.getObligationsByIds(obligationIdList);
            checkObligationLevel(obligations, sw360User);
            license.setObligationDatabaseIds(obligationIds);
            license.setObligations(obligations);
        }
        return sw360LicenseClient.updateLicense(license, sw360User, sw360User);
    }

    public void checkObligationIds(Set<String> obligationIds) throws TException {
        if (obligationIds.isEmpty()) {
            throw new HttpMessageNotReadableException("Cannot update because no obliagtion id input");
        }
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        ArrayList<String> obligationIdsIncorrect = new ArrayList<>();
        for (String obligationId : obligationIds) {
            if (isNullEmptyOrWhitespace(obligationId)) {
                throw new HttpMessageNotReadableException("Obligation id cannot be empty");
            }
            try {
                sw360LicenseClient.getObligationsById(obligationId);
            } catch (Exception e) {
                obligationIdsIncorrect.add(obligationId);
            }
        }
        if (!obligationIdsIncorrect.isEmpty()) {
            throw new HttpMessageNotReadableException("Obligation ids " + obligationIdsIncorrect + " incorrect");
        }
    }

    public void checkObligationLevel(List<Obligation> obligations, User sw360User) {
        List<String> obligationsLevelIncorrect = new ArrayList<>();
        for (Obligation oblig : obligations) {
            if (oblig.getObligationLevel().toString().equals("LICENSE_OBLIGATION")) {
                oblig.addToWhitelist(sw360User.getDepartment());
            } else {
                obligationsLevelIncorrect.add(oblig.getId());
            }
        }
        if (!obligationsLevelIncorrect.isEmpty()) {
            throw new HttpMessageNotReadableException("Obligation with ids: " + obligationsLevelIncorrect + " level is not License Obligation");
        }
    }

    private LicenseService.Iface getThriftLicenseClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/licenses/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new LicenseService.Client(protocol);
    }
    public void importSpdxInformation(User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            RequestSummary allSPDXLicenseStatus = sw360LicenseClient.importAllSpdxLicenses(sw360User);
        } else {
            throw new HttpMessageNotReadableException("Unable to import All Spdx license. User is not admin");
        }
    }
}
