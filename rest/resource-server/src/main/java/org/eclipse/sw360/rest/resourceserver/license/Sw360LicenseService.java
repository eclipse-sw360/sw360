/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 * Copyright Ritankar Saha <ritankar.saha786@gmail.com>, 2025.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.license;

import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.exporter.LicsExporter;
import org.eclipse.sw360.exporter.utils.ZipTools;
import org.eclipse.sw360.importer.LicsImporter;
import org.eclipse.sw360.rest.resourceserver.core.BadRequestClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360LicenseService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;
    private static String CONTENT_TYPE = "application/zip";
    LicenseType lType = new LicenseType();

    public List<License> getLicenses() throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        return sw360LicenseClient.getLicenseSummary();
    }

    public License getLicenseById(String licenseId) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        // TODO Kai Tödter 2017-01-26
        // What is the semantics of the second parameter (organization)?
        License license = null;
        try {
            license = sw360LicenseClient.getByID(licenseId, "?");
        } catch (SW360Exception exp) {
            if (exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException(exp.getWhy());
            } else {
                throw new RuntimeException(exp.getWhy());
            }
        }
        return license;
    }

    public void deleteLicenseById(String licenseId, User user) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        RequestStatus deleteLicenseStatus = sw360LicenseClient.deleteLicense(licenseId, user);

        if (deleteLicenseStatus == RequestStatus.IN_USE) {
            throw new BadRequestClientException("Unable to delete license. License is in Use");
        } else if (deleteLicenseStatus == RequestStatus.FAILURE) {
            throw new RuntimeException("Unable to delete License");
        }
    }

    public void deleteAllLicenseInfo(User user) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, user)) {
            RequestSummary deleteLicenseStatus = sw360LicenseClient.deleteAllLicenseInformation(user);
        } else {
            throw new BadRequestClientException("Unable to delete license. User is not admin");
        }
    }

    public License createLicense(License license, User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        license.setId(license.getShortname());
        List<License> licenses = sw360LicenseClient.addLicenses(Collections.singletonList(license), sw360User);
        for (License newLicense : licenses) {
            if (license.getFullname().equals(newLicense.getFullname())) {
                return newLicense;
            }
        }
        return null;
    }

    public RequestStatus updateLicenseToDB(License license, Set<String> obligationIds, User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (obligationIds.isEmpty()){
            license.unsetObligations();
        } else {
            List<String> obligationIdList = new ArrayList<>(obligationIds);
            List<Obligation> obligations = sw360LicenseClient.getObligationsByIds(obligationIdList);
            checkObligationLevel(obligations, sw360User);
            license.setObligationDatabaseIds(obligationIds);
            license.setObligations(obligations);
        }
        return sw360LicenseClient.updateLicense(license, sw360User, sw360User);
    }

    public RequestStatus updateLicense(License license, User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (null != license){
            Set<String> obligationIds = license.getObligationDatabaseIds();
            if (CommonUtils.isNullOrEmptyCollection(obligationIds)) {
                license.unsetObligations();
            } else {
                List<String> obligationIdList = new ArrayList<>(obligationIds);
                List<Obligation> obligations = sw360LicenseClient.getObligationsByIds(obligationIdList);
                checkObligationLevel(obligations, sw360User);
                license.setObligationDatabaseIds(obligationIds);
                license.setObligations(obligations);
            }
        }
        return sw360LicenseClient.updateLicense(license, sw360User, sw360User);
    }

    public Set<String> getIdObligationsContainWhitelist(User sw360User, String licenseId, Set<String> diffIds) throws TException {
        Set<String> obligationIdTrue = new HashSet<>();
        String organisation = sw360User.getDepartment();
        String businessUnit = SW360Utils.getBUFromOrganisation(organisation);
        List<Obligation> obligations = getObligationsByLicenseId(licenseId);
        for (Obligation obligation : obligations) {
            String obligationId = obligation.getId();
            Set<String> currentWhitelist = obligation.whitelist != null ? obligation.whitelist : new HashSet<>();
            if (diffIds.contains(obligationId) && currentWhitelist.contains(businessUnit)) {
                obligationIdTrue.add(obligationId);
            }
        }
        return obligationIdTrue;
    }

    public RequestStatus updateWhitelist(Set<String> obligationIds, String licenseId, User user) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        return sw360LicenseClient.updateWhitelist(licenseId, ImmutableSet.copyOf(obligationIds), user);
    }

    public List<Obligation> getObligationsByLicenseId(String id) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        List<Obligation> obligations = sw360LicenseClient.getObligationsByLicenseId(id);
        if (CommonUtils.isNullOrEmptyCollection(obligations)) {
            return Collections.emptyList();
        }
        return obligations;
    }

    public List<LicenseType> getLicenseTypes() throws TException  {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        List<LicenseType> licenseTypes = sw360LicenseClient.getLicenseTypes();
        if (CommonUtils.isNullOrEmptyCollection(licenseTypes)) {
            return Collections.emptyList();
        }
        return licenseTypes;
    }

    public void checkObligationIds(Set<String> obligationIds) throws TException {
        if (obligationIds.isEmpty()) {
            throw new BadRequestClientException("Cannot update because no obligation id input");
        }
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        ArrayList<String> obligationIdsIncorrect = new ArrayList<>();
        for (String obligationId : obligationIds) {
            if (isNullEmptyOrWhitespace(obligationId)) {
                throw new BadRequestClientException("Obligation id cannot be empty");
            }
            try {
                sw360LicenseClient.getObligationsById(obligationId);
            } catch (Exception e) {
                obligationIdsIncorrect.add(obligationId);
            }
        }
        if (!obligationIdsIncorrect.isEmpty()) {
            throw new BadRequestClientException("Obligation ids " + obligationIdsIncorrect + " incorrect");
        }
    }

    public void checkObligationLevel(List<Obligation> obligations, User sw360User) {
        List<String> obligationsLevelIncorrect = new ArrayList<>();
        for (Obligation oblig : obligations) {
            if (oblig.getObligationLevel().toString().equals("LICENSE_OBLIGATION")) {
                oblig.addToWhitelist(sw360User.getDepartment());
            } else {
                obligationsLevelIncorrect.add(oblig.getId());
            }
        }
        if (!obligationsLevelIncorrect.isEmpty()) {
            throw new BadRequestClientException("Obligation with ids: " + obligationsLevelIncorrect + " level is not License Obligation");
        }
    }

    private LicenseService.Iface getThriftLicenseClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/licenses/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new LicenseService.Client(protocol);
    }
    public RequestSummary importSpdxInformation(User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            RequestSummary allSPDXLicenseStatus = sw360LicenseClient.importAllSpdxLicenses(sw360User);
            return allSPDXLicenseStatus;
        } else {
            throw new BadRequestClientException("Unable to import All Spdx license. User is not admin");
        }
    }

    public void getDownloadLicenseArchive(User sw360User ,HttpServletRequest request,HttpServletResponse response) throws TException,IOException{
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new BadRequestClientException("Unable to download archive license. User is not admin");
        }
        try {
            LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
            String fileConstant="LicensesBackup.lics";
            Map<String, InputStream> fileNameToStreams = (new LicsExporter(sw360LicenseClient)).getFilenameToCSVStreams();
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            final ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream);

            for (Map.Entry<String, InputStream> entry : fileNameToStreams.entrySet()) {
                 ZipTools.addToZip(zipOutputStream, entry.getKey(), entry.getValue());
            }
            zipOutputStream.flush();
            zipOutputStream.close();
            final ByteArrayInputStream zipFile = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            String filename = String.format(fileConstant, SW360Utils.getCreatedOn());
            response.setContentType(CONTENT_TYPE);
            response.setHeader("Content-Disposition", String.format("license; filename=\"%s\"", filename));
            copyDataStreamToResponse(response, zipFile);
        } catch (SW360Exception exp) {
            if (exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException(exp.getWhy());
            } else {
                throw new RuntimeException(exp.getWhy());
            }
        }
    }

    private void copyDataStreamToResponse(HttpServletResponse response, ByteArrayInputStream buffer) throws IOException {
        FileCopyUtils.copy(buffer, response.getOutputStream());
    }

    public void uploadLicense(User sw360User, MultipartFile file, boolean overwriteIfExternalIdMatches, boolean overwriteIfIdMatchesEvenWithoutExternalIdMatch) throws IOException, TException {
        final HashMap<String, InputStream> inputMap = new HashMap<>();

        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new BadRequestClientException("Unable to upload license file. User is not admin");
        }
        try {
            InputStream inputStream = file.getInputStream();
            ZipTools.extractZipToInputStreamMap(inputStream, inputMap);
            LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
            final LicsImporter licsImporter = new LicsImporter(sw360LicenseClient, overwriteIfExternalIdMatches, overwriteIfIdMatchesEvenWithoutExternalIdMatch);
            licsImporter.importLics(sw360User, inputMap);

        }finally {
            for (InputStream inputStream : inputMap.values()) {
                inputStream.close();
            }
        }
	}

    public RequestSummary importOsadlInformation(User sw360User) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            return sw360LicenseClient.importAllOSADLLicenses(sw360User);
        } else {
            throw new BadRequestClientException("Unable to import All Spdx license. User is not admin");
        }
    }

    public RequestStatus addLicenseType(User sw360User, String licenseType, HttpServletRequest request) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        if (StringUtils.isNotEmpty(licenseType)) {
             lType.setLicenseType(licenseType);
        }
        else {
              throw new BadRequestClientException("license type is empty");
        }
        try {
            if (PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
                RequestStatus status = sw360LicenseClient.addLicenseType(lType, sw360User);
            } else {
                throw new BadRequestClientException("Unable to create License Type. User is not admin");
            }
         } catch ( Exception e) {
                 throw new TException(e.getMessage());
         }
         return RequestStatus.SUCCESS;
     }

     public List<LicenseType> quickSearchLicenseType(String searchElem) {
         try {
             LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
             List<LicenseType> rawResults = sw360LicenseClient.searchByLicenseType(searchElem);

             Map<String, LicenseType> uniqueResults = new LinkedHashMap<>();

             for (LicenseType license : rawResults) {
                 String id = license.getId();
                 if (id != null && !id.isEmpty()) {
                     uniqueResults.put(id, license);
                 }
             }

             List<LicenseType> sortedResults = new ArrayList<>(uniqueResults.values());
             sortedResults.sort(Comparator.comparing(LicenseType::getLicenseType, String.CASE_INSENSITIVE_ORDER));

             return sortedResults;

         } catch (TException e) {
             throw new RuntimeException("Error performing licenseType search", e);
         }
     }

     public RequestStatus deleteLicenseType(String id, User sw360User) throws TException {
         LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();

         if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
             throw new AccessDeniedException("Unable to delete license type. User is not admin");
         }

         try {
             sw360LicenseClient.getLicenseTypeById(id);
         } catch (TException e) {
             throw new ResourceNotFoundException("License type not found with ID: " + id);
         }

         RequestStatus status = sw360LicenseClient.deleteLicenseType(id, sw360User);
         if (status == RequestStatus.FAILURE) {
             throw new SW360Exception("License type could not be deleted.");
         }

         return status;
     }

    public int getLicenseTypeUsageCount(String licenseTypeId, User sw360User) throws TException{
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new AccessDeniedException("User is not authorized to retrieve license type usage details.");
        }

        try {
            LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();

            List<License> allLicenses = sw360LicenseClient.getLicenseSummary();
            return (int) allLicenses.stream()
                    .filter(license -> license.getLicenseType() != null && license.getLicenseType().getId().equals(licenseTypeId))
                    .count();
        } catch (SW360Exception sw360Exp) {
            if (sw360Exp.getErrorCode() == 404) {
                throw new ResourceNotFoundException("License type not found with ID: " + licenseTypeId);
            } else {
                throw sw360Exp;
            }
        }
    }

    /**
     * Search licenses for given text in fullname or shortname.
     * @param searchText String to search.
     * @return Sorted list of licenses.
     * @throws TException If Thrift had issues.
     */
    public List<License> searchLicenses(String searchText) throws TException {
        LicenseService.Iface sw360LicenseClient = getThriftLicenseClient();
        List<License> licenses = sw360LicenseClient.searchLicense(searchText);
        licenses.sort(Comparator.comparing(License::getFullname, String.CASE_INSENSITIVE_ORDER));
        return licenses;
    }
}
