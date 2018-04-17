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
package org.eclipse.sw360.search;

import org.eclipse.sw360.datahandler.thrift.search.SearchService;
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
public class SearchServlet extends Sw360ThriftServlet {

    public SearchServlet() throws IOException {
        // Create a service processor using the provided handler
        super(new SearchService.Processor<SearchHandler>(new SearchHandler()), new TCompactProtocol.Factory());
    }
}
