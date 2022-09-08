/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.apache.thrift.protocol.TCompactProtocol;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Thrift Servlet instantiation
 *
 * @author alex.borodin@evosoft.com
 */
public class LicenseInfoServlet extends Sw360ThriftServlet {

    public LicenseInfoServlet() throws IOException {
        // Create a service processor using the provided handler
        super(new LicenseInfoService.Processor<>(new LicenseInfoHandler()), new TCompactProtocol.Factory());
    }

}
