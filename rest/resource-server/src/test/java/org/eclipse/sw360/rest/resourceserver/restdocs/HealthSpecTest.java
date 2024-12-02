/*
 * Copyright Bosch.IO GmbH 2020..
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.restdocs;

import org.eclipse.sw360.rest.resourceserver.SW360RestHealthIndicator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HealthSpecTest extends TestRestDocsSpecBase{

    @MockBean
    private SW360RestHealthIndicator restHealthIndicatorMock;

    @Test
    public void should_document_get_health() throws Exception {
        SW360RestHealthIndicator.RestState restState = new SW360RestHealthIndicator.RestState();
        restState.isThriftReachable = true;
        restState.isDbReachable = true;

        Health spring_health = Health.up()
                .withDetail("Rest State", restState)
                .build();
        given(this.restHealthIndicatorMock.health()).willReturn(spring_health);


        mockMvc.perform(get("/api/health")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("status").description("The overall status of the health."),
                                fieldWithPath("components.diskSpace.status").description("The status of the health of the diskspace."),
                                fieldWithPath("components.diskSpace.details.total").description("The total volume of the diskspace."),
                                fieldWithPath("components.diskSpace.details.free").description("The free space of the diskspace."),
                                fieldWithPath("components.diskSpace.details.threshold").description("The threshold of the diskspace."),
                                fieldWithPath("components.diskSpace.details.exists").description("The existance of diskspace."),
                                fieldWithPath("components.diskSpace.details.path").description("The path being monitored for disk space."),
                                fieldWithPath("components.ping.status").description("The status of the health of the specific health indicator 'SW360 Rest'.")
                        )
                ));
    }

    @Test
    public void should_document_get_health_unhealthy() throws Exception {
        SW360RestHealthIndicator.RestState restState = new SW360RestHealthIndicator.RestState();
        restState.isThriftReachable = false;
        restState.isDbReachable = true;

        Health spring_health_down = Health.down()
                .withDetail("Rest State", restState)
                .withException(new Exception("Fake"))
                .build();
        given(this.restHealthIndicatorMock.health()).willReturn(spring_health_down);

        mockMvc.perform(get("/api/health")
                    .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andDo(this.documentationHandler.document(
                        responseFields(
                                fieldWithPath("status").description("The overall status of the health."),
                                fieldWithPath("components.diskSpace.status").description("The status of the health of the diskspace."),
                                fieldWithPath("components.diskSpace.details.total").description("The total volume of the diskspace."),
                                fieldWithPath("components.diskSpace.details.free").description("The free space of the diskspace."),
                                fieldWithPath("components.diskSpace.details.threshold").description("The threshold of the diskspace."),
                                fieldWithPath("components.diskSpace.details.exists").description("The existance of diskspace."),
                                //TODO: Reverify
                                fieldWithPath("components.diskSpace.details.path").description("The path of the file."),
                                fieldWithPath("components.ping.status").description("The status of the health of the specific health indicator 'SW360 Rest'.")
                        )
                ));
    }
}
