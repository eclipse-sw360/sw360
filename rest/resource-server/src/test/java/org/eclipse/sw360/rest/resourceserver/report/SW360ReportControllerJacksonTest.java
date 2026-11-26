/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.report;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.projects.SW360ReportBean;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.MAIL_REQUEST_FOR_REPORT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class SW360ReportControllerJacksonTest {
    @Test
    void shouldPreserveProjectReportNotificationResponse() throws Exception {
        SW360ReportService service = mock(SW360ReportService.class);
        SW360ReportController controller = new SW360ReportController(
                mock(RestControllerHelper.class), service, JsonMapper.builderWithJackson2Defaults().build());
        MockHttpServletResponse response = new MockHttpServletResponse();
        SW360ReportBean report = new SW360ReportBean();
        User user = new User();

        try (MockedStatic<SW360Utils> config = mockStatic(SW360Utils.class)) {
            config.when(() -> SW360Utils.readConfig(MAIL_REQUEST_FOR_REPORT, false)).thenReturn(true);
            ReflectionTestUtils.invokeMethod(controller, "getProjectReports",
                    response, user, "project", "project-id", report);
        }

        assertEquals("{\"response\":\"The downloaded report link will be send to the end user.\"}",
                response.getContentAsString());
        verify(service).getUploadedProjectPath(user, report, "project-id");
    }
}
