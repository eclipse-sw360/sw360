/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.assertj.core.api.Assertions.*;

class NewRequestBodyBuilderImplTest {
    private ObjectMapper mapper;
    private NewRequestBodyBuilderImpl builder;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        builder = new NewRequestBodyBuilderImpl(mapper);
    }

    @Test
    void file_shouldThrowIfFileDoesNotExist() {
        Path nonExistent = Path.of("nonexistent-file.txt");
        assertThatThrownBy(() -> builder.file(nonExistent, "text/plain"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist");
    }

    @DisabledOnOs(OS.WINDOWS)
    @Test
    void file_shouldThrowIfFileNotReadable() throws IOException {
        Path temp = Files.createTempFile("sw360-test-unreadable", ".txt");
        try {
            Files.writeString(temp, "test");
            temp.toFile().setReadable(false);
            assertThatThrownBy(() -> builder.file(temp, "text/plain"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not readable");
        } finally {
            temp.toFile().setReadable(true);
            Files.deleteIfExists(temp);
        }
    }

    @Test
    void file_shouldSetBodyAndFileName() throws IOException {
        Path temp = Files.createTempFile("sw360-test", ".txt");
        try {
            Files.writeString(temp, "hello world", StandardOpenOption.WRITE);
            builder.file(temp, "text/plain");
            assertThat(builder.getBody()).isNotNull();
            assertThat(builder.getFileName()).isEqualTo(temp.getFileName().toString());
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
