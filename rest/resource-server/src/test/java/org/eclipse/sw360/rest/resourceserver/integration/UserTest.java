/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.TestHelper;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;

@RunWith(SpringJUnit4ClassRunner.class)
public class UserTest extends TestIntegrationBase {

    @Value("${local.server.port}")
    private int port;

    @MockBean
    private Sw360UserService userServiceMock;

    @Before
    public void before() {
        List<User> userList = new ArrayList<>();

        User user = new User();
        user.setId("123456789");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");
        userList.add(user);

        user = new User();
        user.setId("987654321");
        user.setEmail("jane@sw360.org");
        user.setFullname("Jane Doe");
        userList.add(user);

        given(this.userServiceMock.getAllUsers()).willReturn(userList);

        given(this.userServiceMock.getUser(user.getId())).willReturn(user);
    }

    @Test
    public void should_get_all_users() throws IOException {
        ResponseEntity<String> response = sendRequest("http://localhost:" + port + "/api/users");
        assertThat(HttpStatus.OK, is(response.getStatusCode()));

        TestHelper.checkResponse(response.getBody(), "users", 2);
    }

    @Test
    public void should_get_single_user_containing_the_correct_self_link() throws IOException {
        String userLink = "http://localhost:" + port + "/api/users/byid/987654321";

        ResponseEntity<String> response = sendRequest(userLink);
        assertThat(HttpStatus.OK, is(response.getStatusCode()));

        JsonNode responseBody = new ObjectMapper().readTree(response.getBody());
        assertEquals(responseBody.get("_links").get("self").get("href").textValue(), userLink);
    }

    private ResponseEntity<String> sendRequest(String url) throws IOException {
        HttpHeaders headers = getHeaders(port);
        return new TestRestTemplate().exchange(url,
                HttpMethod.GET,
                new HttpEntity<>(null, headers),
                String.class);
    }
}
