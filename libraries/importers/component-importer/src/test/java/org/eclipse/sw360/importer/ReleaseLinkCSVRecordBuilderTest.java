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

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author johannes.najjar@tngtech.com
 */
public class ReleaseLinkCSVRecordBuilderTest {
    @Test
    public void testFillComponent() throws Exception {
        final String componentName = "myCompo";

        final Component component = new Component();
        component.setName(componentName);

        final ReleaseLinkCSVRecordBuilder releaseLinkCSVRecordBuilder = new ReleaseLinkCSVRecordBuilder();
        releaseLinkCSVRecordBuilder.fill(component);

        final ReleaseLinkCSVRecord filledRecord = releaseLinkCSVRecordBuilder.build();

        assertThat(filledRecord.getComponentName(), is(componentName));
    }

    @Test
    public void testFillRelease() throws Exception {
        final String releaseName =  "myRelease";
        final String releaseVersion =  "1.862b";

        final Release release = new Release();
        release.setName(releaseName).setVersion(releaseVersion);
        final ReleaseLinkCSVRecordBuilder releaseLinkCSVRecordBuilder = new ReleaseLinkCSVRecordBuilder();
        releaseLinkCSVRecordBuilder.fill(release);
        final ReleaseLinkCSVRecord filledRecord = releaseLinkCSVRecordBuilder.build();

        assertThat(filledRecord.getReleaseIdentifier(), is(SW360Utils.getVersionedName(releaseName, releaseVersion)));
    }

    @Test
    public void testFillLinkedComponent() throws Exception {
        final String componentName = "myCompo";

        final Component component = new Component();
        component.setName(componentName);

        final ReleaseLinkCSVRecordBuilder releaseLinkCSVRecordBuilder = new ReleaseLinkCSVRecordBuilder();
        releaseLinkCSVRecordBuilder.fillLinking(component);

        final ReleaseLinkCSVRecord filledRecord = releaseLinkCSVRecordBuilder.build();

        assertThat(filledRecord.getLinkedComponentName(), is(componentName));
    }

    @Test
    public void testFillLinkedRelease() throws Exception {
        final String releaseName =  "myRelease";
        final String releaseVersion =  "1.862b";

        final Release release = new Release();
        release.setName(releaseName).setVersion(releaseVersion);
        final ReleaseLinkCSVRecordBuilder releaseLinkCSVRecordBuilder = new ReleaseLinkCSVRecordBuilder();
        releaseLinkCSVRecordBuilder.fillLinking(release);
        final ReleaseLinkCSVRecord filledRecord = releaseLinkCSVRecordBuilder.build();

        assertThat(filledRecord.getLinkedReleaseIdentifier(), is(SW360Utils.getVersionedName(releaseName, releaseVersion)));
    }


    @Test
    public void testReleaseReleationship() throws Exception {
        final ReleaseRelationship releaseRelationship = ReleaseRelationship.CONTAINED;

        final ReleaseLinkCSVRecordBuilder releaseLinkCSVRecordBuilder = new ReleaseLinkCSVRecordBuilder();
        releaseLinkCSVRecordBuilder.setRelationship(releaseRelationship);
        final ReleaseLinkCSVRecord filledRecord = releaseLinkCSVRecordBuilder.build();

        assertThat(filledRecord.getRelationship(), is (releaseRelationship));
    }
}