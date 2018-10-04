/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * With modifications by Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.wsimport.entitytranslation.helper;

import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;

public class ReleaseRelation {
    private final String releaseId;
    private final ReleaseRelationship releaseRelationship;

    public ReleaseRelation(String releaseId, ReleaseRelationship releaseRelationship) {
        this.releaseId = releaseId;
        this.releaseRelationship = releaseRelationship;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public ReleaseRelationship getReleaseRelationship() {
        return releaseRelationship;
    }

    public ProjectReleaseRelationship getProjectReleaseRelationship() {
        return new ProjectReleaseRelationship(releaseRelationship, MainlineState.OPEN);
    }
}
