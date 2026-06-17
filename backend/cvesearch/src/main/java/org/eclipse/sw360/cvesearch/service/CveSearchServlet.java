/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.service;

import org.eclipse.sw360.datahandler.thrift.cvesearch.CveSearchService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.apache.thrift.protocol.TCompactProtocol;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;

public class CveSearchServlet extends Sw360ThriftServlet {
    public CveSearchServlet() throws MalformedURLException, FileNotFoundException {
        // Create a service processor using the provided handler
        super(new CveSearchService.Processor<>(new CveSearchHandler()), new TCompactProtocol.Factory());
    }
}
