package org.eclipse.sw360.rest.resourceserver.vendoropenapi;

import org.eclipse.sw360.datahandler.thrift.users.User;
import java.util.List;
import java.util.UUID;
import org.eclipse.sw360.datahandler.postgres.VendorPG;
import org.eclipse.sw360.datahandler.postgresql.VendorRepositoryPG;

public class VendorServicePG {
    private final VendorRepositoryPG vendorRepository = new VendorRepositoryPG();

    public VendorPG createVendor(VendorPG vendor, User user) {
        if (vendor.getShortname() == null && vendor.getFullname() == null) {
            throw new IllegalArgumentException("Vendor name is required");
        }

        return new VendorRepositoryPG().saveVendor(vendor);
    }

    public VendorPG getVendorByFullName(String fullname) {
        VendorPG vendor = vendorRepository.getVendorByFullName(fullname);
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor not found");
        }

        return vendor;
    }

    public VendorPG updateVendor(VendorPG internalVendor, User user) {
        if (internalVendor.getId() == null) {
            throw new IllegalArgumentException("Vendor ID is required");
        }

        VendorPG existingVendor = vendorRepository.getVendorById(internalVendor.getId());
        if (existingVendor == null) {
            throw new IllegalArgumentException("Vendor not found");
        }

        existingVendor.setShortname(internalVendor.getShortname());
        existingVendor.setFullname(internalVendor.getFullname());
        existingVendor.setUrl(internalVendor.getUrl());

        return vendorRepository.updateVendor(existingVendor);
    }

    public void deleteVendor(UUID fromString, User user) {
        VendorPG vendor = vendorRepository.getVendorById(fromString);
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor not found");
        }

        vendorRepository.deleteVendor(vendor);
    }

    public VendorPG getVendorById(String defaultVendorId) {
        VendorPG vendor = vendorRepository.getVendorById(UUID.fromString(defaultVendorId));
        if (vendor == null) {
            throw new IllegalArgumentException("Vendor not found");
        }

        return vendor;
    }

    public List<VendorPG> getVendors() {
        List<VendorPG> vendors = vendorRepository.getVendors();
        if (vendors == null) {
            throw new IllegalArgumentException("No vendors found");
        }
        return vendors;
    }
}
