/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.spring.CouchDbPropertyLoader;
import org.eclipse.sw360.datahandler.spring.DatabaseConfig;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * An abstract base class for Spring's WebApplicationInitializer.
 * This class provides common setup for a Spring web application,
 * including creating the root application context, registering common
 * configuration classes, and handling property loading from both
 * classpath and a system-wide configuration path with override capabilities.
 * Subclasses must implement {@link #getServletName()} to specify
 * their unique servlet identifier for registration with Tomcat.
 */
public abstract class AbstractBackendServletInitializer implements WebApplicationInitializer {
    private static final Logger log = LogManager.getLogger(AbstractBackendServletInitializer.class);

    /**
     * Subclasses must provide their unique application name.
     *
     * @return The unique name of the backend module.
     */
    protected abstract String getServletName();

    protected abstract ServletRegistration.Dynamic getServletRegistration(
            @NotNull ServletContext servletContext,
            AnnotationConfigWebApplicationContext rootContext
    );

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        log.info("Starting up backend module: {}", getServletName());

        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(DatabaseConfig.class);

        CouchDbPropertyLoader.loadCouchDbProperties(rootContext.getEnvironment());

        rootContext.refresh();

        servletContext.addListener(new ContextLoaderListener(rootContext));
        servletContext.addListener(new SW360ServiceContextListener());

        ServletRegistration.Dynamic servletRegistration = getServletRegistration(servletContext, rootContext);
        if (servletRegistration == null) {
            throw new ServletException("Servlet registration failed for: " + getServletName());
        }

        servletRegistration.setLoadOnStartup(1);
        servletRegistration.addMapping("/thrift");
    }
}
