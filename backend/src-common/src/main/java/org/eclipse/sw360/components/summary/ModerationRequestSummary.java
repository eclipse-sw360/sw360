/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ModerationRequestSummary extends DocumentSummary<ModerationRequest> {

    @Override
    protected ModerationRequest summary(SummaryType type, ModerationRequest document) {
        ModerationRequest copy = new ModerationRequest();

        copyField(document, copy, ModerationRequest._Fields.ID);
        copyField(document, copy, ModerationRequest._Fields.DOCUMENT_ID);
        copyField(document, copy, ModerationRequest._Fields.DOCUMENT_TYPE);
        copyField(document, copy, ModerationRequest._Fields.DOCUMENT_NAME);
        copyField(document, copy, ModerationRequest._Fields.MODERATION_STATE);
        copyField(document, copy, ModerationRequest._Fields.REQUESTING_USER);
        copyField(document, copy, ModerationRequest._Fields.MODERATORS);
        copyField(document, copy, ModerationRequest._Fields.TIMESTAMP);
        copyField(document, copy, ModerationRequest._Fields.TIMESTAMP_OF_DECISION);
        copyField(document, copy, ModerationRequest._Fields.REQUESTING_USER_DEPARTMENT);
        copyField(document, copy, ModerationRequest._Fields.COMPONENT_TYPE);

        return copy;
    }

}
