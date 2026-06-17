/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022.  Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxdocument;

import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocumentService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.net.MalformedURLException;

public class SPDXDocumentServlet extends Sw360ThriftServlet {

    public SPDXDocumentServlet() throws MalformedURLException {
        // Create a service processor using the provided handler
        super(new SPDXDocumentService.Processor<>(new SPDXDocumentHandler()), new TCompactProtocol.Factory());
    }

}
