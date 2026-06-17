/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Obligation.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.obligation;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;


@Component
@RequiredArgsConstructor
class ObligationResourceProcessor implements RepresentationModelProcessor<EntityModel<Obligation>> {

    @Override
    public EntityModel<Obligation> process(EntityModel<Obligation> resource) {
        Obligation obligation = resource.getContent();
        Link selfLink = linkTo(ObligationController.class)
                .slash("api" + ObligationController.OBLIGATION_URL + "/" + obligation.getId()).withSelfRel();
        resource.add(selfLink);
        return resource;
    }
}
