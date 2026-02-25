/*
 * Copyright Siemens AG, 2017,2019. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.restdocs;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360CustomUserDetailsService;
import org.eclipse.sw360.rest.resourceserver.security.basic.Sw360GrantedAuthority;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Rule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@ContextConfiguration
public abstract class TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected FilterChainProxy springSecurityFilterChain;

    @Autowired
    protected ObjectMapper objectMapper;

    protected MockMvc mockMvc;

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    protected RestDocumentationResultHandler documentationHandler;

    @MockitoBean
    Sw360CustomUserDetailsService sw360CustomUserDetailsService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @MockitoBean
    protected Sw360UserService userServiceMock;

    @Before
    public void setupRestDocs() {
        this.documentationHandler = document("{method-name}",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()));

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .addFilter(springSecurityFilterChain)
                .apply(documentationConfiguration(this.restDocumentation))
                .apply(documentationConfiguration(this.restDocumentation).uris()
                        .withScheme("https")
                        .withHost("sw360.org")
                        .withPort(443))
                .alwaysDo(this.documentationHandler)
                .build();

        when(sw360CustomUserDetailsService.loadUserByUsername("admin@sw360.org")).thenReturn(new org.springframework.security.core.userdetails.User("admin@sw360.org", encoder.encode("12345"), List.of(new SimpleGrantedAuthority(Sw360GrantedAuthority.ADMIN.getAuthority()))));
    }

    public void testAttachmentUpload(String url, String id) throws Exception {
        String attachment = "{ \"filename\":\"spring-core-4.3.4.RELEASE.jar\", \"attachmentContentId\":\"2\", \"attachmentType\":\"SOURCE\", \"checkStatus\":\"ACCEPTED\", \"createdComment\":\"Uploading Sources.\" }";
        /*
         * TODO Suggestion to use better library in future, instead of MockMultipartFile
         * to have better document generation feature. below logic is used to generate
         * the correct restapi end point documentation
         */
        MockMultipartFile jsonFile = new MockMultipartFile("attachment", "", "application/json",
                new ByteArrayInputStream(attachment.getBytes()));
        MockMultipartFile[] files = new MockMultipartFile[]{jsonFile};
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(url + id + "/attachments")
                .file("file", "@/spring-core-4.3.4.RELEASE.jar".getBytes())
                .file(files[0])
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword));
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }

    public void testAttachmentUploadProject(String url, String id) throws Exception {
        String attachment = "[{ \"filename\":\"spring-core-4.3.4.RELEASE.jar\", \"attachmentType\":\"SOURCE\", \"checkStatus\":\"ACCEPTED\", \"createdComment\":\"Uploading Sources.\" }]";
        /*
         * TODO Suggestion to use better library in future, instead of MockMultipartFile
         * to have better document generation feature. below logic is used to generate
         * the correct restapi end point documentation
         */

        List<Attachment> attchList = new ArrayList<>();
        Attachment att1 = new Attachment();
        att1.setFilename("spring-core-4.3.4.RELEASE.jar");
        att1.setAttachmentContentId("2");
        att1.setAttachmentType(AttachmentType.valueOf("SOURCE"));
        att1.setCheckStatus(CheckStatus.valueOf("ACCEPTED"));
        att1.setCreatedComment("Uploading Sources.");

        attchList.add(att1);

        ObjectMapper objectMapper = new ObjectMapper();
        String attachmentJson = objectMapper.writeValueAsString(attchList);
        MockMultipartFile jsonFile = new MockMultipartFile("attachment", "", "application/json",
                new ByteArrayInputStream(attachment.getBytes()));
        MockMultipartFile[] files = new MockMultipartFile[] { jsonFile };
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.multipart(url + id + "/attachments")
                .file("file", "@/spring-core-4.3.4.RELEASE.jar".getBytes()).file(files[0])
                .param("attachments", attachmentJson).contentType(MediaType.MULTIPART_FORM_DATA)
                .header("Authorization", TestHelper.generateAuthHeader(testUserId, testUserPassword));
        this.mockMvc.perform(builder).andExpect(status().isOk()).andDo(this.documentationHandler.document());
    }
}
