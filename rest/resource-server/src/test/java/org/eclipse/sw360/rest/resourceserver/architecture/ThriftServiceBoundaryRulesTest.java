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

import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Validates that REST module classes do not bypass the Thrift service layer.
 * <p>
 * In the SW360 architecture, the REST module communicates with backend services
 * exclusively through <strong>Thrift clients</strong> (via {@code ThriftClients}).
 * Direct access to:
 * <ul>
 *   <li>Backend handler classes ({@code *DatabaseHandler})</li>
 *   <li>Repository classes ({@code *Repository})</li>
 *   <li>CouchDB/Cloudant client classes (except for the health indicator)</li>
 * </ul>
 * is prohibited from the REST layer.
 */
@DisplayName("Thrift Service Boundary Rules")
class ThriftServiceBoundaryRulesTest extends SW360ArchitectureTest {

    @Test
    @DisplayName("REST module should not directly access database handlers")
    void restModuleShouldNotAccessDatabaseHandlers() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .haveSimpleNameEndingWith("DatabaseHandler")
                .as("REST module must not bypass Thrift services to access database handlers directly");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("REST module should not directly access repository classes")
    void restModuleShouldNotAccessRepositories() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .should().dependOnClassesThat()
                .resideInAPackage("..datahandler.db..")
                .as("REST module must not access backend repository classes in the datahandler.db package");

        rule.check(restClasses);
    }

    @Test
    @DisplayName("REST module (except health indicator) should not directly use Cloudant/CouchDB client classes")
    void restModuleShouldNotAccessCouchDbDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..rest.resourceserver..")
                .and().doNotHaveSimpleName("SW360RestHealthIndicator")
                .should().dependOnClassesThat()
                .resideInAPackage("..datahandler.cloudantclient..")
                .as("REST module (except health indicator) must not access CouchDB/Cloudant client classes directly — " +
                        "use Thrift services via ThriftClients");

        rule.check(restClasses);
    }
}
