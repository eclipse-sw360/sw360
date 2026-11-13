/*
 * Copyright Pratyay Patel, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.sw360.rest.resourceserver.license.Sw360LicenseService;
import org.eclipse.sw360.rest.resourceserver.obligation.Sw360ObligationService;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.eclipse.sw360.rest.resourceserver.vendor.Sw360VendorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class RestControllerHelperExternalIdsTest {

    @Mock
    private Sw360UserService userService;

    @Mock
    private Sw360VendorService vendorService;

    @Mock
    private Sw360LicenseService licenseService;

    @Mock
    private Sw360ObligationService obligationService;

    private RestControllerHelper<Object> helper;

    @BeforeEach
    public void setUp() {
        helper = new RestControllerHelper<>(userService, vendorService, licenseService, obligationService,
                new SimpleModule());
    }

    @Test
    public void shouldMapKeyWithoutValueToEmptyString() {
        MultiValueMap<String, String> parameters = helper.parseQueryStringForExtIds("package-url");

        assertThat(parameters.get("package-url")).containsExactly("");
    }

    @Test
    public void shouldMapKeyWithEmptyValueToEmptyString() {
        MultiValueMap<String, String> parameters = helper.parseQueryStringForExtIds("package-url=");

        assertThat(parameters.get("package-url")).containsExactly("");
    }

    @Test
    public void shouldKeepValuesWhenKeyWithoutValueIsMixedWithOtherKeys() {
        MultiValueMap<String, String> parameters =
                helper.parseQueryStringForExtIds("package-url&mainline-id-component=1432&mainline-id-component=4876");

        assertThat(parameters.get("package-url")).containsExactly("");
        assertThat(parameters.get("mainline-id-component")).containsExactly("1432", "4876");
    }

    @Test
    public void shouldDecodeEncodedValues() {
        MultiValueMap<String, String> parameters =
                helper.parseQueryStringForExtIds("package-url=pkg%3Anpm%2F%2540ngx-translate%2Fcore%4014.0.0");

        assertThat(parameters.get("package-url")).containsExactly("pkg:npm/%40ngx-translate/core@14.0.0");
    }

    @Test
    public void shouldReturnEmptyMapForMissingQuery() {
        assertThat(helper.parseQueryStringForExtIds(null)).isEmpty();
        assertThat(helper.parseQueryStringForExtIds("")).isEmpty();
    }
}
