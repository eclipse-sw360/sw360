/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.handler;

import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;
import org.eclipse.sw360.fossology.db.FossologyFingerPrintRepository;
import org.apache.thrift.TException;
import org.ektorp.DocumentOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;

@Component
public class FossologyHostKeyHandler {
    private final FossologyFingerPrintRepository fossologyHostKeyConnector;

    @Autowired
    public FossologyHostKeyHandler(FossologyFingerPrintRepository fossologyHostKeyConnector) {
        this.fossologyHostKeyConnector = fossologyHostKeyConnector;
    }

    public List<FossologyHostFingerPrint> getFingerPrints() throws TException {
        final List<FossologyHostFingerPrint> fingerPrints = fossologyHostKeyConnector.getAll();
        return nullToEmptyList(fingerPrints);
    }

    public RequestStatus setFingerPrints(List<FossologyHostFingerPrint> fingerPrints) throws TException {

        final List<DocumentOperationResult> documentOperationResults = fossologyHostKeyConnector.executeBulk(fingerPrints);

        if (documentOperationResults.isEmpty()) {
            return RequestStatus.SUCCESS;
        } else {
            return RequestStatus.FAILURE;
        }
    }
}
