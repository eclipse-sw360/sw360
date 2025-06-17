/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

public class ScheduleConstants {
    public static final String CVESEARCH_OFFSET_DEFAULT  = 0 + "" ; // default 00:00 am, in seconds
    public static final String CVESEARCH_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVMSYNC_OFFSET_DEFAULT  = (1*60*60) + "" ; // default 01:00 am, in seconds
    public static final String SVMSYNC_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVMMATCH_OFFSET_DEFAULT  = (2*60*60) + "" ; // default 01:00 am, in seconds
    public static final String SVMMATCH_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVM_LIST_UPDATE_OFFSET_DEFAULT  = (3*60*60) + "" ; // default 03:00 am, in seconds
    public static final String SVM_LIST_UPDATE_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SVM_TRACKING_FEEDBACK_OFFSET_DEFAULT  = (4*60*60) + "" ; // default 04:00 am, in seconds
    public static final String SVM_TRACKING_FEEDBACK_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String SRC_UPLOAD_SERVICE_OFFSET_DEFAULT = (22*60*60) + ""; //default 10:00 pm, in seconds
    public static final String SRC_UPLOAD_SERVICE_INTERVAL_DEFAULT = (24*60*60)+"" ; // default 24h, in seconds;

    public static final String DELETE_ATTACHMENT_OFFSET_DEFAULT  = "0"; // default 00:00 am, in seconds
    public static final String DELETE_ATTACHMENT_INTERVAL_DEFAULT  = (24*60*60) + "" ; // default 24h, in seconds
    public static final String DEPARTMENT_OFFSET_DEFAULT  = "0" ; // default 00:00 am, in seconds
    public static final String DEPARTMENT_INTERVAL_DEFAULT  = (24*60*60) + "" ; // default 24h, in seconds

    private ScheduleConstants() {
        // Utility class with only static functions
    }
}
