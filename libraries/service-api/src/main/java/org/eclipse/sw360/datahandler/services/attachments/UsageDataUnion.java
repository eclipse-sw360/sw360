/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.attachments;

/**
 * Reads a {@link UsageData} the way it is stored, replacing Thrift's union accessors — the
 * counterpart of {@code SourceUnion} for the other union on an attachment usage.
 *
 * <p>The names returned by {@link #storedTypeOf} are the Thrift {@code _Fields} constant names, and
 * are a stored contract: attachment-usage documents hold them, the CouchDB views in
 * {@code AttachmentUsageRepository} key on them, and callers pass them through as view keys.
 * Respelling one silently stops matching stored documents.
 */
public final class UsageDataUnion {

    public static final String LICENSE_INFO = "LICENSE_INFO";
    public static final String SOURCE_PACKAGE = "SOURCE_PACKAGE";
    public static final String MANUALLY_SET = "MANUALLY_SET";

    private UsageDataUnion() {
    }

    /**
     * Which variant is set, as stored, or {@code null} when none is. Equivalent to Thrift's
     * {@code getSetField().toString()}.
     */
    public static String storedTypeOf(UsageData usageData) {
        if (usageData == null) {
            return null;
        }
        if (usageData.getLicenseInfo() != null) {
            return LICENSE_INFO;
        }
        if (usageData.getSourcePackage() != null) {
            return SOURCE_PACKAGE;
        }
        if (usageData.getManuallySet() != null) {
            return MANUALLY_SET;
        }
        return null;
    }

    /**
     * Whether both hold the same variant. Two usages with no variant set count as the same, matching
     * how a Thrift union with nothing set compared equal to another.
     */
    public static boolean sameVariant(UsageData one, UsageData other) {
        String a = storedTypeOf(one);
        String b = storedTypeOf(other);
        return a == null ? b == null : a.equals(b);
    }
}
