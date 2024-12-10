/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

public class DatabaseConstants extends SW360Constants {

    public static final String SVM_JSON_LOG_OUTPUT_LOCATION;
    public static final boolean IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED;
    public static final String ATTACHMENT_STORE_FILE_SYSTEM_LOCATION;
    public static final String ATTACHMENT_STORE_FILE_SYSTEM_PERMISSION;
    public static final String ATTACHMENT_DELETE_NO_OF_DAYS;
    public static final boolean IS_SW360CHANGELOG_ENABLED;
    public static final String CHANGE_LOG_CONFIG_FILE_PATH;
    public static final String SW360CHANGELOG_OUTPUT_PATH;
    public static final boolean AUTO_SET_ECC_STATUS;

    static {
        SVM_JSON_LOG_OUTPUT_LOCATION = props.getProperty("svm.json.log.output.location", "/tmp");
        ATTACHMENT_STORE_FILE_SYSTEM_LOCATION = props.getProperty("attachment.store.file.system.location",
                "/opt/sw360tempattachments");
        ATTACHMENT_STORE_FILE_SYSTEM_PERMISSION = props.getProperty("attachment.store.file.system.permission",
                "rwx------");
        IS_STORE_ATTACHMENT_TO_FILE_SYSTEM_ENABLED = Boolean.parseBoolean(props.getProperty("enable.attachment.store.to.file.system", "false"));
        ATTACHMENT_DELETE_NO_OF_DAYS = props.getProperty("attachemnt.delete.no.of.days",
                "30");
        IS_SW360CHANGELOG_ENABLED = Boolean.parseBoolean(props.getProperty("enable.sw360.change.log", "false"));
        CHANGE_LOG_CONFIG_FILE_PATH = props.getProperty("sw360changelog.config.file.location",
                "/etc/sw360/log4j2.xml");
        SW360CHANGELOG_OUTPUT_PATH = props.getProperty("sw360changelog.output.path",
                "sw360changelog/sw360changelog");
        AUTO_SET_ECC_STATUS = Boolean.parseBoolean(props.getProperty("auto.set.ecc.status", "false"));
    }
}
