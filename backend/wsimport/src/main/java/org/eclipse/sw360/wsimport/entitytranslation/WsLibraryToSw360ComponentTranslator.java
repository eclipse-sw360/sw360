/*
 * Copyright (c) Verifa Oy, 2018-2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.entitytranslation;

import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.wsimport.domain.WsLibrary;
import org.eclipse.sw360.datahandler.thrift.components.Component;

import java.util.Collections;
import java.util.HashSet;

/**
 * @author ksoranko@verifa.io
 */
public class WsLibraryToSw360ComponentTranslator implements EntityTranslator<WsLibrary, org.eclipse.sw360.datahandler.thrift.components.Component> {

    @Override
    public org.eclipse.sw360.datahandler.thrift.components.Component apply(WsLibrary wsLibrary) {

        Component sw360Component = new Component(wsLibrary.getName());
        sw360Component.setCategories(new HashSet<>(Collections.singletonList(wsLibrary.getType())));
        sw360Component.setComponentType(ComponentType.OSS);

        return sw360Component;
    }

}
