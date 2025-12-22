/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.vendors;

import org.apache.thrift.protocol.TProtocolFactory;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Thrift Servlet instantiation
 *
 * @author cedric.bodet@tngtech.com
 * @author johannes.najjar@tngtech.com
 * @author Andreas.Reichel@tngtech.com
 */
@Component
public class VendorServlet extends Sw360ThriftServlet {

    @Autowired
    public VendorServlet(VendorHandler vendorHandler, TProtocolFactory thriftProtocolFactory) {
        // Create a service processor using the provided handler
        super(new VendorService.Processor<>(vendorHandler), thriftProtocolFactory);
    }

}
