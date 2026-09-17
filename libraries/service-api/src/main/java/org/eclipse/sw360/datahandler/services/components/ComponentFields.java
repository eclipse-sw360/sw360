/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.components;

/**
 * CouchDB document keys for {@link Component}, for use in Mango selectors, sort clauses and index
 * definitions.
 *
 * <p>These replace the thrift {@code Component._Fields.X.getFieldName()} calls that the Mango
 * queries used before the POJO migration. Every constant here must name a declared field of
 * {@link Component}; {@code ComponentFieldsTest} enforces that by reflection, so renaming a field
 * without updating this class fails the build instead of silently breaking a query at runtime.
 */
public final class ComponentFields {

    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String CATEGORIES = "categories";
    public static final String COMPONENT_TYPE = "componentType";
    public static final String LANGUAGES = "languages";
    public static final String SOFTWARE_PLATFORMS = "softwarePlatforms";
    public static final String OPERATING_SYSTEMS = "operatingSystems";
    public static final String VENDOR_NAMES = "vendorNames";
    public static final String MAIN_LICENSE_IDS = "mainLicenseIds";
    public static final String CREATED_BY = "createdBy";
    public static final String CREATED_ON = "createdOn";

    private ComponentFields() {}
}
