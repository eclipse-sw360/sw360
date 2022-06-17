/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;

import java.io.IOException;
import java.util.List;

/**
 * Small client for testing a service
 *
 * @author cedric.bodet@tngtech.com
 */
public class TestLicenseClient {

    public static void main(String[] args) throws TException, IOException {
        THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080/licenses/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        LicenseService.Iface client = new LicenseService.Client(protocol);

        List<License> licenses = client.getLicenseSummary();

        System.out.println("Fetched " + licenses.size() + " licenses from license service");

        final List<License> licenseList = client.getDetailedLicenseSummary("", ImmutableList.of("AFL-2.1","Artistic-1.0"));
        System.out.println(licenseList.toString());

    }

}
