/*
 * Copyright Siemens AG, 2014-2016, 2019, 2026. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 * With modifications by Bosch.IO GmbH, 2020.
 * With modifications by Verifa Oy, 2018.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thrift;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TConfiguration;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.configurations.SW360ConfigsService;
import org.eclipse.sw360.datahandler.thrift.cvesearch.CveSearchService;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.health.HealthService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationService;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.datahandler.thrift.projectimport.ProjectImportService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.FileInformationService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponentService;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityService;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by bodet on 11/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author stefan.jaeger@evosoft.com
 */
public class ThriftClients {

    private final static Logger log = LogManager.getLogger(ThriftClients.class);

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    public static final String BACKEND_URL;
    public static final String BACKEND_PROXY_URL;
    public static final int THRIFT_CONNECTION_TIMEOUT;
    public static final int THRIFT_READ_TIMEOUT;
    public static final int THRIFT_MAX_MESSAGE_SIZE;
    public static final int THRIFT_MAX_FRAME_SIZE;
    public static final int THRIFT_MAX_TOTAL_CONNECTIONS;
    public static final int THRIFT_MAX_CONNECTIONS_PER_ROUTE;
    /**
     * Connections idle for longer than this many seconds are validated before
     * being handed out from the pool (replaces the deprecated
     * {@code evictIdleConnections} builder method). Should be set below
     * Tomcat's {@code keepAliveTimeout} so stale connections are detected
     * before reuse.
     */
    public static final int THRIFT_IDLE_EVICT_SECONDS;
    /**
     * Maximum lifetime of a pooled connection in seconds. Connections older
     * than this are discarded and replaced, preventing accumulation of
     * server-side half-closed connections.
     */
    public static final int THRIFT_CONNECTION_TTL_SECONDS;

    /**
     * Shared, pooled HTTP client used for ALL Thrift-over-HTTP backend calls.
     * Using a single pooled, self-evicting client (instead of a per-call
     * {@code HttpURLConnection}-backed {@link THttpClient}) prevents the
     * CLOSE_WAIT socket / file-descriptor leak that otherwise accumulates one
     * stuck loopback connection per backend call and exhausts the ephemeral
     * port range.
     */
    private static final CloseableHttpClient SHARED_HTTP_CLIENT;

    /**
     * Connection manager backing {@link #SHARED_HTTP_CLIENT}. Retained so that
     * live pool statistics (leased / pending / available / max) can be exposed
     * through the REST health endpoint for operational visibility.
     */
    private static final PoolingHttpClientConnectionManager SHARED_CONNECTION_MANAGER;

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
    private static final String VM_SERVICE_URL = "/vmcomponents/thrift";
    private static final String WSIMPORT_SERVICE_URL = "/wsimport/thrift";
    private static final String CHANGELOGS_SERVICE_URL = "/changelogs/thrift";
    private static final String HEALTH_SERVICE_URL = "/health/thrift";
    private static final String SPDX_SERVICE_URL = "/spdxdocument/thrift";
    private static final String SPDX_DOCUMENT_INFO_SERVICE_URL = "/spdxdocumentcreationinfo/thrift";
    private static final String SPDX_PACKAGE_INFO_SERVICE_URL = "/spdxpackageinfo/thrift";
    private static final String SPDX_FILE_INFO_SERVICE_URL = "/fileinformation/thrift";
    private static final String PACKAGE_SERVICE_URL = "/packages/thrift";
    private static final String SW360_CONFIGS_SERVICE_URL = "/configurations/thrift";

    // A service which has to be scheduled by the scheduler should be registered here!
    // names of services that can be scheduled by the schedule service, i.e. that have an "update" method
    public static final String CVESEARCH_SERVICE = "cvesearchService";
    public static final String SVMSYNC_SERVICE = "svmsyncService";
    public static final String SVMMATCH_SERVICE = "svmmatchService";
    public static final String SVM_LIST_UPDATE_SERVICE = "svmListUpdateService";
    public static final String SVM_TRACKING_FEEDBACK_SERVICE = "svmTrackingFeedbackService";
    public static final String DELETE_ATTACHMENT_SERVICE = "deleteattachmentService";
    public static final String IMPORT_DEPARTMENT_SERVICE = "importdepartmentService";
    public static final String SRC_UPLOAD_SERVICE = "srcAttachmentUploadService";
    public static final String LICENSEDB_SYNC_SERVICE = "licensedbsyncService";


    static {
        Properties props = CommonUtils.loadProperties(ThriftClients.class, PROPERTIES_FILE_PATH);

        BACKEND_URL = props.getProperty("backend.url", "http://127.0.0.1:8080");
        //Proxy can be set e.g. with "http://localhost:3128". if set all request to the thrift backend are routed through the proxy
        BACKEND_PROXY_URL = props.getProperty("backend.proxy.url", null);
        // maximum timeout for connecting and reading
        THRIFT_CONNECTION_TIMEOUT = Integer.parseInt(props.getProperty("backend.timeout.connection", "5000"));
        THRIFT_READ_TIMEOUT = Integer.parseInt(props.getProperty("backend.timeout.read", "600000"));

        THRIFT_MAX_MESSAGE_SIZE = Integer.parseInt(props.getProperty("backend.thrift.max.message.size", String.valueOf(TConfiguration.DEFAULT_MAX_MESSAGE_SIZE)));
        THRIFT_MAX_FRAME_SIZE = Integer.parseInt(props.getProperty("backend.thrift.max.frame.size", String.valueOf(TConfiguration.DEFAULT_MAX_FRAME_SIZE)));

        // Pooled HTTP client sizing for Thrift-over-HTTP backend calls.
        THRIFT_MAX_TOTAL_CONNECTIONS = Integer.parseInt(props.getProperty("backend.thrift.max.connections.total", "200"));
        THRIFT_MAX_CONNECTIONS_PER_ROUTE = Integer.parseInt(props.getProperty("backend.thrift.max.connections.per.route", "100"));
        // Validate idle connections before reuse; set below Tomcat keepAliveTimeout (default 30 s).
        THRIFT_IDLE_EVICT_SECONDS = Integer.parseInt(props.getProperty("backend.thrift.idle.evict.seconds", "15"));
        // Force-retire connections older than this (seconds) regardless of activity.
        THRIFT_CONNECTION_TTL_SECONDS = Integer.parseInt(props.getProperty("backend.thrift.connection.ttl.seconds", "60"));

        log.info("""
                The following configuration will be used for connections to the backend:
                \tURL                      : {}
                \tProxy                    : {}
                \tTimeout Connecting (ms)  : {}
                \tTimeout Read (ms)        : {}
                """,
                BACKEND_URL, BACKEND_PROXY_URL, THRIFT_CONNECTION_TIMEOUT, THRIFT_READ_TIMEOUT);

        SHARED_CONNECTION_MANAGER = createSharedConnectionManager();
        SHARED_HTTP_CLIENT = createSharedHttpClient();
    }

    private ThriftClients() {
    }

    /**
     * Creates a Thrift Compact Protocol object linked to the given address,
     * backed by the shared pooled HTTP client.
     */
    private static TProtocol makeProtocol(String service) {
        final String destinationAddress = ThriftClients.BACKEND_URL + service;
        THttpClient thriftClient = null;
        try {
            thriftClient = makeThriftHttpClient(destinationAddress);
        } catch (TTransportException e) {
            log.error("cannot connect to backend on {}", destinationAddress, e);
        }
        return new TCompactProtocol(thriftClient);
    }

    /**
     * Builds a {@link THttpClient} for the given destination backed by the
     * shared, pooled and self-evicting HTTP client. Any caller that previously
     * created {@code new THttpClient(url)} directly MUST use this method
     * instead: the bare constructor uses a per-call {@code HttpURLConnection}
     * that is never closed, leaking one CLOSE_WAIT socket / file descriptor per
     * backend call until the ephemeral port range is exhausted.
     *
     * @param destinationAddress full backend thrift endpoint URL
     * @return a configured THttpClient sharing the pooled connection manager
     */
    private static THttpClient makeThriftHttpClient(String destinationAddress) throws TTransportException {
        final TConfiguration thriftConfigure = TConfiguration.custom()
                .setMaxMessageSize(THRIFT_MAX_MESSAGE_SIZE)
                .setMaxFrameSize(THRIFT_MAX_FRAME_SIZE).build();
        // Connect/response timeouts are configured on the shared pooled client
        // (ConnectionConfig / RequestConfig); the deprecated per-transport
        // setConnectTimeout/setReadTimeout setters are intentionally not used.
        return new THttpClient(thriftConfigure, destinationAddress, SHARED_HTTP_CLIENT);
    }

    /**
     * Builds the bounded, self-evicting connection pool shared by every Thrift
     * backend call. Kept separate so the manager reference can be retained for
     * health-endpoint pool statistics.
     *
     * <p>Eviction is handled through {@link ConnectionConfig} properties
     * ({@code setTimeToLive} / {@code setValidateAfterInactivity}) rather than
     * the deprecated {@code evictExpiredConnections} / {@code evictIdleConnections}
     * builder methods removed in Apache HttpClient 5.4+.
     */
    private static PoolingHttpClientConnectionManager createSharedConnectionManager() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(THRIFT_CONNECTION_TIMEOUT))
                // Validate a connection that has been idle longer than this before reuse.
                // Keeps the pool from handing out a stale socket after the server (Tomcat)
                // closed its keepalive side — the primary driver of CLOSE_WAIT accumulation.
                .setValidateAfterInactivity(Timeout.ofSeconds(THRIFT_IDLE_EVICT_SECONDS))
                // Hard upper bound on connection lifetime. Ensures periodic renewal even for
                // connections that stay active, preventing silent half-closed accumulation.
                .setTimeToLive(TimeValue.ofSeconds(THRIFT_CONNECTION_TTL_SECONDS))
                .build();

        return PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(THRIFT_MAX_TOTAL_CONNECTIONS)
                .setMaxConnPerRoute(THRIFT_MAX_CONNECTIONS_PER_ROUTE)
                .setDefaultConnectionConfig(connectionConfig)
                .build();
    }

    /**
     * Creates the JVM-wide pooled HTTP client used for every Thrift backend
     * call. The pool bounds the number of connections, reuses keep-alive
     * connections and actively evicts idle/expired ones via {@link ConnectionConfig}
     * TTL and validate-after-inactivity settings — which removes the unbounded
     * CLOSE_WAIT accumulation caused by per-call HttpURLConnection transports.
     */
    private static CloseableHttpClient createSharedHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(THRIFT_READ_TIMEOUT))
                .build();

        var builder = HttpClients.custom()
                .setConnectionManager(ThriftClients.SHARED_CONNECTION_MANAGER)
                .setDefaultRequestConfig(requestConfig);

        if (BACKEND_PROXY_URL != null) {
            try {
                URL proxyUrl = new URI(BACKEND_PROXY_URL).toURL();
                HttpHost proxy = new HttpHost(proxyUrl.getProtocol(), proxyUrl.getHost(), proxyUrl.getPort());
                builder.setRoutePlanner(new DefaultProxyRoutePlanner(proxy));
            } catch (MalformedURLException | URISyntaxException | IllegalArgumentException e) {
                log.error("cannot configure http proxy (REASON:MalformedURLException) to thrift backend", e);
            }
        }
        return builder.build();
    }

    /**
     * Gracefully closes the shared HTTP client and its underlying connection
     * manager. Should be called during application shutdown (e.g. via a Spring
     * {@code @PreDestroy} hook or a JVM shutdown hook) to cleanly release all
     * pooled connections without waiting for GC.
     *
     * <p>This method is idempotent and safe to call multiple times.
     */
    public static void closeSharedClient() {
        try {
            SHARED_HTTP_CLIENT.close();
            log.info("Shared Thrift HTTP client closed.");
        } catch (Exception e) {
            log.warn("Error closing shared Thrift HTTP client", e);
        }
    }

    /**
     * Exposes live statistics of the shared Thrift connection pool for the REST
     * {@code /health} endpoint. Useful to spot a recurrence of the CLOSE_WAIT
     * leak (leased growing without bound) or pool saturation (pending &gt; 0).
     *
     * @return ordered map with {@code leased}, {@code pending},
     *         {@code available} and {@code max} connection counts
     */
    public static Map<String, Integer> getThriftConnectionPoolStats() {
        PoolStats stats = SHARED_CONNECTION_MANAGER.getTotalStats();
        Map<String, Integer> poolStats = new LinkedHashMap<>();
        poolStats.put("leased", stats.getLeased());
        poolStats.put("pending", stats.getPending());
        poolStats.put("available", stats.getAvailable());
        poolStats.put("max", stats.getMax());
        return poolStats;
    }

    public static AttachmentService.Iface makeAttachmentClient() {
        return new AttachmentService.Client(makeProtocol(ATTACHMENT_SERVICE_URL));
    }

    public static ComponentService.Iface makeComponentClient() {
        return new ComponentService.Client(makeProtocol(COMPONENT_SERVICE_URL));
    }

    public static CveSearchService.Iface makeCvesearchClient() {
        return new CveSearchService.Client(makeProtocol(CVESEARCH_SERVICE_URL));
    }

    public static FossologyService.Iface makeFossologyClient() {
        return new FossologyService.Client(makeProtocol(FOSSOLOGY_SERVICE_URL));
    }

    public static LicenseService.Iface makeLicenseClient() {
        return new LicenseService.Client(makeProtocol(LICENSE_SERVICE_URL));
    }

    public static ModerationService.Iface makeModerationClient() {
        return new ModerationService.Client(makeProtocol(MODERATION_SERVICE_URL));
    }

    public static ProjectService.Iface makeProjectClient() {
        return new ProjectService.Client(makeProtocol(PROJECT_SERVICE_URL));
    }

    public static SearchService.Iface makeSearchClient() {
        return new SearchService.Client(makeProtocol(SEARCH_SERVICE_URL));
    }

    public static UserService.Iface makeUserClient() {
        return new UserService.Client(makeProtocol(USER_SERVICE_URL));
    }

    public static VendorService.Iface makeVendorClient() {
        return new VendorService.Client(makeProtocol(VENDOR_SERVICE_URL));
    }

    public static ProjectImportService.Iface makeProjectImportClient() {
        return new ProjectImportService.Client(makeProtocol(PROJECTIMPORT_SERVICE_URL));
    }

    public static VulnerabilityService.Iface makeVulnerabilityClient() {
        return new VulnerabilityService.Client(makeProtocol(VULNERABILITY_SERVICE_URL));
    }

    public static VMComponentService.Iface makeVMClient() {
        return new VMComponentService.Client(makeProtocol(VM_SERVICE_URL));
    }

    public static LicenseInfoService.Client makeLicenseInfoClient() {
        return new LicenseInfoService.Client(makeProtocol(LICENSEINFO_SERVICE_URL));
    }

    public static ScheduleService.Iface makeScheduleClient() {
        return new ScheduleService.Client(makeProtocol(SCHEDULE_SERVICE_URL));
    }

    public static ProjectImportService.Iface makeWsImportClient() {
        return new ProjectImportService.Client(makeProtocol(WSIMPORT_SERVICE_URL));
    }

    public static ChangeLogsService.Iface makeChangeLogsClient() {
        return new ChangeLogsService.Client(makeProtocol(CHANGELOGS_SERVICE_URL));
    }

    public static HealthService.Iface makeHealthClient() {
        return new HealthService.Client(makeProtocol(HEALTH_SERVICE_URL));
    }

    public static SPDXDocumentService.Iface makeSPDXClient() {
        return new SPDXDocumentService.Client(makeProtocol(SPDX_SERVICE_URL));
    }

    public static DocumentCreationInformationService.Iface makeSPDXDocumentInfoClient() {
        return new DocumentCreationInformationService.Client(makeProtocol(SPDX_DOCUMENT_INFO_SERVICE_URL));
    }

    public static PackageInformationService.Iface makeSPDXPackageInfoClient() {
        return new PackageInformationService.Client(makeProtocol(SPDX_PACKAGE_INFO_SERVICE_URL));
    }

    public static FileInformationService.Iface makeSPDXFileInfoClient() {
        return new FileInformationService.Client(makeProtocol(SPDX_FILE_INFO_SERVICE_URL));
    }

    public static PackageService.Iface makePackageClient() {
        return new PackageService.Client(makeProtocol(PACKAGE_SERVICE_URL));
    }

    public static SW360ConfigsService.Iface makeSW360ConfigsClient() {
        return new SW360ConfigsService.Client(makeProtocol(SW360_CONFIGS_SERVICE_URL));
    }
}
