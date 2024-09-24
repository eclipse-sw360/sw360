/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.fossology;

import org.eclipse.sw360.datahandler.thrift.fossology.FossologyService;
import org.apache.thrift.protocol.TCompactProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.net.MalformedURLException;

/**
 * Thrift Servlet instantiation
 *
 * @author daniele.fognini@tngtech.com
 */
@Controller
public class FossologyServlet extends SpringTServlet {

    @Autowired
    public FossologyServlet(FossologyHandler fossologyHandler) throws MalformedURLException {
        // Create a service processor using the provided handler
        super(new FossologyService.Processor<>(fossologyHandler), new TCompactProtocol.Factory());
    }

}
