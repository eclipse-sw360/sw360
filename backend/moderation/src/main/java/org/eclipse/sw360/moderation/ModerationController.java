/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.moderation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.services.common.Comment;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.moderation.AcceptModerationRequest;
import org.eclipse.sw360.datahandler.services.moderation.ClearingSearchRequest;
import org.eclipse.sw360.datahandler.services.moderation.ExactValuesSearchRequest;
import org.eclipse.sw360.datahandler.services.moderation.ModeratorPageRequest;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.services.moderation.RefineSearchRequest;
import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ModerationHandler handler;

    public ModerationController(ModerationHandler handler) {
        this.handler = handler;
    }

    private static User user(String email, String department, String userGroup) {
        return UserUtils.buildUser(email, department, userGroup);
    }

    // ---- create moderation requests ----

    @PostMapping("/requests/component")
    public RequestStatus createComponentRequest(@RequestBody Component component,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createComponentRequest(ModerationRestMapper.toThriftComponent(component), user(email, department, userGroup)));
    }

    @PostMapping("/requests/release")
    public RequestStatus createReleaseRequest(@RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createReleaseRequest(ModerationRestMapper.toThriftRelease(release), user(email, department, userGroup)));
    }

    @PostMapping("/requests/release/ecc")
    public RequestStatus createReleaseRequestForEcc(@RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createReleaseRequestForEcc(ModerationRestMapper.toThriftRelease(release), user(email, department, userGroup)));
    }

    @PostMapping("/requests/project")
    public RequestStatus createProjectRequest(@RequestBody Project project,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createProjectRequest(ModerationRestMapper.toThriftProject(project), user(email, department, userGroup)));
    }

    @PostMapping("/requests/license")
    public RequestStatus createLicenseRequest(@RequestBody License license,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createLicenseRequest(ModerationRestMapper.toThriftLicense(license), user(email, department, userGroup)));
    }

    @PostMapping("/requests/spdx-document")
    public RequestStatus createSPDXDocumentRequest(@RequestBody SPDXDocument spdx,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createSPDXDocumentRequest(ModerationRestMapper.toThriftSpdxDocument(spdx), user(email, department, userGroup)));
    }

    @PostMapping("/requests/spdx-document-creation-info")
    public RequestStatus createSpdxDocumentCreationInfoRequest(@RequestBody DocumentCreationInformation info,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createSpdxDocumentCreationInfoRequest(ModerationRestMapper.toThriftDocumentCreationInfo(info), user(email, department, userGroup)));
    }

    @PostMapping("/requests/spdx-package-info")
    public RequestStatus createSpdxPackageInfoRequest(@RequestBody PackageInformation info,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.createSpdxPackageInfoRequest(ModerationRestMapper.toThriftPackageInfo(info), user(email, department, userGroup)));
    }

    @PostMapping("/requests/user")
    public ResponseEntity<Void> createUserRequest(@RequestBody org.eclipse.sw360.datahandler.services.users.User bodyUser) throws TException {
        handler.createUserRequest(ModerationRestMapper.toThriftUser(bodyUser));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/component/delete")
    public ResponseEntity<Void> createComponentDeleteRequest(@RequestBody Component component,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.createComponentDeleteRequest(ModerationRestMapper.toThriftComponent(component), user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/release/delete")
    public ResponseEntity<Void> createReleaseDeleteRequest(@RequestBody Release release,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.createReleaseDeleteRequest(ModerationRestMapper.toThriftRelease(release), user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/project/delete")
    public ResponseEntity<Void> createProjectDeleteRequest(@RequestBody Project project,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.createProjectDeleteRequest(ModerationRestMapper.toThriftProject(project), user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/spdx-document/delete")
    public ResponseEntity<Void> createSPDXDocumentDeleteRequest(@RequestBody SPDXDocument spdx,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.createSPDXDocumentDeleteRequest(ModerationRestMapper.toThriftSpdxDocument(spdx), user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/spdx-document-creation-info/delete")
    public ResponseEntity<Void> createSpdxDocumentCreationInfoDeleteRequest(@RequestBody DocumentCreationInformation info,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.createSpdxDocumentCreationInfoDeleteRequest(ModerationRestMapper.toThriftDocumentCreationInfo(info), user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/spdx-package-info/delete")
    public ResponseEntity<Void> createSpdxPackageInfoDeleteRequest(@RequestBody PackageInformation info,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.createSpdxPackageInfoDeleteRequest(ModerationRestMapper.toThriftPackageInfo(info), user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // ---- read / update moderation ----

    @GetMapping("/requests/by-document/{documentId}")
    public List<ModerationRequest> getModerationRequestByDocumentId(@PathVariable String documentId) throws TException {
        return ModerationRestMapper.fromThriftModerationRequests(handler.getModerationRequestByDocumentId(documentId));
    }

    @GetMapping("/requests/{id}")
    public ModerationRequest getModerationRequestById(@PathVariable String id) throws TException {
        return ModerationRestMapper.fromThriftModerationRequest(handler.getModerationRequestById(id));
    }

    @PutMapping("/requests")
    public RequestStatus updateModerationRequest(@RequestBody ModerationRequest request) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.updateModerationRequest(ModerationRestMapper.toThriftModerationRequest(request)));
    }

    @PostMapping("/requests/accept")
    public RequestStatus acceptRequest(@RequestBody AcceptModerationRequest body) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.acceptRequest(ModerationRestMapper.toThriftModerationRequest(body.getRequest()),
                        body.getModerationDecisionComment(), body.getReviewer()));
    }

    @PostMapping("/requests/{requestId}/refuse")
    public ResponseEntity<Void> refuseRequest(@PathVariable String requestId,
            @RequestParam String moderationDecisionComment,
            @RequestParam String reviewer) throws TException {
        handler.refuseRequest(requestId, moderationDecisionComment, reviewer);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/{requestId}/remove-assignee")
    public RemoveModeratorRequestStatus removeUserFromAssignees(@PathVariable String requestId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRemoveModeratorStatus(
                handler.removeUserFromAssignees(requestId, user(email, department, userGroup)));
    }

    @PostMapping("/requests/{requestId}/cancel")
    public ResponseEntity<Void> cancelInProgress(@PathVariable String requestId) throws TException {
        handler.cancelInProgress(requestId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/requests/{requestId}/in-progress")
    public ResponseEntity<Void> setInProgress(@PathVariable String requestId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.setInProgress(requestId, user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/requests/by-document/{documentId}")
    public ResponseEntity<Void> deleteRequestsOnDocument(@PathVariable String documentId) throws TException {
        handler.deleteRequestsOnDocument(documentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/requests/{id}")
    public RequestStatus deleteModerationRequest(@PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws SW360Exception {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.deleteModerationRequest(id, user(email, department, userGroup)));
    }

    @GetMapping("/requests/by-moderator")
    public List<ModerationRequest> getRequestsByModerator(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftModerationRequests(
                handler.getRequestsByModerator(user(email, department, userGroup)));
    }

    @PostMapping("/requests/by-moderator/page-no-filter")
    public List<ModerationRequest> getRequestsByModeratorWithPaginationNoFilter(@RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftModerationRequests(
                handler.getRequestsByModeratorWithPaginationNoFilter(user(email, department, userGroup),
                        ModerationRestMapper.toThriftPagination(pageData)));
    }

    @PostMapping("/requests/by-moderator/page")
    public PaginatedResult<ModerationRequest> getRequestsByModeratorWithPagination(
            @RequestBody ModeratorPageRequest body,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftPaginatedModerationRequests(
                handler.getRequestsByModeratorWithPagination(user(email, department, userGroup),
                        ModerationRestMapper.toThriftPagination(body.getPageData()), body.isOpen()));
    }

    @PostMapping("/requests/by-moderator/page-all-details")
    public PaginatedResult<ModerationRequest> getRequestsByModeratorWithPaginationAllDetails(
            @RequestBody ModeratorPageRequest body,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftPaginatedModerationRequests(
                handler.getRequestsByModeratorWithPaginationAllDetails(user(email, department, userGroup),
                        ModerationRestMapper.toThriftPagination(body.getPageData()), body.isOpen()));
    }

    @GetMapping("/requests/by-requesting-user")
    public List<ModerationRequest> getRequestsByRequestingUser(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftModerationRequests(
                handler.getRequestsByRequestingUser(user(email, department, userGroup)));
    }

    @PostMapping("/requests/by-requesting-user/page")
    public List<ModerationRequest> getRequestsByRequestingUserWithPagination(@RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftModerationRequests(
                handler.getRequestsByRequestingUserWithPagination(user(email, department, userGroup),
                        ModerationRestMapper.toThriftPagination(pageData)));
    }

    // ---- clearing requests ----

    @PostMapping("/clearing-requests")
    public String createClearingRequest(@RequestBody ClearingRequest clearingRequest,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return handler.createClearingRequest(ModerationRestMapper.toThriftClearingRequest(clearingRequest),
                user(email, department, userGroup));
    }

    @PutMapping("/clearing-requests")
    public RequestStatus updateClearingRequest(@RequestBody ClearingRequest clearingRequest,
            @RequestParam String projectUrl,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.updateClearingRequest(ModerationRestMapper.toThriftClearingRequest(clearingRequest),
                        user(email, department, userGroup), projectUrl));
    }

    @GetMapping("/clearing-requests/mine")
    public Set<ClearingRequest> getMyClearingRequests(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftClearingRequestSet(
                handler.getMyClearingRequests(user(email, department, userGroup)));
    }

    @GetMapping("/clearing-requests/by-bu")
    public Set<ClearingRequest> getClearingRequestsByBU(@RequestParam String businessUnit) throws TException {
        return ModerationRestMapper.fromThriftClearingRequestSet(handler.getClearingRequestsByBU(businessUnit));
    }

    @GetMapping("/clearing-requests/by-project/{projectId}")
    public ClearingRequest getClearingRequestByProjectId(@PathVariable String projectId,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftClearingRequest(
                handler.getClearingRequestByProjectId(projectId, user(email, department, userGroup)));
    }

    @GetMapping("/clearing-requests/{id}")
    public ClearingRequest getClearingRequestById(@PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftClearingRequest(
                handler.getClearingRequestById(id, user(email, department, userGroup)));
    }

    @GetMapping("/clearing-requests/{id}/for-edit")
    public ClearingRequest getClearingRequestByIdForEdit(@PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftClearingRequest(
                handler.getClearingRequestByIdForEdit(id, user(email, department, userGroup)));
    }

    @DeleteMapping("/clearing-requests/{id}")
    public RequestStatus deleteClearingRequest(@PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.deleteClearingRequest(id, user(email, department, userGroup)));
    }

    @PostMapping("/clearing-requests/project-deletion")
    public ResponseEntity<Void> updateClearingRequestForProjectDeletion(@RequestBody Project project,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.updateClearingRequestForProjectDeletion(ModerationRestMapper.toThriftProject(project),
                user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/clearing-requests/{crId}/business-unit")
    public ResponseEntity<Void> updateClearingRequestForChangeInProjectBU(@PathVariable String crId,
            @RequestParam String businessUnit,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        handler.updateClearingRequestForChangeInProjectBU(crId, businessUnit, user(email, department, userGroup));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/clearing-requests/{id}/comments")
    public RequestStatus addCommentToClearingRequest(@PathVariable String id, @RequestBody Comment comment,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftRequestStatus(
                handler.addCommentToClearingRequest(id, ModerationRestMapper.toThriftComment(comment),
                        user(email, department, userGroup)));
    }

    @PostMapping("/clearing-requests/recent/page")
    public PaginatedResult<ClearingRequest> getRecentClearingRequestsWithPagination(@RequestBody PaginationData pageData,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftPaginatedClearingRequests(
                handler.getRecentClearingRequestsWithPagination(user(email, department, userGroup),
                        ModerationRestMapper.toThriftPagination(pageData)));
    }

    @PostMapping("/clearing-requests/search")
    public PaginatedResult<ClearingRequest> searchClearingRequestsByFilters(
            @RequestBody ClearingSearchRequest body,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return ModerationRestMapper.fromThriftPaginatedClearingRequests(
                handler.searchClearingRequestsByFilters(user(email, department, userGroup), body.getFilterMap(),
                        ModerationRestMapper.toThriftPagination(body.getPageData())));
    }

    @GetMapping("/clearing-requests/open-critical-count")
    public int getOpenCriticalCrCountByGroup(@RequestParam String group) throws TException {
        return handler.getOpenCriticalCrCountByGroup(group);
    }

    // ---- search / counts ----

    @PostMapping("/requests/refine-search")
    public List<ModerationRequest> refineSearch(@RequestBody RefineSearchRequest body) throws TException {
        return ModerationRestMapper.fromThriftModerationRequests(
                handler.refineSearch(body.getText(), body.getSubQueryRestrictions(),
                        ModerationRestMapper.toThriftPagination(body.getPageData())));
    }

    @PostMapping("/requests/search-by-exact-values")
    public List<ModerationRequest> searchModerationRequestsByExactValues(
            @RequestBody ExactValuesSearchRequest body) throws TException {
        return ModerationRestMapper.fromThriftModerationRequests(
                handler.searchModerationRequestsByExactValues(body.getSubQueryRestrictions(),
                        ModerationRestMapper.toThriftPagination(body.getPageData())));
    }

    @GetMapping("/requests/count/by-moderation-state")
    public Map<String, Long> getCountByModerationState(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return handler.getCountByModerationState(user(email, department, userGroup));
    }

    @GetMapping("/requests/count/by-requester")
    public Map<String, Long> getCountByRequester(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return handler.getCountByRequester(user(email, department, userGroup));
    }

    @PostMapping("/requests/count/by-moderation-state-and-requesting-user")
    public Map<String, Long> getCountByModerationStateAndRequestingUser(
            @RequestBody org.eclipse.sw360.datahandler.services.users.User requestingUser,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        return handler.getCountByModerationStateAndRequestingUser(user(email, department, userGroup),
                ModerationRestMapper.toThriftUser(requestingUser));
    }

    @GetMapping("/requesting-user-depts")
    public Set<String> getRequestingUserDepts() {
        return handler.getRequestingUserDepts();
    }
}
