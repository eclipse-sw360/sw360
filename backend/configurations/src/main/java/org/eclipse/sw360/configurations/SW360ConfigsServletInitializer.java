/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.configurations;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.apache.thrift.server.TServlet;
import org.eclipse.sw360.AbstractBackendServletInitializer;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

public class SW360ConfigsServletInitializer extends AbstractBackendServletInitializer {
    private static final String SERVLET_NAME = "SW360ConfigsService";

    @Override
    protected String getServletName() {
        return SERVLET_NAME;
    }

    @Override
    protected ServletRegistration.Dynamic getServletRegistration(
            @NotNull ServletContext servletContext,
            AnnotationConfigWebApplicationContext rootContext
    ) {
        TServlet servletInstance = rootContext.getBean("configsServlet", TServlet.class);
        return servletContext.addServlet(
                getServletName(),
                servletInstance
        );
    }
}
