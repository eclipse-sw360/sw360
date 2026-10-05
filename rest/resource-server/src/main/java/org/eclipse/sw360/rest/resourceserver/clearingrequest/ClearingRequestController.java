/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.clearingrequest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.resourcelists.PaginationResult;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.eclipse.sw360.rest.resourceserver.core.OpenAPIPaginationHelper;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.moderationrequest.Sw360ModerationRequestService;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@BasePathAwareController
@RequiredArgsConstructor
@RestController
@SecurityRequirement(name = "tokenAuth")
@SecurityRequirement(name = "basic")
public class ClearingRequestController implements RepresentationModelProcessor<RepositoryLinksResource> {

    public static final String CLEARING_REQUEST_URL = "/clearingrequest";

    public static final String CLEARING_REQUESTS_URL = "/clearingrequests";

    private static final Logger log = LogManager.getLogger(ClearingRequestController.class);

    @Autowired
    private Sw360ClearingRequestService sw360ClearingRequestService;

    @NonNull
    private final RestControllerHelper restControllerHelper;

    @NonNull
    private final Sw360ProjectService projectService;

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    @NonNull
    private final Sw360ModerationRequestService moderationRequestService;

    @Operation(
            summary = "Get clearing request by id.",
            description = "Get a clearing request by id.",
            tags = {"ClearingRequest"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clearing request successfully retrieved."),
            @ApiResponse(responseCode = "204", description = "No clearing request found.",
                content = @Content)
    })
    @GetMapping(value = CLEARING_REQUEST_URL + "/{id}")
    public ResponseEntity<EntityModel<ClearingRequest>> getClearingRequestById(
            @Parameter(description = "id of the clearing request")
            @PathVariable("id") String docId
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestById(docId, sw360User);
        HalResource<ClearingRequest> halClearingRequest = createHalClearingRequestWithAllDetails(clearingRequest, sw360User, true);
        HttpStatus status = halClearingRequest == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(halClearingRequest, status);
    }

    @Operation(
            summary = "Get the ClearingRequest based on the project id.",
            description = "Get the ClearingRequest based on the project id.",
            tags = {"ClearingRequest"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clearing request for project successfully retrieved."),
            @ApiResponse(responseCode = "204", description = "No clearing request found for this project.",
                content = @Content)
    })
    @GetMapping(value = CLEARING_REQUEST_URL + "/project/{id}")
    public ResponseEntity<EntityModel<ClearingRequest>> getClearingRequestByProjectId(
            @Parameter(description = "id of the project")
            @PathVariable("id") String projectId
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestByProjectId(projectId, sw360User);
        HalResource<ClearingRequest> halClearingRequest = createHalClearingRequestWithAllDetails(clearingRequest, sw360User, true);
        HttpStatus status = halClearingRequest == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
        return new ResponseEntity<>(halClearingRequest, status);
    }

    private HalResource<ClearingRequest> createHalClearingRequestWithAllDetails(
            ClearingRequest clearingRequest, User sw360User, boolean isSingleRequest
    ) {
        HalResource<ClearingRequest> halClearingRequest = new HalResource<>(clearingRequest);
        if (StringUtils.hasText(clearingRequest.projectId)) {
            try{
                Project project = projectService.getProjectForUserById(clearingRequest.getProjectId(), sw360User);
                if (isSingleRequest) {
                    // Only process comments for single request view
                    sw360ClearingRequestService.convertTimestampAndEmail(clearingRequest);
                }
                Project projectWithClearingInfo = projectService.getClearingInfo(project, sw360User);
                restControllerHelper.addEmbeddedReleaseDetails(halClearingRequest, projectWithClearingInfo);
                restControllerHelper.addEmbeddedProjectDTO(halClearingRequest, project);
            }catch (Exception e){
                log.info("Clearing request with id: {} is linked to project that has restricted visibility.", clearingRequest.getId());
                return null;
            }
        }
        User requestingUser = restControllerHelper.getUserByEmail(clearingRequest.getRequestingUser());
        restControllerHelper.addEmbeddedUser(halClearingRequest, requestingUser, "requestingUser");
        if(isSingleRequest){
            User clearingTeam = restControllerHelper.getUserByEmail(clearingRequest.getClearingTeam());
            restControllerHelper.addEmbeddedUser(halClearingRequest, clearingTeam, "clearingTeam");
        }
        if(clearingRequest.getClearingState().equals(ClearingRequestState.CLOSED) || clearingRequest.getClearingState().equals(ClearingRequestState.REJECTED)){
            restControllerHelper.addEmbeddedTimestampOfDecision(halClearingRequest,clearingRequest.getTimestampOfDecision());
        }
        restControllerHelper.addEmbeddedDatesClearingRequest(halClearingRequest, clearingRequest, isSingleRequest);
        return halClearingRequest;
    }


    @Operation(
            summary = "List all clearing requests visible to the user.",
            description = "List all clearing requests visible to user",
            tags = {"ClearingRequest"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clearing requests successfully retrieved."),
            @ApiResponse(responseCode = "204", description = "No clearing requests found.",
                content = @Content)
    })
    @GetMapping(value = CLEARING_REQUESTS_URL)
    public ResponseEntity<CollectionModel<?>> getClearingRequests(
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable,
            @Parameter(description = "Project ID to filter")
            @RequestParam(value = "projectId", required = false) String projectId,
            @Parameter(description = "Status of the clearing request (comma-separated for multiple values, e.g., 'NEW,ACCEPTED,IN_PROGRESS')")
            @RequestParam(value = "status", required = false) Set<ClearingRequestState> status,
            @Parameter(description = "Priority of the clearing request (comma-separated for multiple values, e.g., 'LOW,HIGH')")
            @RequestParam(value = "priority", required = false) Set<ClearingRequestPriority> priority,
            @Parameter(description = "Type of the clearing request. Possible values are: DEEP, HIGH")
            @RequestParam(value = "clearingType", required = false) String clearingType,
            @Parameter(description = "Requesting user email to filter")
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @Parameter(description = "BA-BL / group of the project the clearing request belongs to. "
                    + "Matched exactly against the project business unit. "
                    + "The selectable values are available from /api/projects/groups.")
            @RequestParam(value = "group", required = false) String group,
            @Parameter(description = "Date clearing request was created on (timestamp).")
            @RequestParam(value = "createdOn", required = false) String createdOn,
            @Parameter(description = "Date field to apply the date range filter on. Defaults to 'createdOn' when a range is given.",
                    schema = @Schema(allowableValues = {"createdOn", "requestedClearingDate", "agreedClearingDate", "modifiedOn", "closedOn"}))
            @RequestParam(value = "dateField", required = false) String dateField,
            @Parameter(description = "Inclusive start of the date range in ISO format (yyyy-MM-dd). Cannot be combined with 'days'.",
                    example = "2026-08-01")
            @RequestParam(value = "fromDate", required = false) String fromDate,
            @Parameter(description = "Inclusive end of the date range in ISO format (yyyy-MM-dd). Cannot be combined with 'days'.",
                    example = "2026-08-31")
            @RequestParam(value = "toDate", required = false) String toDate,
            @Parameter(description = "Relative date range in days: 0 = today, negative = the last N days up to today, "
                    + "positive = today up to the next N days. Cannot be combined with 'fromDate'/'toDate'. "
                    + "Positive values are only valid for 'requestedClearingDate' and 'agreedClearingDate'.",
                    example = "-30")
            @RequestParam(value = "days", required = false) Integer days,
            HttpServletRequest request
    ) throws TException {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);

        Map<PaginationData, List<ClearingRequest>> paginatedClearingRequests = null;
        Map<String, Set<String>> filterMap = getFilterMapForClearingRequests(projectId, status, priority, clearingType,
                createdBy, group, createdOn, dateField, fromDate, toDate, days);

        try {
            if (filterMap.isEmpty()) {
                paginatedClearingRequests = sw360ClearingRequestService.getRecentClearingRequestsWithPagination(sw360User, pageable);
            } else {
                paginatedClearingRequests = sw360ClearingRequestService.searchClearingRequestsByFilters(sw360User, filterMap, pageable);
            }

            PaginationResult<ClearingRequest> paginationResult;
            paginationResult = restControllerHelper.paginationResultFromPaginatedList(
                    request, pageable, paginatedClearingRequests);

            final List<EntityModel<ClearingRequest>> clearingRequestResources = new ArrayList<>();
            for (ClearingRequest cr : paginationResult.getResources()) {
                ClearingRequest embeddedCR = restControllerHelper.convertToEmbeddedClearingRequest(cr);
                HalResource<ClearingRequest> halResource = createHalClearingRequestWithAllDetails(embeddedCR, sw360User, false);
                if(halResource != null) clearingRequestResources.add(halResource);
            }

            CollectionModel<EntityModel<ClearingRequest>> resources;
            if (clearingRequestResources.isEmpty()) {
                resources = restControllerHelper.emptyPageResource(ClearingRequest.class, paginationResult);
            } else {
                resources = restControllerHelper.generatePagesResource(paginationResult, clearingRequestResources);
            }
            HttpStatus status1 = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
            return new ResponseEntity<>(resources, status1);

        } catch (ResourceNotFoundException | AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    private Map<String, Set<String>> getFilterMapForClearingRequests(
            String projectId, Set<ClearingRequestState> status, Set<ClearingRequestPriority> priority, String clearingType,
            String createdBy, String group, String createdOn, String dateField, String fromDate, String toDate, Integer days) {
        Map<String, Set<String>> filterMap = new HashMap<>();

        if (CommonUtils.isNotNullEmptyOrWhitespace(projectId)) {
            filterMap.put(ClearingRequest._Fields.PROJECT_ID.getFieldName(), Collections.singleton(projectId));
        }
        if (status != null && !status.isEmpty()) {
            Set<String> statusValues = status.stream()
                    .map(ClearingRequestState::toString)
                    .collect(Collectors.toSet());
            filterMap.put(ClearingRequest._Fields.CLEARING_STATE.getFieldName(), statusValues);
        }
        if (priority != null && !priority.isEmpty()) {
            Set<String> priorityValues = priority.stream()
                    .map(ClearingRequestPriority::toString)
                    .collect(Collectors.toSet());
            filterMap.put(ClearingRequest._Fields.PRIORITY.getFieldName(), priorityValues);
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(clearingType)) {
            String normalizedClearingType = clearingType.trim().toUpperCase();
            try {
                ClearingRequestType.valueOf(normalizedClearingType);
            } catch (IllegalArgumentException e) {
                throw new BadRequestClientException(
                        "Invalid value for clearingType: " + clearingType + ". Allowed values are " + Arrays.toString(ClearingRequestType.values()));
            }
            filterMap.put(ClearingRequest._Fields.CLEARING_TYPE.getFieldName(), Collections.singleton(normalizedClearingType));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(createdBy)) {
            filterMap.put(ClearingRequest._Fields.REQUESTING_USER.getFieldName(), Collections.singleton(createdBy));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(group)) {
            filterMap.put(ClearingRequest._Fields.PROJECT_BU.getFieldName(), Collections.singleton(group.trim()));
        }
        if (CommonUtils.isNotNullEmptyOrWhitespace(createdOn)) {
            filterMap.put(ClearingRequest._Fields.TIMESTAMP.getFieldName(), Collections.singleton(createdOn));
        }

        addDateRangeFilter(filterMap, dateField, fromDate, toDate, days);
        return filterMap;
    }

    /**
     * Date fields of a {@link ClearingRequest} that can be filtered on by a date range,
     * mapping the API parameter value to the underlying document field.
     */
    private enum ClearingRequestDateField {
        CREATED_ON("createdOn", ClearingRequest._Fields.TIMESTAMP, false, false),
        REQUESTED_CLEARING_DATE("requestedClearingDate", ClearingRequest._Fields.REQUESTED_CLEARING_DATE, true, true),
        AGREED_CLEARING_DATE("agreedClearingDate", ClearingRequest._Fields.AGREED_CLEARING_DATE, true, true),
        MODIFIED_ON("modifiedOn", ClearingRequest._Fields.MODIFIED_ON, false, false),
        CLOSED_ON("closedOn", ClearingRequest._Fields.TIMESTAMP_OF_DECISION, false, false);

        private final String parameterValue;
        private final ClearingRequest._Fields field;

        /** Whether the field is persisted as an ISO {@code yyyy-MM-dd} string rather than epoch millis. */
        private final boolean isoDateString;

        /** Whether the field may legitimately hold a date in the future. */
        private final boolean futureAllowed;

        ClearingRequestDateField(String parameterValue, ClearingRequest._Fields field,
                                 boolean isoDateString, boolean futureAllowed) {
            this.parameterValue = parameterValue;
            this.field = field;
            this.isoDateString = isoDateString;
            this.futureAllowed = futureAllowed;
        }

        private static ClearingRequestDateField from(String value) {
            return Arrays.stream(values())
                    .filter(f -> f.parameterValue.equalsIgnoreCase(value.trim()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestClientException("Invalid value for dateField: " + value
                            + ". Allowed values are " + allowedValues() + "."));
        }

        private static String allowedValues() {
            return Arrays.stream(values()).map(f -> f.parameterValue).collect(Collectors.joining(", "));
        }
    }

    /**
     * Validates the date range parameters and, if a range was requested, writes the resolved
     * inclusive bounds into the filter map using the sentinel keys understood by
     * {@code ClearingRequestRepository}.
     * @param filterMap Filter map to populate
     * @param dateFieldParam Date field to filter on, defaults to {@code createdOn}
     * @param fromDate Inclusive lower bound as ISO {@code yyyy-MM-dd}, may be null
     * @param toDate Inclusive upper bound as ISO {@code yyyy-MM-dd}, may be null
     * @param days Relative range in days, may be null. Mutually exclusive with fromDate/toDate
     */
    private void addDateRangeFilter(Map<String, Set<String>> filterMap, String dateFieldParam,
                                    String fromDate, String toDate, Integer days) {
        boolean hasExplicitRange = CommonUtils.isNotNullEmptyOrWhitespace(fromDate)
                || CommonUtils.isNotNullEmptyOrWhitespace(toDate);
        if (days != null && hasExplicitRange) {
            throw new BadRequestClientException(
                    "The 'days' parameter cannot be combined with 'fromDate' or 'toDate'.");
        }
        if (days == null && !hasExplicitRange) {
            // dateField without a range is a no-op, nothing to filter on.
            return;
        }

        ClearingRequestDateField dateField = CommonUtils.isNotNullEmptyOrWhitespace(dateFieldParam)
                ? ClearingRequestDateField.from(dateFieldParam)
                : ClearingRequestDateField.CREATED_ON;

        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to;
        if (days != null) {
            if (days > 0 && !dateField.futureAllowed) {
                throw new BadRequestClientException("A future date range is not allowed for dateField '"
                        + dateField.parameterValue + "'. Use a value of 'days' that is less than or equal to 0.");
            }
            from = days < 0 ? today.plusDays(days) : today;
            to = days > 0 ? today.plusDays(days) : today;
        } else {
            from = parseIsoDate(fromDate, "fromDate");
            to = parseIsoDate(toDate, "toDate");
            if (from != null && to != null && from.isAfter(to)) {
                throw new BadRequestClientException("'fromDate' must not be after 'toDate'.");
            }
            if (!dateField.futureAllowed && ((from != null && from.isAfter(today)) || (to != null && to.isAfter(today)))) {
                throw new BadRequestClientException("A future date range is not allowed for dateField '"
                        + dateField.parameterValue + "'.");
            }
        }

        filterMap.put(SW360Constants.CLEARING_REQUEST_DATE_FIELD_KEY,
                Collections.singleton(dateField.field.getFieldName()));
        if (from != null) {
            filterMap.put(SW360Constants.CLEARING_REQUEST_DATE_FROM_KEY,
                    Collections.singleton(formatLowerBound(from, dateField)));
        }
        if (to != null) {
            filterMap.put(SW360Constants.CLEARING_REQUEST_DATE_TO_KEY,
                    Collections.singleton(formatUpperBound(to, dateField)));
        }
    }

    private LocalDate parseIsoDate(String value, String parameterName) {
        if (!CommonUtils.isNotNullEmptyOrWhitespace(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new BadRequestClientException(
                    "Invalid value for " + parameterName + ": " + value + ". Expected format is yyyy-MM-dd.");
        }
    }

    /**
     * Renders the inclusive lower bound: the ISO date itself for string fields,
     * or the epoch millis at the very start of that day for timestamp fields.
     */
    private String formatLowerBound(LocalDate date, ClearingRequestDateField dateField) {
        if (dateField.isoDateString) {
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return String.valueOf(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    /**
     * Renders the inclusive upper bound: the ISO date itself for string fields,
     * or the epoch millis at the very end of that day for timestamp fields.
     */
    private String formatUpperBound(LocalDate date, ClearingRequestDateField dateField) {
        if (dateField.isoDateString) {
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        return String.valueOf(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1);
    }

    @Operation(
            summary = "Get comments for a specific clearing request",
            description = "Fetch a paginated list of comments associated with the given clearing request ID.",
            tags = {"ClearingRequest"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the comments",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Comment.class))),
            @ApiResponse(responseCode = "404", description = "Clearing request not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping(value = CLEARING_REQUEST_URL + "/{id}/comments")
    public ResponseEntity<CollectionModel<?>> getCommentsByClearingRequestId(
            @PathVariable("id") String crId,
            HttpServletRequest request,
            @Parameter(description = "Pagination requests", schema = @Schema(implementation = OpenAPIPaginationHelper.class))
            Pageable pageable
    ) throws SW360Exception {
        User sw360User = restControllerHelper.getSw360UserFromAuthentication();
        restControllerHelper.throwIfSecurityUser(sw360User);
        try {
            ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestById(crId, sw360User);

            List<Comment> commentList = clearingRequest.getComments().stream().sorted((c1, c2) -> Long.compare(c2.getCommentedOn(), c1.getCommentedOn()))
                    .collect(Collectors.toList());
            PaginationResult<Comment> paginationResult = restControllerHelper.createPaginationResult(request, pageable, commentList, SW360Constants.TYPE_COMMENT);
            final List<EntityModel<Comment>> commentResources = new ArrayList<>();
            for (Comment comment : paginationResult.getResources()) {
                Comment embeddedComment = restControllerHelper.convertToEmbeddedComment(comment);
                HalResource<Comment> commentHalResource = createHalComment(embeddedComment);
                commentResources.add(commentHalResource);
            }
            CollectionModel<EntityModel<Comment>> resources;
            if (commentResources.isEmpty()) {
                resources = restControllerHelper.emptyPageResource(Comment.class, paginationResult);
            } else {
                resources = restControllerHelper.generatePagesResource(paginationResult, commentResources);
            }
            HttpStatus status = resources == null ? HttpStatus.NO_CONTENT : HttpStatus.OK;
            return new ResponseEntity<>(resources, status);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new SW360Exception(e.getMessage());
        }
    }

    @Operation(
            summary = "Add a new comment to a clearing request.",
            description = "Create a new comment for the clearing request.",
            tags = {"ClearingRequest"}
    )
    @PreAuthorize("hasAuthority('WRITE')")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comment added successfully.")
    })
    @PostMapping(value = CLEARING_REQUEST_URL + "/{id}/comments")
    public ResponseEntity<?> addComment(
            @Parameter(description = "ID of the clearing request")
            @PathVariable("id") String crId,
            @Parameter(description = "Comment to be added to the clearing request",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Comment.class)))
            @RequestBody Comment comment
    ) throws SW360Exception {
        try {
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();
            ClearingRequest existingClearingRequest = sw360ClearingRequestService.getClearingRequestById(crId, sw360User);
            ClearingRequest updatedClearingRequest = sw360ClearingRequestService.addCommentToClearingRequest(crId, comment, sw360User);

            List<Comment> sortedComments = updatedClearingRequest.getComments().stream()
                    .sorted((c1, c2) -> Long.compare(c2.getCommentedOn(), c1.getCommentedOn()))
                    .toList();
            List<EntityModel<Comment>> commentList = new ArrayList<>();

            for (Comment c : sortedComments) {
                HalResource<Comment> resource = createHalComment(c);
                commentList.add(resource);
            }
            CollectionModel<EntityModel<Comment>> resources = CollectionModel.of(commentList);
            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new BadRequestClientException(e.getMessage(), e);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Clearing request not found.");
        } catch (TException e) {
            throw new SW360Exception("An error occurred while processing the request.");
        }
    }

    private HalResource<Comment> createHalComment(Comment comment) throws TException {
        HalResource<Comment> halComment = new HalResource<>(comment);
        User commentinguser = restControllerHelper.getUserByEmail(comment.getCommentedBy());
        restControllerHelper.addEmbeddedUser(halComment, commentinguser, "commentingUser");
        return halComment;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ClearingRequestController.class).slash("api" + CLEARING_REQUEST_URL).withRel("clearingRequests"));
        return resource;
    }

    @PreAuthorize("hasAuthority('WRITE')")
    @Operation(
            summary = "Update clearing request",
            description = "Update a clearing request by id.",
            tags = {"ClearingRequest"}
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Clearing request updated successfully.")
    })
    @PatchMapping(value = CLEARING_REQUEST_URL + "/{id}")
    public ResponseEntity<HalResource<ClearingRequest>> patchClearingRequest(
            @Parameter(description = "id of the clearing request")
            @PathVariable("id") String id,
            @Parameter(description = "The updated fields of clearing request.",
                    schema = @Schema(implementation = ClearingRequest.class))
            @RequestBody Map<String, Object> reqBodyMap,
            HttpServletRequest request
    ) throws SW360Exception {
        try{
            User sw360User = restControllerHelper.getSw360UserFromAuthentication();

            ClearingRequest clearingRequest = sw360ClearingRequestService.getClearingRequestById(id, sw360User);
            String projectId = clearingRequest.getProjectId();

            ClearingRequest updatedClearingRequest = convertToClearingRequest(reqBodyMap);
            updatedClearingRequest.setId(clearingRequest.getId());
            updatedClearingRequest.setProjectId(clearingRequest.getProjectId());
            updatedClearingRequest.setTimestamp(clearingRequest.getTimestamp());
            updatedClearingRequest.setProjectBU(clearingRequest.getProjectBU());
            updatedClearingRequest.setComments(clearingRequest.getComments());
            updatedClearingRequest.setModifiedOn(System.currentTimeMillis());

            if(CommonUtils.isNotNullEmptyOrWhitespace(updatedClearingRequest.getRequestingUser()) && PermissionUtils.isAdmin(sw360User)){
                User updatedRequestingUser = restControllerHelper.getUserByEmailOrNull(updatedClearingRequest.getRequestingUser());
                if (updatedRequestingUser == null) {
                    throw new BadRequestClientException("Requesting user is not a valid");
                }else{
                    updatedClearingRequest.setRequestingUser(updatedRequestingUser.getEmail());
                }
            }

            if (CommonUtils.isNotNullEmptyOrWhitespace(updatedClearingRequest.getRequestedClearingDate())) {
                if (!clearingRequest.getRequestingUser().equals(sw360User.getEmail())) {
                    throw new AccessDeniedException("Requested Clearing Date can only be updated by the requesting user");
                }
                if (!SW360Utils.isValidDate(clearingRequest.getRequestedClearingDate(), updatedClearingRequest.getRequestedClearingDate(), DateTimeFormatter.ISO_LOCAL_DATE)) {
                    throw new BadRequestClientException("Invalid clearing date requested");
                }
            }

            if ((updatedClearingRequest.getClearingType() != null || updatedClearingRequest.getPriority() != null ) &&
                    !(PermissionUtils.isClearingAdmin(sw360User) || PermissionUtils.isAdmin(sw360User))) {
                throw new AccessDeniedException("Update not allowed for field ClearingType, Priority with user role");
            }

            if (updatedClearingRequest.getClearingTeam() != null) {
                User updatedClearingTeam = restControllerHelper.getUserByEmailOrNull(updatedClearingRequest.getClearingTeam());
                if (updatedClearingTeam == null) {
                    throw new BadRequestClientException("ClearingTeam is not a valid user");
                }
            }

            if (updatedClearingRequest.getAgreedClearingDate() != null) {
                if (PermissionUtils.isClearingAdmin(sw360User) || PermissionUtils.isAdmin(sw360User)) {
                    String currentAgreedClearingDate = CommonUtils.isNotNullEmptyOrWhitespace(clearingRequest.getAgreedClearingDate()) ? clearingRequest.getAgreedClearingDate() : "1980-01-01";
                    if (!SW360Utils.isValidDate(currentAgreedClearingDate, updatedClearingRequest.getAgreedClearingDate(), DateTimeFormatter.ISO_LOCAL_DATE)) {
                        throw new BadRequestClientException("Invalid agreed clearing date requested");
                    }
                } else {
                    throw new AccessDeniedException("Update not allowed for field Agreed Clearing Date with user role");
                }
            }

            clearingRequest = this.restControllerHelper.updateClearingRequest(clearingRequest, updatedClearingRequest);

            String baseURL = restControllerHelper.getBaseUrl(request);
            RequestStatus updateCRStatus = sw360ClearingRequestService.updateClearingRequest(clearingRequest, sw360User, baseURL, projectId);
            HalResource<ClearingRequest> halClearingRequest = createHalClearingRequestWithAllDetails(clearingRequest, sw360User, true);

            if (updateCRStatus == RequestStatus.ACCESS_DENIED) {
                throw new AccessDeniedException("Edit action is not allowed for this user role");
            }

            return new ResponseEntity<>(halClearingRequest, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new BadRequestClientException(e.getMessage(), e);
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException("Clearing request not found.");
        } catch (SW360Exception e) {
            throw e;
        } catch (TException e) {
            throw new SW360Exception("An error occurred while processing the request.");
        }
    }

    private ClearingRequest convertToClearingRequest(Map<String, Object> requestBody){
        Map<String, Object> sanitizedBody = new HashMap<>();
        requestBody.forEach((key, value) -> {
            if (value != null && !(value instanceof String && ((String) value).isEmpty())) {
                sanitizedBody.put(key, value);
            }
        });

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(sw360Module);
        return mapper.convertValue(sanitizedBody, ClearingRequest.class);
    }
}
