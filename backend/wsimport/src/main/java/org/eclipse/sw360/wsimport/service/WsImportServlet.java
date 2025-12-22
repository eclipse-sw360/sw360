/*
 * Copyright (c) Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.service;

import org.apache.thrift.protocol.TProtocolFactory;
import org.eclipse.sw360.datahandler.thrift.projectimport.ProjectImportService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author ksoranko@verifa.io
 */
@Component
public class WsImportServlet extends Sw360ThriftServlet {
    @Autowired
    public WsImportServlet(WsImportHandler importHandler, TProtocolFactory thriftProtocolFactory) {
        // Create a service processor using the provided handler
        super(new ProjectImportService.Processor<>(importHandler), thriftProtocolFactory);
    }
}
