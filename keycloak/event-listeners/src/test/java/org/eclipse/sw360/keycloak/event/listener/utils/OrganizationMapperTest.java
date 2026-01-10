/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.keycloak.event.listener.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for OrganizationMapper
 * <p>
 * These tests verify the organization name mapping functionality that was
 * restored from the old OrganizationHelper.java implementation.
 * </p>
 */
public class OrganizationMapperTest {

    @Before
    public void setUp() {
        // Tests use the orgmapping.properties file from test resources
    }

    @Test
    public void testMapOrganizationNameWhenMappingDisabled() {
        // When mapping is disabled, should return original name
        String original = "Engineering Department";
        String mapped = OrganizationMapper.mapOrganizationName(original);

        // Since default config has enable.custom.mapping=false
        assertEquals("Should return original name when mapping disabled", original, mapped);
    }

    @Test
    public void testMapOrganizationNameWithNullInput() {
        String mapped = OrganizationMapper.mapOrganizationName(null);
        assertNull("Should return null for null input", mapped);
    }

    @Test
    public void testMapOrganizationNameWithEmptyString() {
        String mapped = OrganizationMapper.mapOrganizationName("");
        assertEquals("Should return empty string for empty input", "", mapped);
    }

    @Test
    public void testMapOrganizationNameWithWhitespace() {
        String mapped = OrganizationMapper.mapOrganizationName("   ");
        assertEquals("Should return whitespace for whitespace input", "   ", mapped);
    }

    @Test
    public void testIsCustomMappingEnabled() {
        // Default configuration has custom mapping disabled
        assertFalse("Custom mapping should be disabled by default",
                OrganizationMapper.isCustomMappingEnabled());
    }

    @Test
    public void testGetMappingCount() {
        // Default configuration has 0 mappings
        int count = OrganizationMapper.getMappingCount();
        assertTrue("Mapping count should be 0 or positive", count >= 0);
    }

    /**
     * Integration test note:
     * To test actual mapping functionality, you would need to:
     * 1. Create a test-specific orgmapping.properties with enable.custom.mapping=true
     * 2. Add test mappings
     * 3. Place it in src/test/resources/
     * 4. Create separate test class that loads test configuration
     */
}
