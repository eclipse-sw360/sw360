/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
