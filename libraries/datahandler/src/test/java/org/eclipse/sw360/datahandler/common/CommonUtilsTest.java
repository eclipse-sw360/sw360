/*
 * Copyright Siemens AG, 2015-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.apache.logging.log4j.LogManager;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.*;

/**
 * @author daniele.fognini@tngtech.com
 */
public class CommonUtilsTest {

    @Test
    public void testIsUrl() throws Exception {
        Assert.assertTrue(isValidUrl("http://www.google.com"));
    }

    @Test
    public void testIsUrl1() throws Exception {
        Assert.assertFalse(isValidUrl("www.google.com"));
    }

    @Test
    public void testIsUrl2() throws Exception {
        Assert.assertTrue(isValidUrl("ftp://www.google.com"));
    }

    @Test
    public void testIsUrl3() throws Exception {
        Assert.assertFalse(isValidUrl("httpwww://www.google.com"));
    }

    @Test
    public void testIsUrl4() throws Exception {
        Assert.assertFalse(isValidUrl("http://"));
    }

    @Test
    public void testIsUrl5() throws Exception {
        Assert.assertFalse(isValidUrl(null));
    }

    @Test
    public void testIsUrl6() throws Exception {
        Assert.assertFalse(isValidUrl(""));
    }

    @Test
    public void testNameOfUrl() throws Exception {
        Assert.assertTrue(getTargetNameOfUrl("http://www.google.com").isEmpty());
    }

    @Test
    public void testNameOfUrl1() throws Exception {
        Assert.assertTrue(getTargetNameOfUrl("www.google.com").isEmpty());
    }

    @Test
    public void testNameOfUrl2() throws Exception {
        Assert.assertTrue(getTargetNameOfUrl("ftp://www.google.com").isEmpty());
    }

    @Test
    public void testNameOfUrl3() throws Exception {
        Assert.assertTrue(getTargetNameOfUrl("httpwww://www.google.com").isEmpty());
    }

    @Test
    public void testNameOfUrl4() throws Exception {
        Assert.assertEquals("file", getTargetNameOfUrl("http://example.com/file"));
    }

    @Test
    public void testNameOfUrl5() throws Exception {
        Assert.assertTrue(getTargetNameOfUrl("http://www.google.com?file=12").isEmpty());
    }

    @Test
    public void testNameOfUrl6() throws Exception {
        Assert.assertEquals("file.xe", getTargetNameOfUrl("ftp://www.google.com/dir/file.xe"));
    }

    @Test
    public void testNameOfUrl7() throws Exception {
        Assert.assertEquals("file.ext", getTargetNameOfUrl("http://www.google.com/dir/file.ext?cookie=14&cr=345"));
    }

    @Test
    public void testFormatTime0() throws Exception {
        Assert.assertEquals("00:00:00", formatTime(0));
    }

    @Test
    public void testFormatTimeSecond() throws Exception {
        Assert.assertEquals("00:00:01", formatTime(1));
    }

    @Test
    public void testFormatTimeMinute() throws Exception {
        Assert.assertEquals("00:01:00", formatTime(60));
    }

    @Test
    public void testFormatTimeHour() throws Exception {
        Assert.assertEquals("01:00:00", formatTime(3600));
    }

    @Test
    public void testFormatTime24Hours() throws Exception {
        Assert.assertEquals("24:00:00", formatTime(86400));
    }

    @Test
    public void testIsMapFieldMapOfStringSets_EmptyMaps() throws Exception {
        Map<String, Set<String>> roleMap = new HashMap<>();
        Project project = new Project().setName("pname").setRoles(roleMap);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project, project, LogManager.getLogger(CommonUtilsTest.class));
        Assert.assertFalse(b);
    }
    @Test
    public void testIsMapFieldMapOfStringSets_EmptySets() throws Exception {
        Map<String, Set<String>> roleMap = new HashMap<>();
        roleMap.put("role1", new HashSet<>());
        roleMap.put("role2", new HashSet<>());
        Project project = new Project().setName("pname").setRoles(roleMap);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project, project, LogManager.getLogger(CommonUtilsTest.class));
        Assert.assertFalse(b);
    }
    @Test
    public void testIsMapFieldMapOfStringSets_NoSet() throws Exception {
        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("ext1", "id1");
        externalIds.put("ext2", "id2");
        Project project = new Project().setName("pname").setExternalIds(externalIds);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.EXTERNAL_IDS, project, project, project, LogManager.getLogger(CommonUtilsTest.class));
        Assert.assertFalse(b);
    }
    @Test
    public void testIsMapFieldMapOfStringSets_StringSets() throws Exception {
        Map<String, Set<String>> roleMap = new HashMap<>();
        roleMap.put("expert", toSingletonSet("expert@company.com"));
        Project project = new Project().setName("pname").setRoles(roleMap);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project, project, LogManager.getLogger(CommonUtilsTest.class));
        Assert.assertTrue(b);
    }
    @Test
    public void testIsMapFieldMapOfStringSets_SingleStringSets() throws Exception {
        Map<String, Set<String>> roleMap = new HashMap<>();
        Project project = new Project().setName("pname").setRoles(roleMap);
        Map<String, Set<String>> roleMap2 = new HashMap<>();
        roleMap.put("expert", new HashSet<>());
        Project project2 = new Project().setName("pname").setRoles(roleMap2);
        Map<String, Set<String>> roleMap3 = new HashMap<>();
        roleMap.put("expert", toSingletonSet("expert@company.com"));
        Project project3 = new Project().setName("pname").setRoles(roleMap3);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project2, project3, LogManager.getLogger(CommonUtilsTest.class));
        Assert.assertTrue(b);
    }

    @Test
    public void testAllHaveSameLengthWithNoParameter() {
        Assert.assertTrue(allHaveSameLength());
    }

    @Test
    public void testAllHaveSameLengthWithOneParameter() {
        int[] array = { 1, 2, 3 };
        Assert.assertTrue(allHaveSameLength(array));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfSameLength() {
        int[] iArray = { 1, 2, 3 };
        String[] sArray = { "foo", "bar", "42" };
        boolean[] bArray = { true, false, false };

        Assert.assertTrue(allHaveSameLength(iArray, sArray, bArray));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfDifferentLength1() {
        int[] iArray = { 1, 2, 3 };
        String[] sArray = { "foo", "bar" };
        boolean[] bArray = { true, false, false };

        Assert.assertFalse(allHaveSameLength(iArray, sArray, bArray));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfDifferentLength2() {
        int[] iArray = { 1, 2, 3 };
        String[] sArray = { "foo", "bar", "42" };
        boolean[] bArray = { true, false };

        Assert.assertFalse(allHaveSameLength(iArray, sArray, bArray));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfDifferentLength3() {
        int[] iArray = { 1, 2 };
        String[] sArray = { "foo", "bar", "42" };
        boolean[] bArray = { true, false, false };

        Assert.assertFalse(allHaveSameLength(iArray, sArray, bArray));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllHaveSameLengthWithOtherObjects1() {
        int i = 1;

        Assert.assertFalse(allHaveSameLength(i));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllHaveSameLengthWithOtherObjects2() {
        int i = 1;
        String s = "s";

        Assert.assertFalse(allHaveSameLength(i, s));
    }

    @Test
    public void testGetIntOrDefault() throws Exception {
        Assert.assertEquals(25, getIntOrDefault("25", 6));
        Assert.assertEquals(-25, getIntOrDefault("-25", 25));
        Assert.assertEquals(6, getIntOrDefault("25z", 6));
        Assert.assertEquals(42, getIntOrDefault(null, 42));
    }

    @Test
    public void testGetFileNameExtension() {
        Assert.assertEquals("doc", getExtensionFromFileName("example.doc"));
        Assert.assertEquals("TAR.gz", getExtensionFromFileName("source.TAR.gz"));
        Assert.assertEquals("jpg", getExtensionFromFileName("testfile.test.V1 12.jpg"));
        Assert.assertEquals("", getExtensionFromFileName("testfile-without-extension"));
        Assert.assertNull(getExtensionFromFileName(null));
    }
}
