/*
 * Copyright Siemens AG, 2013-2015.
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.admin;

import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.PortalUtil;

import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.LicsExporter;
import org.eclipse.sw360.exporter.utils.ZipTools;
import org.eclipse.sw360.importer.LicsImporter;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import javax.portlet.*;

import static org.eclipse.sw360.portal.common.PortalConstants.LICENSE_ADMIN_PORTLET_NAME;

@Component(
    immediate = true,
    properties = {
            "/org/eclipse/sw360/portal/portlets/base.properties",
            "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + LICENSE_ADMIN_PORTLET_NAME,

        "javax.portlet.display-name=License Administration",
        "javax.portlet.info.short-title=License",
        "javax.portlet.info.title=License Administration",

        "javax.portlet.init-param.view-template=/html/admin/licenseAdmin/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class LicenseAdminPortlet extends Sw360Portlet {
    private static final Logger log = Logger.getLogger(LicenseAdminPortlet.class);

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException {
        String action = request.getParameter(PortalConstants.ACTION);

        if (action == null) {
            log.error("Invalid action 'null'");
            return;
        }

        switch (action) {
            case PortalConstants.DOWNLOAD_LICENSE_BACKUP:
                try {
                    backUpLicenses(request, response);
                } catch (IOException | TException e) {
                    log.error("Something went wrong with the license zip creation", e);
                }
                break;
            case PortalConstants.ACTION_DELETE_ALL_LICENSE_INFORMATION:
                deleteAllLicenseInformation(request, response);
                break;
            case PortalConstants.ACTION_IMPORT_SPDX_LICENSE_INFORMATION:
                try {
                    importLicensesFromSPDX(request, response);
                } catch (TException e) {
                    throw new PortletException(e);
                }
                break;
            default:
                log.warn("The LicenseAdminPortlet was called with unsupported action=[" + action + "]");
        }
    }

    @UsedAsLiferayAction
    public void updateLicenses(ActionRequest request, ActionResponse response) throws IOException, TException {
        final HashMap<String, InputStream> inputMap = new HashMap<>();
        User user = UserCacheHolder.getUserFromRequest(request);
        try {
            fillFilenameInputStreamMap(request, inputMap);

            final LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();


            boolean overwriteIfExternalIdMatches = "true".equals(request.getParameter("overwriteIfExternalIdMatches"));
            boolean overwriteIfIdMatchesEvenWithoutExternalIdMatch = "true".equals(request.getParameter("overwriteIfIdMatchesEvenWithoutExternalIdMatch"));

            final LicsImporter licsImporter = new LicsImporter(licenseClient, overwriteIfExternalIdMatches, overwriteIfIdMatchesEvenWithoutExternalIdMatch);
            licsImporter.importLics(user, inputMap);
        } finally {
            for (InputStream inputStream : inputMap.values()) {
                inputStream.close();
            }
        }
    }

    private void deleteAllLicenseInformation(ResourceRequest request, ResourceResponse response){
        User user = UserCacheHolder.getUserFromRequest(request);
        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
        try {
            RequestSummary requestSummary = licenseClient.deleteAllLicenseInformation(user);
            renderRequestSummary(request, response, requestSummary);
        } catch (TException te){
            log.error("Got TException when trying to delete all license information." ,te);
        }
    }

    private void importLicensesFromSPDX(ResourceRequest request, ResourceResponse response) throws TException {
        User user = UserCacheHolder.getUserFromRequest(request);
        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
        RequestSummary requestSummary = licenseClient.importAllSpdxLicenses(user);
        renderRequestSummary(request, response, requestSummary);
    }

    private void fillFilenameInputStreamMap(ActionRequest request, HashMap<String, InputStream> fileNameToStream) throws IOException {
        InputStream in = null;
        try {
            in = getInputStreamFromRequest(request, "file");
            ZipTools.extractZipToInputStreamMap(in, fileNameToStream);
        } finally {
            if (in != null) in.close();
        }
    }

    private InputStream getInputStreamFromRequest(PortletRequest request, String fileUploadFormId) throws IOException {
        final UploadPortletRequest uploadPortletRequest = PortalUtil.getUploadPortletRequest(request);
        return uploadPortletRequest.getFileAsStream(fileUploadFormId);
    }

    public void backUpLicenses(ResourceRequest request, ResourceResponse response) throws IOException, TException {
        final LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
        Map<String, InputStream> fileNameToStreams = (new LicsExporter(licenseClient)).getFilenameToCSVStreams();

        final ByteArrayOutputStream outB = new ByteArrayOutputStream();
        final ZipOutputStream zipOutputStream = new ZipOutputStream(outB);

        for (Map.Entry<String, InputStream> entry : fileNameToStreams.entrySet()) {
            ZipTools.addToZip(zipOutputStream, entry.getKey(), entry.getValue());
        }

        zipOutputStream.flush();
        zipOutputStream.close(); // this closes outB

        final ByteArrayInputStream zipFile = new ByteArrayInputStream(outB.toByteArray());
        PortletResponseUtil.sendFile(request, response, "LicensesBackup.lics", zipFile, "application/zip");
    }
}
