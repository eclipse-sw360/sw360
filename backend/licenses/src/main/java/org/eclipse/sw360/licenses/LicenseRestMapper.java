/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.sw360.common.utils.converter.common.CustomPropertiesConverter;
import org.eclipse.sw360.common.utils.converter.common.PaginationDataConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestStatusConverter;
import org.eclipse.sw360.common.utils.converter.common.RequestSummaryConverter;
import org.eclipse.sw360.common.utils.converter.licenses.LicenseConverter;
import org.eclipse.sw360.common.utils.converter.licenses.LicenseTypeConverter;
import org.eclipse.sw360.common.utils.converter.licenses.ObligationConverter;
import org.eclipse.sw360.common.utils.converter.licenses.ObligationElementConverter;
import org.eclipse.sw360.common.utils.converter.licenses.ObligationLevelConverter;
import org.eclipse.sw360.common.utils.converter.licenses.ObligationNodeConverter;
import org.eclipse.sw360.datahandler.services.common.CustomProperties;
import org.eclipse.sw360.datahandler.services.common.PaginationData;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.RequestSummary;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.licenses.LicenseType;
import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.datahandler.services.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.services.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.services.licenses.ObligationNode;
final class LicenseRestMapper {

    private LicenseRestMapper() {}

    static org.eclipse.sw360.datahandler.thrift.PaginationData toThriftPagination(PaginationData pojo) {
        return PaginationDataConverter.toThrift(pojo);
    }

    static PaginationData fromThriftPagination(org.eclipse.sw360.datahandler.thrift.PaginationData thrift) {
        return PaginationDataConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel toThriftObligationLevel(ObligationLevel pojo) {
        return ObligationLevelConverter.toThrift(pojo);
    }

    static ObligationLevel fromThriftObligationLevel(org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel thrift) {
        return ObligationLevelConverter.fromThrift(thrift);
    }

    static License fromThriftLicense(org.eclipse.sw360.datahandler.thrift.licenses.License thrift) {
        return LicenseConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.licenses.License toThriftLicense(License pojo) {
        return LicenseConverter.toThrift(pojo);
    }

    static List<License> fromThriftLicenses(List<org.eclipse.sw360.datahandler.thrift.licenses.License> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(LicenseConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.licenses.License> toThriftLicenses(List<License> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(LicenseConverter::toThrift).collect(Collectors.toList());
    }

    static LicenseType fromThriftLicenseType(org.eclipse.sw360.datahandler.thrift.licenses.LicenseType thrift) {
        return LicenseTypeConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.licenses.LicenseType toThriftLicenseType(LicenseType pojo) {
        return LicenseTypeConverter.toThrift(pojo);
    }

    static List<LicenseType> fromThriftLicenseTypes(List<org.eclipse.sw360.datahandler.thrift.licenses.LicenseType> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(LicenseTypeConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.licenses.LicenseType> toThriftLicenseTypes(List<LicenseType> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(LicenseTypeConverter::toThrift).collect(Collectors.toList());
    }

    static Obligation fromThriftObligation(org.eclipse.sw360.datahandler.thrift.licenses.Obligation thrift) {
        return ObligationConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.licenses.Obligation toThriftObligation(Obligation pojo) {
        return ObligationConverter.toThrift(pojo);
    }

    static List<Obligation> fromThriftObligations(List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ObligationConverter::fromThrift).collect(Collectors.toList());
    }

    static List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation> toThriftObligations(List<Obligation> pojoList) {
        if (pojoList == null) {
            return List.of();
        }
        return pojoList.stream().map(ObligationConverter::toThrift).collect(Collectors.toList());
    }

    static Set<org.eclipse.sw360.datahandler.thrift.licenses.Obligation> toThriftObligationSet(Set<Obligation> pojoSet) {
        if (pojoSet == null) {
            return Set.of();
        }
        return pojoSet.stream().map(ObligationConverter::toThrift).collect(Collectors.toSet());
    }

    static ObligationNode fromThriftObligationNode(org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode thrift) {
        return ObligationNodeConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode toThriftObligationNode(ObligationNode pojo) {
        return ObligationNodeConverter.toThrift(pojo);
    }

    static List<ObligationNode> fromThriftObligationNodes(List<org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ObligationNodeConverter::fromThrift).collect(Collectors.toList());
    }

    static ObligationElement fromThriftObligationElement(org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement thrift) {
        return ObligationElementConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement toThriftObligationElement(ObligationElement pojo) {
        return ObligationElementConverter.toThrift(pojo);
    }

    static List<ObligationElement> fromThriftObligationElements(List<org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(ObligationElementConverter::fromThrift).collect(Collectors.toList());
    }

    static CustomProperties fromThriftCustomProperties(org.eclipse.sw360.datahandler.thrift.CustomProperties thrift) {
        return CustomPropertiesConverter.fromThrift(thrift);
    }

    static org.eclipse.sw360.datahandler.thrift.CustomProperties toThriftCustomProperties(CustomProperties pojo) {
        return CustomPropertiesConverter.toThrift(pojo);
    }

    static List<CustomProperties> fromThriftCustomPropertiesList(List<org.eclipse.sw360.datahandler.thrift.CustomProperties> thriftList) {
        if (thriftList == null) {
            return List.of();
        }
        return thriftList.stream().map(CustomPropertiesConverter::fromThrift).collect(Collectors.toList());
    }

    static RequestStatus fromThriftRequestStatus(org.eclipse.sw360.datahandler.thrift.RequestStatus thrift) {
        return RequestStatusConverter.fromThrift(thrift);
    }

    static RequestSummary fromThriftRequestSummary(org.eclipse.sw360.datahandler.thrift.RequestSummary thrift) {
        return RequestSummaryConverter.fromThrift(thrift);
    }

    static Map<PaginationData, List<Obligation>> fromThriftPaginatedObligations(
            Map<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>> thriftMap) {
        if (thriftMap == null || thriftMap.isEmpty()) {
            return Map.of();
        }
        Map.Entry<org.eclipse.sw360.datahandler.thrift.PaginationData, List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>> entry =
                thriftMap.entrySet().iterator().next();
        return Map.of(fromThriftPagination(entry.getKey()), fromThriftObligations(entry.getValue()));
    }
}
