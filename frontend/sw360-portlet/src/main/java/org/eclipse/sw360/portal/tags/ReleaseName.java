/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.Release;

/**
 * This prints a relase name and version
 *
 * @author Johannes.Najjar@tngtech.com
 */
public class ReleaseName extends OutTag {
    public void setRelease(Release release) {
        this.value = SW360Utils.printName(release);
    }

}
