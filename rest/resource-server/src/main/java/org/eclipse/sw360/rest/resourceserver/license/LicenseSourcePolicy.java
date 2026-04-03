/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.license;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LicenseSourcePolicy {

    @Value("${sw360.license.source.mode:LEGACY}")
    private String sourceMode;

    public LicenseSourceMode getSourceMode() {
        try {
            return LicenseSourceMode.valueOf(sourceMode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid sw360.license.source.mode: " + sourceMode, e);
        }
    }

    public boolean isLicenseDbOnlyMode() {
        return getSourceMode() == LicenseSourceMode.LICENSEDB_ONLY;
    }
}
