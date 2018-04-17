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
package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

/**
 * @author daniele.fognini@tngtech.com
 */
public class SW360ConstantsTest {
    @Test
    public void testProjectsCanHaveAllAttachmentTypes() throws Exception {
        Collection<AttachmentType> types = SW360Constants.allowedAttachmentTypes(SW360Constants.TYPE_PROJECT);

        assertThat(types, containsInAnyOrder(AttachmentType.values()));
    }

    @Test
    public void testComponentsCanNotHaveReports() throws Exception {
        Collection<AttachmentType> types = SW360Constants.allowedAttachmentTypes(SW360Constants.TYPE_COMPONENT);

        for (AttachmentType attachmentType : AttachmentType.values()) {
            if (attachmentType == AttachmentType.CLEARING_REPORT) {
                assertThat(types, not(hasItem(equalTo(attachmentType))));
            } else {
                assertThat(types, hasItem(equalTo(attachmentType)));
            }
        }

    }
}