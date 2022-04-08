/*
 * Copyright Siemens AG, 2020. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.changelog;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.attachment.AttachmentController;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ChangeLogController implements RepresentationModelProcessor<RepositoryLinksResource> {

    private static final Logger log = LogManager.getLogger(ChangeLogController.class);

    public static final String CHANGE_LOG_URL = "/changelog";
    @Autowired
    private Sw360ChangeLogService sw360ChangeLogService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @RequestMapping(value = CHANGE_LOG_URL + "/document/{id}", method = RequestMethod.GET)
    public ResponseEntity getChangeLogForDocument(Pageable pageable, @PathVariable("id") String docId,
            HttpServletRequest request) throws TException, URISyntaxException, PaginationParameterException,
            ResourceClassNotFoundException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        List<ChangeLogs> changelogs = sw360ChangeLogService.getChangeLogsByDocumentId(docId, sw360User);
        changelogs.stream().forEach(cl -> cl.setChangeTimestamp(cl.getChangeTimestamp().split(" ")[0]));
        PaginationResult<ChangeLogs> paginationResult = restControllerHelper.createPaginationResult(request, pageable,
                changelogs, SW360Constants.TYPE_CHANGELOG);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);

        List<EmbeddedChangeLogs> embeddedChangeLogs = paginationResult.getResources().stream()
                .map(ch -> mapper.convertValue(ch, EmbeddedChangeLogs.class)).collect(Collectors.toList());
        embeddedChangeLogs.forEach(ch -> {
            Object changes = ch.get("changes");
            if (changes != null && !((List) changes).isEmpty()) {
                List<Map<String, Object>> changesAsList = (List<Map<String, Object>>) changes;
                changesAsList.stream().filter(Objects::nonNull).forEach(changesAsMap -> {
                    Object fieldValueOld = changesAsMap.get("fieldValueOld");
                    Object fieldValueNew = changesAsMap.get("fieldValueNew");

                    try {
                        if (fieldValueOld != null && CommonUtils.isNotNullEmptyOrWhitespace(fieldValueOld.toString())) {
                            Object fieldValueOldAsMap = mapper.readValue(fieldValueOld.toString(), Object.class);
                            changesAsMap.put("fieldValueOld", fieldValueOldAsMap);
                        }

                        if (fieldValueNew != null && CommonUtils.isNotNullEmptyOrWhitespace(fieldValueNew.toString())) {

                            Object fieldValueNewAsMap = mapper.readValue(fieldValueNew.toString(), Object.class);
                            changesAsMap.put("fieldValueNew", fieldValueNewAsMap);
                        }
                    } catch (JsonProcessingException e) {
                        log.error("Error while parsing changes", e);
                    }
                });
            }
        });

        CollectionModel resources = null;
        if (CommonUtils.isNotEmpty(embeddedChangeLogs)) {
            resources = restControllerHelper.generatePagesResource(paginationResult, embeddedChangeLogs);
        }

        HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity(resources, HttpStatus.OK);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        final WebMvcLinkBuilder controllerLinkBuilder = linkTo(ChangeLogController.class);
        final Link changelogLink = Link.of(
                UriTemplate.of(controllerLinkBuilder.toUri().toString() + "/api" + CHANGE_LOG_URL + "/document/{id}"),
                "changeLogs");
        resource.add(changelogLink);
        return resource;
    }

    @Relation(collectionRelation = "sw360:changeLogs")
    private static class EmbeddedChangeLogs extends LinkedHashMap {
        public EmbeddedChangeLogs() {
        }
    }
}
