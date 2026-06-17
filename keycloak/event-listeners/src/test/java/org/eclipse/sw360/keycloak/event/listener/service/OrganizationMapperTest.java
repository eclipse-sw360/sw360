/*
 * Copyright Siemens AG, 2024-2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.keycloak.event.listener.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class OrganizationMapperTest {

    @Test
    public void testMapOrganizationName_ExactMappings() {
        assertEquals("MAPPED_DEPT_A", OrganizationMapper.mapOrganizationName("ORG UNIT DEPT TEAM SUBTEAM"));
        assertEquals("MAPPED_DEPT_B", OrganizationMapper.mapOrganizationName("ORG UNIT DEPT TEAM"));
        assertEquals("EXTERNAL", OrganizationMapper.mapOrganizationName("EXTERNAL_COMPANY"));
        assertEquals("ENG", OrganizationMapper.mapOrganizationName("Engineering"));
        assertEquals("UPPER_MAPPED", OrganizationMapper.mapOrganizationName("UPPERCASE"));
    }

    @Test
    public void testMapOrganizationName_PrefixMatching() {
        assertEquals("PREFIX_MAPPED", OrganizationMapper.mapOrganizationName("PREFIX_EXTRA_STUFF"));
        assertEquals("MAPPED_DEPT_A", OrganizationMapper.mapOrganizationName("ORG UNIT DEPT TEAM SUBTEAM"));
    }

    @Test
    public void testMapOrganizationName_NoMapping_ReturnsOriginal() {
        assertEquals("UnmappedDepartment", OrganizationMapper.mapOrganizationName("UnmappedDepartment"));
        assertEquals("Test@Org#123!", OrganizationMapper.mapOrganizationName("Test@Org#123!"));
        String longName = "A".repeat(1000);
        assertEquals(longName, OrganizationMapper.mapOrganizationName(longName));
    }

    @Test
    public void testMapOrganizationName_NullAndEmptyInput() {
        assertNull(OrganizationMapper.mapOrganizationName(null));
        assertEquals("", OrganizationMapper.mapOrganizationName(""));
        assertEquals("   ", OrganizationMapper.mapOrganizationName("   "));
    }

    @Test
    public void testMapOrganizationName_ConsistentResults() {
        String result1 = OrganizationMapper.mapOrganizationName("Engineering");
        String result2 = OrganizationMapper.mapOrganizationName("Engineering");
        assertEquals(result1, result2);
        assertEquals("ENG", result1);
    }
}
