/*
 * Copyright Siemens Healthineers GmBH, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.packages;

import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.apache.thrift.protocol.TCompactProtocol;

import java.io.IOException;
import java.net.MalformedURLException;


/**
 * Thrift Servlet instantiation
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
public class PackageServlet extends Sw360ThriftServlet {

    private static final long serialVersionUID = 1L;

    public PackageServlet() throws MalformedURLException, IOException {
        // Create a service processor using the provided handler
        super(new PackageService.Processor<>(new PackageHandler()), new TCompactProtocol.Factory());
    }

}
