/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.utils;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360LicenseList;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for {@code SW360ResourceUtils}. The main functionality of this
 * class is tested together with the concrete client implementations. This
 * test class just deals with some corner cases.
 */
public class SW360ResourceUtilsTest {
    @Test
    public void testNonExistingEmbeddedElementsAreHandled() {
        SW360LicenseList licenseList = new SW360LicenseList();

        List<SW360SparseLicense> licenses = SW360ResourceUtils.getSw360SparseLicenses(licenseList);
        assertThat(licenses).hasSize(0);
    }
}
