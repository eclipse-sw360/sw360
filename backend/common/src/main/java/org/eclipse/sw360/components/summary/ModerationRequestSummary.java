/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.common.PojoFields;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ModerationRequestSummary extends DocumentSummary<ModerationRequest> {

    private static final String[] SUMMARY_FIELDS = {
            "id",
            "documentId",
            "documentType",
            "documentName",
            "moderationState",
            "requestingUser",
            "moderators",
            "timestamp",
            "timestampOfDecision",
            "requestingUserDepartment",
            "componentType"
    };

    @Override
    protected ModerationRequest summary(SummaryType type, ModerationRequest document) {
        ModerationRequest copy = new ModerationRequest();
        for (String name : SUMMARY_FIELDS) {
            FieldAccessor<ModerationRequest> field = PojoFields.byName(ModerationRequest.class, name);
            if (field != null && field.isSet(document)) {
                field.set(copy, field.get(document));
            }
        }
        return copy;
    }
}
