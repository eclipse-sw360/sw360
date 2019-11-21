/*
 * Copyright Siemens AG, 2016-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.licenseinfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.db.AttachmentDatabaseHandler;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LicenseInfoHandlerTest {

    private LicenseInfoHandler handler;

    @Mock
    private AttachmentDatabaseHandler attachmentDatabaseHandler;

    @Mock
    private AttachmentConnector connector;

    @Mock
    private User user;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        when(attachmentDatabaseHandler.getAttachmentConnector()).thenReturn(connector);
        handler = new LicenseInfoHandler(attachmentDatabaseHandler, null, null);
    }

    @Test(expected = IllegalStateException.class)
    public void testThatAttachmentMustBePartOfTheRelease() throws TException {
        Release release = Mockito.mock(Release.class);
        handler.getLicenseInfoForAttachment(release, "123", user);
    }

    @Test
    public void testThatEmptyLicensesAreFiltered() {
        LicenseInfoParsingResult emptyResult = new LicenseInfoParsingResult();

        LicenseInfoParsingResult emptyLicenseInfo = new LicenseInfoParsingResult();
        emptyLicenseInfo.setLicenseInfo(new LicenseInfo());

        LicenseInfoParsingResult parsingResults = new LicenseInfoParsingResult();
        LicenseInfo licenseInfo = new LicenseInfo();
        // @formatter:off
        licenseInfo.setLicenseNamesWithTexts(ImmutableSet.of(
                createLicense("nameOnly", null, null),
                createLicense(null, null, null),
                createLicense(null, "textOnly", null),
                createLicense("", null, null),
                createLicense(null, null, "ackOnly"),
                createLicense("", "", ""),
                createLicense("name", "text", "ack")
        ));
        // @formatter:on
        parsingResults.setLicenseInfo(licenseInfo);

        handler.filterEmptyLicenses(ImmutableList.of(emptyResult, emptyLicenseInfo, parsingResults));

        // @formatter:off
        Assert.assertThat(parsingResults.getLicenseInfo().getLicenseNamesWithTexts(), Matchers.containsInAnyOrder(
                createLicense("nameOnly", null, null),
                createLicense(null, "textOnly", null),
                createLicense(null, null, "ackOnly"),
                createLicense("name", "text", "ack")
        ));
        // @formatter:on
    }

    @Test
    public void testThatLicensesAreFilteredAndOriginalObejctIsNotTouched() {
        LicenseInfoParsingResult parsingResults = new LicenseInfoParsingResult();
        LicenseInfo licenseInfo = new LicenseInfo();
        // @formatter:off
        licenseInfo.setLicenseNamesWithTexts(ImmutableSet.of(
                createLicense("l1", null, null),
                createLicense("l1", "t1", null),
                createLicense("l2", "t2", null),
                createLicense("l3", "t3", null),
                createLicense("l3", "t3", "a3"),
                createLicense(null, "t4", null),
                createLicense("l4", "t4", null),
                createLicense(null, null, "a5"),
                createLicense("l5", null, "a5"),
                createLicense("l", "t", "a6"),
                createLicense("l", "t", "a7"),
                createLicense("l8", null, null),
                createLicense("l8", "t8", null),
                createLicense("l9", "t9", "a9"),
                createLicense(null, "t9", "a9")
        ));
        // @formatter:on
        parsingResults.setLicenseInfo(licenseInfo);

        // @formatter:off
        LicenseInfoParsingResult filteredResult = handler.filterLicenses(parsingResults, ImmutableSet.of(
                createLicense("l1", "t1", null),
                createLicense("l3", "t3", "a3"),
                createLicense(null, "t4", null),
                createLicense(null, null, "a5"),
                createLicense("l", "t", "a6"),
                createLicense("l8", null, null),
                createLicense(null, "t9", "a9")
        ));
        // @formatter:on

        // @formatter:off
        Assert.assertThat(filteredResult.getLicenseInfo().getLicenseNamesWithTexts(), Matchers.containsInAnyOrder(
                createLicense("l1", null, null),
                createLicense("l2", "t2", null),
                createLicense("l3", "t3", null),
                createLicense("l4", "t4", null),
                createLicense("l5", null, "a5"),
                createLicense("l", "t", "a7"),
                createLicense("l8", "t8", null),
                createLicense("l9", "t9", "a9")
        ));
        // @formatter:on
    }

    private LicenseNameWithText createLicense(String name, String text, String acknowledgements) {
        LicenseNameWithText licenseNameWithText = new LicenseNameWithText();
        licenseNameWithText.setLicenseName(name);
        licenseNameWithText.setLicenseText(text);
        licenseNameWithText.setAcknowledgements(acknowledgements);
        return licenseNameWithText;
    }
}