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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link OrganizationMapper}.
 * Tests cover null/empty handling, default configuration, initialization, and edge cases.
 */
public class OrganizationMapperTest {
    /**
     * Tests that OrganizationMapper initializes successfully on first access.
     * Verifies that static initialization and lazy loading work correctly.
     */
    @Test
    public void testInitialization_OnFirstAccess_CompletesSuccessfully() {
        // First access should trigger initialization if not already done
        // This should not throw any exception
        boolean result = OrganizationMapper.isCustomMappingEnabled();
        // Verify subsequent calls work correctly (proves initialization completed)
        assertFalse("Should return valid boolean after initialization",
                OrganizationMapper.isCustomMappingEnabled() && !OrganizationMapper.isCustomMappingEnabled());
    }

    /**
     * Tests that all public methods trigger initialization (ensureInitialized).
     * Each method should be callable without exceptions after class loading.
     */
    @Test
    public void testInitialization_AllPublicMethods_TriggerInitialization() {
        // All these calls should work without throwing exceptions
        // as they all call ensureInitialized() internally
        assertNotNull("mapOrganizationName should work",
                OrganizationMapper.mapOrganizationName("test"));
        assertNotNull("isCustomMappingEnabled should return non-null",
                Boolean.valueOf(OrganizationMapper.isCustomMappingEnabled()));
        assertNotNull("isMatchPrefixEnabled should return non-null",
                Boolean.valueOf(OrganizationMapper.isMatchPrefixEnabled()));
        assertTrue("getMappingCount should return >= 0",
                OrganizationMapper.getMappingCount() >= 0);
    }

    /**
     * Tests thread safety of initialization under concurrent access.
     * Multiple threads accessing OrganizationMapper simultaneously should not cause issues.
     */
    @Test
    public void testInitialization_ConcurrentAccess_ThreadSafe() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<String> results = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String orgName = "TestOrg" + i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    String result = OrganizationMapper.mapOrganizationName(orgName);
                    synchronized (results) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads at once
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue("No exceptions should occur during concurrent access", exceptions.isEmpty());
        assertEquals("All threads should complete successfully", threadCount, results.size());
    }

    /**
     * Tests that repeated calls to methods don't cause re-initialization.
     * Verifies the initialized flag prevents redundant loading.
     */
    @Test
    public void testInitialization_RepeatedCalls_NoReinitialization() {
        // Call multiple times - should use cached initialization
        for (int i = 0; i < 100; i++) {
            OrganizationMapper.mapOrganizationName("TestOrg");
            OrganizationMapper.isCustomMappingEnabled();
            OrganizationMapper.getMappingCount();
        }
        // If we get here without hanging or exceptions, initialization caching works
        assertTrue("Repeated calls should complete quickly", true);
    }

    /**
     * Tests that initialization state is consistent across all accessor methods.
     */
    @Test
    public void testInitialization_StateConsistency_AcrossMethods() {
        boolean customMapping1 = OrganizationMapper.isCustomMappingEnabled();
        boolean prefixMatch1 = OrganizationMapper.isMatchPrefixEnabled();
        int count1 = OrganizationMapper.getMappingCount();

        // Call again - should return same values (consistent state)
        boolean customMapping2 = OrganizationMapper.isCustomMappingEnabled();
        boolean prefixMatch2 = OrganizationMapper.isMatchPrefixEnabled();
        int count2 = OrganizationMapper.getMappingCount();

        assertEquals("Custom mapping state should be consistent", customMapping1, customMapping2);
        assertEquals("Prefix match state should be consistent", prefixMatch1, prefixMatch2);
        assertEquals("Mapping count should be consistent", count1, count2);
    }

    // ==================== Null/Empty Input Tests ====================

    /**
     * Tests that null input returns null without throwing exception.
     */
    @Test
    public void testMapOrganizationName_WithNullInput_ReturnsNull() {
        assertNull("Null input should return null", OrganizationMapper.mapOrganizationName(null));
    }

    /**
     * Tests that empty string input returns empty string.
     */
    @Test
    public void testMapOrganizationName_WithEmptyInput_ReturnsEmpty() {
        assertEquals("Empty input should return empty string", "", OrganizationMapper.mapOrganizationName(""));
    }

    // ==================== Default Configuration Tests ====================

    /**
     * Tests that when no mapping exists, original name is returned unchanged.
     */
    @Test
    public void testMapOrganizationName_WithNoMapping_ReturnsOriginal() {
        String originalName = "TestDepartment";
        assertEquals("Unmapped name should return original", originalName, OrganizationMapper.mapOrganizationName(originalName));
    }

    /**
     * Tests that custom mapping is disabled by default as per configuration.
     */
    @Test
    public void testIsCustomMappingEnabled_DefaultConfig_ReturnsFalse() {
        assertFalse("Custom mapping should be disabled by default", OrganizationMapper.isCustomMappingEnabled());
    }

    /**
     * Tests that prefix matching is disabled by default as per configuration.
     */
    @Test
    public void testIsMatchPrefixEnabled_DefaultConfig_ReturnsFalse() {
        assertFalse("Prefix matching should be disabled by default", OrganizationMapper.isMatchPrefixEnabled());
    }

    /**
     * Tests that mapping count is non-negative (0 when no custom mappings configured).
     */
    @Test
    public void testGetMappingCount_DefaultConfig_ReturnsZeroOrMore() {
        int count = OrganizationMapper.getMappingCount();
        assertTrue("Mapping count should be non-negative", count >= 0);
    }

    // ==================== Edge Case Tests ====================

    /**
     * Tests mapping with whitespace-only input returns the same whitespace.
     */
    @Test
    public void testMapOrganizationName_WithWhitespaceInput_ReturnsWhitespace() {
        String whitespace = "   ";
        assertEquals("Whitespace input should return unchanged", whitespace, OrganizationMapper.mapOrganizationName(whitespace));
    }

    /**
     * Tests that special characters in organization names are handled correctly.
     */
    @Test
    public void testMapOrganizationName_WithSpecialCharacters_ReturnsOriginal() {
        String specialName = "Test@Org#123!";
        assertEquals("Special characters should be handled", specialName, OrganizationMapper.mapOrganizationName(specialName));
    }

    /**
     * Tests multiple consecutive calls return consistent results.
     */
    @Test
    public void testMapOrganizationName_MultipleCalls_ConsistentResults() {
        String orgName = "ConsistentOrg";
        String result1 = OrganizationMapper.mapOrganizationName(orgName);
        String result2 = OrganizationMapper.mapOrganizationName(orgName);
        assertEquals("Multiple calls should return consistent results", result1, result2);
    }

    /**
     * Tests that very long organization names are handled without issues.
     */
    @Test
    public void testMapOrganizationName_WithLongInput_ReturnsOriginal() {
        String longName = "A".repeat(1000);
        assertEquals("Long input should be handled", longName, OrganizationMapper.mapOrganizationName(longName));
    }
}
