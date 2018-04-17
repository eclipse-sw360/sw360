/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.importer;

import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * @author johannes.najjar@tngtech.com
 */
public class ReleaseLinkCSVRecordBuilder extends CustomizedCSVRecordBuilder<ReleaseLinkCSVRecord> {
    private String componentName;
    private String releaseName;
    private String releaseVersion;

    private String linkedComponentName;
    private String linkedReleaseName;
    private String linkedReleaseVersion;
    private ReleaseRelationship relationship;

    ReleaseLinkCSVRecordBuilder(CSVRecord record) {
        int i = 0;
        componentName = record.get(i++);
        releaseName = record.get(i++);
        releaseVersion = record.get(i++);
        linkedComponentName = record.get(i++);
        linkedReleaseName = record.get(i++);
        linkedReleaseVersion = record.get(i++);
        relationship = ThriftEnumUtils.stringToEnum(record.get(i), ReleaseRelationship.class);
    }

    ReleaseLinkCSVRecordBuilder() {
        componentName =null;
        releaseName =null;
        releaseVersion =null;
        linkedComponentName =null;
        linkedReleaseName =null;
        linkedReleaseVersion =null;
        relationship = null;
    }

    @Override
    public ReleaseLinkCSVRecord build() {
        return new ReleaseLinkCSVRecord(componentName, releaseName, releaseVersion,
                linkedComponentName, linkedReleaseName,
                linkedReleaseVersion, relationship);
    }

    public void fill(Component component) {
        setComponentName(component.getName());
    }

    public void fill (Release release) {
        setReleaseName(release.getName());
        setReleaseVersion(release.getVersion());
    }

    public void fillLinking(Component component) {
        setLinkedComponentName(component.getName());
    }

    public void fillLinking (Release release) {
        setLinkedReleaseName(release.getName());
        setLinkedReleaseVersion(release.getVersion());
    }

    //Auto generated setters
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setReleaseName(String releaseName) {
        this.releaseName = releaseName;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public void setLinkedComponentName(String linkedComponentName) {
        this.linkedComponentName = linkedComponentName;
    }

    public void setLinkedReleaseName(String linkedReleaseName) {
        this.linkedReleaseName = linkedReleaseName;
    }

    public void setLinkedReleaseVersion(String linkedReleaseVersion) {
        this.linkedReleaseVersion = linkedReleaseVersion;
    }

    public void setRelationship(ReleaseRelationship relationship) {
        this.relationship = relationship;
    }
}
