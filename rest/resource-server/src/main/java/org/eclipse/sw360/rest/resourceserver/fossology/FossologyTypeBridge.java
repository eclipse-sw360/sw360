/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.fossology;

import org.eclipse.sw360.datahandler.services.common.ConfigContainer;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcess;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.NonNull;

/**
 * Maps between Thrift types (still used in HAL / controller layer) and service-api POJOs
 * without pulling in {@code backend-common}.
 */
@Component
public class FossologyTypeBridge {

    @NonNull
    private final com.fasterxml.jackson.databind.Module sw360Module;

    private final ObjectMapper mapper;

    public FossologyTypeBridge(com.fasterxml.jackson.databind.Module sw360Module) {
        this.sw360Module = sw360Module;
        this.mapper = new ObjectMapper().registerModule(sw360Module);
    }

    public ConfigContainer toPojo(org.eclipse.sw360.datahandler.thrift.ConfigContainer thrift) {
        return thrift == null ? null : mapper.convertValue(thrift, ConfigContainer.class);
    }

    public org.eclipse.sw360.datahandler.thrift.ConfigContainer toThrift(ConfigContainer pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.ConfigContainer.class);
    }

    public ExternalToolProcess toPojo(org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess thrift) {
        return thrift == null ? null : mapper.convertValue(thrift, ExternalToolProcess.class);
    }

    public org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess toThrift(ExternalToolProcess pojo) {
        return pojo == null ? null
                : mapper.convertValue(pojo, org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess.class);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus toThriftRequestStatus(RequestStatus pojo) {
        return pojo == null ? null : org.eclipse.sw360.datahandler.thrift.RequestStatus.valueOf(pojo.name());
    }
}
