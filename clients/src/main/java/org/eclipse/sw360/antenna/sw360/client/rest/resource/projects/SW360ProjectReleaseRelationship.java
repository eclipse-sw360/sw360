/*
 * Copyright (c) Bosch Software Innovations GmbH 2017-2018.
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.antenna.sw360.client.rest.resource.projects;

import java.util.Objects;

public final class SW360ProjectReleaseRelationship {
    private SW360ReleaseRelationship releaseRelation;
    private SW360MainlineState mainlineState;

    public  SW360ProjectReleaseRelationship() {}

    public SW360ProjectReleaseRelationship(SW360ReleaseRelationship releaseRelation, SW360MainlineState mainlineState) {
        this.releaseRelation = releaseRelation;
        this.mainlineState = mainlineState;
    }

    public SW360ReleaseRelationship getReleaseRelation() {
        return this.releaseRelation;
    }

    public SW360ProjectReleaseRelationship setReleaseRelation(SW360ReleaseRelationship releaseRelation) {
        this.releaseRelation = releaseRelation;
        return this;
    }

    public SW360MainlineState getMainlineState() {
        return this.mainlineState;
    }

    public SW360ProjectReleaseRelationship setMainlineState(SW360MainlineState mainlineState) {
        this.mainlineState = mainlineState;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SW360ProjectReleaseRelationship that = (SW360ProjectReleaseRelationship) o;
        return releaseRelation == that.releaseRelation &&
                mainlineState == that.mainlineState;
    }

    @Override
    public int hashCode() {
        return Objects.hash(releaseRelation, mainlineState);
    }
}
