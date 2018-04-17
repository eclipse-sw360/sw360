/*
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.parsers;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.Visibility;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoParsingResult;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.eclipse.sw360.licenseinfo.TestHelper.makeAttachment;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class LicenseInfoParserTest {

    private User dummyUser = new User().setEmail("dummy@some.domain");

    @Test
    public void testIsApplicableTo() throws Exception {
        LicenseInfoParser parser = new LicenseInfoParser(null, null) {
            @Override
            public List<String> getApplicableFileExtensions() {
                return Arrays.asList(".ext1", ".ext2");
            }

            @Override
            public <T> List<LicenseInfoParsingResult> getLicenseInfos(Attachment attachment, User user, T context) throws TException {
                return null;
            }
        };
        Arrays.stream(AttachmentType.values()).filter(SW360Constants.LICENSE_INFO_ATTACHMENT_TYPES::contains)
                .forEach(attachmentType -> parser.getApplicableFileExtensions().stream().forEach(extension -> {
                    String filename = "filename" + extension;
                    Attachment attachment = makeAttachment(filename, attachmentType);
                    try {
                        assertThat(
                                parser.isApplicableTo(attachment, dummyUser,
                                        new Project().setVisbility(Visibility.ME_AND_MODERATORS).setCreatedBy(dummyUser.getEmail())
                                                .setAttachments(Collections.singleton(
                                                        new Attachment().setAttachmentContentId(attachment.getAttachmentContentId())))),
                                is(true));
                    } catch (TException e) {
                        e.printStackTrace();
                    }
                }));
    }
}
