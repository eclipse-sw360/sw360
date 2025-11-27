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
import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.db.AttachmentContentRepository;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.ImportCSV;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
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
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.eclipse.sw360.datahandler.TestUtils.*;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToCompCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToComponentAttachmentCSVRecords;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author daniele.fognini@tngtech.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class ComponentAndAttachmentAwareDBTest {

    protected ComponentService.Iface componentClient;
    protected VendorService.Iface vendorClient;
    protected AttachmentService.Iface attachmentClient;
    protected AttachmentContentRepository attachmentContentRepository;
    protected User user;

    @Autowired
    private Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ATTACHMENTS")
    private String attachmentsDbName;

    @Autowired
    @Qualifier("LUCENE_SEARCH_LIMIT")
    private int luceneSearchLimit;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    protected DatabaseConnectorCloudant getDBConnector(String couchDbDatabase) {
        return new DatabaseConnectorCloudant(client, couchDbDatabase, luceneSearchLimit);
    }

    protected AttachmentContentRepository getAttachmentContentRepository() {
        return new AttachmentContentRepository(
                getDBConnector(attachmentsDbName));
    }

    protected static FluentIterable<ComponentCSVRecord> getCompCSVRecordsFromTestFile(
            String fileName) {
        InputStream testStream = ComponentImportUtilsTest.class.getResourceAsStream(fileName);
        List<CSVRecord> testRecords = ImportCSV.readAsCSVRecords(testStream);
        return convertCSVRecordsToCompCSVRecords(testRecords);
    }

    protected static FluentIterable<ComponentAttachmentCSVRecord> getCompAttachmentCSVRecordsFromTestFile(
            String fileName) {
        InputStream testStream = ComponentImportUtilsTest.class.getResourceAsStream(fileName);

        List<CSVRecord> testRecords = ImportCSV.readAsCSVRecords(testStream);
        return convertCSVRecordsToComponentAttachmentCSVRecords(testRecords);
    }

    protected static ThriftClients getThriftClients() throws TException, IOException {

        ThriftClients thriftClients = failingMock(ThriftClients.class);

        ModerationService.Iface moderationService = failingMock(ModerationService.Iface.class);

        doNothing().when(moderationService).deleteRequestsOnDocument(anyString());

        doReturn(moderationService).when(thriftClients).makeModerationClient();

        return thriftClients;
    }

    @Before
    public void setUp() throws Exception {
        ThriftClients thriftClients = getThriftClients();

        componentClient = thriftClients.makeComponentClient();
        vendorClient = thriftClients.makeVendorClient();
        attachmentClient = thriftClients.makeAttachmentClient();
        attachmentContentRepository = getAttachmentContentRepository();
        user = getAdminUser(getClass());
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }
}
