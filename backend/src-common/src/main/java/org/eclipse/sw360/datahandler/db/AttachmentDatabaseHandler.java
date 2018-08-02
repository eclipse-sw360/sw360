/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.attachments.db;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentContent;
import org.eclipse.sw360.datahandler.thrift.attachments.DatabaseAddress;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.ektorp.DocumentOperationResult;
import org.ektorp.http.HttpClient;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.Duration.durationOf;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertUser;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.validateAttachment;

/**
 * Class for accessing the CouchDB database for attachment objects
 *
 * @author: alex.borodin@evosoft.com
 */
public class AttachmentDatabaseHandler {
    private final DatabaseConnector db;
    private final AttachmentRepository repository;
    private final AttachmentConnector attachmentConnector;

    private static final Logger log = Logger.getLogger(AttachmentDatabaseHandler.class);

    public AttachmentDatabaseHandler(Supplier<HttpClient> httpClient, String attachmentDbName) throws MalformedURLException {
        db = new DatabaseConnector(httpClient, attachmentDbName);
        attachmentConnector = new AttachmentConnector(httpClient, attachmentDbName, durationOf(30, TimeUnit.SECONDS));
        repository = new AttachmentRepository(db);
    }

    public AttachmentConnector getAttachmentConnector(){
        return attachmentConnector;
    }

    public AttachmentContent add(AttachmentContent attachmentContent){
        repository.add(attachmentContent);
        return attachmentContent;
    }
    public List<AttachmentContent> makeAttachmentContents(List<AttachmentContent> attachmentContents) throws TException {
        final List<DocumentOperationResult> documentOperationResults = repository.executeBulk(attachmentContents);
        if (!documentOperationResults.isEmpty())
            log.error("Failed Attachment store results " + documentOperationResults);

        return attachmentContents.stream().filter(AttachmentContent::isSetId).collect(Collectors.toList());
    }
    public AttachmentContent getAttachmentContent(String id) throws TException {
        AttachmentContent attachment = repository.get(id);
        assertNotNull(attachment, "Cannot find "+ id + " in database.");
        validateAttachment(attachment);

        return attachment;
    }
    public void updateAttachmentContent(AttachmentContent attachment) throws TException {
        attachmentConnector.updateAttachmentContent(attachment);
    }
    public RequestSummary bulkDelete(List<String> ids) throws TException {
        final List<DocumentOperationResult> documentOperationResults = repository.deleteIds(ids);
        return CommonUtils.getRequestSummary(ids, documentOperationResults);
    }
    public RequestStatus deleteAttachmentContent(String attachmentId) throws TException {
        attachmentConnector.deleteAttachment(attachmentId);

        return RequestStatus.SUCCESS;
    }
    public RequestSummary vacuumAttachmentDB(User user, Set<String> usedIds) throws TException {
        assertUser(user);
        return repository.vacuumAttachmentDB(user, usedIds);
    }
    public String getSha1FromAttachmentContentId(String attachmentContentId){
        return attachmentConnector.getSha1FromAttachmentContentId(attachmentContentId);
    }
}
