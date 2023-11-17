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

package org.eclipse.sw360.rest.resourceserver.admin.fossology;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.Map;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ConfigContainer;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FossologyAdminController implements RepresentationModelProcessor<RepositoryLinksResource> {
    public static final String FOSSOLOGY_URL = "/fossology";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    Sw360FossologyAdminServices sw360FossologyAdminServices;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(FossologyAdminController.class).slash("api" + FOSSOLOGY_URL).withRel("fossology"));
        return resource;
    }

    @PostMapping(value = FOSSOLOGY_URL + "/saveConfig", consumes  = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> saveConfigration(@RequestBody Map<String, String> request) throws TException {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            String url = request.get("url");
            String folderId = request.get("folderId");
            String token = request.get("token");
            sw360FossologyAdminServices.saveConfig(sw360User, url, folderId, token);
        } catch (Exception e) {
            throw new TException(e.getMessage());
        }
        return ResponseEntity.ok(Series.SUCCESSFUL);

    }
}
