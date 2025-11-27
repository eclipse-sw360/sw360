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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;

import java.util.Set;
import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * CRUD access for the Vendor class
 *
 */
@org.springframework.stereotype.Component
public class VendorRepository extends DatabaseRepositoryCloudantClient<Vendor> {

    private static final String BY_LOWERCASE_VENDOR_SHORTNAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vendor' && doc.shortname != null) {" +
                    "    emit(doc.shortname.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String BY_LOWERCASE_VENDOR_FULLNAME_VIEW =
            "function(doc) {" +
                    "  if (doc.type == 'vendor' && doc.fullname != null) {" +
                    "    emit(doc.fullname.toLowerCase(), doc._id);" +
                    "  } " +
                    "}";

    private static final String ALL = "function(doc) { if (doc.type == 'vendor') emit(null, doc._id) }";

    @Autowired
    public VendorRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, Vendor.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("vendorbyshortname", createMapReduce(BY_LOWERCASE_VENDOR_SHORTNAME_VIEW, null));
        views.put("vendorbyfullname", createMapReduce(BY_LOWERCASE_VENDOR_FULLNAME_VIEW, null));
        initStandardDesignDocument(views, db);
    }

    public List<Vendor> searchByFullname(String fullname) {
        List<Vendor> vendorsMatchingFullname =  new ArrayList<Vendor>(get(queryForIdsAsValue("vendorbyfullname", fullname)));
        return vendorsMatchingFullname;
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

    public void fillVendor(Project project) {
        if (project.isSetVendorId()) {
            final String vendorId = project.getVendorId();
            if (!isNullOrEmpty(vendorId)) {
                final Vendor vendor = get(vendorId);
                if (vendor != null)
                    project.setVendor(vendor);
            }
            project.unsetVendorId();
        }
    }

    public void fillVendor(Release release) {
        fillVendor(release, null);
    }

    public void fillVendor(Release release, Map<String, Vendor> vendorCache) {
        if (release.isSetVendorId()) {
            final String vendorId = release.getVendorId();
            if (!isNullOrEmpty(vendorId)) {
                final Vendor vendor = vendorCache == null ? get(vendorId) : vendorCache.computeIfAbsent(vendorId, this::get);
                if (vendor != null) {
                    release.setVendor(vendor);
                }
            }
            release.unsetVendorId();
        }
    }

    public Set<String> getVendorByLowercaseShortnamePrefix(String shortnamePrefix) {
        return queryForIdsByPrefix("vendorbyshortname", shortnamePrefix != null ? shortnamePrefix.toLowerCase() : shortnamePrefix);
    }

    public Set<String> getVendorByLowercaseFullnamePrefix(String fullnamePrefix) {
        return queryForIdsByPrefix("vendorbyfullname", fullnamePrefix != null ? fullnamePrefix.toLowerCase() : fullnamePrefix);
    }
}
