/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.projects;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.common.AddDocumentRequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseClearingStatusDataConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.common.utils.converter.projects.ClearingRequestConverter;
import org.eclipse.sw360.common.utils.converter.projects.ObligationListConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectDataConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectLinkConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectProjectRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.projects.UsedReleaseRelationsConverter;
import org.eclipse.sw360.datahandler.services.common.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.components.ReleaseClearingStatusData;
import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.services.projects.ObligationList;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.projects.ProjectData;
import org.eclipse.sw360.datahandler.services.projects.ProjectLink;
import org.eclipse.sw360.datahandler.services.projects.ProjectProjectRelationship;
import org.eclipse.sw360.datahandler.services.projects.UsedReleaseRelations;

final class ProjectRestMapper {

    private ProjectRestMapper() {}

    // ---- Project ----

    static Project fromThriftProject(org.eclipse.sw360.datahandler.thrift.projects.Project thrift) {
        return ProjectConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.Project toThriftProject(Project pojo) {
        return ProjectConverter.toThrift(pojo);
    }

    static List<Project> fromThriftProjects(List<org.eclipse.sw360.datahandler.thrift.projects.Project> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ProjectConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.projects.Project> toThriftProjects(List<Project> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ProjectConverter::toThrift).collect(Collectors.toList());
    }

    static Set<Project> fromThriftProjectSet(Set<org.eclipse.sw360.datahandler.thrift.projects.Project> thriftSet) {
        if (thriftSet == null) {
            return Set.of();
        }
        return thriftSet.stream().map(ProjectConverter::fromThrift).collect(Collectors.toSet());
    }

    static Set<org.eclipse.sw360.datahandler.thrift.projects.Project> toThriftProjectSet(Set<Project> pojoSet) {
        if (pojoSet == null) {
            return Set.of();
        }
        return pojoSet.stream().map(ProjectConverter::toThrift).collect(Collectors.toSet());
    }

    // ---- ProjectLink ----

    static ProjectLink fromThriftProjectLink(org.eclipse.sw360.datahandler.thrift.projects.ProjectLink thrift) {
        return ProjectLinkConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.ProjectLink toThriftProjectLink(ProjectLink pojo) {
        return ProjectLinkConverter.toThrift(pojo);
    }

    static List<ProjectLink> fromThriftProjectLinks(List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ProjectLinkConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.projects.ProjectLink> toThriftProjectLinks(List<ProjectLink> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ProjectLinkConverter::toThrift).collect(Collectors.toList());
    }

    // ---- ProjectProjectRelationship ----

    static ProjectProjectRelationship fromThriftProjectProjectRelationship(
            org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship thrift) {
        return ProjectProjectRelationshipConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship toThriftProjectProjectRelationship(
            ProjectProjectRelationship pojo) {
        return ProjectProjectRelationshipConverter.toThrift(pojo);
    }

    // ---- ProjectData ----

    static ProjectData fromThriftProjectData(org.eclipse.sw360.datahandler.thrift.projects.ProjectData thrift) {
        return ProjectDataConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.ProjectData toThriftProjectData(ProjectData pojo) {
        return ProjectDataConverter.toThrift(pojo);
    }

    // ---- ClearingRequest ----

    static ClearingRequest fromThriftClearingRequest(org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest thrift) {
        return ClearingRequestConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest toThriftClearingRequest(ClearingRequest pojo) {
        return ClearingRequestConverter.toThrift(pojo);
    }

    // ---- ObligationList ----

    static ObligationList fromThriftObligationList(org.eclipse.sw360.datahandler.thrift.projects.ObligationList thrift) {
        return ObligationListConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.ObligationList toThriftObligationList(ObligationList pojo) {
        return ObligationListConverter.toThrift(pojo);
    }

    // ---- UsedReleaseRelations ----

    static UsedReleaseRelations fromThriftUsedReleaseRelations(
            org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations thrift) {
        return UsedReleaseRelationsConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations toThriftUsedReleaseRelations(
            UsedReleaseRelations pojo) {
        return UsedReleaseRelationsConverter.toThrift(pojo);
    }

    static List<UsedReleaseRelations> fromThriftUsedReleaseRelationsList(
            List<org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(UsedReleaseRelationsConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations> toThriftUsedReleaseRelationsList(
            List<UsedReleaseRelations> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(UsedReleaseRelationsConverter::toThrift).collect(Collectors.toList());
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

    // ---- ReleaseLink (reuses component-domain converter) ----

    static List<ReleaseLink> fromThriftReleaseLinks(List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseLinkConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> toThriftReleaseLinks(List<ReleaseLink> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ReleaseLinkConverter::toThrift).collect(Collectors.toList());
    }

    // ---- ReleaseNode ----

    static List<ReleaseNode> fromThriftReleaseNodes(List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseNodeConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> toThriftReleaseNodes(List<ReleaseNode> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ReleaseNodeConverter::toThrift).collect(Collectors.toList());
    }

    // ---- Paginated convenience wrappers ----

    static PaginatedResult<Project> fromThriftPaginatedProjects(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData,
                    List<org.eclipse.sw360.datahandler.thrift.projects.Project>> thriftMap) {
        return fromThriftPaginatedResult(thriftMap, ProjectConverter::fromThrift);
    }

    // ---- ProjectProjectRelationship map ----

    static Map<String, org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship> toThriftProjectRelationshipMap(
            Map<String, ProjectProjectRelationship> pojoMap) {
        if (pojoMap == null) {
            return Map.of();
        }
        return pojoMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ProjectProjectRelationshipConverter.toThrift(e.getValue())));
    }

    static Map<String, ProjectProjectRelationship> fromThriftProjectRelationshipMap(
            Map<String, org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship> thriftMap) {
        if (thriftMap == null) {
            return Map.of();
        }
        return thriftMap.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> ProjectProjectRelationshipConverter.fromThrift(e.getValue())));
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
