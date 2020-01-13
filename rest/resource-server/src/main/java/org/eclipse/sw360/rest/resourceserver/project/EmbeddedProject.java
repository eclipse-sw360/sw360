/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.project;

import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.springframework.hateoas.core.Relation;

/**
 * 
 * This class is used for json customization for embedded project.
 * 
 * @author smruti.sahoo@siemens.com
 *
 */
@Relation(collectionRelation = "sw360:projects")
public class EmbeddedProject extends Project {

	private static final long serialVersionUID = 1L;

}
