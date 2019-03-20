/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.db;

import com.google.common.collect.Sets;

import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.*;

import org.junit.Before;
import org.junit.Test;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static org.eclipse.sw360.datahandler.TestUtils.assertTestString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AttachmentDatabaseHandlerTest {

    private static final String dbName = DatabaseSettings.COUCH_DB_DATABASE;
    private static final String attachmentsDbName = DatabaseSettings.COUCH_DB_ATTACHMENTS;

    private AttachmentDatabaseHandler uut;

    @Before
    public void setup() throws MalformedURLException {
        assertTestString(dbName);
        assertTestString(attachmentsDbName);

        // currently we are only testing methods that do not access the database so we
        // only need the parameters to call the constructor
        // when this changes, the database has to be created before and deleted
        // afterwards - see e.g. ProjectDatabaseHandlerTest
        uut = new AttachmentDatabaseHandler(DatabaseSettings.getConfiguredHttpClient(), dbName, attachmentsDbName);
    }

    @Test
    public void testDistinctAttachmentUsagesSimple() {
        // given:
        List<AttachmentUsage> attachmentUsagesIn = new ArrayList<>();

        AttachmentUsage au1 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua1 = new UsageData();
        LicenseInfoUsage liu1 = new LicenseInfoUsage(Sets.newHashSet("e1", "e2"));
        liu1.setProjectPath("p1:p2");
        ua1.setLicenseInfo(liu1);
        au1.setUsageData(ua1);
        attachmentUsagesIn.add(au1);

        // when:
        List<AttachmentUsage> actual = uut.distinctAttachmentUsages(attachmentUsagesIn);

        // then:
        assertTrue(actual.size() == 1);
    }

    @Test
    public void testDistinctAttachmentUsagesLicenseInfos() {
        // given:
        List<AttachmentUsage> attachmentUsagesIn = new ArrayList<>();

        AttachmentUsage au1 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua1 = new UsageData();
        LicenseInfoUsage liu1 = new LicenseInfoUsage(Sets.newHashSet("el1", "el2"));
        liu1.setProjectPath("p1:p2");
        ua1.setLicenseInfo(liu1);
        au1.setUsageData(ua1);
        attachmentUsagesIn.add(au1);

        // same attachment of same release in same project root, but with in different
        // subproject path => should not be a duplicate
        AttachmentUsage au2 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua2 = new UsageData();
        LicenseInfoUsage liu2 = new LicenseInfoUsage(Sets.newHashSet("el3"));
        liu2.setProjectPath("p1:p2:p3");
        ua2.setLicenseInfo(liu2);
        au2.setUsageData(ua2);
        attachmentUsagesIn.add(au2);

        // same attachment of different release in same project root with different
        // subproject path => should not be a duplicate
        AttachmentUsage au3 = new AttachmentUsage(Source.releaseId("r2"), "aci1", Source.projectId("p1"));
        UsageData ua3 = new UsageData();
        LicenseInfoUsage liu3 = new LicenseInfoUsage(Sets.newHashSet("el4"));
        liu3.setProjectPath("p1:p2");
        ua3.setLicenseInfo(liu3);
        au3.setUsageData(ua3);
        attachmentUsagesIn.add(au3);

        // same attachment of same release in same project root with same
        // subproject path => should be a duplicate (and can only happen with old data
        // where a release could have the same attachment twice)
        AttachmentUsage au4 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua4 = new UsageData();
        LicenseInfoUsage liu4 = new LicenseInfoUsage(Sets.newHashSet("el5"));
        liu4.setProjectPath("p1:p2");
        ua4.setLicenseInfo(liu4);
        au4.setUsageData(ua4);
        attachmentUsagesIn.add(au4);

        // when:
        List<AttachmentUsage> actual = uut.distinctAttachmentUsages(attachmentUsagesIn);

        // then:
        assertTrue(attachmentUsagesIn.size() == 4);
        assertTrue(actual.size() == 3);
        assertEquals(attachmentUsagesIn.get(0), actual.get(0));
        assertEquals(attachmentUsagesIn.get(1), actual.get(1));
        assertEquals(attachmentUsagesIn.get(2), actual.get(2));
    }

    @Test
    public void testDistinctAttachmentUsagesSourcePackage() {
        // given:
        List<AttachmentUsage> attachmentUsagesIn = new ArrayList<>();

        AttachmentUsage au1 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua1 = new UsageData();
        SourcePackageUsage spu1 = new SourcePackageUsage();
        spu1.setDummy("d1");
        ua1.setSourcePackage(spu1);
        au1.setUsageData(ua1);
        attachmentUsagesIn.add(au1);

        // same attachment of same release in same project root, but with different
        // dummy => should be a duplicate
        AttachmentUsage au2 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua2 = new UsageData();
        SourcePackageUsage spu2 = new SourcePackageUsage();
        spu2.setDummy("d2");
        ua2.setSourcePackage(spu2);
        au2.setUsageData(ua2);
        attachmentUsagesIn.add(au2);

        // same attachment of different release in same project root => should not be a
        // duplicate
        AttachmentUsage au3 = new AttachmentUsage(Source.releaseId("r2"), "aci1", Source.projectId("p1"));
        UsageData ua3 = new UsageData();
        SourcePackageUsage spu3 = new SourcePackageUsage();
        spu3.setDummy("d1");
        ua3.setSourcePackage(spu3);
        au3.setUsageData(ua3);
        attachmentUsagesIn.add(au3);

        // when:
        List<AttachmentUsage> actual = uut.distinctAttachmentUsages(attachmentUsagesIn);

        // then:
        assertTrue(attachmentUsagesIn.size() == 3);
        assertTrue(actual.size() == 2);
        assertEquals(attachmentUsagesIn.get(0), actual.get(0));
        assertEquals(attachmentUsagesIn.get(2), actual.get(1));
    }

    @Test
    public void testDistinctAttachmentUsagesMixed() {
        // given:
        List<AttachmentUsage> attachmentUsagesIn = new ArrayList<>();

        AttachmentUsage au1 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua1 = new UsageData();
        LicenseInfoUsage liu1 = new LicenseInfoUsage(Sets.newHashSet("el1", "el2"));
        liu1.setProjectPath("p1:p2");
        ua1.setLicenseInfo(liu1);
        au1.setUsageData(ua1);
        attachmentUsagesIn.add(au1);

        // same attachment of same release in same project root with same
        // subproject path => should be a duplicate (and can only happen with old data
        // where a release could have the same attachment twice)
        AttachmentUsage au2 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua2 = new UsageData();
        LicenseInfoUsage liu2 = new LicenseInfoUsage(Sets.newHashSet("el5"));
        liu2.setProjectPath("p1:p2");
        ua2.setLicenseInfo(liu2);
        au2.setUsageData(ua2);
        attachmentUsagesIn.add(au2);

        // same attachment of same release in same project root, but with different
        // usage type => should not be a duplicate
        AttachmentUsage au3 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua3 = new UsageData();
        SourcePackageUsage spu1 = new SourcePackageUsage();
        spu1.setDummy("d1");
        ua3.setSourcePackage(spu1);
        au3.setUsageData(ua3);
        attachmentUsagesIn.add(au3);

        // same attachment of same release in same project root, but with different
        // usage type => should be a duplicate
        AttachmentUsage au4 = new AttachmentUsage(Source.releaseId("r1"), "aci1", Source.projectId("p1"));
        UsageData ua4 = new UsageData();
        SourcePackageUsage spu2 = new SourcePackageUsage();
        spu2.setDummy("d2");
        ua4.setSourcePackage(spu2);
        au4.setUsageData(ua4);
        attachmentUsagesIn.add(au4);

        // when:
        List<AttachmentUsage> actual = uut.distinctAttachmentUsages(attachmentUsagesIn);

        // then:
        assertTrue(attachmentUsagesIn.size() == 4);
        assertTrue(actual.size() == 2);
        assertEquals(attachmentUsagesIn.get(0), actual.get(0));
        assertEquals(attachmentUsagesIn.get(2), actual.get(1));
    }

}
