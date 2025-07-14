package org.eclipse.sw360.rest.resourceserver.licenseopenapi;

import org.eclipse.sw360.datahandler.postgres.LicensePG;
import org.eclipse.sw360.datahandler.postgresql.LicenseRepositoryPG;

public class LicenseServicePG {
    private LicenseRepositoryPG licenseRepository = new LicenseRepositoryPG();

    public LicensePG createLicense(LicensePG license) {
        LicensePG internalLicense = new LicensePG(license);

        return licenseRepository.saveLicense(internalLicense);
    }

}
