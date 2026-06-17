/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.moderationrequest;

import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.springframework.hateoas.server.core.Relation;

/**
 * 
 * This class is used for json customization for embedded moderation requests.
 * 
 * @author mishra.gaurav@siemens.com
 *
 */
@Relation(collectionRelation = "sw360:moderationRequests")
public class EmbeddedModerationRequest extends ModerationRequest {
	private static final long serialVersionUID = 1L;
}
