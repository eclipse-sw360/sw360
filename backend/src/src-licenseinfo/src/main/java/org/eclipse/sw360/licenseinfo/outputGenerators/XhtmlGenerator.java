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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Collection;
import java.util.Map;

public class XhtmlGenerator extends OutputGenerator<String> {
    private static final Logger LOGGER = Logger.getLogger(XhtmlGenerator.class);

    private static final String XHTML_TEMPLATE_FILE = "xhtmlLicenseInfoFile.vm";
    private static final String XHTML_MIME_TYPE = "application/xhtml+xml";
    private static final String XHTML_OUTPUT_TYPE = "html";

    public XhtmlGenerator(OutputFormatVariant outputFormatVariant, String description) {
        super(XHTML_OUTPUT_TYPE, description, false, XHTML_MIME_TYPE, outputFormatVariant);
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
            return renderTemplateWithDefaultValues(projectLicenseInfoResults, XHTML_TEMPLATE_FILE, projectTitle, convertHeaderTextToHTML(licenseInfoHeaderText), convertHeaderTextToHTML(obligationsText), externalIds);
        } catch (Exception e) {
            LOGGER.error("Could not generate xhtml license info file for project " + projectTitle, e);
            return "License information could not be generated.\nAn exception occured: " + e.toString();
        }
    }

    private String convertHeaderTextToHTML(String headerText) {
        String html = StringEscapeUtils.escapeHtml(headerText);
        html = html.replace("\n", "<br>");
        return html;
    }
}
