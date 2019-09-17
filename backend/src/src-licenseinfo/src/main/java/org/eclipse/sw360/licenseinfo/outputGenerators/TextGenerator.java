/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * With modifications by Siemens AG, 2017-2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.licenseinfo.outputGenerators;

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Collection;
import java.util.Map;

public class TextGenerator extends OutputGenerator<String> {
    private static final Logger LOGGER = Logger.getLogger(TextGenerator.class);

    private static final String TXT_TEMPLATE_FILE = "textLicenseInfoFile.vm";
    private static final String TXT_MIME_TYPE = "text/plain";
    private static final String TXT_OUTPUT_TYPE = "txt";

    public TextGenerator(OutputFormatVariant outputFormatVariant, String outputDescription) {
        super(TXT_OUTPUT_TYPE, outputDescription, false, TXT_MIME_TYPE, outputFormatVariant);
    }

    @Override
    public String generateOutputFile(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, Project project, Collection<ObligationParsingResult> obligationResults, User user, Map<String,String> externalIds) throws SW360Exception {
        String projectName = project.getName();
        String projectVersion = project.getVersion();
        String licenseInfoHeaderText = project.getLicenseInfoHeaderText();
        String obligationsText = project.getObligationsText();

        switch (getOutputVariant()) {
            case DISCLOSURE:
                return generateDisclosure(projectLicenseInfoResults, projectName + " " + projectVersion, licenseInfoHeaderText, obligationsText, externalIds);
            default:
                throw new IllegalArgumentException("Unknown generator variant type: " + getOutputVariant());
        }
    }
    private String generateDisclosure(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, String projectTitle, String licenseInfoHeaderText, String obligationsText, Map<String, String> externalIds) {
        try {
            return renderTemplateWithDefaultValues(projectLicenseInfoResults, TXT_TEMPLATE_FILE, projectTitle, licenseInfoHeaderText, obligationsText, externalIds);
        } catch (Exception e) {
            LOGGER.error("Could not generate text licenseinfo file for project " + projectTitle, e);
            return "License information could not be generated.\nAn exception occurred: " + e.toString();
        }
    }
}
