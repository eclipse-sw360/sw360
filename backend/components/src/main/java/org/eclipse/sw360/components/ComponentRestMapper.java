/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.attachments.AttachmentConverter;
import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.ProjectReleaseRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.common.ReleaseRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.components.BulkOperationNodeConverter;
import org.eclipse.sw360.common.utils.converter.components.ComponentConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseClearingStatusDataConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.common.utils.converter.users.RequestedActionConverter;
import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;

final class ComponentRestMapper {

    private ComponentRestMapper() {}

    // ---- Component ----

    static Component fromThriftComponent(org.eclipse.sw360.datahandler.thrift.components.Component thrift) {
        return ComponentConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.components.Component toThriftComponent(Component pojo) {
        return ComponentConverter.toThrift(pojo);
    }

    static List<Component> fromThriftComponents(
            List<org.eclipse.sw360.datahandler.thrift.components.Component> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ComponentConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.components.Component> toThriftComponents(
            List<Component> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ComponentConverter::toThrift).collect(Collectors.toList());
    }

    // ---- Release ----

    static Release fromThriftRelease(org.eclipse.sw360.datahandler.thrift.components.Release thrift) {
        return ReleaseConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.components.Release toThriftRelease(Release pojo) {
        return ReleaseConverter.toThrift(pojo);
    }

    static List<Release> fromThriftReleases(
            List<org.eclipse.sw360.datahandler.thrift.components.Release> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.components.Release> toThriftReleases(
            List<Release> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ReleaseConverter::toThrift).collect(Collectors.toList());
    }

    static Set<Release> fromThriftReleaseSet(
            Set<org.eclipse.sw360.datahandler.thrift.components.Release> thriftSet) {
        if (thriftSet == null) {
            return Set.of();
        }
        return thriftSet.stream().map(ReleaseConverter::fromThrift).collect(Collectors.toSet());
    }

    static Set<org.eclipse.sw360.datahandler.thrift.components.Release> toThriftReleaseSet(
            Set<Release> pojoSet) {
        if (pojoSet == null) {
            return Set.of();
        }
        return pojoSet.stream().map(ReleaseConverter::toThrift).collect(Collectors.toSet());
    }

    // ---- ReleaseLink ----

    static ReleaseLink fromThriftReleaseLink(org.eclipse.sw360.datahandler.thrift.components.ReleaseLink thrift) {
        return ReleaseLinkConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.components.ReleaseLink toThriftReleaseLink(ReleaseLink pojo) {
        return ReleaseLinkConverter.toThrift(pojo);
    }

    static List<ReleaseLink> fromThriftReleaseLinks(
            List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseLinkConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> toThriftReleaseLinks(
            List<ReleaseLink> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ReleaseLinkConverter::toThrift).collect(Collectors.toList());
    }

    // ---- ReleaseNode ----

    static ReleaseNode fromThriftReleaseNode(org.eclipse.sw360.datahandler.thrift.components.ReleaseNode thrift) {
        return ReleaseNodeConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.components.ReleaseNode toThriftReleaseNode(ReleaseNode pojo) {
        return ReleaseNodeConverter.toThrift(pojo);
    }

    static List<ReleaseNode> fromThriftReleaseNodes(
            List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseNodeConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> toThriftReleaseNodes(
            List<ReleaseNode> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ReleaseNodeConverter::toThrift).collect(Collectors.toList());
    }

    // ---- BulkOperationNode ----

    static BulkOperationNode fromThriftBulkOperationNode(
            org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode thrift) {
        return BulkOperationNodeConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode toThriftBulkOperationNode(
            BulkOperationNode pojo) {
        return BulkOperationNodeConverter.toThrift(pojo);
    }

    // ---- Attachment ----

    static Set<Attachment> fromThriftAttachments(
            Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment> thriftSet) {
        if (thriftSet == null) {
            return Set.of();
        }
        return thriftSet.stream().map(AttachmentConverter::fromThrift).collect(Collectors.toSet());
    }

    static Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment> toThriftAttachments(
            Set<Attachment> pojoSet) {
        if (pojoSet == null) {
            return Set.of();
        }
        return pojoSet.stream().map(AttachmentConverter::toThrift).collect(Collectors.toSet());
    }

    // ---- AddDocumentRequestSummary ----

    static AddDocumentRequestSummary fromThriftAddDocumentRequestSummary(
            org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary thrift) {
        return AddDocumentRequestSummaryConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary toThriftAddDocumentRequestSummary(
            AddDocumentRequestSummary pojo) {
        return AddDocumentRequestSummaryConverter.toThrift(pojo);
    }

    // ---- RequestStatus ----

    static RequestStatus fromThriftRequestStatus(org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        return RequestStatusConverter.fromThrift(thrift);
    }

    // ---- RequestSummary ----

    static RequestSummary fromThriftRequestSummary(org.eclipse.sw360.datahandler.thrift.RequestSummary thrift) {
        return RequestSummaryConverter.fromThrift(thrift);
    }

    // ---- PaginationData ----

    static org.eclipse.sw360.datahandler.thrift.PaginationData toThriftPagination(PaginationData pojo) {
        return PaginationDataConverter.toThrift(pojo);
    }

    static PaginationData fromThriftPagination(org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        return PaginationDataConverter.fromThrift(thrift);
    }

    // ---- ImportBomRequestPreparation (inline — no dedicated converter) ----

    static ImportBomRequestPreparation fromThriftImportBomRequestPreparation(
            org.eclipse.sw360.datahandler.thrift.ImportBomRequestPreparation thrift) {
        if (thrift == null) {
            return null;
        }
        ImportBomRequestPreparation pojo = new ImportBomRequestPreparation();
        if (thrift.isSetRequestStatus()) {
            pojo.setRequestStatus(fromThriftRequestStatus(thrift.getRequestStatus()));
        }
        if (thrift.isSetIsComponentDuplicate()) {
            pojo.setIsComponentDuplicate(thrift.isIsComponentDuplicate());
        }
        if (thrift.isSetIsReleaseDuplicate()) {
            pojo.setIsReleaseDuplicate(thrift.isIsReleaseDuplicate());
        }
        if (thrift.isSetComponentsName()) {
            pojo.setComponentsName(thrift.getComponentsName());
        }
        if (thrift.isSetReleasesName()) {
            pojo.setReleasesName(thrift.getReleasesName());
        }
        if (thrift.isSetVersion()) {
            pojo.setVersion(thrift.getVersion());
        }
        if (thrift.isSetMessage()) {
            pojo.setMessage(thrift.getMessage());
        }
        return pojo;
    }

    // ---- ProjectReleaseRelationship (Map parameter) ----

    static Map<String, ProjectReleaseRelationship> fromThriftReleaseIdToUsage(
            Map<String, org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship> thriftMap) {
        if (thriftMap == null) {
            return Map.of();
        }
        return thriftMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ProjectReleaseRelationshipConverter.fromThrift(e.getValue())));
    }

    static Map<String, org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship> toThriftReleaseIdToUsage(
            Map<String, ProjectReleaseRelationship> pojoMap) {
        if (pojoMap == null) {
            return Map.of();
        }
        return pojoMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ProjectReleaseRelationshipConverter.toThrift(e.getValue())));
    }

    // ---- ReleaseRelationship (Map parameter) ----

    static Map<String, ReleaseRelationship> fromThriftReleaseIdToRelationship(
            Map<String, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship> thriftMap) {
        if (thriftMap == null) {
            return Map.of();
        }
        return thriftMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ReleaseRelationshipConverter.fromThrift(e.getValue())));
    }

    static Map<String, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship> toThriftReleaseIdToRelationship(
            Map<String, ReleaseRelationship> pojoMap) {
        if (pojoMap == null) {
            return Map.of();
        }
        return pojoMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ReleaseRelationshipConverter.toThrift(e.getValue())));
    }

    // ---- RequestedAction (enum) ----

    static RequestedAction fromThriftRequestedAction(
            org.eclipse.sw360.datahandler.thrift.users.RequestedAction thrift) {
        return RequestedActionConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.users.RequestedAction toThriftRequestedAction(
            RequestedAction pojo) {
        return RequestedActionConverter.toThrift(pojo);
    }

    // ---- Component (Set) ----

    static Set<Component> fromThriftComponentSet(
            Set<org.eclipse.sw360.datahandler.thrift.components.Component> thriftSet) {
        if (thriftSet == null) {
            return Set.of();
        }
        return thriftSet.stream().map(ComponentConverter::fromThrift).collect(Collectors.toSet());
    }

    static Set<org.eclipse.sw360.datahandler.thrift.components.Component> toThriftComponentSet(
            Set<Component> pojoSet) {
        if (pojoSet == null) {
            return Set.of();
        }
        return pojoSet.stream().map(ComponentConverter::toThrift).collect(Collectors.toSet());
    }

    // ---- Paginated convenience wrappers ----

    static PaginatedResult<Component> toPaginatedComponents(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData,
                    List<org.eclipse.sw360.datahandler.thrift.components.Component>> thriftMap) {
        return fromThriftPaginatedResult(thriftMap, ComponentConverter::fromThrift);
    }

    static PaginatedResult<Release> toPaginatedReleases(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData,
                    List<org.eclipse.sw360.datahandler.thrift.components.Release>> thriftMap) {
        return fromThriftPaginatedResult(thriftMap, ReleaseConverter::fromThrift);
    }

    // ---- ProjectReleaseRelationship / ReleaseRelationship map aliases ----

    static Map<String, org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship> toThriftProjectReleaseRelationshipMap(
            Map<String, ProjectReleaseRelationship> pojoMap) {
        return toThriftReleaseIdToUsage(pojoMap);
    }

    static Map<String, org.eclipse.sw360.datahandler.thrift.ReleaseRelationship> toThriftReleaseRelationshipMap(
            Map<String, ReleaseRelationship> pojoMap) {
        return toThriftReleaseIdToRelationship(pojoMap);
    }

    // ---- ReleaseClearingStatusData ----

    static List<ReleaseClearingStatusData> fromThriftReleaseClearingStatusDataList(
            List<org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseClearingStatusDataConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStatusData> toThriftReleaseClearingStatusDataList(
            List<ReleaseClearingStatusData> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ReleaseClearingStatusDataConverter::toThrift).collect(Collectors.toList());
    }

    // ---- PaginatedResult helper ----

    static <T, P> PaginatedResult<P> fromThriftPaginatedResult(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<T>> thriftMap,
            Function<T, P> converter) {
        if (thriftMap == null || thriftMap.isEmpty()) {
            return new PaginatedResult<>(new PaginationData(), List.of());
        }
        Map.Entry<org.eclipse.sw360.datahandler.thrift.PaginationData, List<T>> entry =
                thriftMap.entrySet().iterator().next();
        PaginationData paginationData = fromThriftPagination(entry.getKey());
        List<P> data = entry.getValue() == null
                ? List.of()
                : entry.getValue().stream().map(converter).collect(Collectors.toList());
        return new PaginatedResult<>(paginationData, data);
    }
}
