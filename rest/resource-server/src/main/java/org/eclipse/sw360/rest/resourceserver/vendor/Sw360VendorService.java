/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Vendor.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.vendor;

import lombok.RequiredArgsConstructor;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestStatus;
import org.eclipse.sw360.datahandler.thrift.AddDocumentRequestSummary;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360VendorService {
    @Value("${sw360.thrift-server-url:http://localhost:8080}")
    private String thriftServerUrl;

    public List<Vendor> getVendors() {
        try {
            return getAllVendorList();
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
            for (Vendor vendor : getAllVendorList()) {
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
            if (CommonUtils.isNullEmptyOrWhitespace(vendor.getFullname()) || CommonUtils.isNullEmptyOrWhitespace(vendor.getShortname())
                    || CommonUtils.isNullEmptyOrWhitespace(vendor.getUrl())) {
                throw new HttpMessageNotReadableException("A Vendor cannot have null or empty 'Full Name' or 'Short Name' or 'URL'!");
            }
            AddDocumentRequestSummary summary = sw360VendorClient.addVendor(vendor);
            if (AddDocumentRequestStatus.SUCCESS.equals(summary.getRequestStatus())) {
                vendor.setId(summary.getId());
                return vendor;
            } else if (AddDocumentRequestStatus.DUPLICATE.equals(summary.getRequestStatus())) {
                throw new DataIntegrityViolationException("A Vendor with same full name '" + vendor.getFullname() + "' and URL already exists!");
            } else if (AddDocumentRequestStatus.FAILURE.equals(summary.getRequestStatus())) {
                throw new HttpMessageNotReadableException(summary.getMessage());
            }
            return null;
        } catch (TException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateVendor(Vendor vendor, User sw360User) {
        try {
            VendorService.Iface sw360VendorClient = getThriftVendorClient();
            RequestStatus requestStatus = sw360VendorClient.updateVendor(vendor, sw360User);
            if (RequestStatus.SUCCESS.equals(requestStatus)) {
                return;
            } else if (RequestStatus.DUPLICATE.equals(requestStatus)) {
                throw new DataIntegrityViolationException("A Vendor with same full name '" + vendor.getFullname() + "' and URL already exists!");
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
            List<Vendor> vendors = getAllVendorList();
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

    public ByteBuffer exportExcel() throws TException {
        List<Vendor> vendors = getAllVendorList();
        VendorService.Iface sw360VendorClient = getThriftVendorClient();
        return sw360VendorClient.getVendorReportDataStream(vendors);
    }

    private List<Vendor> getAllVendorList() throws TException {
        VendorService.Iface sw360VendorClient = getThriftVendorClient();
        return sw360VendorClient.getAllVendors();
    }
}
