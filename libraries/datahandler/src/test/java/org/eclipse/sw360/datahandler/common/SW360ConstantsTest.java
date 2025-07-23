/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;

import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

/**
 * @author daniele.fognini@tngtech.com
 */
public class SW360ConstantsTest {
    @Test
    public void testProjectsCanHaveAllAttachmentTypes() {
        Collection<AttachmentType> types = SW360Constants.allowedAttachmentTypes(SW360Constants.TYPE_PROJECT);

        Assert.assertTrue(containsInAnyOrder(AttachmentType.values()).matches(types));
    }

    @Test
    public void testComponentsCanNotHaveReports() {
        Collection<AttachmentType> types = SW360Constants.allowedAttachmentTypes(SW360Constants.TYPE_COMPONENT);

        for (AttachmentType attachmentType : AttachmentType.values()) {
            if (attachmentType == AttachmentType.CLEARING_REPORT) {
                Assert.assertFalse(types.contains(attachmentType));
            } else {
                Assert.assertTrue(types.contains(attachmentType));
            }
        }
    }
}
