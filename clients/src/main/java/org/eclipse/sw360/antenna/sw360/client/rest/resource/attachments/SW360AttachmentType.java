/*
 * Copyright (c) Bosch Software Innovations GmbH 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.attachments;

public enum SW360AttachmentType {
    DOCUMENT(0),
    SOURCE(1),
    DESIGN(2),
    REQUIREMENT(3),
    CLEARING_REPORT(4),
    COMPONENT_LICENSE_INFO_XML(5),
    COMPONENT_LICENSE_INFO_COMBINED(6),
    SCAN_RESULT_REPORT(7),
    SCAN_RESULT_REPORT_XML(8),
    SOURCE_SELF(9),
    BINARY(10),
    BINARY_SELF(11),
    DECISION_REPORT(12),
    LEGAL_EVALUATION(13),
    LICENSE_AGREEMENT(14),
    SCREENSHOT(15),
    OTHER(16),
    README_OSS(17);

    private final int value;

    SW360AttachmentType(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static SW360AttachmentType findByValue(int value) {
        switch (value) {
            case 0:
                return DOCUMENT;
            case 1:
                return SOURCE;
            case 2:
                return DESIGN;
            case 3:
                return REQUIREMENT;
            case 4:
                return CLEARING_REPORT;
            case 5:
                return COMPONENT_LICENSE_INFO_XML;
            case 6:
                return COMPONENT_LICENSE_INFO_COMBINED;
            case 7:
                return SCAN_RESULT_REPORT;
            case 8:
                return SCAN_RESULT_REPORT_XML;
            case 9:
                return SOURCE_SELF;
            case 10:
                return BINARY;
            case 11:
                return BINARY_SELF;
            case 12:
                return DECISION_REPORT;
            case 13:
                return LEGAL_EVALUATION;
            case 14:
                return LICENSE_AGREEMENT;
            case 15:
                return SCREENSHOT;
            case 17:
                return README_OSS;
            default:
                return OTHER;
        }
    }
}
