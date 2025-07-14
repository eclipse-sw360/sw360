package org.eclipse.sw360.datahandler.postgres;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentTypeAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.VendorAPI;

@JsonInclude(value = Include.NON_EMPTY, content = Include.NON_NULL)
@Entity
@Table(name = "component")
@Transactional
public class ComponentPG extends ComponentAPI implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;
    @Column(nullable = true)
    private String revision;
    @Column(nullable = true)
    private String description;
    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private ComponentTypeAPI componentType;
    @Column(nullable = true)
    private String createdBy;
    @Column(nullable = true)
    private String ownerAccountingUnit;
    @Column(nullable = true)
    private String ownerGroup;
    @Column(nullable = true)
    private String ownerCountry;
    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private VisibilityEnum visibility;
    @Column(nullable = true)
    private String businessUnit;
    @Column(nullable = true)
    private String cdxComponentType;
    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> externalIds;
    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> additionalData;
    @Column(nullable = true)
    private List<String> mainLicenseIds;
    @Column(nullable = true)
    private String defaultVendorId;
    @Column(nullable = true)
    private List<String> categories;
    @Column(nullable = true)
    private Date createdOn;
    @Column(nullable = true)
    private String componentOwner;
    @Column(nullable = true)
    private String modifiedBy;
    @Column(nullable = true)
    private Date modifiedOn;
    @Column(nullable = true)
    private String homepage;
    @Column(nullable = true)
    private String mailinglist;
    @Column(nullable = true)
    private String wiki;
    @Column(nullable = true)
    private String blog;
    @Column(nullable = true)
    private String wikipedia;
    @Column(nullable = true)
    private String openHub;
    @Column(nullable = true)
    private String vcs;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Boolean> permissions;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> languages;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> softwarePlatforms;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> operatingSystems;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> vendorNames;

    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<ReleasePG> releases;

    @ManyToMany(cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    @JoinTable(name = "component_vendors", joinColumns = @JoinColumn(name = "component_id"),
            inverseJoinColumns = @JoinColumn(name = "vendor_id"))
    private Set<VendorPG> vendors = new HashSet<>();

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    public ComponentPG(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public List<VendorAPI> getVendors() {
        return vendors.stream().map(vendor -> {
            VendorAPI vendorModel = new VendorAPI();
            vendorModel.setRevision(vendor.getRevision());
            vendorModel.setShortname(vendor.getShortname());
            vendorModel.setUrl(vendor.getUrl());
            vendorModel.setFullname(vendor.getFullname());
            vendorModel.setId(vendor.getId());
            return vendorModel;
        }).collect(Collectors.toList());
    }

    public void setVendors(List<VendorAPI> vendors) {
        for (VendorAPI vendor : vendors) {
            if (!this.vendors.contains(new VendorPG(vendor))) {
                this.vendors.add(new VendorPG(vendor));
            }
        }
    }

    @Override
    public List<String> getVendorNames() {
        return vendors.stream().map(vendor -> vendor.getFullname()).collect(Collectors.toList());
    }

    public ComponentPG(UUID id, String name, String revision, String description,
            ComponentTypeAPI componentType, String createdBy, String ownerAccountingUnit,
            String ownerGroup, String ownerCountry, VisibilityEnum visibility, String businessUnit,
            String cdxComponentType, Map<String, String> externalIds,
            Map<String, String> additionalData, List<String> mainLicenseIds, String defaultVendorId,
            List<String> categories, Date createdOn, String componentOwner, String modifiedBy,
            Date modifiedOn, String homepage, String mailinglist, String wiki, String blog,
            String wikipedia, String openHub, String vcs, Map<String, Boolean> permissions,
            List<String> languages, List<String> softwarePlatforms, List<String> operatingSystems,
            List<String> vendorNames) {
        this.id = id;
        this.name = name;
        this.revision = revision;
        this.description = description;
        this.componentType = componentType;
        this.createdBy = createdBy;
        this.ownerAccountingUnit = ownerAccountingUnit;
        this.ownerGroup = ownerGroup;
        this.ownerCountry = ownerCountry;
        this.visibility = visibility;
        this.businessUnit = businessUnit;
        this.cdxComponentType = cdxComponentType;
        this.externalIds = externalIds;
        this.additionalData = additionalData;
        this.mainLicenseIds = mainLicenseIds;
        this.defaultVendorId = defaultVendorId;
        this.categories = categories;
        this.createdOn = createdOn;
        this.componentOwner = componentOwner;
        this.modifiedBy = modifiedBy;
        this.modifiedOn = modifiedOn;
        this.homepage = homepage;
        this.mailinglist = mailinglist;
        this.wiki = wiki;
        this.blog = blog;
        this.wikipedia = wikipedia;
        this.openHub = openHub;
        this.vcs = vcs;
        this.permissions = permissions;
        this.languages = languages;
        this.softwarePlatforms = softwarePlatforms;
        this.operatingSystems = operatingSystems;
        this.vendorNames = vendorNames;
    }

    public ComponentPG() {
        this.createdOn = new Date();
        this.modifiedOn = new Date();
    }

    public ComponentPG(ComponentAPI component) {
        this.id = component.getId();
        this.createdBy = component.getCreatedBy();
        this.modifiedBy = component.getModifiedBy();
        this.name = component.getName();
        this.revision = component.getRevision();
        this.description = component.getDescription();
        this.additionalData = component.getAdditionalData();
        this.ownerAccountingUnit = component.getOwnerAccountingUnit();
        this.ownerGroup = component.getOwnerGroup();
        this.ownerCountry = component.getOwnerCountry();
        this.businessUnit = component.getBusinessUnit();
        this.externalIds = component.getExternalIds();
        this.mainLicenseIds = component.getMainLicenseIds();
        this.defaultVendorId = component.getDefaultVendorId();
        this.categories = component.getCategories();
        this.componentOwner = component.getComponentOwner();
        this.componentType = component.getComponentType();
        this.homepage = component.getHomepage();
        this.mailinglist = component.getMailinglist();
        this.wiki = component.getWiki();
        this.blog = component.getBlog();
        this.wikipedia = component.getWikipedia();
        this.openHub = component.getOpenHub();
        this.vcs = component.getVcs();
        this.permissions = component.getPermissions();
        this.languages = component.getLanguages() != null ? component.getLanguages() : List.of();
        this.softwarePlatforms =
                component.getSoftwarePlatforms() != null ? component.getSoftwarePlatforms()
                        : List.of();
        this.operatingSystems =
                component.getOperatingSystems() != null ? component.getOperatingSystems()
                        : List.of();
        this.vendorNames =
                component.getVendorNames() != null ? component.getVendorNames() : List.of();
    }

    @Override
    public String toString() {
        return "Component{" + "id=" + id + ", name='" + name + '\'' + ", revision='" + revision
                + '\'' + ", description='" + description + '\'' + ", componentType='"
                + componentType + '\'' + ", createdBy='" + createdBy + '\''
                + ", ownerAccountingUnit='" + ownerAccountingUnit + '\'' + ", ownerGroup='"
                + ownerGroup + '\'' + ", ownerCountry='" + ownerCountry + '\'' + ", visibility='"
                + visibility + '\'' + ", businessUnit='" + businessUnit + '\''
                + ", cdxComponentType='" + cdxComponentType + '\'' + ", externalIds='" + externalIds
                + '\'' + ", additionalData='" + additionalData + '\'' + ", mainLicenseIds='"
                + mainLicenseIds + '\'' + ", defaultVendorId='" + defaultVendorId + '\''
                + ", categories='" + categories + '\'' + ", createdOn='" + createdOn + '\''
                + ", componentOwner='" + componentOwner + '\'' + ", modifiedBy='" + modifiedBy
                + '\'' + ", modifiedOn='" + modifiedOn + '\'' + ", homepage='" + homepage + '\''
                + ", mailinglist='" + mailinglist + '\'' + ", wiki='" + wiki + '\'' + ", blog='"
                + blog + '\'' + ", wikipedia='" + wikipedia + '\'' + ", openHub='" + openHub + '\''
                + ", vcs='" + vcs + '\'' + ", permissions='" + permissions + '\'' + ", languages='"
                + languages + '\'' + ", softwarePlatforms='" + softwarePlatforms + '\''
                + ", operatingSystems='" + operatingSystems + '\'' + ", vendorNames='" + vendorNames
                + '\'' + '}';
    }

    @Override
    public String getRevision() {
        return revision;
    }

    @Override
    @Schema(name = "releases", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    public List<org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI> getReleases() {
        if (releases == null) {
            return List.of();
        } else if (releases.isEmpty()) {
            return List.of();
        } else {
            return releases.stream().map(release -> {
                org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI releaseModel =
                        new org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI();
                releaseModel.setId(release.getId());
                releaseModel.setName(release.getName());
                releaseModel.setVersion(release.getVersion());
                return releaseModel;
            }).toList();
        }
    }

    public List<ReleasePG> getReleasesPG() {
        return releases;
    }

    @Override
    public void setReleases(
            List<org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI> releases) {
        this.releases = releases.stream().map(ReleasePG::new).toList();
    }

    @Override
    public void setRevision(String revision) {
        this.revision = revision;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String getOwnerAccountingUnit() {
        return ownerAccountingUnit;
    }

    @Override
    public void setOwnerAccountingUnit(String ownerAccountingUnit) {
        this.ownerAccountingUnit = ownerAccountingUnit;
    }

    @Override
    public String getOwnerGroup() {
        return ownerGroup;
    }

    @Override
    public void setOwnerGroup(String ownerGroup) {
        this.ownerGroup = ownerGroup;
    }

    @Override
    public String getOwnerCountry() {
        return ownerCountry;
    }

    @Override
    public void setOwnerCountry(String ownerCountry) {
        this.ownerCountry = ownerCountry;
    }

    @Override
    public void setVisibility(VisibilityEnum visibility) {
        this.visibility = visibility;
    }

    @Override
    public String getBusinessUnit() {
        return businessUnit;
    }

    @Override
    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    @Override
    public void setCdxComponentType(String cdxComponentType) {
        this.cdxComponentType = cdxComponentType;
    }

    @Override
    public void setExternalIds(Map<String, String> externalIds) {
        this.externalIds = externalIds;
    }

    @Override
    public void setMainLicenseIds(List<String> mainLicenseIds) {
        this.mainLicenseIds = mainLicenseIds;
    }

    public void setDefaultVendor(VendorPG defaultVendor) {
        this.defaultVendorId =
                defaultVendor.getId() != null ? defaultVendor.getId().toString() : null;
    }

    @Override
    public String getDefaultVendorId() {
        return defaultVendorId;
    }

    @Override
    public void setDefaultVendorId(String defaultVendorId) {
        this.defaultVendorId = defaultVendorId;
    }

    @Override
    public LocalDate getCreatedOn() {
        return createdOn != null ? new java.sql.Date(createdOn.getTime()).toLocalDate() : null;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public String getComponentOwner() {
        return componentOwner;
    }

    @Override
    public void setComponentOwner(String componentOwner) {
        this.componentOwner = componentOwner;
    }

    @Override
    public ComponentTypeAPI getComponentType() {
        return componentType;
    }

    @Override
    public void setComponentType(ComponentTypeAPI componentType) {
        this.componentType = componentType;
    }

    @Override
    public String getModifiedBy() {
        return modifiedBy;
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public Map<String, Boolean> getPermissions() {
        return permissions;
    }

    @Override
    public void setPermissions(Map<String, Boolean> permissions) {
        this.permissions = permissions;
    }

    @Override
    public List<String> getLanguages() {
        return languages;
    }

    @Override
    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    @Override
    public List<String> getSoftwarePlatforms() {
        return softwarePlatforms;
    }

    @Override
    public void setSoftwarePlatforms(List<String> softwarePlatforms) {
        this.softwarePlatforms = softwarePlatforms;
    }

    @Override
    public List<String> getOperatingSystems() {
        return operatingSystems;
    }

    @Override
    public void setOperatingSystems(List<String> operatingSystems) {
        this.operatingSystems = operatingSystems;
    }

    @Override
    public void setVendorNames(List<String> vendorNames) {
        this.vendorNames = vendorNames;
    }

}
