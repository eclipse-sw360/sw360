package org.eclipse.sw360.rest.resourceserver.componentopenapi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.eclipse.sw360.datahandler.postgres.VendorPG;
import org.eclipse.sw360.datahandler.postgres.ComponentPG;
import org.eclipse.sw360.datahandler.postgresql.ComponentRepositoryPG;
import org.eclipse.sw360.datahandler.postgresql.ReleaseRepositoryPG;
import org.eclipse.sw360.datahandler.postgresql.VendorRepositoryPG;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.VendorAPI;


public class ComponentServicePG {
    private ComponentRepositoryPG componentRepository = new ComponentRepositoryPG();

    private ReleaseRepositoryPG releaseRepository = new ReleaseRepositoryPG();

    private VendorRepositoryPG vendorRepository = new VendorRepositoryPG();

    public List<ComponentPG> listComponents(Map<String, String> params) {
        return componentRepository.getComponents(params);
    }

    public List<ComponentPG> listOAPIComponents(Map<String, String> params) {
        List<ComponentPG> components = componentRepository.getComponents(params);

        return components;
    }

    public ComponentPG getComponentById(String id) {
        return componentRepository.getComponentById(id);
    }

    public ComponentPG updateComponent(String id, ComponentPG component, User user) {
        ComponentPG internalComponent = componentRepository.getComponentById(id);
        if (internalComponent == null) {
            throw new IllegalArgumentException("Component not found");
        }
        component.setId(internalComponent.getId());

        return componentRepository.updateComponent(component);
    }

    public ComponentPG createComponent(ComponentAPI component, User user) {

        if (component.getName() == null || component.getName().isEmpty()) {
            throw new IllegalArgumentException("Component name is required");
        }

        ComponentPG internalComponent = new ComponentPG(component);
        if (component.getDefaultVendor() != null) {
            internalComponent.setDefaultVendor(component.getDefaultVendor());
        }

        if (component.getVendors() != null) {
            List<VendorAPI> vendors = new ArrayList<>();
            for (VendorAPI vendor : component.getVendors()) {
                if (vendor.getFullname() == null || vendor.getFullname().isEmpty()) {
                    throw new IllegalArgumentException("Vendor full name is required");
                }

                VendorPG existingVendor =
                        vendorRepository.getVendorByFullName(vendor.getFullname());
                if (existingVendor != null) {
                    vendors.add(existingVendor);
                } else {
                    vendors.add(vendor);
                }

            }

            internalComponent.setVendors(vendors);
        }

        if (component.getVendorNames() != null && !component.getVendorNames().isEmpty()) {
            List<VendorAPI> vendors = component.getVendorNames().stream().map(vendorName -> {
                VendorPG vendor = vendorRepository.getVendorByFullName(vendorName);
                return vendor.ToVendorAPI();
            }).toList();

            internalComponent.setVendors(vendors);
        }

        if (component.getReleases() == null) {
            internalComponent.setReleases(List.of());
        } else {
            internalComponent.setReleases(component.getReleases());
        }

        internalComponent.setCreatedOn(Date.from(Instant.now()));
        if (user != null && user.getEmail() != null) {
            internalComponent.setCreatedBy(user.getEmail());
        } else {
            internalComponent.setCreatedBy("system");
        }

        return componentRepository.saveComponent(internalComponent);
    }

    public List<EntityModel<ComponentPG>> getUsedByResources(String id) {
        return List.of();
    }

    public List<ComponentPG> getRecentComponents(int count) {
        return componentRepository.getComponents(Map.of()).stream().limit(count).toList();
    }

    public List<ComponentPG> getSubscribedComponents(User user) {
        return List.of();
    }

    public List<ComponentPG> getComponentsByExternalIds(Map<String, String> externalIds) {
        return componentRepository.getComponents(externalIds);
    }

    public HttpStatus deleteComponent(String id, User user) {
        ComponentPG component = componentRepository.getComponentById(id);
        if (component != null) {
            try {
                componentRepository.deleteComponent(component);
            } catch (Exception e) {
                e.printStackTrace();
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
            return HttpStatus.OK;
        } else {
            return HttpStatus.NOT_FOUND;
        }
    }

}
