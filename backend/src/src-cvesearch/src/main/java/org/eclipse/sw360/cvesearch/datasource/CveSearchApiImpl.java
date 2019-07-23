/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.cvesearch.datasource;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.sw360.cvesearch.datasource.json.ListCveSearchJsonParser;
import org.eclipse.sw360.cvesearch.datasource.json.SingleCveSearchJsonParser;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CveSearchApiImpl implements CveSearchApi {

    private Logger log = Logger.getLogger(CveSearchApiImpl.class);

    private String host;

    private String CVE_SEARCH_SEARCH  = "search";
    private String CVE_SEARCH_CVEFOR  = "cvefor";
    private String CVE_SEARCH_BROWSE  = "browse";
    private String CVE_SEARCH_CVE     = "cve";
    public String CVE_SEARCH_WILDCARD = ".*";

    private Type META_TARGET_TYPE = new TypeToken<Map<String,Object>>(){}.getType();

    public CveSearchApiImpl(String host) {
        this.host = host;
    }

    private <T> T getParsedContentFor(String query, Function<BufferedReader,T> parser) throws IOException {
        log.debug("Execute query: " + query);
        try(InputStream is = new URL(query).openStream();
            InputStreamReader inputStreamReader = new InputStreamReader(is, Charset.forName("UTF-8"));
            BufferedReader content = new BufferedReader(inputStreamReader)) {
            return parser.apply(content);
        }
    }

    private String composeQuery(String call, String ... path) throws UnsupportedEncodingException {
        StringBuilder query = new StringBuilder(host + "/api/" + call);
        for (String p : path){
            query.append("/").append(URLEncoder.encode(p, "UTF-8"));
        }
        return query.toString();
    }

    private List<CveSearchData> getParsedCveSearchDatas(String query) throws IOException {
        return getParsedContentFor(query, new ListCveSearchJsonParser());
    }

    private CveSearchData getParsedCveSearchData(String query) throws IOException {
        return getParsedContentFor(query, new SingleCveSearchJsonParser());
    }

    private List<String> getParsedCveSearchMetadata(String query, String key) throws IOException {
        Map<String,List<String>> rawMap = getParsedContentFor(query, json -> new Gson().fromJson(json, META_TARGET_TYPE));
        if(rawMap != null && rawMap.containsKey(key)){
            return rawMap.get(key);
        }
        return new ArrayList<>();
    }

    @Override
    public List<CveSearchData> search(String vendor, String product) throws IOException {
        Function<String,String> unifyer = s -> {
            if(Strings.isNullOrEmpty(s)) {
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
        String query = composeQuery(CVE_SEARCH_BROWSE,vendorName);

        return getParsedCveSearchMetadata(query, "product");
    }

}
