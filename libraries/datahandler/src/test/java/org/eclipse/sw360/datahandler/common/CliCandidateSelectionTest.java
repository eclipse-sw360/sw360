/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliCandidateSelectionTest {
    private final Attachment approved = cli("approved", CheckStatus.ACCEPTED);
    private final Attachment selected = cli("selected", CheckStatus.NOTCHECKED);
    private final Attachment other = cli("other", CheckStatus.NOTCHECKED);

    @Test
    void usageOverridesApprovedCli() {
        check(List.of(approved, selected), Set.of("selected"), List.of(selected), null);
    }

    @Test
    void multipleUsageCandidatesDoNotFallBackToSingleApprovedCli() {
        check(List.of(approved, selected, other), Set.of("selected", "other"),
                List.of(selected, other), "Attachment Usage");
    }

    @Test
    void usageCandidatesAreNotNarrowedToTheirApprovedSubset() {
        check(List.of(approved, selected, other), Set.of("approved", "selected"),
                List.of(approved, selected), "Attachment Usage");
    }

    @Test
    void emptyUsageFallsBackToApprovedCli() {
        check(List.of(approved, selected), Set.of(), List.of(approved), null);
    }

    @Test
    void multipleApprovedCandidatesDoNotFallBackToAllCli() {
        Attachment secondApproved = cli("approved-2", CheckStatus.ACCEPTED);
        check(List.of(approved, secondApproved, selected), Set.of(),
                List.of(approved, secondApproved), "approved CLI attachments");
    }

    @Test
    void noApprovedCandidatesFallsBackToAnyCli() {
        check(List.of(selected), Set.of(), List.of(selected), null);
        check(List.of(selected, other), Set.of(), List.of(selected, other), "available CLI attachments");
    }

    @Test
    void staleAndNonCliUsageDoesNotPreventFallback() {
        Attachment nonCli = cli("not-cli", CheckStatus.ACCEPTED).setAttachmentType(AttachmentType.SOURCE);
        check(List.of(approved, selected, nonCli), Set.of("missing", "not-cli"), List.of(approved), null);
    }

    @Test
    void noCliReturnsNoCandidatesOrWarnings() {
        check(List.of(), Set.of("missing"), List.of(), null);
    }

    @Test
    void optionalUsageAndWarningsDoNotChangeSelection() {
        Release release = release(List.of(selected, other));
        assertEquals(List.of(selected, other), SW360Utils.selectCliCandidatesForRelease(release, null, null));
        assertEquals(List.of(), SW360Utils.selectCliCandidatesForRelease(new Release().setId("empty"), null, null));
    }

    private void check(List<Attachment> attachments, Set<String> usedIds, List<Attachment> expected, String warningSource) {
        Release release = release(attachments);
        Release original = release.deepCopy();
        List<String> warnings = new ArrayList<>();
        assertEquals(expected, SW360Utils.selectCliCandidatesForRelease(release, Map.of("r1", usedIds), warnings));
        assertEquals(original, release);
        assertEquals(warningSource == null ? 0 : 1, warnings.size());
        if (warningSource != null) {
            assertTrue(warnings.getFirst().contains("from " + warningSource));
            assertTrue(warnings.getFirst().contains(expected.size() + " CLI files"));
            for (Attachment attachment : expected) {
                assertTrue(warnings.getFirst().contains(attachment.getFilename()));
            }
        }
    }

    private Release release(List<Attachment> attachments) {
        return new Release().setId("r1").setName("Release 1").setAttachments(new LinkedHashSet<>(attachments));
    }

    private Attachment cli(String id, CheckStatus status) {
        return new Attachment().setAttachmentContentId(id).setFilename(id + ".xml")
                .setAttachmentType(AttachmentType.COMPONENT_LICENSE_INFO_XML).setCheckStatus(status);
    }
}
