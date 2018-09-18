/*
 * Copyright (c) Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.wsimport.entitytranslation;

import org.eclipse.sw360.wsimport.domain.WsLicense;
import org.eclipse.sw360.wsimport.utility.TranslationConstants;
import org.eclipse.sw360.datahandler.thrift.licenses.License;

import java.util.Arrays;
import java.util.HashMap;

import static org.eclipse.sw360.wsimport.utility.TranslationConstants.SUSPECTED;

/**
 * @author ksoranko@verifa.io
 */
public class WsLicenseToSw360LicenseTranslator implements EntityTranslator<WsLicense, org.eclipse.sw360.datahandler.thrift.licenses.License> {

    @Override
    public License apply(WsLicense wsLicense) {
        String license = wsLicense.getName().replaceFirst(SUSPECTED, "").trim();
        String licenseWithDashes = license.replaceAll("[\\s+/]","-");

        License sw360License = new License();
        sw360License.setId(licenseWithDashes);
        sw360License.setExternalIds(new HashMap<>());
        sw360License.getExternalIds().put(TranslationConstants.WS_ID, wsLicense.getName());
        sw360License.setShortname(licenseWithDashes);
        sw360License.setFullname(license);
        sw360License.setExternalLicenseLink(wsLicense.getUrl());

        return sw360License;
    }

}
