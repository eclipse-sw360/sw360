/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.portal.tags.OutTag;

/**
 * This prints a component name
 *
 * @author thomas.maier@evosoft.com
 */
public class ComponentName extends OutTag {
    public void setComponent(Component component) {
        this.value = SW360Utils.printName(component);
    }
}
