/*
 * Copyright (c) Bosch Software Innovations GmbH 2017.
 * Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.vendors;

import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.VendorRepository;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.ektorp.http.HttpClient;

import java.net.MalformedURLException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.datahandler.thrift.ThriftValidate.prepareVendor;

public class VendorDatabaseHandler {
    private static final Logger log = Logger.getLogger(VendorDatabaseHandler.class);
    private final VendorRepository repository;

    public VendorDatabaseHandler(Supplier<HttpClient> httpClient, String dbName) throws MalformedURLException {
        DatabaseConnector db = new DatabaseConnector(httpClient, dbName);
        repository = new VendorRepository(db);
    }

    public VendorDatabaseHandler(DatabaseConnector db) throws MalformedURLException {
        repository = new VendorRepository(db);
    }

    public Vendor getByID(String id) throws TException {
        return repository.get(id);
    }

    public List<Vendor> getAllVendors() throws TException {
        return repository.getAll();
    }

    public String addVendor(Vendor vendor) throws TException {
        prepareVendor(vendor);
        repository.add(vendor);
        return vendor.getId();
    }

    public RequestStatus deleteVendor(String id, User user) throws SW360Exception {
        Vendor vendor = repository.get(id);
        assertNotNull(vendor);

        if (makePermission(vendor, user).isActionAllowed(RequestedAction.DELETE)) {
            repository.remove(id);
            return RequestStatus.SUCCESS;
        } else {
            log.error("User is not allowed to delete!");
            return RequestStatus.FAILURE;
        }


    }

    public RequestStatus updateVendor(Vendor vendor, User user) {
        if (makePermission(vendor, user).isActionAllowed(RequestedAction.WRITE)) {
            repository.update(vendor);
            return RequestStatus.SUCCESS;
        } else {
            log.error("User is not allowed to delete!");
            return RequestStatus.FAILURE;
        }
    }


    public void fillVendor(Release release){
        repository.fillVendor(release);
    }
}
