/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.spring.CouchDbContextInitializer;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.junit.After;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Set;

/**
 * Created by bodet on 10/12/14.
 *
 * @author cedric.bodet@tngtech.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(
        classes = {DatabaseConfig.class},
        initializers = {CouchDbContextInitializer.class}
)
@ActiveProfiles("test")
public class TestComponentClient {

    @Autowired
    Cloudant client;

    @Autowired
    @Qualifier("COUCH_DB_ALL_NAMES")
    private Set<String> allDatabaseNames;

    @After
    public void tearDown() throws MalformedURLException {
        TestUtils.deleteAllDatabases(client, allDatabaseNames);
    }

    private static final User user = new User().setEmail("cedric.bodet@tngtech.com").setDepartment("AB CD EF");

    public static void main(String[] args) throws TException, IOException {
        THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        ComponentService.Iface client = new ComponentService.Client(protocol);

//        List<Component> components = client.getComponentSummary(user);
//        List<Component> recentComponents = client.getRecentComponents();
//        List<Release> releases = client.getReleaseSummary(user);
//
//        System.out.println("Fetched " + components.size() + " components from license service");
//        System.out.println("Fetched " + releases.size() + " releases from license service");
//        System.out.println("Fetched " + recentComponents.size() + " recent components from license service");
//
//        String referenceId =null;
//        for (Component component : recentComponents) {
//            if(referenceId==null) referenceId=component.getId();
//            System.out.println(component.getId() + ": " + component.getName());
//        }
//
//        if(referenceId!=null) {
//            System.out.println(client.getComponentById(referenceId, user).toString());
//            Component component = new ComponentHandler("http://localhost:5984", "sw360db", "sw360attachments").getComponentById(referenceId, user);
//            System.out.println(component.toString());
//            System.out.println(client.getComponentById(referenceId, user).toString());
//        }
//
//        for(Release release : releases) {
//                System.out.println(release.toString());
//            }
//        // This fails with a thrift error... debug!
//        if(releases.size()>0) {
//            String releaseNameStart = releases.get(0).getName().substring(0,1);
//            System.out.println("The following releases start with " + releaseNameStart );
//
//            List<Release> releases1 = client.searchReleaseByName(releaseNameStart);
//            for(Release release : releases1) {
//                System.out.println(release.toString());
//            }
//
//        }


//        final Component cpe = client.getComponentForReportFromCPEId("cpe");

//        System.out.println(cpe.toString());

    }
}
