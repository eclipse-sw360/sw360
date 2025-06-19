/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 * With modifications from Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.schedule.timer;


import org.eclipse.sw360.datahandler.common.SW360ConfigKeys;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.common.ScheduleConstants;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class ScheduleHelper {
    private static final Logger log = LogManager.getLogger(ScheduleHelper.class);

    private ScheduleHelper(){}

    public static int getRunOffset(@NotNull String serviceName) {
        String offset = switch (serviceName) {
            case ThriftClients.CVESEARCH_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.CVESEARCH_OFFSET_PROPERTY_NAME, ScheduleConstants.CVESEARCH_OFFSET_DEFAULT);
            case ThriftClients.SVMSYNC_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVMSYNC_OFFSET_PROPERTY_NAME, ScheduleConstants.SVMSYNC_OFFSET_DEFAULT);
            case ThriftClients.SVMMATCH_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVMMATCH_OFFSET_PROPERTY_NAME, ScheduleConstants.SVMMATCH_OFFSET_DEFAULT);
            case ThriftClients.SVM_LIST_UPDATE_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVM_LIST_UPDATE_OFFSET_PROPERTY_NAME, ScheduleConstants.SVM_LIST_UPDATE_OFFSET_DEFAULT);
            case ThriftClients.SVM_TRACKING_FEEDBACK_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVM_TRACKING_FEEDBACK_OFFSET_PROPERTY_NAME, ScheduleConstants.SVM_TRACKING_FEEDBACK_OFFSET_DEFAULT);
            case ThriftClients.SRC_UPLOAD_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SRC_UPLOAD_SERVICE_OFFSET_PROPERTY_NAME, ScheduleConstants.SRC_UPLOAD_SERVICE_OFFSET_DEFAULT);
            default -> "";
        };
        try {
            return Integer.parseInt(offset);
        } catch (NumberFormatException nfe) {
            log.error("Property {} is not an integer.", serviceName);
            return -1;
        }
    }

    public static int getIntervalSec(@NotNull String serviceName) {
        String interval = switch (serviceName) {
            case ThriftClients.CVESEARCH_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.CVESEARCH_INTERVAL_PROPERTY_NAME, ScheduleConstants.CVESEARCH_INTERVAL_DEFAULT);
            case ThriftClients.SVMSYNC_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVMSYNC_INTERVAL_PROPERTY_NAME, ScheduleConstants.SVMSYNC_INTERVAL_DEFAULT);
            case ThriftClients.SVMMATCH_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVMMATCH_INTERVAL_PROPERTY_NAME, ScheduleConstants.SVMMATCH_INTERVAL_DEFAULT);
            case ThriftClients.SVM_LIST_UPDATE_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVM_LIST_UPDATE_INTERVAL_PROPERTY_NAME, ScheduleConstants.SVM_LIST_UPDATE_INTERVAL_DEFAULT);
            case ThriftClients.SVM_TRACKING_FEEDBACK_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SVM_TRACKING_FEEDBACK_INTERVAL_PROPERTY_NAME, ScheduleConstants.SVM_TRACKING_FEEDBACK_INTERVAL_DEFAULT);
            case ThriftClients.SRC_UPLOAD_SERVICE -> SW360Utils.readConfig(SW360ConfigKeys.SRC_UPLOAD_SERVICE_INTERVAL_PROPERTY_NAME, ScheduleConstants.SRC_UPLOAD_SERVICE_INTERVAL_DEFAULT);
            default -> "";
        };
        try {
            return Integer.parseInt(interval);
        } catch (NumberFormatException nfe) {
            log.error("Property {} is not an integer.", serviceName);
            return -1;
        }
    }
}
