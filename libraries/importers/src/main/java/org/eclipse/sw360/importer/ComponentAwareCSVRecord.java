/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.importer;


import org.eclipse.sw360.datahandler.common.SW360Utils;

/**
 * @author johannes.najjar@tngtech.com
 */
public abstract class ComponentAwareCSVRecord implements CustomizedCSVRecord {
    protected final String componentName;
    protected final String releaseName;
    protected final String releaseVersion;

    protected ComponentAwareCSVRecord(String componentName, String releaseName, String releaseVersion) {
        this.componentName = componentName;
        this.releaseName = releaseName;
        this.releaseVersion = releaseVersion;
    }

    public String getReleaseIdentifier() {
        return SW360Utils.getVersionedName(releaseName, releaseVersion);
    }

    public String getComponentName() {
        return componentName;
    }
}
