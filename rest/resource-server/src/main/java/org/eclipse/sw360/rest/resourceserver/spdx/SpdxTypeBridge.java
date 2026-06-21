/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.spdx;

import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

/**
 * Maps between Thrift SPDX types (still used in moderation / merge logic) and service-api POJOs
 * without pulling in {@code backend-common}.
 */
@Component
public class SpdxTypeBridge {

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    private final ObjectMapper mapper;

    public SpdxTypeBridge(com.fasterxml.jackson.databind.Module sw360Module) {
        this.sw360Module = sw360Module;
        this.mapper = new ObjectMapper().registerModule(sw360Module);
    }

    public org.eclipse.sw360.datahandler.services.spdx.SPDXDocument toPojo(
            org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument thrift) {
        return thrift == null ? null
                : mapper.convertValue(thrift, org.eclipse.sw360.datahandler.services.spdx.SPDXDocument.class);
    }

    public org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument toThrift(
            org.eclipse.sw360.datahandler.services.spdx.SPDXDocument pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument.class);
    }

    public org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation toPojo(
            org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation thrift) {
        return thrift == null ? null
                : mapper.convertValue(thrift, org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation.class);
    }

    public org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation toThrift(
            org.eclipse.sw360.datahandler.services.spdx.DocumentCreationInformation pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo,
                        org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation.class);
    }

    public org.eclipse.sw360.datahandler.services.spdx.PackageInformation toPojo(
            org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation thrift) {
        return thrift == null ? null
                : mapper.convertValue(thrift, org.eclipse.sw360.datahandler.services.spdx.PackageInformation.class);
    }

    public org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation toThrift(
            org.eclipse.sw360.datahandler.services.spdx.PackageInformation pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation.class);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus toThriftRequestStatus(RequestStatus pojo) {
        return pojo == null ? null : org.eclipse.sw360.datahandler.thrift.RequestStatus.valueOf(pojo.name());
    }
}
