/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.projects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.portlet.PortletSession;
import javax.portlet.ResourceRequest;

import java.util.Map;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class ProjectPortletUtilsTest {

    @Mock
    private ResourceRequest request;

    @Mock
    private PortletSession portletSession;

    @Test
    public void testGetExcludedLicensesPerAttachmantIdFromRequest() {
        // checkboxes (note: checked boxes mark licenses NOT to be excluded :-))
        Mockito.when(request.getParameterValues("a1")).thenReturn(new String[] { "1", "2" });
        Mockito.when(request.getParameterValues("a2")).thenReturn(new String[] {});
        Mockito.when(request.getParameterValues("a3")).thenReturn(new String[] { "0", "1", "2", "3", "4" });
        // temporary keys (the key are always completely transmitted, the selection is
        // done by the checkboxes

        // which attachment id has which keys
        Mockito.when(request.getParameterValues("a1_key")).thenReturn(new String[] { "a1l1", "a1l2", "a1l3", "a1l4" });
        Mockito.when(request.getParameterValues("a2_key")).thenReturn(new String[] { "a2l1", "a2l2", "a2l3" });
        Mockito.when(request.getParameterValues("a3_key")).thenReturn(new String[] { "a3l1", "a3l2", "a3l3", "a3l4", "a3l5" });

        // @formatter:off
        // example of the temporary mapping to keys
        Mockito.when(request.getPortletSession()).thenReturn(portletSession);
        Mockito.when(portletSession.getAttribute("license-store-a1")).thenReturn(ImmutableMap.of(
                "a1l1", createLicense("a1l1", "a1l1_t"),
                "a1l2", createLicense("a1l2", "a1l2_t"),
                "a1l3", createLicense("a1l3", "a1l3_t"),
                "a1l4", createLicense("a1l4", "a1l4_t")
        ));
        Mockito.when(portletSession.getAttribute("license-store-a2")).thenReturn(ImmutableMap.of(
                "a2l1", createLicense("a2l1", "a2l1_t"),
                "a2l2", createLicense("a2l2", "a2l2_t"),
                "a2l3", createLicense("a2l3", "a2l3_t")
        ));
        Mockito.when(portletSession.getAttribute("license-store-a3")).thenReturn(ImmutableMap.of(
                "a3l1", createLicense("a3l1", "a3l1_t"),
                "a3l2", createLicense("a3l2", "a3l2_t"),
                "a3l3", createLicense("a3l3", "a3l3_t"),
                "a3l4", createLicense("a3l4", "a3l4_t"),
                "a3l5", createLicense("a3l5", "a3l5_t")
        ));
        // @formatter:on

        Map<String, Set<LicenseNameWithText>> excludedLicenses = ProjectPortletUtils
                .getExcludedLicensesPerAttachmentIdFromRequest(ImmutableSet.of("a1", "a2", "a3"), request);

        // Every license not checked is excluded now
        Assert.assertThat(excludedLicenses.keySet(), Matchers.containsInAnyOrder("a1", "a2", "a3"));
        Assert.assertThat(excludedLicenses.get("a1"),
                Matchers.containsInAnyOrder(createLicense("a1l1", "a1l1_t"), createLicense("a1l4", "a1l4_t")));
        Assert.assertThat(excludedLicenses.get("a2"), Matchers.containsInAnyOrder(createLicense("a2l1", "a2l1_t"),
                createLicense("a2l2", "a2l2_t"), createLicense("a2l3", "a2l3_t")));
        Assert.assertTrue(excludedLicenses.get("a3").isEmpty());
    }

    private LicenseNameWithText createLicense(String name, String text) {
        LicenseNameWithText licenseNameWithText = new LicenseNameWithText();
        licenseNameWithText.setLicenseName(name);
        licenseNameWithText.setLicenseText(text);
        return licenseNameWithText;
    }
}
