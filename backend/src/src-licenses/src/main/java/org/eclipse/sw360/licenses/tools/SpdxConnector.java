/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenses.tools;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.spdx.compare.LicenseCompareHelper;
import org.spdx.compare.SpdxCompareException;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.LicenseInfoFactory;
import org.spdx.rdfparser.license.SpdxListedLicense;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SpdxConnector {

    private static final Logger log = Logger.getLogger(SpdxConnector.class);

    public static List<String> getAllSpdxLicenseIds() {
        return Arrays.asList(LicenseInfoFactory.getSpdxListedLicenseIds());
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

    public static Optional<License> getSpdxLicenseAsSW360License(SpdxListedLicense spdxListedLicense){
        License license = new License()
                .setId(spdxListedLicense.getLicenseId())
                .setShortname(spdxListedLicense.getLicenseId())
                .setFullname(spdxListedLicense.getName())
                .setText(spdxListedLicense.getLicenseText())
                .setExternalLicenseLink("https://spdx.org/licenses/" + spdxListedLicense.getLicenseId()+ ".html");
        return Optional.of(license);
    }

    /**
     * checks whether the given license matches the corresponding SPDX license
     * @param license
     * @return
     */
    public static boolean matchesSpdxLicenseText(License license) {
        return matchesSpdxLicenseText(license, license.getShortname());
    }

    public static boolean matchesSpdxLicenseText(License license, String spdxId) {
        final Optional<SpdxListedLicense> spdxLicense = getSpdxLicense(spdxId);
        if(!spdxLicense.isPresent()) {
            // A license, which has no SPDX counterpart, does not not match its SPDX-license
            return true;
        }
        return matchesSpdxLicenseText(license, spdxLicense.get());
    }

    private static boolean matchesSpdxLicenseText(License license, SpdxListedLicense spdxLicense) {
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
