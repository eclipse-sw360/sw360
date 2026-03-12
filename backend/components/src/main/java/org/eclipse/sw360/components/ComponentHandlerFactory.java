/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.db.ComponentDatabaseHandler;
import org.eclipse.sw360.datahandler.db.ComponentSearchHandler;
import org.eclipse.sw360.datahandler.db.ReleaseSearchHandler;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;

import java.io.IOException;

final class ComponentHandlerFactory {

    private ComponentHandlerFactory() {
    }

    static ComponentHandler createDefault() throws IOException {
        return create(DatabaseSettings.getConfiguredClient(),
                DatabaseSettings.COUCH_DB_DATABASE,
                DatabaseSettings.COUCH_DB_CHANGE_LOGS,
                DatabaseSettings.COUCH_DB_ATTACHMENTS,
                null);
    }

    static ComponentHandler createWithThriftClients(ThriftClients thriftClients) throws IOException {
        return create(DatabaseSettings.getConfiguredClient(),
                DatabaseSettings.COUCH_DB_DATABASE,
                DatabaseSettings.COUCH_DB_CHANGE_LOGS,
                DatabaseSettings.COUCH_DB_ATTACHMENTS,
                thriftClients);
    }

    static ComponentHandler create(Cloudant client, String dbName, String changeLogsDbName, String attachmentDbName) throws IOException {
        return create(client, dbName, changeLogsDbName, attachmentDbName, null);
    }

    static ComponentHandler create(Cloudant client, String dbName, String changeLogsDbName, String attachmentDbName,
            ThriftClients thriftClients) throws IOException {
        ComponentDatabaseHandler handler = thriftClients == null
                ? new ComponentDatabaseHandler(client, dbName, changeLogsDbName, attachmentDbName)
                : new ComponentDatabaseHandler(client, dbName, changeLogsDbName, attachmentDbName, thriftClients);
        ComponentSearchHandler componentSearchHandler = new ComponentSearchHandler(client, dbName);
        ReleaseSearchHandler releaseSearchHandler = new ReleaseSearchHandler(client, dbName);
        return new ComponentHandler(handler, componentSearchHandler, releaseSearchHandler);
    }
}
