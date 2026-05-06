/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.common;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.db.SvmHttpClientFactory;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.apache.thrift.TBase;

import java.io.IOException;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class SVMUtils {

    private static final Logger log = LogManager.getLogger(SVMUtils.class);

    private SVMUtils() {}

    /**
     * Performs an HTTP GET request against the given URL and returns the response body as a String.
     *
     * @param url the URL to call
     * @return the response body
     * @throws IOException if the request fails or returns a non-200 status code
     */
    public static String prepareJSONRequestAndGetResponse(String url) throws IOException {
        log.debug("Call URL: {}", url);
        CloseableHttpClient httpClient = SvmHttpClientFactory.getTrustedClient();
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");
        return httpClient.execute(httpGet, response -> {
            int code = response.getCode();
            if (code != 200) {
                String errorMessage = "Failed : HTTP error code : " + code;
                log.error(errorMessage);
                throw new IOException(errorMessage);
            }
            String body = new String(response.getEntity().getContent().readAllBytes());
            log.debug("Response from Server .... \n{}", body);
            return body;
        });
    }

    public static RequestSummary newRequestSummary(RequestStatus status, int totalElements, int totalAffectedElements, String message) {
        RequestSummary summary = new RequestSummary();
        summary.setRequestStatus(status);
        summary.setTotalElements(totalElements);
        summary.setTotalAffectedElements(totalAffectedElements);
        summary.setMessage(message);
        return summary;
    }

    public static <T extends TBase> String getVmid(T t) {
        if (VMComponent.class.isAssignableFrom(t.getClass()))
            return ((VMComponent) t).getVmid();
        else if (VMAction.class.isAssignableFrom(t.getClass()))
            return ((VMAction) t).getVmid();
        else if (VMPriority.class.isAssignableFrom(t.getClass()))
            return ((VMPriority) t).getVmid();
        else if (Vulnerability.class.isAssignableFrom(t.getClass()))
            return ((Vulnerability) t).getExternalId();
        else
            throw new IllegalArgumentException("unknown type " + t.getClass().getSimpleName());
    }

    public static <T extends TBase> String getId(T t) {
        if (VMComponent.class.isAssignableFrom(t.getClass()))
            return ((VMComponent) t).getId();
        else if (VMAction.class.isAssignableFrom(t.getClass()))
            return ((VMAction) t).getId();
        else if (VMPriority.class.isAssignableFrom(t.getClass()))
            return ((VMPriority) t).getId();
        else
            throw new IllegalArgumentException("unknown type " + t.getClass().getSimpleName());
    }

}
