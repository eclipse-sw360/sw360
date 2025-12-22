/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.vendors;

import jakarta.servlet.Servlet;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VendorServletConfig {
    @Bean(name="vendorsServlet")
    public Servlet vendors(TProtocolFactory thriftProtocolFactory, VendorHandler vendorHandler) {
        return new VendorServlet(vendorHandler, thriftProtocolFactory);
    }
}
