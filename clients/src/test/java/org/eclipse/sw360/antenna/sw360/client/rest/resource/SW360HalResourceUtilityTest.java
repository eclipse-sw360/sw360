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
package org.eclipse.sw360.antenna.sw360.client.rest.resource;

import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SW360HalResourceUtilityTest {
    @Test
    public void testGetLastIndexOfSelfLinkNoLinkObjects() {
        Optional<String> optId = SW360HalResourceUtility.getLastIndexOfSelfLink((LinkObjects) null);

        assertThat(optId).isNotPresent();
    }

    @Test
    public void testGetLastIndexOfSelfLinkNullHRef() {
        Self self = new Self();

        Optional<String> optId = SW360HalResourceUtility.getLastIndexOfSelfLink(self);
        assertThat(optId).isNotPresent();
    }

    @Test
    public void testGetLastIndexOfSelfLinkEmptyHref() {
        Self self = new Self("");

        Optional<String> optId = SW360HalResourceUtility.getLastIndexOfSelfLink(self);
        assertThat(optId).isNotPresent();
    }
}