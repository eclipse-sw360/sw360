/*
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.parsers;

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.spdx.rdfparser.InvalidSPDXAnalysisException;
import org.spdx.rdfparser.license.*;
import org.spdx.rdfparser.model.SpdxDocument;
import org.spdx.rdfparser.model.SpdxFile;
import org.spdx.rdfparser.model.SpdxItem;
import org.spdx.rdfparser.model.SpdxPackage;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

public class SPDXParserTools {

    private static final Logger log = Logger.getLogger(SPDXParserTools.class);

    private static final String LICENSE_REF_PREFIX = "LicenseRef-";

    private static String extractLicenseName(AnyLicenseInfo licenseConcluded) {
        return licenseConcluded.getResource().getLocalName();
    }

    private static String extractLicenseName(ExtractedLicenseInfo extractedLicenseInfo) {
        return !isNullEmptyOrWhitespace(extractedLicenseInfo.getName())
                ? extractedLicenseInfo.getName()
                : extractedLicenseInfo.getLicenseId().replaceAll(LICENSE_REF_PREFIX, "");
    }


    private static Stream<LicenseNameWithText> getAllLicenseTextsFromInfo(AnyLicenseInfo spdxLicenseInfo) {
        log.trace("Seen the spdxLicenseInfo=" + spdxLicenseInfo.toString() + "]");
        if (spdxLicenseInfo instanceof LicenseSet) {

            LicenseSet LicenseSet = (LicenseSet) spdxLicenseInfo;
            return Arrays.stream(LicenseSet.getMembers())
                    .flatMap(SPDXParserTools::getAllLicenseTextsFromInfo);

        } else if (spdxLicenseInfo instanceof ExtractedLicenseInfo) {

            ExtractedLicenseInfo extractedLicenseInfo = (ExtractedLicenseInfo) spdxLicenseInfo;
            return Stream.of(new LicenseNameWithText()
                    .setLicenseName(extractLicenseName(extractedLicenseInfo))
                    .setLicenseText(extractedLicenseInfo.getExtractedText()));

        } else if (spdxLicenseInfo instanceof License) {

            License license = (License) spdxLicenseInfo;
            return Stream.of(new LicenseNameWithText()
                    .setLicenseName(extractLicenseName(license))
                    .setLicenseText(license.getLicenseText()));

        } else if (spdxLicenseInfo instanceof OrLaterOperator) {

            OrLaterOperator orLaterOperator = (OrLaterOperator) spdxLicenseInfo;
            return getAllLicenseTextsFromInfo(orLaterOperator.getLicense())
                    .map(lnwt -> lnwt.setLicenseName(lnwt.getLicenseName() + " or later"));

        } else if (spdxLicenseInfo instanceof WithExceptionOperator) {

            WithExceptionOperator withExceptionOperator = (WithExceptionOperator) spdxLicenseInfo;
            String licenseExceptionText = withExceptionOperator.getException()
                    .getLicenseExceptionText();
            return getAllLicenseTextsFromInfo(withExceptionOperator.getLicense())
                    .map(licenseNWT -> licenseNWT
                            .setLicenseText(licenseNWT.getLicenseText() + "\n\n" + licenseExceptionText)
                            .setLicenseName(licenseNWT.getLicenseName() + " with " + withExceptionOperator.getException().getName()));

        }
        log.debug("the spdxLicenseInfo=[" + spdxLicenseInfo.toString() + "] did not contain any license information");

        return Stream.empty();
    }

    private static Stream<LicenseNameWithText> getAllLicenseTexts(SpdxItem spdxItem, boolean useLicenseInfoFromFiles) {
        Stream<LicenseNameWithText> licenseTexts = getAllLicenseTextsFromInfo(spdxItem.getLicenseConcluded());

        if (useLicenseInfoFromFiles) {
            licenseTexts = Stream.concat(licenseTexts,
                    Arrays.stream(spdxItem.getLicenseInfoFromFiles())
                            .flatMap(SPDXParserTools::getAllLicenseTextsFromInfo));
        }

        if (spdxItem instanceof SpdxPackage) {
            SpdxPackage spdxPackage = (SpdxPackage) spdxItem;
            try {
                licenseTexts = Stream.concat(licenseTexts,
                        getAllLicenseTextsFromInfo(spdxPackage.getLicenseDeclared()));

                for (SpdxFile spdxFile : spdxPackage.getFiles()) {
                    licenseTexts = Stream.concat(licenseTexts,
                            getAllLicenseTexts(spdxFile, useLicenseInfoFromFiles));
                }
            } catch (InvalidSPDXAnalysisException e) {
                log.error("Failed to analyse spdx package: " + spdxPackage.getName(), e);
                throw new UncheckedInvalidSPDXAnalysisException(e);
            }
        }

        return licenseTexts;
    }

    private static Stream<String> getAllCopyrights(SpdxItem spdxItem) {
        Stream<String> copyrights = Stream.of(spdxItem.getCopyrightText().trim());
        if (spdxItem instanceof SpdxPackage) {
            SpdxPackage spdxPackage = (SpdxPackage) spdxItem;
            try {
                copyrights = Stream.concat(copyrights,
                        Arrays.stream(spdxPackage.getFiles())
                                .flatMap(spdxFile -> getAllCopyrights(spdxFile)));
            } catch (InvalidSPDXAnalysisException e) {
                log.error("Failed to get files of package: " + spdxPackage.getName(), e);
                throw new UncheckedInvalidSPDXAnalysisException(e);
            }
        }
        return copyrights;
    }


    protected static LicenseInfoParsingResult getLicenseInfoFromSpdx(AttachmentContent attachmentContent, SpdxDocument doc) {
        LicenseInfo licenseInfo = new LicenseInfo().setFilenames(Arrays.asList(attachmentContent.getFilename()));
        licenseInfo.setLicenseNamesWithTexts(new HashSet<>());
        licenseInfo.setCopyrights(new HashSet<>());

        try {
            for (SpdxItem spdxItem : doc.getDocumentDescribes()) {
                licenseInfo.getLicenseNamesWithTexts()
                        .addAll(getAllLicenseTexts(spdxItem, true)
                                .collect(Collectors.toSet()));
                licenseInfo.getCopyrights()
                        .addAll(getAllCopyrights(spdxItem)
                                .collect(Collectors.toSet()));
            }
        } catch (UncheckedInvalidSPDXAnalysisException e) {
            return new LicenseInfoParsingResult()
                    .setStatus(LicenseInfoRequestStatus.FAILURE)
                    .setMessage(e.getInvalidSPDXAnalysisExceptionCause().getMessage());
        } catch (InvalidSPDXAnalysisException e) {
            return new LicenseInfoParsingResult()
                    .setStatus(LicenseInfoRequestStatus.FAILURE)
                    .setMessage(e.getMessage());
        }

        return new LicenseInfoParsingResult()
                .setLicenseInfo(licenseInfo)
                .setStatus(LicenseInfoRequestStatus.SUCCESS);
    }


    private static class UncheckedInvalidSPDXAnalysisException extends RuntimeException {
        UncheckedInvalidSPDXAnalysisException(InvalidSPDXAnalysisException te) {
            super(te);
        }

        InvalidSPDXAnalysisException getInvalidSPDXAnalysisExceptionCause() {
            return (InvalidSPDXAnalysisException) getCause();
        }
    }
}
