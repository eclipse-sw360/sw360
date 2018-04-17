/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Vendor.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.vendor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360VendorService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public List<Vendor> getVendors() {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            return sw360VendorClient.getAllVendors();
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Vendor getVendorById(String vendorId) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            return sw360VendorClient.getByID(vendorId);
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Vendor getVendorByFullName(String fullName) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            for (Vendor vendor : sw360VendorClient.getAllVendors()) {
                if(fullName.equals(vendor.getFullname())) {
                    return vendor;
                }
            }
            return null;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public Vendor createVendor(Vendor vendor) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            String vendorId = sw360VendorClient.addVendor(vendor);
            vendor.setId(vendorId);
            return vendor;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateVendor(Vendor vendor, User sw360User) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            RequestStatus requestStatus = sw360VendorClient.updateVendor(vendor, sw360User);
            if (requestStatus == RequestStatus.SUCCESS) {
                return;
            }
            throw new RuntimeException("sw360 vendor with full name '" + vendor.getFullname() + " cannot be updated.");
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteVendor(Vendor vendor, User sw360User) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            RequestStatus requestStatus = sw360VendorClient.deleteVendor(vendor.getId(), sw360User);
            if (requestStatus == RequestStatus.SUCCESS) {
                return;
            }
            throw new RuntimeException("sw360 vendor with name '" + vendor.getFullname() + " cannot be deleted.");
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAllVendors(User sw360User) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            List<Vendor> vendors = sw360VendorClient.getAllVendors();
            for (Vendor vendor : vendors) {
                sw360VendorClient.deleteVendor(vendor.getId(), sw360User);
            }
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    private VendorService.Iface getThriftVendorClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/vendors/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new VendorService.Client(protocol);
    }
}
