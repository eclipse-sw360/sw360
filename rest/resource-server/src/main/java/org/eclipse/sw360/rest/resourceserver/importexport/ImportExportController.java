/*
 * Copyright Siemens AG, 2024-2025.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.importexport;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@RestController
@SecurityRequirement(name = "tokenAuth")
public class ImportExportController implements  RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String IMPORTEXPORT_URL = "/importExport";

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ImportExportService importExportService;

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ImportExportController.class).slash("api/importExport").withRel("importExport"));
        return resource;
    }

    @Operation(
            summary = "Download csv component template.",
            description = "Download csv component template.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true ),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = IMPORTEXPORT_URL + "/downloadComponentTemplate", method = RequestMethod.GET)
    public void downloadComponentTemplate( HttpServletResponse response ) throws TException, IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        importExportService.getDownloadCsvComponentTemplate(sw360User,response);
    }

    @Operation(
            summary = "Download csv attachment sample info.",
            description = "Download csv attachment sample information.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = IMPORTEXPORT_URL + "/downloadAttachmentSample", method = RequestMethod.GET)
    public void downloadAttachmentSample( HttpServletResponse response ) throws TException, IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        importExportService.getDownloadAttachmentTemplate(sw360User,response);
    }

    @Operation(
            summary = "Download csv attachment information.",
            description = "Download csv attachment information.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = IMPORTEXPORT_URL + "/downloadAttachmentInfo", method = RequestMethod.GET)
    public void downloadAttachmentInfo( HttpServletResponse response ) throws TException, IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        importExportService.getDownloadAttachmentInfo(sw360User,response);
    }

    @Operation(
            summary = "Download csv release sample.",
            description = "Download csv release link sample.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = IMPORTEXPORT_URL + "/downloadReleaseSample", method = RequestMethod.GET)
    public void downloadReleaseSample( HttpServletResponse response ) throws TException, IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        importExportService.getDownloadReleaseSample(sw360User,response);
    }

    @Operation(
            summary = "Download csv release link.",
            description = "Download csv release link information.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = IMPORTEXPORT_URL + "/downloadReleaseLink", method = RequestMethod.GET)
    public void downloadReleaseLink( HttpServletResponse response ) throws TException, IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        importExportService.getDownloadReleaseLink(sw360User,response);
    }

    @Operation(
            summary = "Download component in csv format.",
            description = "Download component.",
            tags = {"ImportExport"},
            parameters = {
                    @Parameter(name = "Accept", in = ParameterIn.HEADER, required = true ),
            }
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @RequestMapping(value = IMPORTEXPORT_URL + "/downloadComponent", method = RequestMethod.GET)
    public void downloadComponent(HttpServletResponse response) throws TException, IOException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        importExportService.getComponentDetailedExport(sw360User, response);
    }
}
