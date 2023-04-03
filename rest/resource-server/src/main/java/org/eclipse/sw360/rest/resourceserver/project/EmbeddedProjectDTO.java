/*
 * Copyright TOSHIBA CORPORATION, 2023. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import org.eclipse.sw360.datahandler.thrift.projects.ProjectDTO;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "sw360:projectDTOs")
public class EmbeddedProjectDTO extends ProjectDTO {

    private static final long serialVersionUID = 1L;

}
