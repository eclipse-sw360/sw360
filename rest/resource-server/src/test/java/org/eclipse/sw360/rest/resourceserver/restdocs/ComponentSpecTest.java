/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import com.google.common.collect.ImmutableSet;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.component.Sw360ComponentService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
public class ComponentSpecTest extends TestRestDocsSpecBase {

    @Value("${sw360.test-user-id}")
    private String testUserId;

    @Value("${sw360.test-user-password}")
    private String testUserPassword;

    @MockBean
    private Sw360UserService userServiceMock;

    @MockBean
    private Sw360ComponentService componentServiceMock;

    private Component angularComponent;

    @Before
    public void before() throws TException {
        List<Component> componentList = new ArrayList<>();
        List<Component> componentListByName = new ArrayList<>();

        angularComponent = new Component();
        angularComponent.setId("17653524");
        angularComponent.setName("Angular");
        angularComponent.setComponentOwner("John");
        angularComponent.setDescription("Angular is a development platform for building mobile and desktop web applications.");
        angularComponent.setCreatedOn("2016-12-15");
        angularComponent.setCreatedBy("admin@sw360.org");
        angularComponent.setComponentType(ComponentType.OSS);
        angularComponent.setVendorNames(new HashSet<>(Collections.singletonList("Google")));
        angularComponent.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "john@sw360.org")));
        angularComponent.setOwnerAccountingUnit("4822");
        angularComponent.setOwnerCountry("DE");
        angularComponent.setOwnerGroup("AA BB 123 GHV2-DE");
        angularComponent.setCategories(ImmutableSet.of("java", "javascript", "sql"));
        angularComponent.setLanguages(ImmutableSet.of("EN", "DE"));
        angularComponent.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        componentList.add(angularComponent);
        componentListByName.add(angularComponent);

        Component springComponent = new Component();
        springComponent.setId("678dstzd8");
        springComponent.setName("Spring Framework");
        springComponent.setComponentOwner("Jane");
        springComponent.setDescription("The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications.");
        springComponent.setCreatedOn("2016-12-18");
        springComponent.setCreatedBy("jane@sw360.org");
        springComponent.setComponentType(ComponentType.OSS);
        springComponent.setVendorNames(new HashSet<>(Collections.singletonList("Pivotal")));
        springComponent.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        springComponent.setOwnerAccountingUnit("5661");
        springComponent.setOwnerCountry("FR");
        springComponent.setOwnerGroup("SIM-KA12");
        springComponent.setCategories(ImmutableSet.of("jdbc", "java"));
        springComponent.setLanguages(ImmutableSet.of("EN", "DE"));
        springComponent.setOperatingSystems(ImmutableSet.of("Windows", "Linux"));
        componentList.add(springComponent);

        when(this.componentServiceMock.createComponent(anyObject(), anyObject())).then(invocation -> {
            springComponent.setType("component");
            springComponent.setCreatedOn("2016-12-20");
            springComponent.setModerators(null);
            springComponent.setVendorNames(null);
            return springComponent;
        });

        given(this.componentServiceMock.getComponentsForUser(anyObject())).willReturn(componentList);
        given(this.componentServiceMock.getComponentForUserById(eq("17653524"), anyObject())).willReturn(angularComponent);
        given(this.componentServiceMock.searchComponentByName(eq(angularComponent.getName()))).willReturn(componentListByName);

        User user = new User();
        user.setId("admin@sw360.org");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");
        user.setDepartment("sw360");

        given(this.userServiceMock.getUserByEmail("admin@sw360.org")).willReturn(user);

        List<Release> releaseList = new ArrayList<>();
        Release release = new Release();
        release.setId("3765276512");
        release.setName("Angular 2.3.0");
        release.setCpeid("cpe:/a:Google:Angular:2.3.0:");
        release.setReleaseDate("2016-12-07");
        release.setVersion("2.3.0");
        release.setCreatedOn("2016-12-18");
        release.setCreatedBy("admin@sw360.org");
        release.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release.setComponentId(springComponent.getId());
        releaseList.add(release);

        Release release2 = new Release();
        release2.setId("3765276512");
        release2.setName("Angular 2.3.1");
        release2.setCpeid("cpe:/a:Google:Angular:2.3.1:");
        release2.setReleaseDate("2016-12-15");
        release2.setVersion("2.3.1");
        release2.setCreatedOn("2016-12-18");
        release2.setCreatedBy("admin@sw360.org");
        release2.setModerators(new HashSet<>(Arrays.asList("admin@sw360.org", "jane@sw360.org")));
        release2.setComponentId(springComponent.getId());
        releaseList.add(release2);

        angularComponent.setReleases(releaseList);
    }

    @Test
    public void should_document_get_components() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:components[]name").description("The name of the component"),
                                fieldWithPath("_embedded.sw360:components[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_component() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components/17653524")
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("self").description("The <<resources-components,Component resource>>")
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the component"),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("description").description("The component description"),
                                fieldWithPath("createdOn").description("The date the component was created"),
                                fieldWithPath("componentOwner").description("The owner name of the component"),
                                fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the component"),
                                fieldWithPath("ownerGroup").description("The owner group of the component"),
                                fieldWithPath("ownerCountry").description("The owner country of the component"),
                                fieldWithPath("categories").description("The component categories"),
                                fieldWithPath("languages").description("The language of the component"),
                                fieldWithPath("operatingSystems").description("The OS on which the component operates"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources"),
                                fieldWithPath("_embedded.createdBy").description("The user who created this component"),
                                fieldWithPath("_embedded.sw360:releases").description("An array of all component releases with version and link to their <<resources-releases,Releases resource>>"),
                                fieldWithPath("_embedded.sw360:moderators").description("An array of all component moderators with email and link to their <<resources-user-get,User resource>>"),
                                fieldWithPath("_embedded.sw360:vendors").description("An array of all component vendors with ful name and link to their <<resources-vendor-get,Vendor resource>>")
                        )));
    }

    @Test
    public void should_document_create_component() throws Exception {
        Map<String, String> component = new HashMap<>();

        component.put("name", "Spring Framework");
        component.put("description", "The Spring Framework provides a comprehensive programming and configuration model for modern Java-based enterprise applications.");
        component.put("createdBy", "jane@sw360.org");
        component.put("componentType", ComponentType.OSS.toString());

        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        this.mockMvc.perform(
                post("/api/components")
                        .contentType(MediaTypes.HAL_JSON)
                        .content(this.objectMapper.writeValueAsString(component))
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andDo(this.documentationHandler.document(
                        requestFields(
                                fieldWithPath("name").description("The name of the component"),
                                fieldWithPath("description").description("The component description"),
                                fieldWithPath("createdBy").description("The user who created this component"),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values()))
                        ),
                        responseFields(
                                fieldWithPath("name").description("The name of the component"),
                                fieldWithPath("componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("componentOwner").description("The owner name of the component"),
                                fieldWithPath("description").description("The component description"),
                                fieldWithPath("createdOn").description("The date the component was created"),
                                fieldWithPath("ownerAccountingUnit").description("The owner accounting unit of the component"),
                                fieldWithPath("ownerGroup").description("The owner group of the component"),
                                fieldWithPath("ownerCountry").description("The owner country of the component"),
                                fieldWithPath("categories").description("The component categories"),
                                fieldWithPath("languages").description("The language of the component"),
                                fieldWithPath("operatingSystems").description("The OS on which the component operates"),
                                fieldWithPath("_embedded.createdBy").description("The user who created this component"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_components_by_type() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components?type=" + angularComponent.getComponentType())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:components[]name").description("The name of the component"),
                                fieldWithPath("_embedded.sw360:components[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }

    @Test
    public void should_document_get_components_by_name() throws Exception {
        String accessToken = TestHelper.getAccessToken(mockMvc, testUserId, testUserPassword);
        mockMvc.perform(get("/api/components?name=" + angularComponent.getName())
                .header("Authorization", "Bearer " + accessToken)
                .accept(MediaTypes.HAL_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        links(
                                linkWithRel("curies").description("Curies are used for online documentation")
                        ),
                        responseFields(
                                fieldWithPath("_embedded.sw360:components[]name").description("The name of the component"),
                                fieldWithPath("_embedded.sw360:components[]componentType").description("The component type, possible values are: " + Arrays.asList(ComponentType.values())),
                                fieldWithPath("_embedded.sw360:components").description("An array of <<resources-components, Components resources>>"),
                                fieldWithPath("_links").description("<<resources-index-links,Links>> to other resources")
                        )));
    }
}
