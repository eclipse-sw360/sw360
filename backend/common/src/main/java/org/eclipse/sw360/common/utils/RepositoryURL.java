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
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.VCS_HOSTS;

public class RepositoryURL {
    private static final Logger log = LogManager.getLogger(RepositoryURL.class);
    private static final String SCHEMA_PATTERN = ".+://(\\w*(?:[\\-@.\\\\s,_:/][/(.\\-)A-Za-z0-9]+)*)";
    private static final String VCS_HOSTS_STRING = SW360Utils.readConfig(VCS_HOSTS,"");
    private static final Map<String, String> KNOWN_VCS_HOSTS = parseVCSHosts(VCS_HOSTS_STRING);

    public String processURL(String url) {
        return sanitizeVCS(url);
    }

    private static String formatVCSUrl(String host, String[] urlParts) {
        String formatString = KNOWN_VCS_HOSTS.get(host);

        int paramCount = formatString.split("%s", -1).length - 1;

        List<String> extractedParams = new ArrayList<>();
        for (int i = 3; i < urlParts.length && extractedParams.size() < paramCount; i++) {
            String part = urlParts[i];
            if(i == urlParts.length-1) part = part.replaceAll("\\.git.*|#.*", "");

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
        return vcs.replaceAll("\\.git.*|#.*", "");
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

        return Arrays.stream(propertyValue.split(","))
                .map(entry -> entry.split(":", 2)) // Split each key-value pair
                .filter(parts -> parts.length == 2) // Ensure valid mappings
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

}
