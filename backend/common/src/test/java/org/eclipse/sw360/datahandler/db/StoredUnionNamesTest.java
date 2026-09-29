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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.eclipse.sw360.datahandler.services.attachments.LicenseInfoUsage;
import org.eclipse.sw360.datahandler.services.attachments.ManuallySetUsage;
import org.eclipse.sw360.datahandler.services.attachments.SourcePackageUsage;
import org.eclipse.sw360.datahandler.services.attachments.UsageData;
import org.eclipse.sw360.datahandler.services.attachments.UsageDataUnion;
import org.eclipse.sw360.datahandler.services.common.Source;
import org.eclipse.sw360.datahandler.services.common.SourceUnion;
import org.junit.Test;

/**
 * The union discriminators are written into attachment-usage documents, keyed on by the CouchDB
 * views, and passed by callers straight through as view keys. Java and the views therefore have to
 * agree on their exact spelling, and nothing else enforces that: a mismatch does not fail, it just
 * stops matching stored documents.
 */
public class StoredUnionNamesTest {

    @Test
    public void sourceDiscriminatorsAppearInTheViewThatKeysOnThem() throws Exception {
        String view = mapFunction("REFERENCES_RELEASEID");
        assertTrue("SourceUnion.RELEASE_ID (" + SourceUnion.RELEASE_ID + ") is not the value "
                + "referencesReleaseId matches against", view.contains("'" + SourceUnion.RELEASE_ID + "'"));
    }

    @Test
    public void usageDataDiscriminatorsAppearInTheViewsThatKeyOnThem() throws Exception {
        for (String viewName : new String[] {"USAGESBYATTACHMENTUSAGETYPE", "USEDATTACHMENTUSAGESTYPE"}) {
            String view = mapFunction(viewName);
            for (String stored : new String[] {UsageDataUnion.LICENSE_INFO,
                    UsageDataUnion.SOURCE_PACKAGE, UsageDataUnion.MANUALLY_SET}) {
                assertTrue(viewName + " does not emit " + stored + ", so usages of that type would "
                        + "not be found once documents are written in the service-api shape",
                        view.contains("'" + stored + "'"));
            }
        }
    }

    @Test
    public void sourceUnionReadsWhicheverIdIsSet() {
        assertEquals("p-1", SourceUnion.idOf(SourceUnion.ofProject("p-1")));
        assertEquals("c-1", SourceUnion.idOf(SourceUnion.ofComponent("c-1")));
        assertEquals("r-1", SourceUnion.idOf(SourceUnion.ofRelease("r-1")));

        assertEquals(SourceUnion.PROJECT_ID, SourceUnion.storedTypeOf(SourceUnion.ofProject("p-1")));
        assertEquals(SourceUnion.COMPONENT_ID, SourceUnion.storedTypeOf(SourceUnion.ofComponent("c-1")));
        assertEquals(SourceUnion.RELEASE_ID, SourceUnion.storedTypeOf(SourceUnion.ofRelease("r-1")));

        assertTrue(SourceUnion.isRelease(SourceUnion.ofRelease("r-1")));
        assertTrue(!SourceUnion.isRelease(SourceUnion.ofProject("p-1")));
    }

    @Test
    public void sourceUnionIsNullSafeAndHandlesNothingSet() {
        assertNull(SourceUnion.idOf(null));
        assertNull(SourceUnion.storedTypeOf(null));
        assertNull(SourceUnion.idOf(new Source()));
        assertNull(SourceUnion.storedTypeOf(new Source()));
        assertTrue(!SourceUnion.isRelease(null));
    }

    @Test
    public void usageDataUnionReadsWhicheverVariantIsSet() {
        assertEquals(UsageDataUnion.LICENSE_INFO, UsageDataUnion.storedTypeOf(
                new UsageData().setLicenseInfo(new LicenseInfoUsage())));
        assertEquals(UsageDataUnion.SOURCE_PACKAGE, UsageDataUnion.storedTypeOf(
                new UsageData().setSourcePackage(new SourcePackageUsage())));
        assertEquals(UsageDataUnion.MANUALLY_SET, UsageDataUnion.storedTypeOf(
                new UsageData().setManuallySet(new ManuallySetUsage())));
    }

    @Test
    public void usageDataUnionIsNullSafeAndComparesVariants() {
        assertNull(UsageDataUnion.storedTypeOf(null));
        assertNull(UsageDataUnion.storedTypeOf(new UsageData()));

        UsageData licenseInfo = new UsageData().setLicenseInfo(new LicenseInfoUsage());
        UsageData otherLicenseInfo = new UsageData().setLicenseInfo(new LicenseInfoUsage());
        UsageData manuallySet = new UsageData().setManuallySet(new ManuallySetUsage());

        assertTrue(UsageDataUnion.sameVariant(licenseInfo, otherLicenseInfo));
        assertTrue(!UsageDataUnion.sameVariant(licenseInfo, manuallySet));
        assertTrue("two usages with nothing set hold the same variant",
                UsageDataUnion.sameVariant(new UsageData(), null));
    }

    private static String mapFunction(String constantName) throws Exception {
        Field field = AttachmentUsageRepository.class.getDeclaredField(constantName);
        field.setAccessible(true);
        return (String) field.get(null);
    }
}
