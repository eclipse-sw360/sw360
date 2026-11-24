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

package org.eclipse.sw360.rest.resourceserver.moderationrequest;

import org.eclipse.sw360.datahandler.thrift.moderation.DocumentType;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModerationRequestMapperTest {
    @Test
    void shouldPreserveModerationRequestResponseFields() {
        ModerationRequest request = new ModerationRequest().setId("mr-1")
                .setDocumentId("project-1").setDocumentType(DocumentType.PROJECT)
                .setRequestingUser("user@example.org").setCommentRequestingUser("example");
        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();

        assertEquals(mapper.readTree("""
                {"id":"mr-1","type":"moderation","timestamp":0,"timestampOfDecision":0,
                 "documentId":"project-1","documentType":"PROJECT","requestingUser":"user@example.org",
                 "requestDocumentDelete":false,"commentRequestingUser":"example"}
                """), mapper.readTree(mapper.writeValueAsString(
                        ModerationRequestController.getModObjectMapper(request))));
    }
}
