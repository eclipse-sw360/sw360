/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.admin.licensedb;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.stereotype.Service;

import java.util.Map;

import static org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper.throwIfNotAdmin;

@Service
public class Sw360LicenseDBAdminService {

    public RequestSummary triggerFullSync(User user) throws TException {
        throwIfNotAdmin(user);
        return ThriftClients.makeLicenseClient().importAllLicenseDBLicenses(user);
    }

    public RequestSummary triggerIncrementalSync(User user) throws TException {
        throwIfNotAdmin(user);
        return ThriftClients.makeLicenseClient().importIncrementalLicenseDBLicenses(user);
    }

    public LicenseDBSyncStatus getHealth(User user) throws TException {
        throwIfNotAdmin(user);
        LicenseService.Iface client = ThriftClients.makeLicenseClient();
        Map<String, String> raw = client.getLicenseDBSyncStatus(user);
        boolean connected = client.pingLicenseDBHealth(user);

        LicenseDBSyncStatus status = new LicenseDBSyncStatus();
        status.setEnabled(Boolean.parseBoolean(raw.getOrDefault("enabled", "false")));
        status.setConnected(connected);
        status.setImportRunning(Boolean.parseBoolean(raw.getOrDefault("importRunning", "false")));
        status.setLastSyncTimestamp(raw.get("lastSyncTimestamp"));
        return status;
    }

    public LicenseDBSyncStatus getSyncStatus(User user) throws TException {
        throwIfNotAdmin(user);
        Map<String, String> raw = ThriftClients.makeLicenseClient().getLicenseDBSyncStatus(user);

        LicenseDBSyncStatus status = new LicenseDBSyncStatus();
        status.setEnabled(Boolean.parseBoolean(raw.getOrDefault("enabled", "false")));
        status.setImportRunning(Boolean.parseBoolean(raw.getOrDefault("importRunning", "false")));
        status.setLastSyncTimestamp(raw.get("lastSyncTimestamp"));
        return status;
    }
}
