/*
 * Copyright Toshiba corporation, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
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
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;


@Component
@RequiredArgsConstructor
class ObligationResourceProcessor implements ResourceProcessor<Resource<Obligation>> {

    @Override
    public Resource<Obligation> process(Resource<Obligation> resource) {
        Obligation obliagtion = resource.getContent();
        Link selfLink = linkTo(ObligationController.class)
                .slash("api" + ObligationController.OBLIGATION_URL + "/" + obliagtion.getId()).withSelfRel();
        resource.add(selfLink);
        return resource;
    }
}
