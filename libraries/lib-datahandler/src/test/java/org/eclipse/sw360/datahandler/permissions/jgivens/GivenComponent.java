/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.permissions.jgivens;

import static org.mockito.Mockito.*;

import org.eclipse.sw360.datahandler.TEnumToString;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.Quoted;
import com.tngtech.jgiven.annotation.ScenarioState;

/**
 * @author kouki1.hama@toshiba.co.jp
 */
public class GivenComponent extends Stage<GivenComponent> {
    @ScenarioState
    private Component component;

    public enum ComponentRole {
        CREATED_BY,
        MODERATOR,
    }

    public GivenComponent a_new_component() {
        component = mock(Component.class);
        Mockito.when(component.getVisbility()).thenReturn(Visibility.EVERYONE);
        return self();
    }

    public GivenComponent a_component_with_$_$(ComponentRole role, @Quoted String m1){
        a_new_component();

        switch (role) {
            case CREATED_BY:
                Mockito.when(component.isSetCreatedBy()).thenReturn(true);
                Mockito.when(component.getCreatedBy()).thenReturn(m1);
                break;
            case MODERATOR:
                Mockito.when(component.isSetModerators()).thenReturn(true);
                Mockito.when(component.getModerators()).thenReturn(ImmutableSet.of(m1));
                break;
        }

        return self();
    }

    public GivenComponent with_visibility_$_and_business_unit_$(@TEnumToString Visibility v1, @Quoted String b1) {
        Mockito.when(component.getVisbility()).thenReturn(v1);
        Mockito.when(component.getBusinessUnit()).thenReturn(b1);
        return self();
    }
}
