/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 2023, Siemens AG. Part of the SW360 Portal Project.
 * SPDX-FileContributor: Gaurav Mishra <mishra.gaurav@siemens.com>
 */

package org.eclipse.sw360.rest.resourceserver.moderationrequest;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
@RequiredArgsConstructor
class ModerationRequestResourceProcessor implements RepresentationModelProcessor<EntityModel<ModerationRequest>> {

    @Override
    public @NotNull EntityModel<ModerationRequest> process(EntityModel<ModerationRequest> moderationRequest) {
        ModerationRequest request = moderationRequest.getContent();
        assert request != null;
        Link selfLink = linkTo(ModerationRequestController.class)
                .slash("api" + ModerationRequestController.MODERATION_REQUEST_URL + "/" + request.getId()).withSelfRel();
        moderationRequest.add(selfLink);
        return moderationRequest;
    }
}
