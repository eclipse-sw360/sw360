/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.packages;

import org.eclipse.sw360.common.utils.converter.packages.PackageConverter;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.springframework.stereotype.Component;

/**
 * Maps between Thrift Package types (still used in HAL / controller layer) and service-api POJOs.
 * Jackson {@code convertValue} with {@code Sw360Module} mixins drops fields such as {@code createdBy}.
 */
@Component
public class PackageTypeBridge {

    public org.eclipse.sw360.datahandler.services.packages.Package toPojo(
            org.eclipse.sw360.datahandler.thrift.packages.Package thrift) {
        return PackageConverter.fromThrift(thrift);
    }

    public org.eclipse.sw360.datahandler.thrift.packages.Package toThrift(
            org.eclipse.sw360.datahandler.services.packages.Package pojo) {
        return PackageConverter.toThrift(pojo);
    }

    public org.eclipse.sw360.datahandler.thrift.RequestStatus toThriftRequestStatus(RequestStatus pojo) {
        return pojo == null ? null : org.eclipse.sw360.datahandler.thrift.RequestStatus.valueOf(pojo.name());
    }
}
