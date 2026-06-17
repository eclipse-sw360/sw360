/*
 * Copyright Siemens Healthineers GmBH, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils;

import java.net.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.json.JSONArray;
import org.json.JSONException;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.VCS_HOSTS;

public class RepositoryURL {
    private static final Logger log = LogManager.getLogger(RepositoryURL.class);
    private static final String SCHEMA_PATTERN = ".+://(\\w*(?:[\\-@.\\\\s,_:/][/(.\\-)A-Za-z0-9]+)*)";
    private static String VCS_HOSTS_STRING = SW360Utils.readConfig(VCS_HOSTS,"[]");
    private static Map<String, String> KNOWN_VCS_HOSTS = parseVCSHosts(VCS_HOSTS_STRING);

    public String processURL(String url) {
        return sanitizeVCS(url);
    }

    private static String formatVCSUrl(String host, String[] urlParts) {
        if (VCS_HOSTS_STRING != null &&
                !VCS_HOSTS_STRING.equals(SW360Utils.readConfig(VCS_HOSTS, "[]"))) {
            // Config has updated, update the cache
            VCS_HOSTS_STRING = SW360Utils.readConfig(VCS_HOSTS,"[]");
            KNOWN_VCS_HOSTS = parseVCSHosts(VCS_HOSTS_STRING);
        }
        String formatString = KNOWN_VCS_HOSTS.get(host);

        int paramCount = formatString.split("%s", -1).length - 1;

        List<String> extractedParams = new ArrayList<>();
        for (int i = 3; i < urlParts.length && extractedParams.size() < paramCount; i++) {
            String part = urlParts[i].replaceAll("\\.git.*|#.*", "");

            if (part.equals("+") || part.equals("-") || CommonUtils.isNullEmptyOrWhitespace(part)) {
                break;
            }

            extractedParams.add(part);
        }

        while (extractedParams.size() < paramCount) {
            extractedParams.add("");
        }

        return String.format(formatString, extractedParams.toArray()).replaceAll("(?<!:)//+", "");
    }

    public static String sanitizeVCSByHost(String vcs, String host) {
        vcs = "https://"+vcs.substring(vcs.indexOf(host));

        try {
            URI uri = URI.create(vcs);
            String[] urlParts = uri.getPath().split("/");

            String formattedUrl = formatVCSUrl(host, urlParts);

            return formattedUrl.endsWith("/") ? formattedUrl.substring(0, formattedUrl.length() - 1) : formattedUrl;

        } catch (IllegalArgumentException e) {
            log.error("Invalid URL format: {}", vcs, e);
            return null;
        }
    }

    public static String sanitizeVCS(String vcs) {
        for (String host : KNOWN_VCS_HOSTS.keySet()) {
            if (vcs.toLowerCase().contains(host.toLowerCase())) {
                return sanitizeVCSByHost(vcs, host);
            }
        }
        return vcs;
    }

    public static String getComponentNameFromVCS(String vcsUrl, boolean isGetVendorandName) {
        String compName = vcsUrl.replaceAll(SCHEMA_PATTERN, "$1");
        String[] parts = compName.split("/");

        if (parts.length < 2) {
            return compName;
        }

        String[] pathParts = Arrays.copyOfRange(parts, 1, parts.length);
        return isGetVendorandName ? String.join("/", pathParts) : pathParts[pathParts.length - 1];
    }

    private static Map<String, String> parseVCSHosts(String propertyValue) {
        if (propertyValue == null || propertyValue.isEmpty()) {
            log.error("VCS_HOSTS property is empty");
            return new HashMap<>();
        }

        try {
            JSONArray jsonArray = new JSONArray(propertyValue);
            Map<String, String> result = new HashMap<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String entry = jsonArray.getString(i);
                String[] parts = entry.split(":", 2);
                if (parts.length == 2) {
                    result.put(parts[0], parts[1]);
                }
            }
            return result;
        } catch (JSONException e) {
            log.error("Failed to parse VCS_HOSTS config: {}", propertyValue, e);
            return new HashMap<>();
        }
    }

}
