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

import com.ibm.cloud.cloudant.v1.model.Document;
import com.ibm.cloud.cloudant.v1.model.DocumentResult;
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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_ADMIN_PRIVATE_ACCESS_ENABLED;
import static org.eclipse.sw360.datahandler.common.SW360ConfigKeys.IS_BULK_RELEASE_DELETING_ENABLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BulkDeleteUtilRollbackTest {

    private static final String ROOT = "root-release";
    private static final String ROOT_COMPONENT = "root-component";
    private static final String SHARED_COMPONENT = "shared-component";
    // Equal hash codes let reversed insertion order exercise a different leaf order.
    private static final List<String> SIBLINGS = List.of("AaAa", "BBBB", "AaBB");
    private static final User ADMIN = new User().setEmail("admin@example.org").setUserGroup(UserGroup.ADMIN);

    @TestFactory
    Stream<DynamicTest> shouldRetainEverySibling_whenSharedDocumentUpdateFails() {
        return Stream.of(ROOT, SHARED_COMPONENT).flatMap(failedId ->
                Stream.of("conflict", "forbidden").flatMap(error ->
                        Stream.of(2, 3).flatMap(count -> Stream.of(false, true).map(reverse ->
                                DynamicTest.dynamicTest(failedId + "/" + error + "/" + count + "/reverse=" + reverse,
                                        () -> {
                                            Fixture fixture = new Fixture(count, reverse);
                                            fixture.failedId = failedId;
                                            fixture.error = error;

                                            BulkOperationNode result = fixture.delete(false);

                                            assertThat(fixture.deletedIds).isEmpty();
                                            assertThat(fixture.releases).containsKeys(ROOT);
                                            assertThat(fixture.releases.keySet()).containsAll(fixture.siblingIds);
                                            assertThat(fixture.releases.get(ROOT).getReleaseIdToRelationship())
                                                    .isEqualTo(fixture.originalRelationships);
                                            assertThat(fixture.components.get(SHARED_COMPONENT).getReleaseIds())
                                                    .containsExactlyInAnyOrderElementsOf(fixture.siblingIds);
                                            assertThat(releaseStates(result).values()).hasSize(count + 1)
                                                    .containsOnly(BulkOperationResultState.FAILED);
                                            verify(fixture.componentModerator, never()).notifyModeratorOnDelete(anyString());
                                        })))));
    }

    @Test
    void shouldDeleteEverySiblingAndEmptyComponent_whenAllUpdatesSucceed() throws Exception {
        Fixture fixture = new Fixture(3, false);

        BulkOperationNode result = fixture.delete(false);

        assertThat(fixture.releases).isEmpty();
        assertThat(fixture.components).isEmpty();
        assertThat(fixture.deletedIds).containsExactlyInAnyOrder(ROOT, ROOT_COMPONENT, SHARED_COMPONENT,
                SIBLINGS.get(0), SIBLINGS.get(1), SIBLINGS.get(2));
        assertThat(releaseStates(result).values()).hasSize(4).containsOnly(BulkOperationResultState.SUCCEEDED);
    }

    @Test
    void shouldOnlyPreviewDeletion_whenPreviewIsRequested() throws Exception {
        Fixture fixture = new Fixture(3, true);

        BulkOperationNode result = fixture.delete(true);

        assertThat(fixture.deletedIds).isEmpty();
        assertThat(fixture.releases).hasSize(4);
        assertThat(fixture.releases.get(ROOT).getReleaseIdToRelationship()).isEqualTo(fixture.originalRelationships);
        assertThat(fixture.components.get(SHARED_COMPONENT).getReleaseIds())
                .containsExactlyInAnyOrderElementsOf(fixture.siblingIds);
        assertThat(releaseStates(result).values()).hasSize(4).containsOnly(BulkOperationResultState.SUCCEEDED);
        verify(fixture.releaseRepository, never()).executeBulk(anyCollection());
        verify(fixture.componentRepository, never()).executeBulk(anyCollection());
        verify(fixture.releaseRepository, never()).update(any(Release.class));
        verify(fixture.componentRepository, never()).update(any(Component.class));
        verifyNoInteractions(fixture.componentDatabaseHandler, fixture.attachmentConnector,
                fixture.attachmentDatabaseHandler, fixture.componentModerator, fixture.dbHandlerUtil);
    }

    private static Map<String, BulkOperationResultState> releaseStates(BulkOperationNode node) {
        Map<String, BulkOperationResultState> states = new LinkedHashMap<>();
        if (node.getType() == BulkOperationNodeType.RELEASE) {
            states.put(node.getId(), node.getState());
        }
        if (node.isSetChildList()) {
            node.getChildList().forEach(child -> states.putAll(releaseStates(child)));
        }
        return states;
    }

    private static class Fixture {
        private final ComponentDatabaseHandler componentDatabaseHandler = mock(ComponentDatabaseHandler.class);
        private final ComponentRepository componentRepository = mock(ComponentRepository.class);
        private final ReleaseRepository releaseRepository = mock(ReleaseRepository.class);
        private final ProjectRepository projectRepository = mock(ProjectRepository.class);
        private final ComponentModerator componentModerator = mock(ComponentModerator.class);
        private final AttachmentConnector attachmentConnector = mock(AttachmentConnector.class);
        private final AttachmentDatabaseHandler attachmentDatabaseHandler = mock(AttachmentDatabaseHandler.class);
        private final DatabaseHandlerUtil dbHandlerUtil = mock(DatabaseHandlerUtil.class);
        private final Map<String, Release> releases = new LinkedHashMap<>();
        private final Map<String, Component> components = new LinkedHashMap<>();
        private final Map<String, ReleaseRelationship> originalRelationships = new LinkedHashMap<>();
        private final List<String> siblingIds;
        private final List<String> deletedIds = new ArrayList<>();
        private String failedId;
        private String error;

        private Fixture(int count, boolean reverse) {
            siblingIds = new ArrayList<>(SIBLINGS.subList(0, count));
            if (reverse) {
                Collections.reverse(siblingIds);
            }
            siblingIds.forEach(id -> {
                originalRelationships.put(id, ReleaseRelationship.CONTAINED);
                releases.put(id, release(id, SHARED_COMPONENT));
            });
            releases.put(ROOT, release(ROOT, ROOT_COMPONENT)
                    .setReleaseIdToRelationship(new LinkedHashMap<>(originalRelationships)));
            components.put(ROOT_COMPONENT, new Component().setId(ROOT_COMPONENT).setName(ROOT_COMPONENT)
                    .setReleaseIds(new LinkedHashSet<>(List.of(ROOT))));
            components.put(SHARED_COMPONENT, new Component().setId(SHARED_COMPONENT).setName(SHARED_COMPONENT)
                    .setReleaseIds(new LinkedHashSet<>(siblingIds)));

            when(releaseRepository.get(anyString())).thenAnswer(call -> releases.get(call.getArgument(0)).deepCopy());
            when(componentRepository.get(anyString())).thenAnswer(call -> components.get(call.getArgument(0)).deepCopy());
            when(releaseRepository.getReferencingReleases(anyString())).thenAnswer(call -> releases.values().stream()
                    .filter(release -> release.isSetReleaseIdToRelationship()
                            && release.getReleaseIdToRelationship().containsKey(call.getArgument(0)))
                    .map(Release::deepCopy).toList());
            when(releaseRepository.executeBulk(anyCollection())).thenAnswer(call -> executeBulk(call.getArgument(0)));
            when(componentRepository.executeBulk(anyCollection())).thenAnswer(call -> executeBulk(call.getArgument(0)));
            doAnswer(call -> {
                Release release = call.getArgument(0);
                releases.put(release.getId(), release.deepCopy());
                return null;
            }).when(releaseRepository).update(any(Release.class));
        }

        private BulkOperationNode delete(boolean preview) throws Exception {
            try (MockedStatic<SW360Utils> config = mockStatic(SW360Utils.class)) {
                config.when(() -> SW360Utils.readConfig(IS_BULK_RELEASE_DELETING_ENABLED, false)).thenReturn(true);
                config.when(() -> SW360Utils.readConfig(IS_ADMIN_PRIVATE_ACCESS_ENABLED, false)).thenReturn(true);
                BulkDeleteUtil util = new BulkDeleteUtil(componentDatabaseHandler, componentRepository,
                        releaseRepository, projectRepository, componentModerator, mock(ReleaseModerator.class),
                        attachmentConnector, attachmentDatabaseHandler, dbHandlerUtil);
                return util.deleteBulkRelease(ROOT, ADMIN, preview);
            }
        }

        private List<DocumentResult> executeBulk(Collection<?> documents) {
            List<DocumentResult> results = new ArrayList<>();
            for (Object document : documents) {
                String id;
                if (document instanceof Release release) {
                    id = release.getId();
                    if (!id.equals(failedId)) {
                        releases.put(id, release.deepCopy());
                    }
                } else if (document instanceof Component component) {
                    id = component.getId();
                    if (!id.equals(failedId)) {
                        components.put(id, component.deepCopy());
                    }
                } else {
                    Document deletion = (Document) document;
                    assertThat(deletion.isDeleted()).isTrue();
                    id = deletion.getId();
                    deletedIds.add(id);
                    releases.remove(id);
                    components.remove(id);
                }
                DocumentResult result = mock(DocumentResult.class);
                when(result.getId()).thenReturn(id);
                when(result.getError()).thenReturn(id.equals(failedId) ? error : null);
                results.add(result);
            }
            return results;
        }

        private static Release release(String id, String componentId) {
            return new Release().setId(id).setName(id).setVersion("1.0").setComponentId(componentId);
        }
    }
}
