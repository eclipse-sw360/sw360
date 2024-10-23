/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
