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
package org.eclipse.sw360.licenses;

import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.net.MalformedURLException;

/**
 * Thrift Servlet instantiation
 *
 * @author cedric.bodet@tngtech.com
 * @author Andreas.Reichel@tngtech.com
 */
public class LicenseServlet extends Sw360ThriftServlet {

    public LicenseServlet() throws MalformedURLException {
        // Create a service processor using the provided handler
        super(new LicenseService.Processor<>(new LicenseHandler()), new TCompactProtocol.Factory());
    }

}
