/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
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
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.license.licensedb.LicenseDbClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LicenseResolutionService {

    private final Sw360LicenseService sw360LicenseService;
    private final LicenseSourcePolicy licenseSourcePolicy;
    private final LicenseDbClient licenseDbClient;

    public License resolveOrCreateMissingLicense(String licenseId, User sw360User) throws TException {
        if (!licenseSourcePolicy.isLicenseDbOnlyMode()) {
            return sw360LicenseService.createLicenseFromAuthoritativeSource(buildFallbackLicense(licenseId), sw360User);
        }

        Optional<License> licenseFromLicenseDb = licenseDbClient.fetchLicenseById(licenseId);
        if (licenseFromLicenseDb.isEmpty()) {
            throw new BadRequestClientException("License '" + licenseId
                    + "' is unknown in SW360 and was not found in LicenseDB.");
        }

        License authoritativeLicense = licenseFromLicenseDb.get();
        if (authoritativeLicense.getId() == null) {
            authoritativeLicense.setId(licenseId);
        }
        if (authoritativeLicense.getShortname() == null) {
            authoritativeLicense.setShortname(licenseId);
        }
        if (authoritativeLicense.getFullname() == null) {
            authoritativeLicense.setFullname(authoritativeLicense.getShortname());
        }

        return sw360LicenseService.createLicenseFromAuthoritativeSource(authoritativeLicense, sw360User);
    }

    private License buildFallbackLicense(String licenseId) {
        License fallbackLicense = new License();
        fallbackLicense.setId(licenseId);
        fallbackLicense.setShortname(licenseId);
        fallbackLicense.setFullname(licenseId);
        return fallbackLicense;
    }
}
