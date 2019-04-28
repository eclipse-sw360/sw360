/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.codescoop;

import com.codescoop.client.ClientException;
import com.codescoop.client.ClosableCodescoopClient;
import com.codescoop.client.CodescoopClient;
import com.codescoop.client.model.autocomplete.AutocompleteResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.codescoop.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Properties;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Implementation of the Thrift service
 *
 * @author alex.skorohod@codescoop.com
 */
@Component
public class CodescoopHandler implements CodescoopService.Iface {

    private static final Logger log = Logger.getLogger(CodescoopHandler.class);

    private static final String PROPERTY_FILE = "/codescoop.properties";
    private static final String PROPERTY_HOST = "codescoop.host";
    private static final String PROPERTY_CLIENT = "codescoop.client";
    private static final String PROPERTY_CREDENTIALS = "codescoop.credentials";
    private static final String PROPERTY_PROXY = "codescoop.proxy";
    private static final ObjectMapper mapper = new ObjectMapper();
    private CodescoopClient client;

    public CodescoopHandler() {
        Properties props = CommonUtils.loadProperties(CodescoopHandler.class, PROPERTY_FILE);
        String host = props.getProperty(PROPERTY_HOST, null);
        String client = props.getProperty(PROPERTY_CLIENT, null);
        String credentials = props.getProperty(PROPERTY_CREDENTIALS, null);
        String proxy = props.getProperty(PROPERTY_PROXY, null);

        log.info("Codescoop client construct : host=" + host + ", client=" + client
                + ", credentials=" + credentials + ", proxy=" + proxy);

        try {
            if (isNotBlank(host) && isNotBlank(client) && isNotBlank(credentials)) {
                this.client = ClosableCodescoopClient
                        .build()
                        .withApiUrl(host)
                        .withApiUser(client)
                        .withApiKey(credentials)
                        .withProxyHost(proxy)
                        .create();
            }
        } catch (Exception e) {
            log.warn("Codescoop init client error", e);
        } finally {
            log.warn(client == null ? "Codescoop integration disabled" : "Codescoop client was established");
        }
    }

    @Override
    public boolean isEnabled() throws TException {
        return this.client != null;
    }

    @Override
    public List<CodescoopComponent> searchComponents(CodescoopComponentSearch request) throws TException {
        try {
            List<com.codescoop.client.model.component.Component> response =
                    this.client.searchComponents(ModelTranslator.translateSearchRequest(request));
            return ModelTranslator.translateComponent(response);
        } catch (ClientException e) {
            log.error("Error search components", e);
            throw new TException("search codescoop component error", e);
        }
    }

    @Override
    public String proceedComponentsJson(String json) throws TException {
        CodescoopComponentSearch request;
        try {
            request = mapper.readValue(json, CodescoopComponentSearch.class);
        } catch (Exception e) {
            log.error("Error parse json", e);
            throw new TException("Error parse json", e);
        }

        try {
            List<CodescoopComponent> response = this.searchComponents(request);
            return mapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error codescoop response", e);
            throw new TException("Error parse codescoop response", e);
        }
    }

    @Override
    public List<CodescoopComponent> searchComponentsComposite(List<CodescoopComponentSearch> requestList)
            throws TException {
        try {
            List<com.codescoop.client.model.component.Component> response =
                    this.client.searchComponentsComposite(ModelTranslator.translateSearchRequest(requestList));
            return ModelTranslator.translateComponent(response);
        } catch (ClientException e) {
            log.error("Error search composite", e);
            throw new TException("fetch codescoop composite error");
        }
    }

    @Override
    public String proceedComponentsCompositeJson(String json) throws TException {
        List<CodescoopComponentSearch> requestList;
        try {
            requestList = mapper.readValue(json, new TypeReference<List<CodescoopComponentSearch>>() {
            });
        } catch (Exception e) {
            log.error("Error parse json", e);
            throw new TException("Error parse json", e);
        }

        try {
            List<CodescoopComponent> codescoopData = this.searchComponentsComposite(requestList);
            return mapper.writeValueAsString(codescoopData);
        } catch (Exception e) {
            log.error("Error parse codescoop response", e);
            throw new TException("Error parse codescoop response", e);
        }
    }

    @Override
    public List<CodescoopComponent> searchComponentsPurl(List<String> purlList) throws TException {
        try {
            List<com.codescoop.client.model.component.Component> response = this.client.searchComponentsPurl(purlList);
            return ModelTranslator.translateComponent(response);
        } catch (ClientException e) {
            log.error("Error search purl", e);
            throw new TException("fetch codescoop purl error");
        }
    }

    @Override
    public String proceedComponentsPurlJson(String json) throws TException {
        List<String> purlList;
        try {
            purlList = mapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            log.error("Error parse json", e);
            throw new TException("Error parse json", e);
        }

        try {
            List<CodescoopComponent> codescoopData = this.searchComponentsPurl(purlList);
            return mapper.writeValueAsString(codescoopData);
        } catch (Exception e) {
            log.error("Error parse codescoop response", e);
            throw new TException("Error parse codescoop response", e);
        }
    }

    @Override
    public List<CodescoopRelease> searchComponentReleases(CodescoopComponentSearch request) throws TException {
        try {
            List<com.codescoop.client.model.component.Release> response =
                    this.client.searchComponentReleases(ModelTranslator.translateSearchRequest(request));
            return ModelTranslator.translateRelease(response);
        } catch (ClientException e) {
            log.error("Error search components", e);
            throw new TException("fetch codescoop component error");
        }
    }

    @Override
    public String proceedComponentReleasesJson(String json) throws TException {
        CodescoopComponentSearch request;
        try {
            request = mapper.readValue(json, CodescoopComponentSearch.class);
        } catch (Exception e) {
            log.error("Error parse json", e);
            throw new TException("Error parse json", e);
        }

        try {
            List<CodescoopRelease> codescoopData = this.searchComponentReleases(request);
            return mapper.writeValueAsString(codescoopData);
        } catch (Exception e) {
            log.error("Error parse codescoop response", e);
            throw new TException("Error parse codescoop response", e);
        }
    }

    @Override
    public CodescoopAutocompleteResponse autocomplete(CodescoopAutocompleteRequest request) throws TException {
        try {
            AutocompleteResponse response =
                    this.client.autocomplete(ModelTranslator.translateAutocompleteRequest(request));
            return ModelTranslator.translateAutocompleteResponse(response);
        } catch (ClientException e) {
            log.error("Error autocomplete codescoop", e);
            throw new TException("autocomplete codescoop error", e);
        }
    }

    @Override
    public String proceedAutocompleteJson(String json) throws TException {
        CodescoopAutocompleteRequest request;
        try {
            request = mapper.readValue(json, CodescoopAutocompleteRequest.class);
        } catch (Exception e) {
            log.error("Error parse json", e);
            throw new TException("Error parse json", e);
        }

        try {
            CodescoopAutocompleteResponse codescoopData = this.autocomplete(request);
            return mapper.writeValueAsString(codescoopData);
        } catch (Exception e) {
            log.error("Error parse codescoop response", e);
            throw new TException("Error parse codescoop response", e);
        }
    }
}
