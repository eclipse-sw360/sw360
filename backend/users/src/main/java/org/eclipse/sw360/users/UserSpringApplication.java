/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Users backend service as a standalone Spring Boot application.
 *
 * <p>Previously the users service was packaged as a WAR and deployed alongside the
 * other backend services on a shared Tomcat instance, where it was exposed at
 * {@code /users/thrift} via Apache Thrift over HTTP.
 *
 * <p>It is now an independent executable JAR running on its own embedded Tomcat
 * (default port {@code 8090}), exposing a plain REST API at {@code /users/**}.
 * The resource-server reaches it via {@code RestTemplate} instead of
 * {@code THttpClient + TCompactProtocol}.
 *
 * <p>Configuration is read from {@code application.properties} on the classpath
 * and can be overridden with standard Spring Boot externalized configuration
 * (environment variables, {@code -D} JVM arguments, etc.).
 */
@SpringBootApplication
public class UserSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserSpringApplication.class, args);
    }
}
