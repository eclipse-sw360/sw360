/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.vmcomponents.common;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatchState;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatchType;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for SVMMapper - specifically for pURL extraction functionality.
 */
public class SVMMapperTest {

    @Test
    public void testUpdateMatchWithPurlFromExternalIds() {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "vmid123")
                .setName("TestComponent")
                .setVersion("1.0.0")
                .setVendor("TestVendor");

        Component relComponent = new Component("TestComponent");
        relComponent.setId("comp123");

        Map<String, String> externalIds = new HashMap<>();
        String expectedPurl = "pkg:maven/org.example/test@1.0.0";
        externalIds.put("package-url", expectedPurl);

        Release release = new Release("TestRelease", "1.0.0", relComponent.getId())
                .setId("release123")
                .setCpeid("cpe:/a:test:release:1.0.0")
                .setExternalIds(externalIds);

        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.CPE);
        VMMatch match = new VMMatch(component.getId(), release.getId(), types, VMMatchState.ACCEPTED);

        SVMMapper.updateMatch(match, component, release, () -> relComponent);

        assertEquals(expectedPurl, match.getReleasePurl());
        assertEquals(release.getId(), match.getReleaseId());
        assertEquals(release.getCpeid(), match.getReleaseCpe());
    }

    @Test
    public void testUpdateMatchWithJsonEncodedPurlList() {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "vmid456");
        component.setId("vmcomp456");

        Component relComponent = new Component("TestComponent");
        relComponent.setId("comp456");

        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("package-url", "[\"pkg:maven/org.example/test@1.0.0\",\"pkg:npm/example@1.0.0\"]");

        Release release = new Release("TestRelease", "1.0.0", relComponent.getId())
                .setId("release456")
                .setExternalIds(externalIds);

        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.NAME_CR);
        VMMatch match = new VMMatch(component.getId(), release.getId(), types, VMMatchState.MATCHING_LEVEL_1);

        SVMMapper.updateMatch(match, component, release, () -> relComponent);

        assertNotNull(match.getReleasePurl());
        assertTrue(match.getReleasePurl().contains("pkg:maven/org.example/test@1.0.0"));
        assertTrue(match.getReleasePurl().contains("pkg:npm/example@1.0.0"));
    }

    @Test
    public void testUpdateMatchWithPurlIdKey() {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "vmid789");
        component.setId("vmcomp789");

        Component relComponent = new Component("TestComponent");
        relComponent.setId("comp789");

        Map<String, String> externalIds = new HashMap<>();
        String expectedPurl = "pkg:pypi/requests@2.28.0";
        externalIds.put("purl.id", expectedPurl);

        Release release = new Release("TestRelease", "2.28.0", relComponent.getId())
                .setId("release789")
                .setExternalIds(externalIds);

        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.NAME_CR);
        VMMatch match = new VMMatch(component.getId(), release.getId(), types, VMMatchState.MATCHING_LEVEL_1);

        SVMMapper.updateMatch(match, component, release, () -> relComponent);

        assertEquals(expectedPurl, match.getReleasePurl());
    }

    @Test
    public void testUpdateMatchWithBothPurlKeys() {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "vmid999");
        component.setId("vmcomp999");

        Component relComponent = new Component("TestComponent");
        relComponent.setId("comp999");

        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("package-url", "pkg:maven/org.example/test@1.0.0");
        externalIds.put("purl.id", "pkg:npm/example@1.0.0");

        Release release = new Release("TestRelease", "1.0.0", relComponent.getId())
                .setId("release999")
                .setExternalIds(externalIds);

        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.NAME_CR);
        VMMatch match = new VMMatch(component.getId(), release.getId(), types, VMMatchState.MATCHING_LEVEL_1);

        SVMMapper.updateMatch(match, component, release, () -> relComponent);

        assertNotNull(match.getReleasePurl());
        assertTrue(match.getReleasePurl().contains("pkg:maven/org.example/test@1.0.0"));
        assertTrue(match.getReleasePurl().contains("pkg:npm/example@1.0.0"));
    }

    @Test
    public void testUpdateMatchWithNullRelease() {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "vmid123")
                .setName("TestComponent")
                .setVersion("1.0.0")
                .setVendor("TestVendor");
        component.setId("vmcomp123");

        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.CPE);
        VMMatch match = new VMMatch(component.getId(), "releaseId", types, VMMatchState.ACCEPTED);

        SVMMapper.updateMatch(match, component, null, () -> null);

        assertEquals(SVMMapper.NOT_FOUND, match.getReleasePurl());
        assertEquals(SVMMapper.NOT_FOUND, match.getReleaseCpe());
        assertEquals(SVMMapper.NOT_FOUND, match.getReleaseVersion());
    }

    @Test
    public void testUpdateMatchWithEmptyExternalIds() {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "vmid123");
        component.setId("vmcomp123");

        Component relComponent = new Component("TestComponent");
        relComponent.setId("comp123");

        Release release = new Release("TestRelease", "1.0.0", relComponent.getId())
                .setId("release123");

        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.NAME_CR);
        VMMatch match = new VMMatch(component.getId(), release.getId(), types, VMMatchState.MATCHING_LEVEL_1);

        SVMMapper.updateMatch(match, component, release, () -> relComponent);

        assertEquals("", match.getReleasePurl());
    }

    @Test
    public void testUpdateMatchWithNullExternalIds() {
        VMComponent component = new VMComponent(SW360Utils.getCreatedOnTime(), "vmid123");
        component.setId("vmcomp123");

        Component relComponent = new Component("TestComponent");
        relComponent.setId("comp123");

        Release release = new Release("TestRelease", "1.0.0", relComponent.getId())
                .setId("release123");

        HashSet<VMMatchType> types = new HashSet<>();
        types.add(VMMatchType.NAME_CR);
        VMMatch match = new VMMatch(component.getId(), release.getId(), types, VMMatchState.MATCHING_LEVEL_1);

        SVMMapper.updateMatch(match, component, release, () -> relComponent);

        assertEquals("", match.getReleasePurl());
    }
}
