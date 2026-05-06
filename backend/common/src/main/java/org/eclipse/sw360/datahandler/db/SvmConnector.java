/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.datahandler.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.mime.ByteArrayBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Class for accessing the SVM service APIs.
 *
 * <p>SSL/TLS configuration (trust store and client certificate) is handled by
 * {@link SvmHttpClientFactory}. Refer to its Javadoc for the trust-store lookup order
 * and the {@code sw360.properties} keys to configure a custom trust store.
 *
 * @author alex.borodin@evosoft.com
 */
public class SvmConnector {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";

    private static final String MONITORING_LIST_API_URL;
    private static final String COMPONENT_MAPPINGS_API_URL;

    private static final Logger log = LogManager.getLogger(SvmConnector.class);

    static {
        Properties props = CommonUtils.loadProperties(SvmConnector.class, PROPERTIES_FILE_PATH);

        MONITORING_LIST_API_URL    = props.getProperty("svm.sw360.api.url", "");
        COMPONENT_MAPPINGS_API_URL = props.getProperty("svm.sw360.componentmappings.api.url", "");
    }

    public void sendProjectExportForMonitoringLists(String jsonString) throws IOException, SW360Exception {
        if (CommonUtils.isNullEmptyOrWhitespace(MONITORING_LIST_API_URL)) {
            return;
        }

        HttpPut httpPut = new HttpPut(MONITORING_LIST_API_URL);
        httpPut.addHeader(new BasicHeader("Expect", "100-continue")); // prevents error 413 when sending large files

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.addPart("data", new ByteArrayBody(jsonString.getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON, "projects.json"));
        httpPut.setEntity(entityBuilder.build());

        CloseableHttpClient httpClient = SvmHttpClientFactory.getMutualTlsClient();
        int[] codeRef = {0};
        String[] reasonRef = {null};
        httpClient.execute(httpPut, response -> {
            codeRef[0]   = response.getCode();
            reasonRef[0] = response.getReasonPhrase();
            return null;
        });
        if (codeRef[0] != 200) {
            String errorMessage = "SVMML: Failed to send monitoring lists to SVM: HTTP error code : " + codeRef[0];
            throw new SW360Exception(errorMessage);
        }
        log.info("SVMML SVM Server replied: {} {}", codeRef[0], reasonRef[0]);
    }

    public Map<String, Map<String, Object>> fetchComponentMappings() throws SW360Exception, IOException {
        if (CommonUtils.isNullEmptyOrWhitespace(COMPONENT_MAPPINGS_API_URL)) {
            return Collections.emptyMap();
        }

        CloseableHttpClient httpClient = SvmHttpClientFactory.getTrustedClient();
        HttpGet httpGet = new HttpGet(COMPONENT_MAPPINGS_API_URL);
        int[] codeRef = {0};
        String[] reasonRef = {null};
        Map<String, Map<String, Object>> componentMappings = httpClient.execute(httpGet, response -> {
            codeRef[0]   = response.getCode();
            reasonRef[0] = response.getReasonPhrase();
            if (codeRef[0] != 200) {
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            try (InputStream contentStream = response.getEntity().getContent()) {
                return mapper.readValue(contentStream,
                        mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Map.class));
            }
        });
        if (codeRef[0] != 200) {
            String errorMessage = "SVMTF: Failed to get component mappings: HTTP error code : " + codeRef[0];
            throw new SW360Exception(errorMessage);
        }
        log.info("SVMTF SVM Server replied: {} {}", codeRef[0], reasonRef[0]);
        log.info("SVMTF: loaded {} component mappings", componentMappings.size());
        return componentMappings;
    }
}
