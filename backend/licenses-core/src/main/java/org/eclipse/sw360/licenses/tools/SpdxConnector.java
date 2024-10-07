/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.Quadratic;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.license.LicenseInfoFactory;
import org.spdx.library.model.license.SpdxListedLicense;
import org.spdx.utility.compare.LicenseCompareHelper;
import org.spdx.utility.compare.SpdxCompareException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpdxConnector {

    private static final Logger log = LogManager.getLogger(SpdxConnector.class);

    public static List<String> getAllSpdxLicenseIds() {
        return LicenseInfoFactory.getSpdxListedLicenseIds();

    }

    protected static Optional<SpdxListedLicense> getSpdxLicense(String licenseId) {
        final SpdxListedLicense listedLicenseById;
        try {
            listedLicenseById = LicenseInfoFactory.getListedLicenseById(licenseId);
        } catch (InvalidSPDXAnalysisException e) {
            log.warn("Failed to find SpdxListedLicense with id: " + licenseId);
            return Optional.empty();
        }
        return Optional.of(listedLicenseById);
    }

    public static Optional<License> getSpdxLicenseAsSW360License(String licenseId){
        return getSpdxLicense(licenseId)
                .flatMap(SpdxConnector::getSpdxLicenseAsSW360License);
    }

    public static Optional<License> getSpdxLicenseAsSW360License(SpdxListedLicense spdxListedLicense) {
        try {
            Quadratic isOSIApproved = spdxListedLicense.isOsiApproved() ? Quadratic.YES : Quadratic.NA;
            Quadratic isFSFLibre = spdxListedLicense.isFsfLibre() ? Quadratic.YES : Quadratic.NA;

            License license = new License()
                    .setId(spdxListedLicense.getLicenseId())
                    .setShortname(spdxListedLicense.getLicenseId())
                    .setFullname(spdxListedLicense.getName())
                    .setText(spdxListedLicense.getLicenseText())
                    .setOSIApproved(isOSIApproved)
                    .setFSFLibre(isFSFLibre)
                    .setExternalLicenseLink("https://spdx.org/licenses/" + spdxListedLicense.getLicenseId()+ ".html")
                    .setExternalIds(Collections.singletonMap("SPDX-License-Identifier", spdxListedLicense.getLicenseId()));
            return Optional.of(license);
        } catch (InvalidSPDXAnalysisException e) {
            throw new RuntimeException("Error get spdx license: "+e.getMessage());
        }
    }

    /**
     * checks whether the given license matches the corresponding SPDX license
     * @param license
     * @return
     */
    public static boolean matchesSpdxLicenseText(License license) throws InvalidSPDXAnalysisException {
        return matchesSpdxLicenseText(license, license.getShortname());
    }

    public static boolean matchesSpdxLicenseText(License license, String spdxId) throws InvalidSPDXAnalysisException {
        final Optional<SpdxListedLicense> spdxLicense = getSpdxLicense(spdxId);
        if(!spdxLicense.isPresent()) {
            // A license, which has no SPDX counterpart, does not not match its SPDX-license
            return true;
        }
        return matchesSpdxLicenseText(license, spdxLicense.get());
    }

    private static boolean matchesSpdxLicenseText(License license, SpdxListedLicense spdxLicense) throws InvalidSPDXAnalysisException {
        return matchesLicenseText(license, spdxLicense.getLicenseText());
    }

    public static boolean matchesLicenseText(License license, String licenseText) {
        return  LicenseCompareHelper.isLicenseTextEquivalent(license.getText(), licenseText);
    }

    /**
     * returns a list of potentially matching SPDX license IDs
     * @param license
     * @return
     * @throws SW360Exception
     */
    public static List<String> findMatchingSpdxLicenseIDs(License license) throws SW360Exception {
        return findMatchingSpdxLicenseIDs(license.getText());
    }

    public static List<String> findMatchingSpdxLicenseIDs(String licenseText) throws SW360Exception {
        try {
            return Arrays.asList(LicenseCompareHelper.matchingStandardLicenseIds(licenseText));
        } catch (InvalidSPDXAnalysisException | SpdxCompareException e) {
            throw new SW360Exception("failed to find matching SPDX license ids due to: " + e.getMessage());
        }
    }

    /**
     * returns list of licenses in haystack, whose license text is equivalent to the one of license
     * @param license
     * @param haystack
     * @return
     */
    public static List<License> findMatchingLicenseIDs(License license, List<License> haystack) {
        return findMatchingLicenseIDs(license.getText(), haystack);
    }

    public static List<License> findMatchingLicenseIDs(String licenseText, List<License> haystack) {
        return haystack.stream()
                .filter(l -> LicenseCompareHelper.isLicenseTextEquivalent(licenseText, l.getText()))
                .collect(Collectors.toList());
    }
}
