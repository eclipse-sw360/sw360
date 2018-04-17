/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.thrift;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ThriftUtilsTest {


    private Function<Attachment, Object> objectIdExtractor;
    private Function<Attachment, String> stringIdExtractor;

    @Before
    public void setUp() throws Exception {
        objectIdExtractor = ThriftUtils.extractField(Attachment._Fields.ATTACHMENT_CONTENT_ID);
        stringIdExtractor = ThriftUtils.extractField(Attachment._Fields.ATTACHMENT_CONTENT_ID, String.class);
    }

    @Test
    public void testExtractId() throws Exception {
        String contentId = "42";
        Attachment attachment = getAttachment(contentId);

        assertThat(objectIdExtractor.apply(attachment), is((Object) contentId));
        assertThat(stringIdExtractor.apply(attachment), is(contentId));
    }

    @Test
    public void testExtractIdTransformer() throws Exception {
        String contentId = "42";
        String contentId1 = "44";
        List<Attachment> input = ImmutableList.of(getAttachment(contentId), getAttachment(contentId1));
        List<String> expected = ImmutableList.of(contentId, contentId1);

        assertThat(input.stream().map(stringIdExtractor).collect(Collectors.toList()), is(expected));
    }

    private static Attachment getAttachment(String contentId) {
        return new Attachment().setAttachmentContentId(contentId);
    }
}