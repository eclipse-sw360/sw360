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

import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.datahandler.thrift.projectimport.ProjectImportService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

/**
 * @author ksoranko@verifa.io
 */
public class WsImportServlet extends Sw360ThriftServlet {
    public WsImportServlet() throws MalformedURLException, FileNotFoundException {
        // Create a service processor using the provided handler
        super(new ProjectImportService.Processor<>(new WsImportHandler()), new TCompactProtocol.Factory());
    }
}
