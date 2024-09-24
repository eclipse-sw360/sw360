/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.common;

import org.eclipse.sw360.datahandler.common.CommonUtils;

import java.util.Properties;

/**
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */
public class SVMConstants {

    private SVMConstants(){}

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    // urls of SVM server
    public static final String COMPONENTS_URL;
    public static final String ACTIONS_URL;
    public static final String PRIORITIES_URL;
    public static final String COMPONENTS_ID_WILDCARD = "#compVmId#";
    public static final String VULNERABILITIES_PER_COMPONENT_URL;
    public static final String VULNERABILITIES_URL;

    // JSON components field names
    public static final String COMPONENT_VENDOR             = "vendor";
    public static final String COMPONENT_NAME               = "component_name";
    public static final String COMPONENT_VERSION            = "version";
    public static final String COMPONENT_URL                = "url";
    public static final String COMPONENT_SECURITY_URL       = "security_url";
    public static final String COMPONENT_EOL_REACHED        = "eol_reached";
    public static final String COMPONENT_CPE                = "cpe_name";
    public static final String COMPONENT_MIN_PATCH_LEVEL    = "minimum_patch_levels";

    // JSON priorities field names
    public static final String PRIORITY_SHORT               = "short_text";
    public static final String PRIORITY_LONG                = "long_text";

    // JSON actions field names
    public static final String ACTION_TEXT                  = "text";

    // JSON vulnerability field names
    public static final String VULNERABILITY_ID             = "id";
    public static final String VULNERABILITY_TITLE          = "title";
    public static final String VULNERABILITY_DESCRIPTION     = "description";
    public static final String VULNERABILITY_PUBLISH_DATE   = "publish_date";
    public static final String VULNERABILITY_LAST_UPDATE    = "last_update";
    public static final String VULNERABILITY_PRIORITY       = "priority";
    public static final String VULNERABILITY_ACTION         = "action";
    public static final String VULNERABILITY_IMPACT         = "impact";
    public static final String VULNERABILITY_COMPONENTS     = "assigned_components";
    public static final String VULNERABILITY_VENDOR_ADVISORIES = "vendor_advisories";
    public static final String VULNERABILITY_LEGAL_NOTICE   = "legal_notice";
    public static final String VULNERABILITY_EXTENDED_DISC  = "extended_description";
    public static final String VULNERABILITY_CVE_REFERENCES = "cve_references";
    public static final String VULNERABILITY_REFERENCES     = "references";
    public static final String VULNERABILITY_VA_VENDOR      = "vendor";
    public static final String VULNERABILITY_VA_NAME        = "name";
    public static final String VULNERABILITY_VA_URL         = "url";
    public static final String VULNERABILITY_CVE_YEAR       = "year";
    public static final String VULNERABILITY_CVE_NUMBER       = "number";

    // processing properties
    public static final int PROCESSING_CORE_POOL_SIZE       = 20;
    public static final int PROCESSING_MAX_POOL_SIZE        = 20;
    public static final int PROCESSING_KEEP_ALIVE_SECONDS   = 60;

    private static final String SVM_BASE_HOST_URL;
    private static final String SVM_API_ROOT_PATH;

    static {
        Properties props = CommonUtils.loadProperties(SVMConstants.class, PROPERTIES_FILE_PATH);

        SVM_BASE_HOST_URL = props.getProperty("svm.base.path", "");
        SVM_API_ROOT_PATH = props.getProperty("svm.api.root.path", "");
        COMPONENTS_URL  = props.getProperty("svm.components.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/components");
        ACTIONS_URL     = props.getProperty("svm.actions.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/actions");
        PRIORITIES_URL  = props.getProperty("svm.priorities.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/priorities");
        VULNERABILITIES_PER_COMPONENT_URL  = props.getProperty("svm.components.vulnerabilities.url",
                            SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/components/" +COMPONENTS_ID_WILDCARD+"/notifications");
        VULNERABILITIES_URL  = props.getProperty("svm.vulnerabilities.url", SVM_BASE_HOST_URL + SVM_API_ROOT_PATH + "/notifications");
    }

}
