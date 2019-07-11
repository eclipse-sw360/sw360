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

import org.eclipse.sw360.wsimport.domain.WsLibrary;
import org.eclipse.sw360.wsimport.utility.TranslationConstants;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import java.util.HashMap;
import java.util.HashSet;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.wsimport.utility.TranslationConstants.*;

/**
 * @author ksoranko@verifa.io
 */
public class WsLibraryToSw360ReleaseTranslator implements EntityTranslator<WsLibrary, Release> {

    @Override
    public Release apply(WsLibrary wsLibrary) {

        Release sw360Release = new Release();
        sw360Release.setExternalIds(new HashMap<>());
        sw360Release.setName(wsLibrary.getName());
        sw360Release.getExternalIds().put(TranslationConstants.WS_ID, Integer.toString(wsLibrary.getKeyId()));
        sw360Release.getExternalIds().put(FILENAME, wsLibrary.getFilename());

        if (wsLibrary.getReferences() != null) {
            sw360Release.setDownloadurl(wsLibrary.getReferences().getUrl());
            if (!isNullOrEmpty(wsLibrary.getReferences().getPomUrl())) {
                sw360Release.getExternalIds().put(POM_FILE_URL, wsLibrary.getReferences().getPomUrl());
            }
            if (!isNullOrEmpty(wsLibrary.getReferences().getScmUrl())) {
                sw360Release.getExternalIds().put(SCM_URL, wsLibrary.getReferences().getScmUrl());
            }
        }

        if (!isNullOrEmpty(wsLibrary.getVersion())) {
            String version = wsLibrary.getVersion().replaceFirst("^(?i)v","");
            sw360Release.setVersion(version.replaceAll(VERSION_SUFFIX_REGEX,"").trim());
        } else {
            sw360Release.setVersion(UNKNOWN);
        }

        sw360Release.setClearingState(ClearingState.NEW_CLEARING);

        return sw360Release;
    }

}
