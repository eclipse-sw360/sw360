/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.moderation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.common.CommentConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RemoveModeratorRequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.components.ComponentConverter;
import org.eclipse.sw360.common.utils.converter.components.ReleaseConverter;
import org.eclipse.sw360.common.utils.converter.licenses.LicenseConverter;
import org.eclipse.sw360.common.utils.converter.moderation.ModerationRequestConverter;
import org.eclipse.sw360.common.utils.converter.projects.ClearingRequestConverter;
import org.eclipse.sw360.common.utils.converter.projects.ProjectConverter;
import org.eclipse.sw360.common.utils.converter.spdx.DocumentCreationInformationConverter;
import org.eclipse.sw360.common.utils.converter.spdx.PackageInformationConverter;
import org.eclipse.sw360.common.utils.converter.spdx.SPDXDocumentConverter;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.services.common.Comment;
import org.eclipse.sw360.datahandler.services.common.PaginatedResult;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RemoveModeratorRequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.services.projects.Project;
import org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.services.spdx.PackageInformation;
import org.eclipse.sw360.datahandler.services.spdx.SPDXDocument;
import org.eclipse.sw360.datahandler.services.users.User;

final class ModerationRestMapper {

    private ModerationRestMapper() {}

    static PaginationData fromThriftPagination(org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        return PaginationDataConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.PaginationData toThriftPagination(PaginationData pojo) {
        return PaginationDataConverter.toThrift(pojo);
    }

    static RequestStatus fromThriftRequestStatus(org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        return RequestStatusConverter.fromThrift(thrift);
    }

    static RemoveModeratorRequestStatus fromThriftRemoveModeratorStatus(
            org.eclipse.sw360.datahandler.thrift.RemoveModeratorRequestStatus thrift) {
        return RemoveModeratorRequestStatusConverter.fromThrift(thrift);
    }

    static ModerationRequest fromThriftModerationRequest(
            org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest thrift) {
        return ModerationRequestConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest toThriftModerationRequest(
            ModerationRequest pojo) {
        return ModerationRequestConverter.toThrift(pojo);
    }

    static List<ModerationRequest> fromThriftModerationRequests(
            List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ModerationRequestConverter::fromThrift).collect(Collectors.toList());
    }

    static ClearingRequest fromThriftClearingRequest(
            org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest thrift) {
        return ClearingRequestConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest toThriftClearingRequest(
            ClearingRequest pojo) {
        return ClearingRequestConverter.toThrift(pojo);
    }

    static Set<ClearingRequest> fromThriftClearingRequestSet(
            Set<org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest> thriftSet) {
        if (thriftSet == null) {
            return Set.of();
        }
        return thriftSet.stream().map(ClearingRequestConverter::fromThrift).collect(Collectors.toSet());
    }

    static org.eclipse.sw360.datahandler.thrift.Comment toThriftComment(Comment pojo) {
        return CommentConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.components.Component toThriftComponent(Component pojo) {
        return ComponentConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.components.Release toThriftRelease(Release pojo) {
        return ReleaseConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.projects.Project toThriftProject(Project pojo) {
        return ProjectConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.licenses.License toThriftLicense(License pojo) {
        return LicenseConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.users.User toThriftUser(User pojo) {
        return UserConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument toThriftSpdxDocument(
            SPDXDocument pojo) {
        return SPDXDocumentConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation toThriftDocumentCreationInfo(
            DocumentCreationInformation pojo) {
        return DocumentCreationInformationConverter.toThrift(pojo);
    }

    static org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation toThriftPackageInfo(
            PackageInformation pojo) {
        return PackageInformationConverter.toThrift(pojo);
    }

    static PaginatedResult<ModerationRequest> fromThriftPaginatedModerationRequests(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData,
                    List<org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest>> thriftMap) {
        return fromThriftPaginatedResult(thriftMap, ModerationRequestConverter::fromThrift);
    }

    static PaginatedResult<ClearingRequest> fromThriftPaginatedClearingRequests(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData,
                    List<org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest>> thriftMap) {
        return fromThriftPaginatedResult(thriftMap, ClearingRequestConverter::fromThrift);
    }

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
