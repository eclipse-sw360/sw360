/*
 * Copyright Siemens AG, 2023-2024.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.schedule;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ScheduleAdminController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String SCHEDULE_URL = "/schedule";
    
    @NonNull
    private final RestControllerHelper restControllerHelper;
    
    @NonNull
    private Sw360ScheduleService scheduleService;
    

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ScheduleAdminController.class).slash("api/schedule").withRel("schedule"));
        return resource;
    }

    @RequestMapping(value = SCHEDULE_URL + "/unscheduleAllServices", method = RequestMethod.DELETE)
    public ResponseEntity<?> unscheduleAllServices()throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        RequestStatus requestStatus = scheduleService.cancelAllServices(sw360User);
        HttpStatus status = HttpStatus.OK;
        return new ResponseEntity<>(requestStatus, status);
    }
}
