/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.importer;

import com.google.common.collect.FluentIterable;
import org.eclipse.sw360.attachments.AttachmentHandler;
import org.eclipse.sw360.datahandler.db.AttachmentContentRepository;
import org.eclipse.sw360.components.ComponentHandler;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.common.ImportCSV;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.vendors.VendorHandler;
import org.apache.commons.csv.CSVRecord;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

import static org.eclipse.sw360.datahandler.TestUtils.*;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToCompCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToComponentAttachmentCSVRecords;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ComponentAndAttachmentAwareDBTest {

    protected ComponentService.Iface componentClient;
    protected VendorService.Iface vendorClient;
    protected AttachmentService.Iface attachmentClient;
    protected AttachmentContentRepository attachmentContentRepository;
    protected User user;

    protected  static  DatabaseConnector getDBConnector(String couchDbDatabase) throws MalformedURLException {
        return new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), couchDbDatabase);
    }


    protected static AttachmentContentRepository getAttachmentContentRepository() throws MalformedURLException {
        return new AttachmentContentRepository(getDBConnector(DatabaseSettings.COUCH_DB_ATTACHMENTS));
    }

    protected static FluentIterable<ComponentCSVRecord> getCompCSVRecordsFromTestFile(String fileName) throws IOException {
        InputStream testStream = spy(ComponentImportUtilsTest.class.getResourceAsStream(fileName));

        List<CSVRecord> testRecords = ImportCSV.readAsCSVRecords(testStream);
        verify(testStream).close();
        return convertCSVRecordsToCompCSVRecords(testRecords);
    }

    protected static FluentIterable<ComponentAttachmentCSVRecord> getCompAttachmentCSVRecordsFromTestFile(String fileName) throws IOException {
        InputStream testStream = spy(ComponentImportUtilsTest.class.getResourceAsStream(fileName));

        List<CSVRecord> testRecords = ImportCSV.readAsCSVRecords(testStream);
        verify(testStream).close();
        return convertCSVRecordsToComponentAttachmentCSVRecords(testRecords);
    }

    protected void deleteDatabases() throws MalformedURLException {
        deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_ATTACHMENTS);
        deleteDatabase(DatabaseSettings.getConfiguredHttpClient(), DatabaseSettings.COUCH_DB_DATABASE);
    }
    protected static ThriftClients getThriftClients() throws TException, IOException {
        assertTestDbNames();

        ThriftClients thriftClients = failingMock(ThriftClients.class);

        ComponentHandler componentHandler = new ComponentHandler(thriftClients);
        VendorHandler vendorHandler = new VendorHandler();
        AttachmentHandler attachmentHandler = new AttachmentHandler();

        ModerationService.Iface moderationService = failingMock(ModerationService.Iface.class);

        doNothing().when(moderationService).deleteRequestsOnDocument(anyString());

        doReturn(componentHandler).when(thriftClients).makeComponentClient();
        doReturn(vendorHandler).when(thriftClients).makeVendorClient();
        doReturn(attachmentHandler).when(thriftClients).makeAttachmentClient();
        doReturn(moderationService).when(thriftClients).makeModerationClient();

        return thriftClients;
    }
    @Before
    public void setUp() throws Exception {
        deleteDatabases();

        ThriftClients thriftClients = getThriftClients();

        componentClient = thriftClients.makeComponentClient();
        vendorClient = thriftClients.makeVendorClient();
        attachmentClient = thriftClients.makeAttachmentClient();
        attachmentContentRepository = getAttachmentContentRepository();
        user = getAdminUser(getClass());


    }

    @After
    public void tearDown() throws Exception {
        deleteDatabases();
    }
}
