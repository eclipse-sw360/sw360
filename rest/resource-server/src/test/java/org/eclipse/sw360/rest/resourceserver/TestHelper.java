/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 * Copyright Bosch Software Innovations GmbH, 2018.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License 2.0 which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentInfo;
import org.eclipse.sw360.rest.resourceserver.core.MultiStatus;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class TestHelper {
    private static final String AUTH_BASIC = "Basic ";

    public static String release1Id = "121831bjh1v2j";
    public static String releaseId2 = "3451831bjh1v2jxxz";
    public static String attachmentShaUsedMultipleTimes = "12345";

    public static void checkResponse(String responseBody, String linkRelation,
            int embeddedArraySize) throws IOException {
        TestHelper.checkResponse(responseBody, linkRelation, embeddedArraySize, null);
    }

    public static void checkResponse(String responseBody, String linkRelation,
            int embeddedArraySize, List<String> fields) throws IOException {
        JsonNode responseBodyJsonNode = new ObjectMapper().readTree(responseBody);

        assertThat(responseBodyJsonNode.has("_embedded"), is(true));

        JsonNode embeddedNode = responseBodyJsonNode.get("_embedded");
        assertThat(embeddedNode.has("sw360:" + linkRelation), is(true));

        JsonNode sw360UsersNode = embeddedNode.get("sw360:" + linkRelation);
        assertThat(sw360UsersNode.isArray(), is(true));
        assertThat(sw360UsersNode.size(), is(embeddedArraySize));
        if (fields != null && embeddedArraySize > 0) {
            JsonNode itemNode = sw360UsersNode.get(0);
            for (String field : fields) {
                assertTrue(itemNode.has(field));
            }
        }

        assertThat(responseBodyJsonNode.has("_links"), is(true));

        JsonNode linksNode = responseBodyJsonNode.get("_links");
        assertThat(linksNode.has("curies"), is(true));

        JsonNode curiesNode = linksNode.get("curies").get(0);
        assertThat(curiesNode.get("href").asText(), endsWith("docs/{rel}.html"));
        assertThat(curiesNode.get("name").asText(), is("sw360"));
        assertThat(curiesNode.get("templated").asBoolean(), is(true));
    }

    public static String generateAuthHeader(String user, String password) {
        String credentials = user + ":" + password;
        String credentialsEncoded =
                Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return AUTH_BASIC + credentialsEncoded;
    }

    public static void handleBatchDeleteResourcesResponse(ResponseEntity<String> response,
            String resourceId, int statusCode) throws IOException {
        handleBatchDeleteResourcesResponse(response, Collections
                .singletonList(new MultiStatus(resourceId, HttpStatus.valueOf(statusCode))));
    }

    public static void handleBatchDeleteResourcesResponse(ResponseEntity<String> response,
            List<MultiStatus> responseStatusList) throws IOException {
        assertEquals(HttpStatus.MULTI_STATUS, response.getStatusCode());

        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());
        assertThat(responseNode.isArray(), is(true));
        assertThat(responseNode.size(), is(responseStatusList.size()));

        for (int i = 0; i < responseStatusList.size(); i++) {
            MultiStatus multiStatus = responseStatusList.get(i);
            JsonNode jsonResult = responseNode.get(i);
            assertThat(jsonResult.get("status").asInt(), is(multiStatus.getStatusCode()));
            assertThat(jsonResult.get("resourceId").asText(), is(multiStatus.getResourceId()));
        }
    }

    public static List<Release> getDummyReleaseListForTest() {
        List<Release> releases = new ArrayList<>();

        Release release1 = new Release();
        release1.setName("Release 1");
        release1.setId(release1Id);
        release1.setComponentId("component123");
        release1.setVersion("1.0.4");
        release1.setCpeid("cpe:id-1231");
        release1.setMainlineState(MainlineState.MAINLINE);
        release1.setClearingState(ClearingState.APPROVED);
        releases.add(release1);

        Release release2 = new Release();
        release2.setName("Release 2");
        release2.setId(releaseId2);
        release2.setComponentId("component456");
        release2.setVersion("2.0.0");
        release2.setCpeid("cpe:id-4567");
        release2.setMainlineState(MainlineState.OPEN);
        release2.setClearingState(ClearingState.NEW_CLEARING);
        releases.add(release2);

        return releases;
    }

    public static List<Attachment> getDummyAttachmentsListForTest() {
        List<Attachment> attachments = new ArrayList<>();
        Attachment attachment1 = new Attachment();
        attachment1.setAttachmentContentId("a1");
        attachment1.setSha1(attachmentShaUsedMultipleTimes);
        attachment1.setMd5("5d41402abc4b2a76b9719d911017c592");
        attachment1.setSha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        attachment1.setFilename("Attachment 1");
        attachment1.setAttachmentType(AttachmentType.BINARY);
        attachments.add(attachment1);

        Attachment attachment2 = new Attachment();
        attachment2.setAttachmentContentId("a2");
        attachment2.setSha1(attachmentShaUsedMultipleTimes);
        attachment2.setMd5("098f6bcd4621d373cade4e832627b4f6");
        attachment2.setSha256("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3");
        attachment2.setFilename("Attachment 2");
        attachment2.setAttachmentType(AttachmentType.SOURCE);
        attachments.add(attachment2);

        return attachments;
    }

    public static AttachmentContent getDummyAttachmentContent() {
        AttachmentContent content = new AttachmentContent();
        content.setContentType(MediaType.APPLICATION_PDF_VALUE);
        content.setFilename("dummy.txt");
        return content;
    }

    public static List<AttachmentInfo> getDummyAttachmentInfoListForTest() {
        Source source1 = new Source(Source._Fields.RELEASE_ID, release1Id);
        Source source2 = new Source(Source._Fields.RELEASE_ID, releaseId2);

        List<AttachmentInfo> attachmentInfos = new ArrayList<>();
        AttachmentInfo attachmentInfo1 =
                new AttachmentInfo(getDummyAttachmentsListForTest().get(0));
        attachmentInfo1.setOwner(source1);
        attachmentInfos.add(attachmentInfo1);

        AttachmentInfo attachmentInfo2 =
                new AttachmentInfo(getDummyAttachmentsListForTest().get(1));
        attachmentInfo2.setOwner(source2);
        attachmentInfos.add(attachmentInfo2);

        return attachmentInfos;
    }

    public static User getTestUser() {
        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");
        user.setUserGroup(UserGroup.ADMIN);
        return user;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OAuthToken {
        @JsonProperty("access_token")
        public String accessToken;
    }

    public static @NotNull ClearingInformation getClearingInformation() {
        ClearingInformation clearingInformation = new ClearingInformation();
        clearingInformation.setComment("Comment");
        clearingInformation.setEvaluated("Evaluated");
        clearingInformation.setProcStart("Proc Start");
        clearingInformation.setRequestID("REQ-111");
        clearingInformation.setScanned("Scanned");
        clearingInformation.setClearingStandard("Clearing Standard");
        clearingInformation.setCountOfSecurityVn(2);
        clearingInformation.setComponentClearingReport(true);
        clearingInformation.setComponentClearingReportIsSet(false);
        clearingInformation.setExternalUrl("https://external.url");
        return clearingInformation;
    }

}
