/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.importer;

import com.google.common.collect.FluentIterable;
//import org.eclipse.sw360.datahandler.db.AttachmentContentRepository;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.ImportCSV;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    protected static DatabaseConnectorCloudant getDBConnector(String couchDbDatabase)
            throws MalformedURLException {
        return new DatabaseConnectorCloudant(DatabaseSettingsTest.getConfiguredClient(),
                couchDbDatabase);
    }

    protected static AttachmentContentRepository getAttachmentContentRepository()
            throws MalformedURLException {
        return new AttachmentContentRepository(
                getDBConnector(DatabaseSettingsTest.COUCH_DB_ATTACHMENTS));
    }

    protected static FluentIterable<ComponentCSVRecord> getCompCSVRecordsFromTestFile(
            String fileName) throws IOException {
        InputStream testStream = ComponentImportUtilsTest.class.getResourceAsStream(fileName);
        List<CSVRecord> testRecords = ImportCSV.readAsCSVRecords(testStream);
        return convertCSVRecordsToCompCSVRecords(testRecords);
    }

    protected static FluentIterable<ComponentAttachmentCSVRecord> getCompAttachmentCSVRecordsFromTestFile(
            String fileName) throws IOException {
        InputStream testStream = ComponentImportUtilsTest.class.getResourceAsStream(fileName);

        List<CSVRecord> testRecords = ImportCSV.readAsCSVRecords(testStream);
        return convertCSVRecordsToComponentAttachmentCSVRecords(testRecords);
    }

    protected void deleteDatabases() throws MalformedURLException {
        deleteDatabase(DatabaseSettingsTest.getConfiguredClient(),
                DatabaseSettingsTest.COUCH_DB_ATTACHMENTS);
        deleteDatabase(DatabaseSettingsTest.getConfiguredClient(),
                DatabaseSettingsTest.COUCH_DB_DATABASE);
    }

    protected static ThriftClients getThriftClients() throws TException, IOException {
        assertTestDbNames();

        ThriftClients thriftClients = failingMock(ThriftClients.class);

        ModerationService.Iface moderationService = failingMock(ModerationService.Iface.class);

        doNothing().when(moderationService).deleteRequestsOnDocument(anyString());

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
