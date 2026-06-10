/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.datasource;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.eclipse.sw360.cvesearch.datasource.json.ListCveSearchJsonParser;
import org.eclipse.sw360.cvesearch.datasource.json.SingleCveSearchJsonParser;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CveSearchApiImpl implements CveSearchApi {

    private static final Logger log = LogManager.getLogger(CveSearchApiImpl.class);

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private final String host;

    private final String CVE_SEARCH_SEARCH = "search";
    private final String CVE_SEARCH_CVEFOR = "cvefor";
    private final String CVE_SEARCH_BROWSE = "browse";
    private final String CVE_SEARCH_CVE = "cve";
    public String CVE_SEARCH_WILDCARD = ".*";

    private final Type META_TARGET_TYPE = new TypeToken<Map<String, Object>>() {}.getType();
    private final Gson gson = new Gson();

    public CveSearchApiImpl(String host) {
        this.host = host;
    }

    private String fetchResponseBody(String query) throws IOException {
        log.debug("Execute query: {}", query);
        HttpURLConnection connection;
        try {
            connection = (HttpURLConnection) new URI(query).toURL().openConnection();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Accept", "application/json");

        int status = connection.getResponseCode();
        String body = readStream(status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream());

        if (status < 200 || status >= 300) {
            throw new IOException("CVE search API returned HTTP " + status + " for " + query
                    + (Strings.isNullOrEmpty(body) ? "" : ": " + abbreviate(body)));
        }
        return body;
    }

    private static String readStream(java.io.InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String abbreviate(String body) {
        String trimmed = body.strip();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "..." : trimmed;
    }

    private <T> T parseBody(String body, Function<BufferedReader, T> parser) throws IOException {
        if (Strings.isNullOrEmpty(body)) {
            return parser.apply(new BufferedReader(new StringReader("[]")));
        }
        try {
            return parser.apply(new BufferedReader(new StringReader(body)));
        } catch (RuntimeException e) {
            throw new IOException("Failed to parse CVE search API JSON response", e);
        }
    }

    private String composeQuery(String call, String... path) throws UnsupportedEncodingException {
        StringBuilder query = new StringBuilder(host + "/api/" + call);
        for (String p : path) {
            query.append("/").append(URLEncoder.encode(p, StandardCharsets.UTF_8));
        }
        return query.toString();
    }

    private List<CveSearchData> getParsedCveSearchDatas(String query) throws IOException {
        return parseBody(fetchResponseBody(query), new ListCveSearchJsonParser());
    }

    private CveSearchData getParsedCveSearchData(String query) throws IOException {
        return parseBody(fetchResponseBody(query), new SingleCveSearchJsonParser());
    }

    private List<String> getParsedCveSearchMetadata(String query, String key) throws IOException {
        String body = fetchResponseBody(query);
        if (Strings.isNullOrEmpty(body)) {
            return new ArrayList<>();
        }
        try {
            JsonElement element = JsonParser.parseString(body);
            if (element.isJsonObject()) {
                JsonObject object = element.getAsJsonObject();
                if (object.has(key) && object.get(key).isJsonArray()) {
                    return toStringList(object.get(key).getAsJsonArray());
                }
                Map<String, List<String>> rawMap = gson.fromJson(element, META_TARGET_TYPE);
                if (rawMap != null && rawMap.containsKey(key)) {
                    return rawMap.get(key);
                }
            } else if (element.isJsonArray()) {
                return toStringList(element.getAsJsonArray());
            }
        } catch (JsonSyntaxException e) {
            throw new IOException("Invalid JSON from CVE search API: " + query, e);
        }
        return new ArrayList<>();
    }

    private static List<String> toStringList(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .collect(Collectors.toList());
    }

    @Override
    public List<CveSearchData> search(String vendor, String product) throws IOException {
        Function<String, String> unifyer = s -> {
            if (Strings.isNullOrEmpty(s)) {
                return CVE_SEARCH_WILDCARD;
            }
            return CommonUtils.nullToEmptyString(s).replace(" ", "_").toLowerCase();
        };

        String query = composeQuery(CVE_SEARCH_SEARCH,
                unifyer.apply(vendor),
                unifyer.apply(product));

        return getParsedCveSearchDatas(query);
    }

    @Override
    public List<CveSearchData> cvefor(String cpe) throws IOException {
        String query = composeQuery(CVE_SEARCH_CVEFOR, cpe.toLowerCase());
        return getParsedCveSearchDatas(query);
    }

    @Override
    public CveSearchData cve(String cve) throws IOException {
        String query = composeQuery(CVE_SEARCH_CVE, cve.toUpperCase());
        return getParsedCveSearchData(query);
    }

    @Override
    public List<String> allVendorNames() throws IOException {
        String query = composeQuery(CVE_SEARCH_BROWSE);
        return getParsedCveSearchMetadata(query, "vendor");
    }

    @Override
    public List<String> allProductsOfVendor(String vendorName) throws IOException {
        String query = composeQuery(CVE_SEARCH_BROWSE, vendorName);
        return getParsedCveSearchMetadata(query, "product");
    }
}
