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

import static org.eclipse.sw360.datahandler.common.ImportCSV.readAsCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToCompCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToComponentAttachmentCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.convertCSVRecordsToReleaseLinkCSVRecords;
import static org.eclipse.sw360.importer.ComponentImportUtils.writeAttachmentsToDatabase;
import static org.eclipse.sw360.importer.ComponentImportUtils.writeReleaseLinksToDatabase;
import static org.eclipse.sw360.importer.ComponentImportUtils.writeToDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.RequestContext;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.importer.ComponentAttachmentCSVRecord;
import org.eclipse.sw360.importer.ComponentCSVRecord;
import org.eclipse.sw360.importer.ReleaseLinkCSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;

import lombok.RequiredArgsConstructor;

@SuppressWarnings("serial")
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ImportExportService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;
    
    @JsonInclude
    public RequestSummary uploadComponent(User sw360User, MultipartFile file, HttpServletRequest request, HttpServletResponse response) throws IOException, TException, ServletException {
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new RuntimeException("Unable to upload component csv file. User is not admin");
        }
        List<CSVRecord> releaseRecords = getCSVFromRequest(request, "file");
        FluentIterable<ComponentCSVRecord> compCSVRecords = convertCSVRecordsToCompCSVRecords(releaseRecords);
        ComponentService.Iface  sw360ComponentClient = getThriftComponentClient();
        VendorService.Iface sw360VendorClient =  getThriftVendorClient();
        AttachmentService.Iface sw360AttachmentClient = getThriftAttachmentClient();
        RequestSummary requestSummary = writeToDatabase(compCSVRecords, sw360ComponentClient, sw360VendorClient, sw360AttachmentClient, sw360User);
        return requestSummary;
    }

    private List<CSVRecord> getCSVFromRequest(HttpServletRequest request, String fileUploadFormId) throws IOException, TException, ServletException {
        final InputStream stream = getInputStreamFromRequest(request, fileUploadFormId);
        return readAsCSVRecords(stream);
    }
    
    private InputStream getInputStreamFromRequest(HttpServletRequest request, String fileUploadFormId) throws IOException, ServletException {
        Collection<Part> parts = request.getParts();

        for (Part part : parts) {
            if (!part.getName().equals(fileUploadFormId)) {
                return part.getInputStream();
            }
        }

        throw new IOException("File not found in the request with the specified field name.");
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }

    private VendorService.Iface getThriftVendorClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/vendors/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new VendorService.Client(protocol);
    }
    
    private AttachmentService.Iface getThriftAttachmentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/attachments/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new AttachmentService.Client(protocol);
    }

    public RequestSummary uploadReleaseLink(User sw360User, MultipartFile file, HttpServletRequest request) throws IOException, TException, ServletException{
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new RuntimeException("Unable to upload component csv file. User is not admin");
        }
        List<CSVRecord> releaseLinkRecords = getCSVFromRequest(request, "file");
        FluentIterable<ReleaseLinkCSVRecord> csvRecords = convertCSVRecordsToReleaseLinkCSVRecords(releaseLinkRecords);
        ComponentService.Iface  sw360ComponentClient = getThriftComponentClient();
        final RequestSummary requestSummary = writeReleaseLinksToDatabase(csvRecords, sw360ComponentClient, sw360User);
        return requestSummary;
    }

    public RequestSummary uploadComponentAttachment(User sw360User, MultipartFile file, HttpServletRequest request) throws IOException, TException, ServletException{
        if (!PermissionUtils.isUserAtLeast(UserGroup.ADMIN, sw360User)) {
            throw new RuntimeException("Unable to upload component attachment csv file. User is not admin");
        }
        List<CSVRecord> attachmentRecords = getCSVFromRequest(request, "file");
        FluentIterable<ComponentAttachmentCSVRecord> compCSVRecords = convertCSVRecordsToComponentAttachmentCSVRecords(attachmentRecords);
        ComponentService.Iface  sw360ComponentClient = getThriftComponentClient();
        AttachmentService.Iface sw360AttachmentClient = getThriftAttachmentClient();
        final RequestSummary requestSummary = writeAttachmentsToDatabase(compCSVRecords, sw360User, sw360ComponentClient, sw360AttachmentClient);
        return requestSummary;
    }
}
