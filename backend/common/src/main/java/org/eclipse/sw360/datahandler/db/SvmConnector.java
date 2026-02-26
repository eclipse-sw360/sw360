/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/

package org.eclipse.sw360.datahandler.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * Class for accessing the SVM service APIs
 *
 * @author alex.borodin@evosoft.com
 */

public class SvmConnector {

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String MONITORING_LIST_API_URL;
    private static final String COMPONENT_MAPPINGS_API_URL;
    private static final char[] KEY_STORE_PASSPHRASE;
    private static final String KEY_STORE_FILENAME;
    private static final char[] JAVA_KEYSTORE_PASSWORD;
    private static final Logger log = Logger.getLogger(SvmConnector.class);

    public void sendProjectExportForMonitoringLists(String jsonString) throws IOException, SW360Exception {
        if(CommonUtils.isNullEmptyOrWhitespace(MONITORING_LIST_API_URL)) {
            return;
        }
        try (CloseableHttpClient httpClient = HttpClients
                .custom()
                .setSSLSocketFactory(createSslSocketFactoryForSVM())
                .build()) {

            HttpPut httpPut = new HttpPut(MONITORING_LIST_API_URL);
            httpPut.addHeader(new BasicHeader("Expect", "100-continue")); // prevents error 413 when sending large files

            MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
            entityBuilder.addPart("data", new ByteArrayBody(jsonString.getBytes(), ContentType.APPLICATION_JSON, "projects.json"));
            httpPut.setEntity(entityBuilder.build());

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPut)) {

                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    String errorMessage = "SVMML: Failed to send monitoring lists to SVM: HTTP error code : " + statusLine.getStatusCode();
                    throw new SW360Exception(errorMessage);
                }

                String response = statusLine.toString();
                log.info("SVMML SVM Server replied: " + response);
            }
        }
    }

    private SSLConnectionSocketFactory createSslSocketFactoryForSVM() throws IOException {
        try {

            KeyStore trustStore = KeyStore.getInstance("JKS");
            String javaKeystoreFilename = System
                    .getProperties()
                    .getProperty("java.home") + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts";
            try (InputStream javaKeystoreStream = new FileInputStream(javaKeystoreFilename)) {
                trustStore.load(javaKeystoreStream, JAVA_KEYSTORE_PASSWORD);
            }

            // Loading KeyStore, i.e., PKCS #12 bundle
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            Optional<InputStream> certStreamOptional = CommonUtils
                    .loadResource(this.getClass(), KEY_STORE_FILENAME)
                    .map(ByteArrayInputStream::new);
            if (!certStreamOptional.isPresent()) {
                throw new IOException("Cannot read SVM client certificate");
            }
            keyStore.load(certStreamOptional.get(), KEY_STORE_PASSPHRASE);

            SSLContext sslcontext = SSLContexts
                    .custom()
                    .setProtocol("TLSv1.2")
                    .loadTrustMaterial(trustStore, null)
                    .loadKeyMaterial(keyStore, KEY_STORE_PASSPHRASE)
                    .build();

            return new SSLConnectionSocketFactory(sslcontext, new String[]{"TLSv1.2"}, null, SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER);

        } catch (KeyStoreException | NoSuchAlgorithmException | KeyManagementException | CertificateException | UnrecoverableKeyException e) {
            throw new IOException(e);
        }

    }

    static {
        Properties props = CommonUtils.loadProperties(SvmConnector.class, PROPERTIES_FILE_PATH);

        MONITORING_LIST_API_URL  = props.getProperty("svm.sw360.api.url", "");
        COMPONENT_MAPPINGS_API_URL  = props.getProperty("svm.sw360.componentmappings.api.url", "");
        KEY_STORE_FILENAME  = props.getProperty("svm.sw360.certificate.filename", "not-configured");
        KEY_STORE_PASSPHRASE = props.getProperty("svm.sw360.certificate.passphrase", "").toCharArray();
        JAVA_KEYSTORE_PASSWORD = props.getProperty("svm.sw360.jks.password", "changeit").toCharArray();
    }


    public Map<String, Map<String, Object>> fetchComponentMappings() throws SW360Exception, IOException {
        if(CommonUtils.isNullEmptyOrWhitespace(COMPONENT_MAPPINGS_API_URL)) {
            return Collections.emptyMap();
        }
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(COMPONENT_MAPPINGS_API_URL);
            try (CloseableHttpResponse httpResponse = httpClient.execute(httpGet)) {

                StatusLine statusLine = httpResponse.getStatusLine();
                if (statusLine.getStatusCode() != 200) {
                    String errorMessage = "SVMTF: Failed to get component mappings: HTTP error code : " + statusLine.getStatusCode();
                    throw new SW360Exception(errorMessage);
                }
                ObjectMapper mapper = new ObjectMapper();
                InputStream contentStream = httpResponse.getEntity().getContent();
                HashMap<String, Map<String, Object>> componentMappings = mapper.readValue(contentStream, HashMap.class);
                String response = statusLine.toString();
                log.info("SVMTF SVM Server replied: " + response);
                log.info(String.format("SVMTF: loaded %d component mappings", componentMappings.size()));
                return componentMappings;
            }
        }
    }
}
