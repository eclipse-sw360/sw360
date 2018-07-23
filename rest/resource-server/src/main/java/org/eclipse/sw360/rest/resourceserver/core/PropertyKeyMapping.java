/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.core;

class PropertyKeyMapping {

    private static final String COMPONENT_VENDOR_KEY_THRIFT = "vendorNames";
    static final String COMPONENT_VENDOR_KEY_JSON = "vendors";

    static String componentThriftKeyFromJSONKey(String jsonKey) {
        switch (jsonKey) {
            case COMPONENT_VENDOR_KEY_JSON:
                return COMPONENT_VENDOR_KEY_THRIFT;
            default:
                return jsonKey;
        }
    }

}
