/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.integration;

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
        user.setId("admin@sw360.org");
        user.setEmail("admin@sw360.org");
        user.setFullname("John Doe");
        userList.add(user);

        user = new User();
        user.setId("jane@sw360.org");
        user.setEmail("jane@sw360.org");
        user.setFullname("Jane Doe");
        userList.add(user);

		given(this.userServiceMock.getAllUsers()
		).willReturn(userList);
	}

	@Test
	public void should_get_all_users() throws IOException {
		HttpHeaders headers = getHeaders(port);
		ResponseEntity<String> response =
				new TestRestTemplate().exchange("http://localhost:" + port + "/api/users",
						HttpMethod.GET,
						new HttpEntity<>(null, headers),
						String.class);
        assertThat(HttpStatus.OK, is(response.getStatusCode()));

		TestHelper.checkResponse(response.getBody(), "users", 2);
	}
}
