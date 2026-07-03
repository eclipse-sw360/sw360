/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.user;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.services.users.User;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;


@Component
@RequiredArgsConstructor
class UserResourceProcessor implements RepresentationModelProcessor<EntityModel<User>> {

    @Override
    public EntityModel<User> process(EntityModel<User> resource) {
        try {
            User user = resource.getContent();
            applyJsonDefaults(user);
            Link selfLink = linkTo(UserController.class).slash("api/users/byid/" + user.getId()).withSelfRel();
            resource.add(selfLink);
            return resource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Default nullable booleans to {@code false} on the way out so that the
     * REST/HAL response always includes {@code wantsMailNotification} and
     * {@code deactivated}, matching the wire format that the legacy Thrift
     * {@code User} (primitive booleans) used to produce. The converter stays
     * lossless so that round-tripping through {@code UserConverter} does not
     * pollute the underlying Thrift struct with synthetic field flags.
     */
    static void applyJsonDefaults(User user) {
        if (user == null) {
            return;
        }
        if (user.getWantsMailNotification() == null) {
            user.setWantsMailNotification(Boolean.FALSE);
        }
        if (user.getDeactivated() == null) {
            user.setDeactivated(Boolean.FALSE);
        }
    }
}
