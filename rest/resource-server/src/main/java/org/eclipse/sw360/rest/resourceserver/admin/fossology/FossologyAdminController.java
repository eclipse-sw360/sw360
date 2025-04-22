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
import java.util.concurrent.TimeUnit;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.StringToClassMapItem;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
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

    @Operation(
            summary = "Save the FOSSology service configuration.",
            description = "Save the FOSSology service configuration.",
            tags = {"Admin"}
    )
    @PostMapping(value = FOSSOLOGY_URL + "/saveConfig", consumes  = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> saveConfigration(
            @Parameter(description = "Request body containing the configuration parameters. The parameters are:\n" +
                    "`url`: The URL of the FOSSology server.\n" +
                    "`folderId`: The ID of the folder in FOSSology.\n" +
                    "`token`: The access token for FOSSology.\n" +
                    "`downloadTimeout`: (Optional) The timeout for attachment download from CouchDB.\n" +
                    "`downloadTimeoutUnit`: (Required with `downloadTimeout`) The unit of the download timeout (e.g., `MINUTES`, `SECONDS`).",
                    schema = @Schema(
                            type = "object",
                            properties = {
                                    @StringToClassMapItem(key = "url", value = String.class),
                                    @StringToClassMapItem(key = "folderId", value = String.class),
                                    @StringToClassMapItem(key = "token", value = String.class),
                                    @StringToClassMapItem(key = "downloadTimeout", value = String.class),
                                    @StringToClassMapItem(key = "downloadTimeoutUnit", value = TimeUnit.class)
                            },
                            example = """
                                    {
                                      "url": "https://fossology.com/repo/api/v1/",
                                      "folderId": "2",
                                      "token": "dead.beef",
                                      "downloadTimeout": "2",
                                      "downloadTimeoutUnit": "MINUTES"
                                    }""",
                            requiredProperties = {"url", "folderId", "token"}
                    )
            )
            @RequestBody Map<String, String> request
    ) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            String url = request.get("url");
            String folderId = request.get("folderId");
            String token = request.get("token");
            String downloadTimeout = request.get("downloadTimeout");
            String downloadTimeoutUnit = request.get("downloadTimeoutUnit");
            sw360FossologyAdminServices.saveConfig(sw360User, url, folderId, token,
                    downloadTimeout, downloadTimeoutUnit);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
        return ResponseEntity.ok(Series.SUCCESSFUL);
    }

    @Operation(
            summary = "Check the FOSSology server connection.",
            description = "Make a test call and check the FOSSology server connection.",
            tags = {"Admin"}
    )
    @RequestMapping(value = FOSSOLOGY_URL + "/reServerConnection", method = RequestMethod.GET)
    public ResponseEntity<?> checkServerConnection() throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            sw360FossologyAdminServices.serverConnection(sw360User);
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
        return ResponseEntity.ok(Series.SUCCESSFUL);

    }
}
