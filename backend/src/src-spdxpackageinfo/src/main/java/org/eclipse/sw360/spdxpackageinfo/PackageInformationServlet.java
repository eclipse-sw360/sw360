/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.spdxpackageinfo;

import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformationService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.net.MalformedURLException;

public class PackageInformationServlet extends Sw360ThriftServlet {

    public PackageInformationServlet() throws MalformedURLException {
        // Create a service processor using the provided handler
        super(new PackageInformationService.Processor<>(new PackageInformationHandler()), new TCompactProtocol.Factory());
    }

}
