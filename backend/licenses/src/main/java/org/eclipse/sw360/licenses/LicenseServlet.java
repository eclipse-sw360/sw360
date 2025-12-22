/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;

import org.apache.thrift.protocol.TProtocolFactory;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Thrift Servlet instantiation
 *
 * @author cedric.bodet@tngtech.com
 * @author Andreas.Reichel@tngtech.com
 */
@Component
public class LicenseServlet extends Sw360ThriftServlet {

    @Autowired
    public LicenseServlet(LicenseHandler licenseHandler, TProtocolFactory thriftProtocolFactory) {
        // Create a service processor using the provided handler
        super(new LicenseService.Processor<>(licenseHandler), thriftProtocolFactory);
    }

}
