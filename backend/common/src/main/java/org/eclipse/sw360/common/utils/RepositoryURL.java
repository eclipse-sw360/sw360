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

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;

public class RepositoryURL {
    private String url;
    private static final Logger log = LogManager.getLogger(RepositoryURL.class);
    private static final String SCHEMA_PATTERN = ".+://(\\w*(?:[\\-@.\\\\s,_:/][/(.\\-)A-Za-z0-9]+)*)";
    private static final String VCS_HOSTS_STRING = SW360Constants.VCS_HOSTS;
    private static final Map<String, String> KNOWN_VCS_HOSTS = parseVCSHosts(VCS_HOSTS_STRING);
    private Set<String> redirectedUrls = new HashSet<>();

    public RepositoryURL(){}

    public RepositoryURL(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        this.url = processURL(url);
    }

    public String processURL(String url) {
        String sanitized = sanitizeVCS(url);
        return handleURLRedirection(sanitized);
    }


    private static String formatVCSUrl(String host, String[] urlParts) {
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
        String encodedVCS = URLEncoder.encode(vcs, StandardCharsets.UTF_8);

        try {
            URI uri = URI.create(encodedVCS);
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

        String domain = parts[0];
        String[] pathParts = Arrays.copyOfRange(parts, 1, parts.length);

        if (KNOWN_VCS_HOSTS.containsKey(domain)) {
            return isGetVendorandName ? String.join("/", pathParts) : pathParts[pathParts.length - 1];
        }

        return isGetVendorandName ? String.join("/", pathParts) : parts[parts.length - 1];
    }

    public String handleURLRedirection(String urlString) {
        URL url;
        HttpURLConnection connection = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            log.error("Invalid URL format: {}", e.getMessage());
            return urlString;
        }

        int redirectCount = 0;

        while (redirectCount < SW360Constants.VCS_REDIRECTION_LIMIT) {
            try {
                connection = openConnection(url);
                int status = connection.getResponseCode();

                if (status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_MOVED_TEMP || status == 308) {
                    String newUrl = connection.getHeaderField("Location");
                    connection.disconnect();

                    // Resolve relative URLs
                    url = new URL(url, newUrl);

                    if (!"https".equalsIgnoreCase(url.getProtocol())) {
                        log.error("Insecure redirection to non-HTTPS URL: {}", url);
                        return urlString;
                    }

                    redirectCount++;
                    redirectedUrls.add(urlString);
                } else {
                    connection.disconnect();
                    break;
                }
            } catch (IOException e) {
                log.error("Error during redirection handling: {}", e.getMessage());
                return urlString;
            }
            finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

        }

        if (redirectCount == 0 || redirectCount == SW360Constants.VCS_REDIRECTION_LIMIT) {
            if (redirectCount == SW360Constants.VCS_REDIRECTION_LIMIT) {
                log.error("Exceeded maximum redirect limit. Returning original URL.");
            }
            return urlString;
        }
        return sanitizeVCS(url.toString());
    }

    private static HttpURLConnection openConnection(URL url) throws IOException{
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(SW360Constants.VCS_REDIRECTION_TIMEOUT_LIMIT);
        connection.setReadTimeout(SW360Constants.VCS_REDIRECTION_TIMEOUT_LIMIT);
        return connection;
    }

    public Set<String> getRiderctedUrls(){
        return redirectedUrls;
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
