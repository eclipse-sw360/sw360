/*
 * Copyright Siemens AG, 2023. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * SPDX-FileCopyrightText: 2023, Siemens AG. Part of the SW360 Portal Project.
 * SPDX-FileContributor: Gaurav Mishra <mishra.gaurav@siemens.com>
 */

package org.eclipse.sw360.rest.resourceserver.moderationrequest;

import lombok.Getter;
import lombok.Setter;

/**
 * Input for PATCH request on moderation request to accept/reject it.
 */
@Setter
@Getter
public class ModerationPatch {
    private ModerationAction action;
    private String comment;

    public ModerationPatch() {
        this.action = null;
        this.comment = null;
    }

    /**
     * Actions which can be performed on the moderation request
     */
    public enum ModerationAction {
        ACCEPT,
        REJECT,
        UNASSIGN,
        ASSIGN,
        POSTPONE
    }
}
