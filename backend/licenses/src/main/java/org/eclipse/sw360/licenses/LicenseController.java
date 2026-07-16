/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.common.utils.UserUtils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.services.common.CustomProperties;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.licenses.LicenseType;
import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.datahandler.services.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.services.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.services.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.exporter.utils.ZipTools;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/licenses")
public class LicenseController {

    private final LicenseHandler licenseHandler;

    public LicenseController(LicenseHandler licenseHandler) {
        this.licenseHandler = licenseHandler;
    }

    @GetMapping("/summary")
    public List<License> getLicenseSummary() throws TException {
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.getLicenseSummary());
    }

    @GetMapping("/summary/export")
    public List<License> getLicenseSummaryForExport() throws TException {
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.getLicenseSummaryForExport());
    }

    @GetMapping(value = "/download-excel", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] downloadExcel(@RequestParam String token) throws TException {
        return toByteArray(licenseHandler.downloadExcel(token));
    }

    @GetMapping(value = "/report-stream", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public byte[] getLicenseReportDataStream() throws TException {
        return toByteArray(licenseHandler.getLicenseReportDataStream());
    }

    @GetMapping("/detailed-summary/export")
    public List<License> getDetailedLicenseSummaryForExport(@RequestParam String organisation) throws TException {
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.getDetailedLicenseSummaryForExport(organisation));
    }

    @PostMapping("/detailed-summary")
    public List<License> getDetailedLicenseSummary(
            @RequestParam String organisation,
            @RequestBody List<String> identifiers) throws TException {
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.getDetailedLicenseSummary(organisation, identifiers));
    }

    @PostMapping("/types")
    public RequestStatus addLicenseType(
            @RequestBody LicenseType licenseType,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.addLicenseType(
                LicenseRestMapper.toThriftLicenseType(licenseType), user));
    }

    @PostMapping("/types/bulk")
    public List<LicenseType> addLicenseTypes(
            @RequestBody List<LicenseType> licenseTypes,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftLicenseTypes(licenseHandler.addLicenseTypes(
                LicenseRestMapper.toThriftLicenseTypes(licenseTypes), user));
    }

    @PostMapping("/bulk")
    public List<License> addLicenses(
            @RequestBody List<License> licenses,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.addLicenses(
                LicenseRestMapper.toThriftLicenses(licenses), user));
    }

    @PostMapping("/bulk/overwrite")
    public List<License> addOrOverwriteLicenses(
            @RequestBody List<License> licenses,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.addOrOverwriteLicenses(
                LicenseRestMapper.toThriftLicenses(licenses), user));
    }

    @PostMapping("/obligations/bulk")
    public List<Obligation> addListOfObligations(
            @RequestBody List<Obligation> obligations,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftObligations(licenseHandler.addListOfObligations(
                LicenseRestMapper.toThriftObligations(obligations), user));
    }

    @GetMapping("/types")
    public List<LicenseType> getLicenseTypes() throws TException {
        return LicenseRestMapper.fromThriftLicenseTypes(licenseHandler.getLicenseTypes());
    }

    @GetMapping
    public List<License> getLicenses() throws TException {
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.getLicenses());
    }

    @GetMapping("/obligations")
    public List<Obligation> getObligations() throws TException {
        return LicenseRestMapper.fromThriftObligations(licenseHandler.getObligations());
    }

    @GetMapping("/obligation-nodes")
    public List<ObligationNode> getObligationNodes() throws TException {
        return LicenseRestMapper.fromThriftObligationNodes(licenseHandler.getObligationNodes());
    }

    @GetMapping("/obligation-elements")
    public List<ObligationElement> getObligationElements() throws TException {
        return LicenseRestMapper.fromThriftObligationElements(licenseHandler.getObligationElements());
    }

    @PostMapping("/types/by-ids")
    public List<LicenseType> getLicenseTypesByIds(@RequestBody List<String> ids) throws TException {
        return LicenseRestMapper.fromThriftLicenseTypes(licenseHandler.getLicenseTypesByIds(ids));
    }

    @PostMapping("/obligations/by-ids")
    public List<Obligation> getObligationsByIds(@RequestBody List<String> ids) throws TException {
        return LicenseRestMapper.fromThriftObligations(licenseHandler.getObligationsByIds(ids));
    }

    @GetMapping("/obligations/by-license/{id}")
    public List<Obligation> getObligationsByLicenseId(@PathVariable String id) throws TException {
        return LicenseRestMapper.fromThriftObligations(licenseHandler.getObligationsByLicenseId(id));
    }

    @GetMapping("/{id}")
    public License getById(
            @PathVariable String id,
            @RequestParam String organisation) throws SW360Exception {
        return LicenseRestMapper.fromThriftLicense(licenseHandler.getByID(id, organisation));
    }

    @GetMapping("/{id}/with-moderation")
    public License getByIdWithOwnModerationRequests(
            @PathVariable String id,
            @RequestParam String organisation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftLicense(licenseHandler.getByIDWithOwnModerationRequests(id, organisation, user));
    }

    @PostMapping("/by-ids")
    public List<License> getByIds(
            @RequestBody Set<String> ids,
            @RequestParam String organisation) throws TException {
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.getByIds(ids, organisation));
    }

    @GetMapping("/types/{id}")
    public LicenseType getLicenseTypeById(@PathVariable String id) throws TException {
        return LicenseRestMapper.fromThriftLicenseType(licenseHandler.getLicenseTypeById(id));
    }

    @GetMapping("/obligations/{id}")
    public Obligation getObligationsById(@PathVariable String id) throws TException {
        return LicenseRestMapper.fromThriftObligation(licenseHandler.getObligationsById(id));
    }

    @GetMapping("/obligation-nodes/{id}")
    public ObligationNode getObligationNodeById(@PathVariable String id) throws TException {
        return LicenseRestMapper.fromThriftObligationNode(licenseHandler.getObligationNodeById(id));
    }

    @GetMapping("/obligation-elements/{id}")
    public ObligationElement getObligationElementById(@PathVariable String id) throws TException {
        return LicenseRestMapper.fromThriftObligationElement(licenseHandler.getObligationElementById(id));
    }

    @PostMapping("/obligations")
    public String addObligations(
            @RequestBody Obligation obligation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return licenseHandler.addObligations(LicenseRestMapper.toThriftObligation(obligation), user);
    }

    @PostMapping("/obligation-elements")
    public String addObligationElements(
            @RequestBody ObligationElement obligationElement,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return licenseHandler.addObligationElements(LicenseRestMapper.toThriftObligationElement(obligationElement), user);
    }

    @PostMapping("/obligation-nodes")
    public String addObligationNodes(
            @RequestBody ObligationNode obligationNode,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return licenseHandler.addObligationNodes(LicenseRestMapper.toThriftObligationNode(obligationNode), user);
    }

    @PostMapping("/obligations/to-license")
    public RequestStatus addObligationsToLicense(
            @RequestBody AddObligationsToLicenseRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.addObligationsToLicense(
                LicenseRestMapper.toThriftObligationSet(request.getObligations()),
                LicenseRestMapper.toThriftLicense(request.getLicense()),
                user));
    }

    @PutMapping
    public RequestStatus updateLicense(
            @RequestBody License license,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup,
            @RequestHeader("X-Requesting-User-Email") String requestingEmail,
            @RequestHeader(value = "X-Requesting-User-Department", required = false) String requestingDepartment,
            @RequestHeader(value = "X-Requesting-User-Group", required = false) String requestingUserGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        User requestingUser = UserUtils.buildUser(requestingEmail, requestingDepartment, requestingUserGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.updateLicense(
                LicenseRestMapper.toThriftLicense(license), user, requestingUser));
    }

    @PutMapping("/from-moderation")
    public RequestStatus updateLicenseFromModerationRequest(
            @RequestBody UpdateLicenseFromModerationRequest request,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup,
            @RequestHeader("X-Requesting-User-Email") String requestingEmail,
            @RequestHeader(value = "X-Requesting-User-Department", required = false) String requestingDepartment,
            @RequestHeader(value = "X-Requesting-User-Group", required = false) String requestingUserGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        User requestingUser = UserUtils.buildUser(requestingEmail, requestingDepartment, requestingUserGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.updateLicenseFromModerationRequest(
                LicenseRestMapper.toThriftLicense(request.getAdditions()),
                LicenseRestMapper.toThriftLicense(request.getDeletions()),
                user,
                requestingUser));
    }

    @PutMapping("/{id}/whitelist")
    public RequestStatus updateWhitelist(
            @PathVariable String id,
            @RequestBody Set<String> whitelist,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.updateWhitelist(id, whitelist, user));
    }

    @DeleteMapping("/{id}")
    public RequestStatus deleteLicense(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.deleteLicense(id, user));
    }

    @GetMapping("/custom-properties")
    public List<CustomProperties> getCustomProperties(@RequestParam String documentType) {
        return LicenseRestMapper.fromThriftCustomPropertiesList(licenseHandler.getCustomProperties(documentType));
    }

    @PutMapping("/custom-properties")
    public RequestStatus updateCustomProperties(
            @RequestBody CustomProperties customProperties,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.updateCustomProperties(
                LicenseRestMapper.toThriftCustomProperties(customProperties), user));
    }

    @DeleteMapping("/all")
    public RequestSummary deleteAllLicenseInformation(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestSummary(licenseHandler.deleteAllLicenseInformation(user));
    }

    @PostMapping("/import-spdx")
    public RequestSummary importAllSpdxLicenses(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestSummary(licenseHandler.importAllSpdxLicenses(user));
    }

    @PostMapping("/import-osadl")
    public RequestSummary importAllOSADLLicenses(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestSummary(licenseHandler.importAllOSADLLicenses(user));
    }

    @DeleteMapping("/obligations/{id}")
    public RequestStatus deleteObligations(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.deleteObligations(id, user));
    }

    @DeleteMapping("/types/{id}")
    public RequestStatus deleteLicenseType(
            @PathVariable String id,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftRequestStatus(licenseHandler.deleteLicenseType(id, user));
    }

    @GetMapping("/types/{id}/in-use")
    public int checkLicenseTypeInUse(@PathVariable String id) throws TException {
        return licenseHandler.checkLicenseTypeInUse(id);
    }

    @PostMapping("/obligation-nodes/add")
    public String addNodes(
            @RequestBody String jsonString,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return licenseHandler.addNodes(jsonString, user);
    }

    @PostMapping("/obligation-nodes/build-text")
    public String buildObligationText(
            @RequestParam String nodes,
            @RequestParam String level) throws TException {
        return licenseHandler.buildObligationText(nodes, level);
    }

    @GetMapping("/obligation-elements/search")
    public List<ObligationElement> searchObligationElement(@RequestParam String text) throws TException {
        return LicenseRestMapper.fromThriftObligationElements(licenseHandler.searchObligationElement(text));
    }

    @PostMapping("/obligations/convert-text")
    public String convertTextToNode(
            @RequestBody Obligation obligation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return licenseHandler.convertTextToNode(LicenseRestMapper.toThriftObligation(obligation), user);
    }

    @PostMapping("/obligations/with-text-nodes")
    public Obligation getWithTextNodes(
            @RequestBody Obligation obligation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return LicenseRestMapper.fromThriftObligation(licenseHandler.getWithTextNodes(
                LicenseRestMapper.toThriftObligation(obligation), user));
    }

    @PutMapping("/obligations")
    public String updateObligation(
            @RequestBody Obligation obligation,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException {
        User user = UserUtils.buildUser(email, department, userGroup);
        return licenseHandler.updateObligation(LicenseRestMapper.toThriftObligation(obligation), user);
    }

    @GetMapping("/types/search")
    public List<LicenseType> searchByLicenseType(@RequestParam String licenseType) {
        return LicenseRestMapper.fromThriftLicenseTypes(licenseHandler.searchByLicenseType(licenseType));
    }

    @GetMapping("/search")
    public List<License> searchLicense(@RequestParam(required = false) String searchText) {
        return LicenseRestMapper.fromThriftLicenses(licenseHandler.searchLicense(searchText));
    }

    @GetMapping("/obligations/search/paginated")
    public PaginatedResult<Obligation> searchObligationTextPaginated(
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) ObligationLevel obligationLevel,
            @ModelAttribute PaginationData pageData) {
        Map<PaginationData, List<Obligation>> result = LicenseRestMapper.fromThriftPaginatedObligations(
                licenseHandler.searchObligationTextPaginated(
                        searchText,
                        LicenseRestMapper.toThriftObligationLevel(obligationLevel),
                        LicenseRestMapper.toThriftPagination(pageData)));
        if (result.isEmpty()) {
            return new PaginatedResult<>(pageData, List.of());
        }
        Map.Entry<PaginationData, List<Obligation>> entry = result.entrySet().iterator().next();
        return new PaginatedResult<>(entry.getKey(), entry.getValue());
    }

    @PostMapping("/import-archive")
    public void importArchive(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean overwriteIfExternalIdMatches,
            @RequestParam(defaultValue = "false") boolean overwriteIfIdMatchesEvenWithoutExternalIdMatch,
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException, IOException {
        User user = UserUtils.buildUser(email, department, userGroup);
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            throw new SW360Exception("Unable to upload license file. User is not admin.").setErrorCode(403);
        }
        if (file == null || file.isEmpty()) {
            throw new SW360Exception("Unable to upload license file. File is null or empty.").setErrorCode(400);
        }
        final HashMap<String, InputStream> inputMap = new HashMap<>();
        try (InputStream inputStream = file.getInputStream()) {
            ZipTools.extractZipToInputStreamMap(inputStream, inputMap);
            licenseHandler.importArchive(user, inputMap, overwriteIfExternalIdMatches, overwriteIfIdMatchesEvenWithoutExternalIdMatch);
        } finally {
            for (InputStream in : inputMap.values()) {
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    @GetMapping(value = "/export-archive", produces = "application/zip")
    public byte[] exportArchive(
            @RequestHeader("X-User-Email") String email,
            @RequestHeader(value = "X-User-Department", required = false) String department,
            @RequestHeader(value = "X-User-Group", required = false) String userGroup) throws TException, IOException {
        User user = UserUtils.buildUser(email, department, userGroup);
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            throw new SW360Exception("Unable to download archive license. User is not admin.").setErrorCode(403);
        }
        return licenseHandler.exportArchive();
    }

    private static byte[] toByteArray(ByteBuffer buffer) {
        if (buffer == null) {
            return new byte[0];
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    public static class AddObligationsToLicenseRequest {
        private Set<Obligation> obligations;
        private License license;

        public Set<Obligation> getObligations() {
            return obligations;
        }

        public void setObligations(Set<Obligation> obligations) {
            this.obligations = obligations;
        }

        public License getLicense() {
            return license;
        }

        public void setLicense(License license) {
            this.license = license;
        }
    }

    public static class UpdateLicenseFromModerationRequest {
        private License additions;
        private License deletions;

        public License getAdditions() {
            return additions;
        }

        public void setAdditions(License additions) {
            this.additions = additions;
        }

        public License getDeletions() {
            return deletions;
        }

        public void setDeletions(License deletions) {
            this.deletions = deletions;
        }
    }
}
