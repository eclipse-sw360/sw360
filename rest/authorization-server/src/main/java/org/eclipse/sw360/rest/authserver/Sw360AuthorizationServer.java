/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.authserver;

import org.eclipse.sw360.rest.common.PropertyUtils;
import org.eclipse.sw360.rest.common.Sw360CORSFilter;
import org.eclipse.sw360.rest.common.Sw360SecurityFilter;
import org.eclipse.sw360.rest.common.Sw360XssFilter;
import org.eclipse.sw360.rest.common.client.service.Sw360OidcUserInfoService;
import org.eclipse.sw360.rest.common.client.service.Sw360UserDetailsService;
import org.eclipse.sw360.rest.common.security.Sw360GrantedAuthoritiesCalculator;
import org.eclipse.sw360.rest.common.security.Sw360TokenCustomizerConfig;
import org.eclipse.sw360.rest.common.security.Sw360UserDetailsProvider;
import org.eclipse.sw360.rest.common.security.authproviders.Sw360UserAuthenticationProvider;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({
        Sw360CORSFilter.class,
        Sw360XssFilter.class,
        Sw360SecurityFilter.class,
        Sw360UserAuthenticationProvider.class,
        Sw360UserDetailsService.class,
        Sw360UserDetailsProvider.class,
        Sw360GrantedAuthoritiesCalculator.class,
        Sw360OidcUserInfoService.class,
        Sw360TokenCustomizerConfig.class
})
public class Sw360AuthorizationServer extends SpringBootServletInitializer {

    private static final String APPLICATION_ID = "authorization";

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder
            .sources(Sw360AuthorizationServer.class)
            .properties(PropertyUtils.createDefaultProperties(APPLICATION_ID));
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Sw360AuthorizationServer.class)
            .properties(PropertyUtils.createDefaultProperties(APPLICATION_ID))
            .build()
            .run(args);
    }
}
