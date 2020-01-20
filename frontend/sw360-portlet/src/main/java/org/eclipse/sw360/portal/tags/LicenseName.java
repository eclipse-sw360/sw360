/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.licenses.License;

/**
 * This prints a license name and version
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class LicenseName extends OutTag {
    public void setLicense(License license) {
        this.value = SW360Utils.printName(license);
    }
}
