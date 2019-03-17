/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.codescoop;

import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.datahandler.thrift.codescoop.CodescoopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Codescoop Servlet instantiation
 *
 * @author alex.skorohod@codescoop.com
 */
@Controller
public class CodescoopServlet extends CodescoopSpringServlet {

    @Autowired
    public CodescoopServlet(CodescoopHandler handler) {
        super(new CodescoopService.Processor<>(handler), new TCompactProtocol.Factory());
    }
}
