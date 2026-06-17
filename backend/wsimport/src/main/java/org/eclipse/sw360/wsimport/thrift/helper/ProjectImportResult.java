/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * With modifications by Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.thrift.helper;

import java.util.Optional;

/**
 * @author: ksoranko@verifa.io
 */
public class ProjectImportResult {
    private final Optional<String> projectId;
    private final Optional<ProjectImportError> error;

    public ProjectImportResult(String projectId) {
        this.projectId = Optional.of(projectId);
        this.error = Optional.empty();
    }

    public ProjectImportResult(ProjectImportError error) {
        this.projectId = Optional.empty();
        this.error = Optional.of(error);
    }

    public boolean isSuccess() {
        return projectId.isPresent();
    }

    public ProjectImportError getError() {
        return error.isPresent() ? error.get() : null;
    }
}
