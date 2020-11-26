/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses;

import org.eclipse.sw360.antenna.sw360.client.rest.resource.SW360ResourcesTestUtils;

public class SW360LicenseTest extends SW360ResourcesTestUtils<SW360License> {
    @Override
    public SW360License prepareItem() {
        SW360License sw360License = new SW360License();
        sw360License.setShortName("Test-2.0");
        sw360License.setFullName("Test License 2.0");
        sw360License.setText("Full License Text");
        return sw360License;
    }

    @Override
    public SW360License prepareItemWithoutOptionalInput() {
        SW360License sw360License = new SW360License();
        sw360License.setFullName("Test License 2.0");
        return sw360License;
    }

    @Override
    public Class<SW360License> getHandledClassType() {
        return SW360License.class;
    }
}
