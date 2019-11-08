/*
 * Copyright Siemens AG, 2015-2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.projects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.portal.common.ThriftJsonSerializer;

import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.portlet.PortletException;
import javax.portlet.PortletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.sw360.portal.common.PortalConstants.LICENSE_INFO_ATTACHMENT_USAGES;
import static org.eclipse.sw360.portal.common.PortalConstants.MANUAL_ATTACHMENT_USAGES;
import static org.eclipse.sw360.portal.common.PortalConstants.SOURCE_CODE_ATTACHMENT_USAGES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author daniele.fognini@tngtech.com
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectPortletTest {
    @Mock
    PortletRequest request;

    @Mock
    ThriftClients thriftClients;

    @Mock
    AttachmentService.Iface attachmentClient;

    private ProjectPortlet portlet;

    @Before
    public void setUp() {
        when(thriftClients.makeAttachmentClient()).thenReturn(attachmentClient);
        portlet = new ProjectPortlet(thriftClients);
    }

    @Test
    public void testJsonOfClearing() throws Exception {
        ReleaseClearingStateSummary releaseClearingStateSummary = new ReleaseClearingStateSummary().setNewRelease(1)
                .setSentToClearingTool(17).setUnderClearing(6).setReportAvailable(5).setApproved(4);

        ThriftJsonSerializer thriftJsonSerializer = new ThriftJsonSerializer();
        String json = thriftJsonSerializer.toJson(releaseClearingStateSummary);

        // assertThat(json, containsString(
        // "{\"newRelease\":1,\"underClearing\":6,\"sentToClearingTool\":17,\"reportAvailable\":5,\"approved\":4}"));

        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        assertThat(map, hasEntry("newRelease", (int) 1));
        assertThat(map, hasEntry("sentToClearingTool", (int) 17));
        assertThat(map, hasEntry("underClearing", (int) 6));
        assertThat(map, hasEntry("reportAvailable", (int) 5));
        assertThat(map, hasEntry("approved", (int) 4));
    }

    @Test
    public void testPutAttachmentUsagesToRequest() throws PortletException, TException {
        AttachmentUsage licInfoUsage1 = new AttachmentUsage(Source.releaseId("r1"), "att1", Source.projectId("p1"))
                .setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(ImmutableSet.of("lic1"))));
        AttachmentUsage licInfoUsage2 = new AttachmentUsage(Source.releaseId("r1"), "att1", Source.projectId("p1"))
                .setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(ImmutableSet.of("lic2"))));
        AttachmentUsage srcPackageUsage1 = new AttachmentUsage(Source.releaseId("r2"), "att2", Source.projectId("p1"))
                .setUsageData(UsageData.sourcePackage(new SourcePackageUsage()));
        AttachmentUsage srcPackageUsage2 = new AttachmentUsage(Source.releaseId("r2"), "att2", Source.projectId("p1"))
                .setUsageData(UsageData.sourcePackage(new SourcePackageUsage()));
        AttachmentUsage manualUsage1 = new AttachmentUsage(Source.releaseId("r3"), "att3", Source.projectId("p1"))
                .setUsageData(UsageData.manuallySet(new ManuallySetUsage()));
        when(attachmentClient.getUsedAttachments(Source.projectId("p1"), null)).thenReturn(
                ImmutableList.of(licInfoUsage1, licInfoUsage2, srcPackageUsage1, srcPackageUsage2, manualUsage1)
        );

        AttachmentUsage mergedLicInfoUsage = new AttachmentUsage(Source.releaseId("r1"), "att1", Source.projectId("p1"))
                .setUsageData(UsageData.licenseInfo(new LicenseInfoUsage(ImmutableSet.of("lic1", "lic2"))));
        Map<String, AttachmentUsage> licInfoUsages = ImmutableMap.of("att1", mergedLicInfoUsage);
        Map<String, AttachmentUsage> srcCodeUsages = ImmutableMap.of("att2", srcPackageUsage1);
        Map<String, AttachmentUsage> manualUsages = ImmutableMap.of("att3", manualUsage1);

        Map<String, Map<String, AttachmentUsage>> maps = new HashMap<>();
        Mockito.doAnswer(invocation -> {
            maps.put((String) invocation.getArguments()[0], (Map<String, AttachmentUsage>) invocation.getArguments()[1]);
            return null;
        }).when(request).setAttribute(anyString(), any());

        portlet.putAttachmentUsagesInRequest(request, "p1");

        assertThat(maps, equalTo(ImmutableMap.of(LICENSE_INFO_ATTACHMENT_USAGES, licInfoUsages,
                SOURCE_CODE_ATTACHMENT_USAGES, srcCodeUsages,
                MANUAL_ATTACHMENT_USAGES, manualUsages)));
    }
}