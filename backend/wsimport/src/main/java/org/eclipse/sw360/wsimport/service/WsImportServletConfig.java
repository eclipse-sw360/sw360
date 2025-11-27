/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.wsimport.service;

import jakarta.servlet.Servlet;
import org.apache.thrift.protocol.TProtocolFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WsImportServletConfig {
    @Bean(name="wsimportServlet")
    public Servlet wsimport(TProtocolFactory thriftProtocolFactory, WsImportHandler importHandler) {
        return new WsImportServlet(importHandler, thriftProtocolFactory);
    }
}
