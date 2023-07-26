/*
 * Copyright Siemens Healthineers GmBH, 20235. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.packages.Package;

/**
 * This tag used used to print the package name and version
 *
 * @author abdul.kapti@siemens-healthineers.com
 */
public class PackageName extends OutTag {
    private static final long serialVersionUID = 1L;

    public void setPkg(Package pkg) {
        this.value = SW360Utils.printName(pkg);
    }

}
