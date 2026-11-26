/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.http;

import tools.jackson.databind.json.JsonMapper;
import org.eclipse.sw360.http.utils.HttpConstants;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Test class for {@code NewRequestBodyBuilderImpl} file body behavior.
 */
public class NewRequestBodyBuilderImplTest {

    @Test
    public void testFileBodyWithExistingFile() throws IOException {
        NewRequestBodyBuilderImpl bodyBuilder = new NewRequestBodyBuilderImpl(JsonMapper.builderWithJackson2Defaults().build());
        Path tempFile = Files.createTempFile("new-request-body", ".txt");
        try {
            Files.writeString(tempFile, "file-body-content");

            bodyBuilder.file(tempFile, HttpConstants.CONTENT_OCTET_STREAM);

            assertThat(bodyBuilder.getBody()).isNotNull();
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testFileBodyWithMissingFile() {
        NewRequestBodyBuilderImpl bodyBuilder = new NewRequestBodyBuilderImpl(JsonMapper.builderWithJackson2Defaults().build());
        Path missingFile = Path.of("target", "does-not-exist", "missing-upload.bin");

        try {
            bodyBuilder.file(missingFile, HttpConstants.CONTENT_OCTET_STREAM);
            fail("No exception thrown!");
        } catch (IllegalStateException ex) {
            assertThat(ex.getCause()).isInstanceOf(IOException.class);
        }
    }
}
