/*
 * Copyright Agastya Kataria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.TestUtils;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.AttachmentConnector;
import org.eclipse.sw360.datahandler.entitlement.ComponentModerator;
import org.eclipse.sw360.datahandler.entitlement.ReleaseModerator;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationNodeType;
import org.eclipse.sw360.datahandler.thrift.components.BulkOperationResultState;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_ADMIN_PRIVATE_ACCESS_ENABLED;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_BULK_RELEASE_DELETING_ENABLED;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class BulkDeleteUtilRollbackIntegrationTest {

    private static final String DB_NAME = DatabaseSettingsTest.COUCH_DB_DATABASE + "_bulk_delete_rollback";
    private static final String ROOT = "root-release";
    private static final String ROOT_COMPONENT = "root-component";
    private static final String SHARED_COMPONENT = "shared-component";
    private static final Set<String> SIBLINGS = Set.of("leaf-a", "leaf-b", "leaf-c");
    private static final Map<String, ReleaseRelationship> RELATIONSHIPS = Map.of(
            "leaf-a", ReleaseRelationship.CONTAINED,
            "leaf-b", ReleaseRelationship.CONTAINED,
            "leaf-c", ReleaseRelationship.CONTAINED);

    private Cloudant client;
    private ReleaseRepository releaseRepository;
    private ComponentRepository componentRepository;
    private BulkDeleteUtil util;
    private BulkDeleteUtil.BulkDeleteUtilInspector inspector;

    @BeforeEach
    void setUp() throws Exception {
        TestUtils.assertTestString(DB_NAME);
        client = DatabaseSettingsTest.getConfiguredClient();
        TestUtils.createDatabase(client, DB_NAME);
        DatabaseConnectorCloudant connector = new DatabaseConnectorCloudant(client, DB_NAME);
        VendorRepository vendorRepository = new VendorRepository(connector);
        releaseRepository = new ReleaseRepository(connector, vendorRepository);
        componentRepository = new ComponentRepository(connector, releaseRepository, vendorRepository);
        ProjectRepository projectRepository = new ProjectRepository(connector);

        componentRepository.add(new Component().setId(ROOT_COMPONENT).setName(ROOT_COMPONENT)
                .setReleaseIds(new HashSet<>(Set.of(ROOT))));
        componentRepository.add(new Component().setId(SHARED_COMPONENT).setName(SHARED_COMPONENT)
                .setReleaseIds(new HashSet<>(SIBLINGS)));
        releaseRepository.add(new Release().setId(ROOT).setName(ROOT).setVersion("1.0")
                .setComponentId(ROOT_COMPONENT).setReleaseIdToRelationship(new HashMap<>(RELATIONSHIPS)));
        for (String sibling : SIBLINGS) {
            releaseRepository.add(new Release().setId(sibling).setName(sibling).setVersion("1.0")
                    .setComponentId(SHARED_COMPONENT));
        }

        util = new BulkDeleteUtil(mock(ComponentDatabaseHandler.class), componentRepository, releaseRepository,
                projectRepository, mock(ComponentModerator.class), mock(ReleaseModerator.class),
                mock(AttachmentConnector.class), mock(AttachmentDatabaseHandler.class), mock(DatabaseHandlerUtil.class));
        inspector = mock(BulkDeleteUtil.BulkDeleteUtilInspector.class);
        util.setInspector(inspector);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            TestUtils.deleteDatabase(client, DB_NAME);
        }
    }

    @Test
    void shouldRetainEverySibling_whenParentRevisionChangesBeforeBulkUpdate() throws Exception {
        doAnswer(call -> {
            List<Release> updates = call.getArgument(1);
            assertThat(updates).extracting(Release::getId).containsExactly(ROOT);
            Release concurrentEdit = releaseRepository.get(ROOT);
            concurrentEdit.addToLanguages("concurrent edit");
            releaseRepository.update(concurrentEdit);
            assertThat(concurrentEdit.getRevision()).isNotEqualTo(updates.getFirst().getRevision());
            return null;
        }).when(inspector).checkUpdatedReferencingReleaseListInLoop(eq(0), anyList());

        BulkOperationNode result = delete();

        assertRetainedGraph(result);
        assertThat(releaseRepository.get(ROOT).getLanguages()).containsExactly("concurrent edit");
        verify(inspector).checkUpdatedReferencingReleaseListInLoop(eq(0), anyList());
    }

    @Test
    void shouldRetainEverySibling_whenComponentRevisionChangesBeforeBulkUpdate() throws Exception {
        doAnswer(call -> {
            List<Component> updates = call.getArgument(1);
            assertThat(updates).extracting(Component::getId).containsExactly(SHARED_COMPONENT);
            Component concurrentEdit = componentRepository.get(SHARED_COMPONENT);
            concurrentEdit.setDescription("concurrent edit");
            componentRepository.update(concurrentEdit);
            assertThat(concurrentEdit.getRevision()).isNotEqualTo(updates.getFirst().getRevision());
            return null;
        }).when(inspector).checkUpdatedComponentListInLoop(eq(0), anyList());

        BulkOperationNode result = delete();

        assertRetainedGraph(result);
        assertThat(componentRepository.get(SHARED_COMPONENT).getDescription()).isEqualTo("concurrent edit");
        verify(inspector).checkUpdatedComponentListInLoop(eq(0), anyList());
    }

    private BulkOperationNode delete() throws Exception {
        try (MockedStatic<SW360Utils> config = mockStatic(SW360Utils.class)) {
            config.when(() -> SW360Utils.readConfig(IS_BULK_RELEASE_DELETING_ENABLED, false)).thenReturn(true);
            config.when(() -> SW360Utils.readConfig(IS_ADMIN_PRIVATE_ACCESS_ENABLED, false)).thenReturn(true);
            return util.deleteBulkRelease(ROOT,
                    new User().setEmail("admin@example.org").setUserGroup(UserGroup.ADMIN), false);
        }
    }

    private void assertRetainedGraph(BulkOperationNode result) {
        assertThat(releaseRepository.getAll()).extracting(Release::getId)
                .containsExactlyInAnyOrder(ROOT, "leaf-a", "leaf-b", "leaf-c");
        assertThat(releaseRepository.get(ROOT).getReleaseIdToRelationship()).isEqualTo(RELATIONSHIPS);
        assertThat(componentRepository.get(SHARED_COMPONENT).getReleaseIds()).isEqualTo(SIBLINGS);
        assertThat(componentRepository.get(ROOT_COMPONENT).getReleaseIds()).containsExactly(ROOT);
        assertFailedReleases(result);
    }

    private void assertFailedReleases(BulkOperationNode node) {
        if (node.getType() == BulkOperationNodeType.RELEASE) {
            assertThat(node.getState()).as("state of %s", node.getId()).isEqualTo(BulkOperationResultState.FAILED);
        }
        if (node.isSetChildList()) {
            node.getChildList().forEach(this::assertFailedReleases);
        }
    }
}
