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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.ProjectReleaseRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.common.ReleaseRelationshipConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.components.BulkOperationNodeConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseLinkConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseNodeConverter;
import org.eclipse.sw360.common.utils.converter.users.RequestedActionConverter;
import org.eclipse.sw360.datahandler.services.common.ImportBomRequestPreparation;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.ReleaseRelationship;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.BulkOperationNode;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.components.ReleaseLink;
import org.eclipse.sw360.datahandler.services.components.ReleaseNode;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;

final class ComponentRestMapper {

    private ComponentRestMapper() {}

    // ---- ReleaseLink ----
    static List<ReleaseLink> fromThriftReleaseLinks(
            List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseLinkConverter::fromThrift).collect(Collectors.toList());
    }
    // ---- ReleaseNode ----
    static List<ReleaseNode> fromThriftReleaseNodes(
            List<org.eclipse.sw360.datahandler.thrift.components.ReleaseNode> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ReleaseNodeConverter::fromThrift).collect(Collectors.toList());
    }
    // ---- BulkOperationNode ----

    static BulkOperationNode fromThriftBulkOperationNode(
            org.eclipse.sw360.datahandler.thrift.components.BulkOperationNode thrift) {
        return BulkOperationNodeConverter.fromThrift(thrift);
    }
    // ---- RequestStatus ----

    static RequestStatus fromThriftRequestStatus(org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        return RequestStatusConverter.fromThrift(thrift);
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
    static org.eclipse.sw360.datahandler.thrift.users.RequestedAction toThriftRequestedAction(
            RequestedAction pojo) {
        return RequestedActionConverter.toThrift(pojo);
    }

    // ---- Paginated results ----

    static PaginatedResult<Component> toPaginatedComponents(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<Component>> map) {
        return toPaginatedResult(map);
    }

    static PaginatedResult<Release> toPaginatedReleases(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<Release>> map) {
        return toPaginatedResult(map);
    }

    /**
     * The payload is already service-api; only the map key is still a thrift
     * {@code PaginationData}, so this unwraps the single entry and converts the key.
     */
    private static <T> PaginatedResult<T> toPaginatedResult(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<T>> map) {
        if (map == null || map.isEmpty()) {
            return new PaginatedResult<>(new PaginationData(), List.of());
        }
        Map.Entry<org.eclipse.sw360.datahandler.thrift.PaginationData, List<T>> entry =
                map.entrySet().iterator().next();
        List<T> data = entry.getValue() == null ? List.of() : new ArrayList<>(entry.getValue());
        return new PaginatedResult<>(fromThriftPagination(entry.getKey()), data);
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
}
