/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
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
import java.net.URL;

import static org.apache.log4j.Logger.getLogger;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class SVMUtils {

    private final static Logger log = getLogger(SVMUtils.class);

    private SVMUtils(){}

    public static String prepareJSONRequestAndGetResponse(String url) throws IOException {
        StringBuffer json = new StringBuffer();
        URL url_ = new URL(url);
        log.debug("Call URL: "+url);
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
