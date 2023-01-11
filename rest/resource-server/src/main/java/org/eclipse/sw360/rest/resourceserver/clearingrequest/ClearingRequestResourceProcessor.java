/*
 * Copyright Healthineers 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.clearingrequest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class ClearingRequestResourceProcessor implements RepresentationModelProcessor<EntityModel<ClearingRequest>> {

	@Override
	public EntityModel<ClearingRequest> process(EntityModel<ClearingRequest> clearingRequest) {
		ClearingRequest request = clearingRequest.getContent();
		Link selfLink = linkTo(ClearingRequestController.class)
				.slash("api" + ClearingRequestController.CLEARING_REQUEST_URL + "/" + request.getId()).withSelfRel();
		clearingRequest.add(selfLink);
		return clearingRequest;
	}
}
