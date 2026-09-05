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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Fields of a {@link Release} that a client payload must never overwrite: on update they are
 * restored from the stored document.
 *
 * <p>Replaces {@code ThriftUtils.IMMUTABLE_OF_RELEASE} and
 * {@code ThriftUtils.IMMUTABLE_OF_RELEASE_FOR_FOSSOLOGY}, which named the same fields with thrift
 * {@code Release._Fields} constants and so forced every caller of
 * {@code ComponentDatabaseHandler.updateRelease} to import thrift.
 */
public enum ReleaseImmutableField {

    CREATED_BY,
    CREATED_ON,
    EXTERNAL_TOOL_PROCESSES;

    /** Applies to a normal release update. */
    public static final Set<ReleaseImmutableField> DEFAULT =
            Collections.unmodifiableSet(EnumSet.allOf(ReleaseImmutableField.class));

    /**
     * Applies when FOSSology writes back a scan result: it is the one caller allowed to replace
     * {@link #EXTERNAL_TOOL_PROCESSES}, since that is the field it exists to update.
     */
    public static final Set<ReleaseImmutableField> FOR_FOSSOLOGY =
            Collections.unmodifiableSet(EnumSet.of(CREATED_BY, CREATED_ON));
}
