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
package org.eclipse.sw360.vendors;

import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.io.IOException;

/**
 * Thrift Servlet instantiation
 *
 * @author cedric.bodet@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author Andreas.Reichel@tngtech.com
 */
public class VendorServlet extends Sw360ThriftServlet {

    public VendorServlet() throws IOException {
        // Create a service processor using the provided handler
        super(new VendorService.Processor<>(new VendorHandler()), new TCompactProtocol.Factory());
    }

}
