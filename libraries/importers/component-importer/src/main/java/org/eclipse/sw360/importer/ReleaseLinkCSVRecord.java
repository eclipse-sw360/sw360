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

import org.eclipse.sw360.commonIO.SampleOptions;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;

/**
 * @author johannes.najjar@tngtech.com
 */
public class ReleaseLinkCSVRecord extends  ComponentAwareCSVRecord{
    //linking Release done by inheritance

    // linked Release
    private final String linkedComponentName;
    private final String linkedReleaseName;
    private final String linkedReleaseVersion;
    private final ReleaseRelationship relationship;

    ReleaseLinkCSVRecord(String componentName, String releaseName, String releaseVersion,
                                String linkedComponentName, String linkedReleaseName,
                                String linkedReleaseVersion, ReleaseRelationship relationship) {
        super(componentName, releaseName, releaseVersion);
        this.linkedComponentName = linkedComponentName;
        this.linkedReleaseName = linkedReleaseName;
        this.linkedReleaseVersion = linkedReleaseVersion;
        this.relationship = relationship;
    }

    @Override
    public Iterable<String> getCSVIterable() {
        final ArrayList<String> elements = new ArrayList<>();

        elements.add(componentName);
        elements.add(releaseName);
        elements.add(releaseVersion);
        elements.add(linkedComponentName);
        elements.add(linkedReleaseName);
        elements.add(linkedReleaseVersion);
        elements.add(relationship.name());

        return elements;
    }

    public static Iterable<String> getCSVHeaderIterable() {
        final ArrayList<String> elements = new ArrayList<>();

        elements.add("componentName");
        elements.add("releaseName");
        elements.add("releaseVersion");
        elements.add("linkedComponentName");
        elements.add("linkedReleaseName");
        elements.add("linkedReleaseVersion");
        elements.add("ReleaseRelationship");

        return elements;
    }

    public static Iterable<String> getSampleInputIterable() {
        final ArrayList<String> elements = new ArrayList<>();

        elements.add("componentName");
        elements.add("releaseName");
        elements.add(SampleOptions.VERSION_OPTION);
        elements.add("linkedComponentName");
        elements.add("linkedReleaseName");
        elements.add(SampleOptions.VERSION_OPTION);
        elements.add(SampleOptions.RELEASE_RELEATIONSHIP_OPTIONS);

        return elements;
    }

    public String getLinkedReleaseIdentifier() {
        return SW360Utils.getVersionedName(linkedReleaseName, linkedReleaseVersion);
    }

    public String getLinkedComponentName() {
        return linkedComponentName;
    }

    public ReleaseRelationship getRelationship() {
        return relationship;
    }

    public static ReleaseLinkCSVRecordBuilder builder(){
        return new ReleaseLinkCSVRecordBuilder();
    }

    public static ReleaseLinkCSVRecordBuilder builder( CSVRecord in){
        return new ReleaseLinkCSVRecordBuilder( in );
    }

}
