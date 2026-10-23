/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * The attachment-usage views key on the stored {@code Source} and {@code UsageData} unions, which
 * exist on disk in two shapes: Thrift's {@code {setField_, value_}} and the service-api shape
 * {@code {releaseId|componentId|projectId}} that {@code SourceTypeAdapter} writes. CouchDB migrates
 * documents lazily, so both shapes coexist and every such view must read either one.
 *
 * <p>Reading only one shape fails silently — the view emits {@code undefined} keys and the lookup
 * returns nothing rather than erroring — so pin the requirement here. This asserts the views
 * reference both shapes; the key-for-key equivalence between them was verified by evaluating the
 * map functions against documents of each shape.
 */
public class AttachmentUsageRepositoryViewsTest {

    /** Views whose emitted keys come from a stored union. */
    private static final List<String> UNION_KEYED_VIEWS = Arrays.asList(
            "USAGESBYATTACHMENT",
            "USEDATTACHMENTS",
            "USAGESBYATTACHMENTUSAGETYPE",
            "USEDATTACHMENTUSAGESTYPE",
            "REFERENCES_RELEASEID");

    /** Views additionally keyed on the {@code UsageData} discriminator. */
    private static final List<String> USAGE_TYPE_KEYED_VIEWS = Arrays.asList(
            "USAGESBYATTACHMENTUSAGETYPE",
            "USEDATTACHMENTUSAGESTYPE");

    @Test
    public void everyUnionKeyedViewReadsBothSourceShapes() throws Exception {
        for (String viewName : UNION_KEYED_VIEWS) {
            String view = mapFunction(viewName);
            assertTrue(viewName + " no longer reads the Thrift union shape, so documents written "
                    + "before the service-api migration would stop being indexed",
                    view.contains("value_"));
            assertTrue(viewName + " does not read the service-api Source shape, so documents "
                    + "written after the migration would stop being indexed",
                    view.contains("releaseId") && view.contains("componentId")
                            && view.contains("projectId"));
        }
    }

    @Test
    public void everyUsageTypeKeyedViewReadsBothUsageDataShapes() throws Exception {
        for (String viewName : USAGE_TYPE_KEYED_VIEWS) {
            String view = mapFunction(viewName);
            assertTrue(viewName + " no longer reads the Thrift union discriminator",
                    view.contains("setField_"));
            assertTrue(viewName + " does not read the service-api UsageData shape",
                    view.contains("licenseInfo") && view.contains("sourcePackage")
                            && view.contains("manuallySet"));
        }
    }

    /**
     * The discriminator strings reach CouchDB as view keys and callers pass them through verbatim,
     * so they have to stay the Thrift {@code _Fields} constant names.
     */
    @Test
    public void usageDataDiscriminatorsKeepTheirStoredNames() throws Exception {
        String view = mapFunction("USAGESBYATTACHMENTUSAGETYPE");
        for (String stored : Arrays.asList("'LICENSE_INFO'", "'SOURCE_PACKAGE'", "'MANUALLY_SET'")) {
            assertTrue("discriminator " + stored + " must be emitted verbatim; renaming it would "
                    + "silently stop matching stored documents", view.contains(stored));
        }
    }

    @Test
    public void releaseDiscriminatorIsMatchedByStoredName() throws Exception {
        assertTrue("referencesReleaseId must still match the stored RELEASE_ID discriminator",
                mapFunction("REFERENCES_RELEASEID").contains("'RELEASE_ID'"));
    }

    private static String mapFunction(String constantName) throws Exception {
        Field field = AttachmentUsageRepository.class.getDeclaredField(constantName);
        field.setAccessible(true);
        return (String) field.get(null);
    }
}
