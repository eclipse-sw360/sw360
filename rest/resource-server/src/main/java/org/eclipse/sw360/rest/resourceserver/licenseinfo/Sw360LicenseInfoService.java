/*
 * Copyright Bosch Software Innovations GmbH, 2017.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.resourceserver.licenseinfo;

import java.util.Map;
import java.util.Set;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoFile;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseInfoService;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360LicenseInfoService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public OutputFormatInfo getOutputFormatInfoForGeneratorClass(String generatorClassName) {
        try {
            LicenseInfoService.Iface sw360LicenseInfoClient = getThriftLicenseInfoClient();
            return sw360LicenseInfoClient.getOutputFormatInfoForGeneratorClass(generatorClassName);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public LicenseInfoFile getLicenseInfoFile(Project project, User sw360User, String generatorClassNameWithVariant,
                                              Map<String, Set<String>> selectedReleaseAndAttachmentIds, Map<String, Set<LicenseNameWithText>> excludedLicenses, String externalIds) {
        try {
            LicenseInfoService.Iface sw360LicenseInfoClient = getThriftLicenseInfoClient();
            return sw360LicenseInfoClient.getLicenseInfoFile(project, sw360User, generatorClassNameWithVariant, selectedReleaseAndAttachmentIds, excludedLicenses, externalIds);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    private LicenseInfoService.Iface getThriftLicenseInfoClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/licenseinfo/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new LicenseInfoService.Client(protocol);
    }
}
