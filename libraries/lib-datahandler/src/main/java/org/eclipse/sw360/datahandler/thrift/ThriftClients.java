/*
 * Copyright Siemens AG, 2014-2016, 2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 * With modifications by Verifa Oy, 2018.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.thrift;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.codescoop.CodescoopService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.cvesearch.CveSearchService;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.projectimport.ProjectImportService;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityService;
import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import static org.apache.log4j.Logger.getLogger;

/**
 * Created by bodet on 11/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author stefan.jaeger@evosoft.com
 */
public class ThriftClients {

    private final static Logger log = getLogger(ThriftClients.class);

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    public static final String BACKEND_URL;
    public static final String BACKEND_PROXY_URL;
    public static final int THRIFT_CONNECTION_TIMEOUT;
    public static final int THRIFT_READ_TIMEOUT;

    //! Service addresses
    private static final String ATTACHMENT_SERVICE_URL = "/attachments/thrift";
    private static final String COMPONENT_SERVICE_URL = "/components/thrift";
    private static final String CVESEARCH_SERVICE_URL = "/cvesearch/thrift";
    private static final String FOSSOLOGY_SERVICE_URL = "/fossology/thrift";
    private static final String LICENSE_SERVICE_URL = "/licenses/thrift";
    private static final String MODERATION_SERVICE_URL = "/moderation/thrift";
    private static final String PROJECT_SERVICE_URL = "/projects/thrift";
    private static final String LICENSEINFO_SERVICE_URL = "/licenseinfo/thrift";
    private static final String SEARCH_SERVICE_URL = "/search/thrift";
    private static final String USER_SERVICE_URL = "/users/thrift";
    private static final String VENDOR_SERVICE_URL = "/vendors/thrift";
    private static final String PROJECTIMPORT_SERVICE_URL = "/bdpimport/thrift";
    private static final String VULNERABILITY_SERVICE_URL = "/vulnerabilities/thrift";
    private static final String SCHEDULE_SERVICE_URL = "/schedule/thrift";
    private static final String WSIMPORT_SERVICE_URL = "/wsimport/thrift";
    private static final String CODESCOOP_SERVICE_URL = "/codescoop/thrift";

    // A service which has to be scheduled by the scheduler should be registered here!
    // names of services that can be scheduled by the schedule service, i.e. that have an "update" method
    public static final String CVESEARCH_SERVICE = "cvesearchService";

    static {
        Properties props = CommonUtils.loadProperties(ThriftClients.class, PROPERTIES_FILE_PATH);

        BACKEND_URL = props.getProperty("backend.url", "http://127.0.0.1:8080");
        //Proxy can be set e.g. with "http://localhost:3128". if set all request to the thrift backend are routed through the proxy
        BACKEND_PROXY_URL = props.getProperty("backend.proxy.url", null);
        // maximum timeout for connecting and reading
        THRIFT_CONNECTION_TIMEOUT = Integer.valueOf(props.getProperty("backend.timeout.connection", "5000"));
        THRIFT_READ_TIMEOUT = Integer.valueOf(props.getProperty("backend.timeout.read", "600000"));

        log.info("The following configuration will be used for connections to the backend:\n" +
            "\tURL                      : " + BACKEND_URL + "\n" +
            "\tProxy                    : " + BACKEND_PROXY_URL + "\n" +
            "\tTimeout Connecting (ms)  : " + THRIFT_CONNECTION_TIMEOUT + "\n" +
            "\tTimeout Read (ms)        : " + THRIFT_READ_TIMEOUT + "\n");
    }
    public ThriftClients() {
    }

    /**
     * Creates a Thrift Compact Protocol object linked to the given address
     */
    private static TProtocol makeProtocol(String url, String service) {
        THttpClient thriftClient = null;
        final String destinationAddress = url + service;
        try {
            if (BACKEND_PROXY_URL != null) {
                URL proxyUrl = new URL(BACKEND_PROXY_URL);
                HttpHost proxy = new HttpHost(proxyUrl.getHost(), proxyUrl.getPort(), proxyUrl.getProtocol());
                DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
                CloseableHttpClient httpClient = HttpClients.custom().setRoutePlanner(routePlanner).build();
                thriftClient = new THttpClient(destinationAddress, httpClient);
            } else {
                thriftClient = new THttpClient(destinationAddress);
            }
            thriftClient.setConnectTimeout(THRIFT_CONNECTION_TIMEOUT);
            thriftClient.setReadTimeout(THRIFT_READ_TIMEOUT);
        } catch (TTransportException e) {
            log.error("cannot connect to backend on " + destinationAddress, e);
        } catch (MalformedURLException e) {
            log.error("cannot connect via http proxy (REASON:MalformedURLException) to thrift backend", e);
        }
        return new TCompactProtocol(thriftClient);
    }

    public AttachmentService.Iface makeAttachmentClient() {
        return new AttachmentService.Client(makeProtocol(BACKEND_URL, ATTACHMENT_SERVICE_URL));
    }

    public ComponentService.Iface makeComponentClient() {
        return new ComponentService.Client(makeProtocol(BACKEND_URL, COMPONENT_SERVICE_URL));
    }

    public CveSearchService.Iface makeCvesearchClient() {
        return new CveSearchService.Client(makeProtocol(BACKEND_URL, CVESEARCH_SERVICE_URL));
    }

    public FossologyService.Iface makeFossologyClient() {
        return new FossologyService.Client(makeProtocol(BACKEND_URL, FOSSOLOGY_SERVICE_URL));
    }

    public LicenseService.Iface makeLicenseClient() {
        return new LicenseService.Client(makeProtocol(BACKEND_URL, LICENSE_SERVICE_URL));
    }

    public ModerationService.Iface makeModerationClient() {
        return new ModerationService.Client(makeProtocol(BACKEND_URL, MODERATION_SERVICE_URL));
    }

    public ProjectService.Iface makeProjectClient() {
        return new ProjectService.Client(makeProtocol(BACKEND_URL, PROJECT_SERVICE_URL));
    }

    public SearchService.Iface makeSearchClient() {
        return new SearchService.Client(makeProtocol(BACKEND_URL, SEARCH_SERVICE_URL));
    }

    public UserService.Iface makeUserClient() {
        return new UserService.Client(makeProtocol(BACKEND_URL, USER_SERVICE_URL));
    }

    public VendorService.Iface makeVendorClient() {
        return new VendorService.Client(makeProtocol(BACKEND_URL, VENDOR_SERVICE_URL));
    }

    public ProjectImportService.Iface makeProjectImportClient() {
        return new ProjectImportService.Client(makeProtocol(BACKEND_URL, PROJECTIMPORT_SERVICE_URL));
    }

    public VulnerabilityService.Iface makeVulnerabilityClient() {
        return new VulnerabilityService.Client(makeProtocol(BACKEND_URL, VULNERABILITY_SERVICE_URL));
    }

    public LicenseInfoService.Client makeLicenseInfoClient() {
        return new LicenseInfoService.Client(makeProtocol(BACKEND_URL, LICENSEINFO_SERVICE_URL));
    }

    public ScheduleService.Iface makeScheduleClient() {
        return new ScheduleService.Client(makeProtocol(BACKEND_URL, SCHEDULE_SERVICE_URL));
    }

    public ProjectImportService.Iface makeWsImportClient() {
        return new ProjectImportService.Client(makeProtocol(BACKEND_URL, WSIMPORT_SERVICE_URL));
    }

    public CodescoopService.Iface makeCodescoopClient() {
        return new CodescoopService.Client(makeProtocol(BACKEND_URL, CODESCOOP_SERVICE_URL));
    }
}
