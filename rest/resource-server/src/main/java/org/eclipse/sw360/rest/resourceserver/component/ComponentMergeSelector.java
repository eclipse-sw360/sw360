/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.component;

import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Pure purpose of the class is to be used as body for /mergecomponents
 * endpoint and allow certain properties like `createdBy` as input, which
 * other data objects like `Component` and `ComponentDTO` does not.
 */
public class ComponentMergeSelector extends Component {
    /**
     * Clone the values from component and create an object of this class.
     * @param component Object to clone values from
     * @return New object
     */
    public static @NotNull ComponentMergeSelector from(Component component) {
        ComponentMergeSelector selector = new ComponentMergeSelector();
        ThriftUtils.copyFields(component, selector, Arrays.asList(Component._Fields.values()));
        return selector;
    }
}
