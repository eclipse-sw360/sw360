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
import java.util.Properties;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
/*
 * TODO: This class under development until the json template or structure is ready.
 */
public class JsonGenerator extends OutputGenerator<String> {

    private static final String JSON_MIME_TYPE = "application/json";
    private static final String JSON_OUTPUT_TYPE = "json";

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    public JsonGenerator(OutputFormatVariant outputFormatVariant, String description) {
        super(JSON_OUTPUT_TYPE, description, true, JSON_MIME_TYPE, outputFormatVariant);
        Properties props = CommonUtils.loadProperties(JsonGenerator.class, PROPERTIES_FILE_PATH);
    }

    @Override
    public String generateOutputFile(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, Project project, Collection<ObligationParsingResult> obligationResults, User user, Map<String, String> externalIds, Map<String, ObligationStatusInfo> obligationsStatus, String fileName) throws SW360Exception {
        return "";
        
    }
}