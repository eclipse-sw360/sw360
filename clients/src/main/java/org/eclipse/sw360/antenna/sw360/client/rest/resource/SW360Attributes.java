/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2019.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.client.rest.resource;

public class SW360Attributes {
    private SW360Attributes() {}

    // Project Controller Attributes
    public static final String PROJECT_SEARCH_BY_NAME = "name";
    public static final String PROJECT_SEARCH_BY_TYPE = "type";
    public static final String PROJECT_SEARCH_BY_UNIT = "group";
    public static final String PROJECT_SEARCH_BY_TAG = "tag";
    public static final String PROJECT_RELEASES = "releases";
    public static final String PROJECT_RELEASES_TRANSITIVE ="transitive";
    public static final String COMPONENT_SEARCH_BY_NAME = "name";

    // Attributes of Sw360License
    public static final String LICENSE_TEXT = "text";
    public static final String LICENSE_SHORT_NAME = "shortName";
    public static final String LICENSE_FULL_NAME = "fullName";

    // Attributes of Sw360Authenticator
    public static final String AUTHENTICATOR_GRANT_TYPE = "grant_type";
    public static final String AUTHENTICATOR_USERNAME = "username";
    public static final String AUTHENTICATOR_PASSWORD = "password";
}
