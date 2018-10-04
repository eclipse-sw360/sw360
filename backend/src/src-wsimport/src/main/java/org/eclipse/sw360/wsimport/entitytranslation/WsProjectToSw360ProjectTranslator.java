/*
 * Copyright (c) Verifa Oy, 2018. Part of the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.wsimport.entitytranslation;

import org.eclipse.sw360.wsimport.domain.WsProject;

import java.util.HashMap;

import static org.eclipse.sw360.wsimport.utility.TranslationConstants.IMPORTED_FROM_WHITESOURCE;
import static org.eclipse.sw360.wsimport.utility.TranslationConstants.WS_ID;

/**
 * @author ksoranko@verifa.io
 */
public class WsProjectToSw360ProjectTranslator implements EntityTranslator<WsProject, org.eclipse.sw360.datahandler.thrift.projects.Project>{

    @Override
    public org.eclipse.sw360.datahandler.thrift.projects.Project apply(WsProject wsProject) {

        org.eclipse.sw360.datahandler.thrift.projects.Project sw360Project = new org.eclipse.sw360.datahandler.thrift.projects.Project();

        sw360Project.setExternalIds(new HashMap<>());
        sw360Project.getExternalIds().put(WS_ID, Integer.toString(wsProject.getId()));
        sw360Project.setDescription(wsProject.getProjectToken());
        sw360Project.setName(wsProject.getProjectName());
        sw360Project.setCreatedOn(wsProject.getCreationDate());
        sw360Project.setDescription(IMPORTED_FROM_WHITESOURCE);

        return sw360Project;
    }

}
