/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents.common;

import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMAction;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponent;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMPriority;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.text.SimpleDateFormat;

import static org.apache.log4j.Logger.getLogger;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class SVMUtils {

    private final static Logger log = getLogger(SVMUtils.class);

    private SVMUtils(){}

    public static String prepareJSONRequestAndGetResponse(String url) throws IOException, URISyntaxException {
        return prepareJSONRequestAndGetResponse(url, null);
    }

    public static String prepareJSONRequestAndGetResponse(String url, String modifiedAfter) throws IOException, URISyntaxException {
        StringBuilder json = new StringBuilder();
        String finalUrl = url;
        if (modifiedAfter != null && !modifiedAfter.isEmpty()) {
            String separator = url.contains("?") ? "&" : "?";
            finalUrl = url + separator + "modified_after=" + modifiedAfter;
        }
        URL url_ = new URI(finalUrl).toURL();
        log.debug("Call URL: "+finalUrl);
        HttpURLConnection conn = (HttpURLConnection) url_.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            String errorMessage = "Failed : HTTP error code : " + conn.getResponseCode();
            log.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

        String line = "";
        while ((line = br.readLine()) != null) {
            json.append(line.trim());
        }

        conn.disconnect();
        String response = json.toString();
        log.debug("Response from Server .... \n"+response);
        return response;
    }

    public static String calculateModifiedAfter(String lastEndDate, int offsetDays) {
        try {
            if (lastEndDate == null || lastEndDate.isEmpty()) {
                return null;
            }
            SimpleDateFormat sw360Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date lastDate = sw360Format.parse(lastEndDate);
            
            LocalDateTime lastDateTime = LocalDateTime.ofInstant(lastDate.toInstant(), java.time.ZoneId.systemDefault());
            LocalDateTime modifiedAfterDateTime = lastDateTime.minus(offsetDays, ChronoUnit.DAYS);
            
            DateTimeFormatter svmFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return modifiedAfterDateTime.format(svmFormat);
        } catch (Exception e) {
            log.error("Error calculating modified_after parameter: " + e.getMessage(), e);
            return null;
        }
    }

    public static RequestSummary newRequestSummary(RequestStatus status, int totalElements, int totalAffectedElements, String message ){
        RequestSummary summary = new RequestSummary();
        summary.setRequestStatus(status);
        summary.setTotalElements(totalElements);
        summary.setTotalAffectedElements(totalAffectedElements);
        summary.setMessage(message);
        return summary;
    }

    public static <T extends TBase> String getVmid(T t){
        if (VMComponent.class.isAssignableFrom(t.getClass()))
            return ((VMComponent)t).getVmid();
        else if (VMAction.class.isAssignableFrom(t.getClass()))
            return ((VMAction)t).getVmid();
        else if (VMPriority.class.isAssignableFrom(t.getClass()))
            return ((VMPriority)t).getVmid();
        else if (Vulnerability.class.isAssignableFrom(t.getClass()))
            return ((Vulnerability)t).getExternalId();
        else
            throw new IllegalArgumentException("unknown type "+ t.getClass().getSimpleName());
    }

    public static <T extends TBase> String getId(T t){
        if (VMComponent.class.isAssignableFrom(t.getClass()))
            return ((VMComponent)t).getId();
        else if (VMAction.class.isAssignableFrom(t.getClass()))
            return ((VMAction)t).getId();
        else if (VMPriority.class.isAssignableFrom(t.getClass()))
            return ((VMPriority)t).getId();
        else
            throw new IllegalArgumentException("unknown type "+ t.getClass().getSimpleName());
    }

}
