/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base configuration for SW360 ArchUnit architecture tests.
 * <p>
 * Provides a shared {@link JavaClasses} instance that imports the production
 * classes of the REST resource-server module. All concrete architecture test
 * classes should reference {@link #restClasses} for their rule checks.
 */
abstract class SW360ArchitectureTest {

    static JavaClasses restClasses;

    @BeforeAll
    static void importClasses() {
        restClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.eclipse.sw360.rest.resourceserver");
    }
}
