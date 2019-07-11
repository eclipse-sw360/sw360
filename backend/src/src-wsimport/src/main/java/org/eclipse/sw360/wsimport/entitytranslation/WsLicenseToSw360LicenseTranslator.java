/*
 * Copyright (c) Verifa Oy, 2018-2019.
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

import java.util.HashMap;

/**
 * @author ksoranko@verifa.io
 */
public class WsLicenseToSw360LicenseTranslator implements EntityTranslator<WsLicense, org.eclipse.sw360.datahandler.thrift.licenses.License> {

    @Override
    public License apply(WsLicense wsLicense) {
        String licenseName = wsLicense.getName();
        String licenseShortName = licenseName
                .replaceAll("[\\s+/]","-");

        License sw360License = new License();
        sw360License.setId(licenseShortName);
        sw360License.setExternalIds(new HashMap<>());
        sw360License.getExternalIds().put(TranslationConstants.WS_ID, wsLicense.getName());
        sw360License.setShortname(licenseShortName);
        sw360License.setFullname(licenseName);
        if (wsLicense.getUrl() != null) {
            sw360License.setExternalLicenseLink(wsLicense.getUrl());
        }
        return sw360License;
    }

}
