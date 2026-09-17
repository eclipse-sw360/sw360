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
 * CouchDB document keys for {@link Release}, for use in Mango selectors, sort clauses and index
 * definitions.
 *
 * <p>These replace the thrift {@code Release._Fields.X.getFieldName()} calls that the Mango queries
 * used before the POJO migration. Every constant here must name a declared field of
 * {@link Release}; {@code ReleaseFieldsTest} enforces that by reflection, so renaming a field
 * without updating this class fails the build instead of silently breaking a query at runtime.
 */
public final class ReleaseFields {

    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String VERSION = "version";
    public static final String COMPONENT_ID = "componentId";
    public static final String CLEARING_STATE = "clearingState";
    public static final String MAINLINE_STATE = "mainlineState";
    public static final String CREATED_ON = "createdOn";
    public static final String ATTACHMENTS = "attachments";

    /** Sub-field of {@code attachments}, i.e. {@code Attachment.attachmentType}. */
    public static final String ATTACHMENT_TYPE = "attachmentType";

    /** Dotted Mango path into the embedded attachment documents. */
    public static final String ATTACHMENTS_ATTACHMENT_TYPE = ATTACHMENTS + "." + ATTACHMENT_TYPE;

    private ReleaseFields() {}
}
