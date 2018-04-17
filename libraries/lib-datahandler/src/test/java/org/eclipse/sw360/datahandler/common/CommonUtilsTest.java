/*
 * Copyright Siemens AG, 2015-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.datahandler.common.CommonUtils.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author daniele.fognini@tngtech.com
 */
public class CommonUtilsTest {

    @Test
    public void testIsUrl() throws Exception {
        assertThat(isValidUrl("http://www.google.com"), is(true));
    }

    @Test
    public void testIsUrl1() throws Exception {
        assertThat(isValidUrl("www.google.com"), is(false));
    }

    @Test
    public void testIsUrl2() throws Exception {
        assertThat(isValidUrl("ftp://www.google.com"), is(true));
    }

    @Test
    public void testIsUrl3() throws Exception {
        assertThat(isValidUrl("httpwww://www.google.com"), is(false));
    }

    @Test
    public void testIsUrl4() throws Exception {
        assertThat(isValidUrl("http://"), is(false));
    }

    @Test
    public void testIsUrl5() throws Exception {
        assertThat(isValidUrl(null), is(false));
    }

    @Test
    public void testIsUrl6() throws Exception {
        assertThat(isValidUrl(""), is(false));
    }

    @Test
    public void testNameOfUrl() throws Exception {
        assertThat(getTargetNameOfUrl("http://www.google.com"), is(""));
    }

    @Test
    public void testNameOfUrl1() throws Exception {
        assertThat(getTargetNameOfUrl("www.google.com"), is(""));
    }

    @Test
    public void testNameOfUrl2() throws Exception {
        assertThat(getTargetNameOfUrl("ftp://www.google.com"), is(""));
    }

    @Test
    public void testNameOfUrl3() throws Exception {
        assertThat(getTargetNameOfUrl("httpwww://www.google.com"), is(""));
    }

    @Test
    public void testNameOfUrl4() throws Exception {
        assertThat(getTargetNameOfUrl("http://example.com/file"), is("file"));
    }

    @Test
    public void testNameOfUrl5() throws Exception {
        assertThat(getTargetNameOfUrl("http://www.google.com?file=12"), is(""));
    }

    @Test
    public void testNameOfUrl6() throws Exception {
        assertThat(getTargetNameOfUrl("ftp://www.google.com/dir/file.xe"), is("file.xe"));
    }

    @Test
    public void testNameOfUrl7() throws Exception {
        assertThat(getTargetNameOfUrl("http://www.google.com/dir/file.ext?cookie=14&cr=345"), is("file.ext"));
    }

    @Test
    public void testFormatTime0() throws Exception {
        assertThat(formatTime(0), is("00:00:00"));
    }

    @Test
    public void testFormatTimeSecond() throws Exception {
        assertThat(formatTime(1), is("00:00:01"));
    }

    @Test
    public void testFormatTimeMinute() throws Exception {
        assertThat(formatTime(60), is("00:01:00"));
    }

    @Test
    public void testFormatTimeHour() throws Exception {
        assertThat(formatTime(3600), is("01:00:00"));
    }

    @Test
    public void testFormatTime24Hours() throws Exception {
        assertThat(formatTime(86400), is("24:00:00"));
    }

    @Test
    public void testIsMapFieldMapOfStringSets_EmptyMaps() throws Exception {
        Map<String, Set<String>> roleMap = new HashMap<>();
        Project project = new Project().setName("pname").setRoles(roleMap);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project, project, Logger.getLogger(CommonUtilsTest.class));
        assertThat(b, is(false));
    }
    @Test
    public void testIsMapFieldMapOfStringSets_EmptySets() throws Exception {
        Map<String, Set<String>> roleMap = new HashMap<>();
        roleMap.put("role1", new HashSet<>());
        roleMap.put("role2", new HashSet<>());
        Project project = new Project().setName("pname").setRoles(roleMap);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project, project, Logger.getLogger(CommonUtilsTest.class));
        assertThat(b, is(false));
    }
    @Test
    public void testIsMapFieldMapOfStringSets_NoSet() throws Exception {
        Map<String, String> externalIds = new HashMap<>();
        externalIds.put("ext1", "id1");
        externalIds.put("ext2", "id2");
        Project project = new Project().setName("pname").setExternalIds(externalIds);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.EXTERNAL_IDS, project, project, project, Logger.getLogger(CommonUtilsTest.class));
        assertThat(b, is(false));
    }
    @Test
    public void testIsMapFieldMapOfStringSets_StringSets() throws Exception {
        Map<String, Set<String>> roleMap = new HashMap<>();
        roleMap.put("expert", toSingletonSet("expert@company.com"));
        Project project = new Project().setName("pname").setRoles(roleMap);
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project, project, Logger.getLogger(CommonUtilsTest.class));
        assertThat(b, is(true));
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
        boolean b = isMapFieldMapOfStringSets(Project._Fields.ROLES, project, project2, project3, Logger.getLogger(CommonUtilsTest.class));
        assertThat(b, is(true));
    }

    @Test
    public void testAllHaveSameLengthWithNoParameter() {
        assertThat(allHaveSameLength(), is(true));
    }

    @Test
    public void testAllHaveSameLengthWithOneParameter() {
        int array[] = { 1, 2, 3 };
        assertThat(allHaveSameLength(array), is(true));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfSameLength() {
        int iArray[] = { 1, 2, 3 };
        String sArray[] = { "foo", "bar", "42" };
        boolean bArray[] = { true, false, false };

        assertThat(allHaveSameLength(iArray, sArray, bArray), is(true));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfDifferentLength1() {
        int iArray[] = { 1, 2, 3 };
        String sArray[] = { "foo", "bar" };
        boolean bArray[] = { true, false, false };

        assertThat(allHaveSameLength(iArray, sArray, bArray), is(false));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfDifferentLength2() {
        int iArray[] = { 1, 2, 3 };
        String sArray[] = { "foo", "bar", "42" };
        boolean bArray[] = { true, false };

        assertThat(allHaveSameLength(iArray, sArray, bArray), is(false));
    }

    @Test
    public void testAllHaveSameLengthWithArraysOfDifferentLength3() {
        int iArray[] = { 1, 2 };
        String sArray[] = { "foo", "bar", "42" };
        boolean bArray[] = { true, false, false };

        assertThat(allHaveSameLength(iArray, sArray, bArray), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllHaveSameLengthWithOtherObjects1() {
        int i = 1;

        assertThat(allHaveSameLength(i), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAllHaveSameLengthWithOtherObjects2() {
        int i = 1;
        String s = "s";

        assertThat(allHaveSameLength(i, s), is(false));
    }

    @Test
    public void testGetIntOrDefault() throws Exception {
        assertThat(getIntOrDefault("25", 6), is(25));
        assertThat(getIntOrDefault("-25", 25), is(-25));
        assertThat(getIntOrDefault("25z", 6), is(6));
        assertThat(getIntOrDefault( null, 42), is(42));
    }
}
