/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.actuator;

import org.eclipse.sw360.rest.resourceserver.Sw360ResourceServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;

/* @DirtiesContext is necessary because the context needs to be reloaded inbetween the tests
    otherwise the responses of previous tests are taken. NoOpCacheManager through @AutoConfigureCache
    was not enough to avoid this bug.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Sw360ResourceServer.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SW360ConfigActuatorTest {

    @LocalServerPort
    private int port;

    @SpyBean
    private SW360ConfigActuator restConfigActuatorMock;

    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * Makes a request to localhost with the default server port and returns
     * the response as a response entity with type Map
     * @param endpoint endpoint that will be called
     * @return response of request
     */
    private ResponseEntity<Map> getMapResponseEntityForHealthEndpointRequest(String endpoint) {
        return this.testRestTemplate.getForEntity(
                "http://localhost:" + this.port + Sw360ResourceServer.REST_BASE_PATH + endpoint, Map.class);
    }

    @Test
    public void config_should_return_200() {
        ResponseEntity<Map> entity = getMapResponseEntityForHealthEndpointRequest("/config");

        then(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
