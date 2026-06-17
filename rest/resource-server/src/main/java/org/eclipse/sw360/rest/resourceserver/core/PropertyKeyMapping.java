/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

class PropertyKeyMapping {

    private static final String COMPONENT_VENDOR_KEY_THRIFT = "vendorNames";
    static final String COMPONENT_VENDOR_KEY_JSON = "vendors";

    private static final String RELEASE_CPEID_KEY_THRIFT = "cpeid";
    static final String RELEASE_CPEID_KEY_JSON = "cpeId";

    static String componentThriftKeyFromJSONKey(String jsonKey) {
        if (COMPONENT_VENDOR_KEY_JSON.equals(jsonKey)) {
            return COMPONENT_VENDOR_KEY_THRIFT;
        }
        return jsonKey;
    }

    static String releaseThriftKeyFromJSONKey(String jsonKey) {
        if (RELEASE_CPEID_KEY_JSON.equals(jsonKey)) {
            return RELEASE_CPEID_KEY_THRIFT;
        }
        return jsonKey;
    }

}
