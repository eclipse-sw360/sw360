/*
 * SPDX-FileCopyrightText: 2026 Eclipse SW360 Contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.migration.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. The real migration does not add a new application — it modifies
 * the existing {@code Sw360ResourceServer} to include the migrated handler beans
 * in its component scan. This class exists solely to make the PoC self-contained.
 */
@SpringBootApplication
public class MigrationPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MigrationPocApplication.class, args);
    }
}
