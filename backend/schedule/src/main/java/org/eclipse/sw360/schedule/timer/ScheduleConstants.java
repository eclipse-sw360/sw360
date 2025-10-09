/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.schedule.timer;


import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class ScheduleConstants {
    private static final Logger log = LogManager.getLogger(ScheduleConstants.class);

    private ScheduleConstants(){}

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    public static final String CVESEARCH_OFFSET_PROPERTY_NAME = "schedule.cvesearch.firstOffset.seconds";
    public static final String CVESEARCH_INTERVAL_PROPERTY_NAME = "schedule.cvesearch.interval.seconds";
    public static final String AUTOSTART_PROPERTY_NAME = "autostart";
    public static final String CVESEARCH_OFFSET_DEFAULT  = 0 + "" ; // default 00:00 am, in seconds
    public static final String CVESEARCH_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVMSYNC_OFFSET_PROPERTY_NAME = "schedule.svmsync.firstOffset.seconds";
    public static final String SVMSYNC_INTERVAL_PROPERTY_NAME = "schedule.svmsync.interval.seconds";
    public static final String SVMSYNC_OFFSET_DEFAULT  = (1*60*60) + "" ; // default 01:00 am, in seconds
    public static final String SVMSYNC_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVMMATCH_OFFSET_PROPERTY_NAME = "schedule.svmmatch.firstOffset.seconds";
    public static final String SVMMATCH_INTERVAL_PROPERTY_NAME = "schedule.svmmatch.interval.seconds";
    public static final String SVMMATCH_OFFSET_DEFAULT  = (2*60*60) + "" ; // default 01:00 am, in seconds
    public static final String SVMMATCH_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME = "schedule.svmlistupdate.firstOffset.seconds";
    public static final String SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME = "schedule.svmlistupdate.interval.seconds";
    public static final String SVM_LIST_UPDATE_OFFSET_DEFAULT  = (3*60*60) + "" ; // default 03:00 am, in seconds
    public static final String SVM_LIST_UPDATE_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVM_TRACKING_FEEDBACK_OFFSET_PROPERTY_NAME = "schedule.trackingfeedback.firstOffset.seconds";
    public static final String SVM_TRACKING_FEEDBACK_INTERVAL_PROPERTY_NAME = "schedule.trackingfeedback.interval.seconds";
    public static final String SVM_TRACKING_FEEDBACK_OFFSET_DEFAULT  = (4*60*60) + "" ; // default 04:00 am, in seconds
    public static final String SVM_TRACKING_FEEDBACK_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SRC_UPLOAD_SERVICE_OFFSET_PROPERTY_NAME = "schedule.srcupload.firstOffset.seconds";
    public static final String SRC_UPLOAD_SERVICE_OFFSET_DEFAULT = (22*60*60) + ""; //default 10:00 pm, in seconds
    public static final String SRC_UPLOAD_SERVICE_INTERVAL_PROPERTY_NAME = "schedule.srcupload.interval.seconds";
    public static final String SRC_UPLOAD_SERVICE_INTERVAL_DEFAULT = (24*60*60)+"" ; // default 24h, in seconds;

    public static final String DELETE_ATTACHMENT_OFFSET_DEFAULT  = "0"; // default 00:00 am, in seconds
    public static final String DELETE_ATTACHMENT_INTERVAL_DEFAULT  = (24*60*60) + "" ; // default 24h, in seconds
    public static final String DELETE_ATTACHMENT_OFFSET_PROPERTY_NAME = "schedule.delete.attachment.firstOffset.seconds";
    public static final String DELETE_ATTACHMENT_INTERVAL_PROPERTY_NAME = "schedule.delete.attachment.interval.seconds";
    public static final String DEPARTMENT_OFFSET_PROPERTY_NAME = "schedule.department.firstOffset.seconds";
    public static final String DEPARTMENT_OFFSET_DEFAULT  = "0" ; // default 00:00 am, in seconds
    public static final String DEPARTMENT_INTERVAL_PROPERTY_NAME = "schedule.department.interval.seconds";
    public static final String DEPARTMENT_INTERVAL_DEFAULT  = (24*60*60) + "" ; // default 24h, in seconds

    // scheduler properties
    public static final ConcurrentHashMap<String, Integer> SYNC_FIRST_RUN_OFFSET_SEC = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> SYNC_INTERVAL_SEC = new ConcurrentHashMap<>();
    public static final String[] autostartServices;
    public static Set<String> invalidConfiguredServices = new HashSet<>();

    static {
        Properties props = CommonUtils.loadProperties(ScheduleConstants.class, PROPERTIES_FILE_PATH);

        loadScheduledServiceProperties(props, ThriftClients.CVESEARCH_SERVICE, CVESEARCH_OFFSET_PROPERTY_NAME, CVESEARCH_OFFSET_DEFAULT, CVESEARCH_INTERVAL_PROPERTY_NAME, CVESEARCH_INTERVAL_DEFAULT);
        loadScheduledServiceProperties(props, ThriftClients.SVMSYNC_SERVICE, SVMSYNC_OFFSET_PROPERTY_NAME, SVMSYNC_OFFSET_DEFAULT, SVMSYNC_INTERVAL_PROPERTY_NAME, SVMSYNC_INTERVAL_DEFAULT);
        loadScheduledServiceProperties(props, ThriftClients.SVMMATCH_SERVICE, SVMMATCH_OFFSET_PROPERTY_NAME, SVMMATCH_OFFSET_DEFAULT, SVMMATCH_INTERVAL_PROPERTY_NAME, SVMMATCH_INTERVAL_DEFAULT);
        loadScheduledServiceProperties(props, ThriftClients.SVM_LIST_UPDATE_SERVICE, SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME, SVM_LIST_UPDATE_OFFSET_DEFAULT, SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME, SVM_LIST_UPDATE_INTERVAL_DEFAULT);
        loadScheduledServiceProperties(props, ThriftClients.SVM_TRACKING_FEEDBACK_SERVICE, SVM_TRACKING_FEEDBACK_OFFSET_PROPERTY_NAME, SVM_TRACKING_FEEDBACK_OFFSET_DEFAULT, SVM_TRACKING_FEEDBACK_INTERVAL_PROPERTY_NAME, SVM_TRACKING_FEEDBACK_INTERVAL_DEFAULT);
        loadScheduledServiceProperties(props, ThriftClients.SRC_UPLOAD_SERVICE, SRC_UPLOAD_SERVICE_OFFSET_PROPERTY_NAME, SRC_UPLOAD_SERVICE_OFFSET_DEFAULT, SRC_UPLOAD_SERVICE_INTERVAL_PROPERTY_NAME, SRC_UPLOAD_SERVICE_INTERVAL_DEFAULT);
        loadScheduledServiceProperties(props, ThriftClients.DELETE_ATTACHMENT_SERVICE, DELETE_ATTACHMENT_OFFSET_PROPERTY_NAME, DELETE_ATTACHMENT_OFFSET_DEFAULT, DELETE_ATTACHMENT_INTERVAL_PROPERTY_NAME, DELETE_ATTACHMENT_INTERVAL_DEFAULT);
        loadScheduledServiceProperties(props, ThriftClients.IMPORT_DEPARTMENT_SERVICE, DEPARTMENT_OFFSET_PROPERTY_NAME, DEPARTMENT_OFFSET_DEFAULT, DEPARTMENT_INTERVAL_PROPERTY_NAME, DEPARTMENT_INTERVAL_DEFAULT);

        String autostartServicesString = props.getProperty(AUTOSTART_PROPERTY_NAME, "").trim();
        autostartServices = java.util.Arrays.stream(autostartServicesString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    private static void loadScheduledServiceProperties(Properties props, String serviceName, String offsetPropertyName, String offsetDefault, String intervalPropertyName, String intervalDefault) {
        loadIntProperty(props, serviceName, offsetPropertyName, offsetDefault, SYNC_FIRST_RUN_OFFSET_SEC);
        loadIntProperty(props, serviceName, intervalPropertyName, intervalDefault, SYNC_INTERVAL_SEC);
    }

    private static void loadIntProperty(Properties props, String serviceName, String propertyName, String defaultValue, ConcurrentHashMap<String, Integer> targetMap) {
        String raw = props.getProperty(propertyName, defaultValue);
        String value = raw == null ? defaultValue : raw.trim();
        int parsed;
        try {
            parsed = Integer.parseInt(value);
            if (parsed < 0) {
                log.warn("Property {} has negative value ({}). Using default: {}", propertyName, value, defaultValue);
                parsed = Integer.parseInt(defaultValue.trim());
                invalidConfiguredServices.add(serviceName); // mark as invalid config
            }
        } catch (NumberFormatException e) {
            log.warn("Property {} is not an integer ({}). Using default: {}", propertyName, value, defaultValue);
            parsed = Integer.parseInt(defaultValue.trim());
            invalidConfiguredServices.add(serviceName);
        }
        targetMap.put(serviceName, parsed);
    }
}