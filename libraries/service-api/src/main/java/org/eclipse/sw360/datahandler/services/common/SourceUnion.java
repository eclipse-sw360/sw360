/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.common;

/**
 * Reads a {@link Source} the way it is stored, replacing Thrift's union accessors.
 *
 * <p>{@code Source} was a Thrift union, so callers used {@code getSetField()} and
 * {@code getFieldValue()} to discover which of the three ids was set. The service-api POJO is three
 * nullable fields instead, and this supplies the same two questions.
 *
 * <p>The names returned by {@link #storedTypeOf} are the Thrift {@code _Fields} constant names.
 * They are not cosmetic: attachment-usage documents hold them on disk, the CouchDB views in
 * {@code AttachmentUsageRepository} key on them, and callers pass them through as view keys. A
 * different spelling here silently stops matching stored documents.
 */
public final class SourceUnion {

    public static final String PROJECT_ID = "PROJECT_ID";
    public static final String COMPONENT_ID = "COMPONENT_ID";
    public static final String RELEASE_ID = "RELEASE_ID";

    private SourceUnion() {
    }

    /**
     * The id this source points at, whichever kind it is, or {@code null} when none is set.
     * Equivalent to Thrift's {@code getFieldValue()}.
     */
    public static String idOf(Source source) {
        if (source == null) {
            return null;
        }
        if (source.getProjectId() != null) {
            return source.getProjectId();
        }
        if (source.getComponentId() != null) {
            return source.getComponentId();
        }
        return source.getReleaseId();
    }

    /**
     * Which kind of id is set, as stored, or {@code null} when none is. Equivalent to Thrift's
     * {@code getSetField().toString()}.
     */
    public static String storedTypeOf(Source source) {
        if (source == null) {
            return null;
        }
        if (source.getProjectId() != null) {
            return PROJECT_ID;
        }
        if (source.getComponentId() != null) {
            return COMPONENT_ID;
        }
        if (source.getReleaseId() != null) {
            return RELEASE_ID;
        }
        return null;
    }

    /** Whether this source points at a release. */
    public static boolean isRelease(Source source) {
        return source != null && source.getReleaseId() != null;
    }

    public static Source ofProject(String projectId) {
        return new Source().setProjectId(projectId);
    }

    public static Source ofComponent(String componentId) {
        return new Source().setComponentId(componentId);
    }

    public static Source ofRelease(String releaseId) {
        return new Source().setReleaseId(releaseId);
    }
}
