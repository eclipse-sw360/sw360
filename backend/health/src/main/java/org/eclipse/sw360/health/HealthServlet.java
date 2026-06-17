/*
 * Copyright Bosch.IO 2020.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.health;

import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.datahandler.thrift.health.HealthService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;

import java.net.MalformedURLException;

public class HealthServlet extends Sw360ThriftServlet {
    public HealthServlet() throws MalformedURLException {
        super(new HealthService.Processor<>(new HealthHandler()), new TCompactProtocol.Factory());
    }
}
