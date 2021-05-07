/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import com.cloudant.client.api.model.DesignDocument.MapReduce;

/**
 * CRUD access for the Vendor class
 *
 */
public class VendorRepository extends DatabaseRepositoryCloudantClient<Vendor> {

    private static final String ALL = "function(doc) { if (doc.type == 'vendor') emit(null, doc._id) }";

    public VendorRepository(DatabaseConnectorCloudant db) {
        super(db, Vendor.class);
        Map<String, MapReduce> views = new HashMap<String, MapReduce>();
        views.put("all", createMapReduce(ALL, null));
        initStandardDesignDocument(views, db);
    }

    public void fillVendor(Component component) {
        if (component.isSetDefaultVendorId()) {
            final String vendorId = component.getDefaultVendorId();
            if (!isNullOrEmpty(vendorId)) {
                final Vendor vendor = get(vendorId);
                if (vendor != null)
                    component.setDefaultVendor(vendor);
            }
        }
    }

    public void fillVendor(Release release) {
        if (release.isSetVendorId()) {
            final String vendorId = release.getVendorId();
            if (!isNullOrEmpty(vendorId)) {
                final Vendor vendor = get(vendorId);
                if (vendor != null)
                    release.setVendor(vendor);
            }
            release.unsetVendorId();
        }
    }
}
