/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.changelogs;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogsService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

/**
 * Thrift Servlet instantiation
 *
 * @author jaideep.palit@siemens.com
 */
public class ChangeLogsServlet extends Sw360ThriftServlet {

    public ChangeLogsServlet() throws MalformedURLException, IOException {
        // Create a service processor using the provided handler
        super(new ChangeLogsService.Processor<>(new ChangeLogsHandler()), new TCompactProtocol.Factory());
    }

}
