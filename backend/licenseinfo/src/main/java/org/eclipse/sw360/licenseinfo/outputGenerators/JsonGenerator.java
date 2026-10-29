/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.outputGenerators;

import java.util.Collection;
import java.util.Map;

import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.services.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.users.User;
/*
 * TODO: This class under development until the json template or structure is ready.
 */
public class JsonGenerator extends OutputGenerator<String> {

    private static final String JSON_MIME_TYPE = "application/json";
    private static final String JSON_OUTPUT_TYPE = "json";

    public JsonGenerator(OutputFormatVariant outputFormatVariant, String description) {
        super(JSON_OUTPUT_TYPE, description, true, JSON_MIME_TYPE, outputFormatVariant);
    }

    @Override
    public String generateOutputFile(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, Project project,
            Collection<ObligationParsingResult> obligationResults, User user, Map<String, String> externalIds,
            Map<String, ObligationStatusInfo> obligationsStatus, String fileName, boolean excludeReleaseVersion) throws SW360Exception {
        return "";
    }
}
