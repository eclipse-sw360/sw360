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

import org.apache.thrift.protocol.TProtocolFactory;
import org.eclipse.sw360.datahandler.thrift.packages.PackageService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serial;


/**
 * Thrift Servlet instantiation
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
@Component
public class PackageServlet extends Sw360ThriftServlet {

    @Serial
    private static final long serialVersionUID = 1L;

    @Autowired
    public PackageServlet(PackageHandler packageHandler, TProtocolFactory thriftProtocolFactory) {
        // Create a service processor using the provided handler
        super(new PackageService.Processor<>(packageHandler), thriftProtocolFactory);
    }

}
