package org.eclipse.sw360.datahandler.postgres;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.util.*;
import org.eclipse.sw360.datahandler.componentsApi.model.ComponentAPI;
import org.eclipse.sw360.datahandler.componentsApi.model.ProjectAPI;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_EMPTY, content = Include.NON_NULL)
@Entity
@Table(name = "project")
@Transactional
public class ProjectPG extends ProjectAPI implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = true)
    private String revision;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String version;

    @Column(nullable = true)
    private String domain;

    @Column(nullable = true)
    private List<String> attachments = new ArrayList<>();

    @Column(nullable = true)
    private String createdOn;

    @Column(nullable = true)
    private String businessUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private StateEnum state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ProjectTypeEnum projectType;

    @Column(nullable = true)
    private String tag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ClearingStateEnum clearingState;

    @Column(nullable = true)
    private String createdBy;

    @Column(nullable = true)
    private String projectResponsible;

    @Column(nullable = true)
    private String leadArchitect;

    @Column(nullable = true)
    private List<String> moderators = new ArrayList<>();

    @Column(nullable = true)
    private List<String> contributors = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private VisibilityEnum visibility;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, List<String>> roles = new HashMap<>();

    @Column(nullable = true)
    private List<String> securityResponsibles = new ArrayList<>();

    @Column(nullable = true)
    private String projectOwner;

    @Column(nullable = true)
    private String ownerAccountingUnit;

    @Column(nullable = true)
    private String ownerGroup;

    @Column(nullable = true)
    private String ownerCountry;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> linkedProjects = new HashMap<>();

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> releaseIdToUsage = new HashMap<>();

    @Column(nullable = true)
    private List<String> packageIds = new ArrayList<>();

    @Column(nullable = true)
    private String clearingTeam;

    @Column(nullable = true)
    private String preevaluationDeadline;

    @Column(nullable = true)
    private String systemTestStart;

    @Column(nullable = true)
    private String systemTestEnd;

    @Column(nullable = true)
    private String deliveryStart;

    @Column(nullable = true)
    private String phaseOutSince;

    @Column(nullable = true)
    private Boolean enableSvm;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> externalIds = new HashMap<>();

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> additionalData = new HashMap<>();

    @Column(nullable = true)
    private Boolean considerReleasesFromExternalList;

    @Column(nullable = true)
    private String licenseInfoHeaderText;

    @Column(nullable = true)
    private Boolean enableVulnerabilitiesDisplay;

    @Column(nullable = true)
    private String obligationsText;

    @Column(nullable = true)
    private String clearingSummary;

    @Column(nullable = true)
    private String specialRisksOSS;

    @Column(nullable = true)
    private String generalRisks3rdParty;

    @Column(nullable = true)
    private String specialRisks3rdParty;

    @Column(nullable = true)
    private String deliveryChannels;

    @Column(nullable = true)
    private String remarksAdditionalRequirements;

    @Column(nullable = true)
    private String documentState;

    @Column(nullable = true)
    private String clearingRequestId;

    @Column(nullable = true)
    private String releaseClearingStateSummary;

    @Column(nullable = true)
    private String linkedObligationId;

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Boolean> permissions = new HashMap<>();

    @Column(nullable = true)
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> externalUrls = new HashMap<>();

    @Column(nullable = true)
    private String vendor;

    @Column(nullable = true)
    private String modifiedBy;

    @Column(nullable = true)
    private String modifiedOn;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "project_components", joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "component_id"))
    private List<ComponentPG> components = new ArrayList<>();

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getRevision() {
        return revision;
    }

    @Override
    public StateEnum getState() {
        return state;
    }

    @Override
    public void setState(StateEnum state) {
        this.state = state;
    }

    @Override
    public ProjectTypeEnum getProjectType() {
        return projectType;
    }

    @Override
    public void setProjectType(ProjectTypeEnum projectType) {
        this.projectType = projectType;
    }

    @Override
    public List<String> getModerators() {
        return moderators;
    }

    @Override
    public void setModerators(List<String> moderators) {
        this.moderators = moderators;
    }

    @Override
    public List<String> getContributors() {
        return contributors;
    }

    @Override
    public void setContributors(List<String> contributors) {
        this.contributors = contributors;
    }

    @Override
    public Map<String, List<String>> getRoles() {
        return roles;
    }

    @Override
    public void setRoles(Map<String, List<String>> roles) {
        this.roles = roles;
    }

    @Override
    public List<String> getSecurityResponsibles() {
        return securityResponsibles;
    }

    @Override
    public void setSecurityResponsibles(List<String> securityResponsibles) {
        this.securityResponsibles = securityResponsibles;
    }

    @Override
    public Map<String, String> getLinkedProjects() {
        return linkedProjects;
    }

    @Override
    public void setLinkedProjects(Map<String, String> linkedProjects) {
        this.linkedProjects = linkedProjects;
    }

    @Override
    public Map<String, String> getReleaseIdToUsage() {
        return releaseIdToUsage;
    }

    @Override
    public void setReleaseIdToUsage(Map<String, String> releaseIdToUsage) {
        this.releaseIdToUsage = releaseIdToUsage;
    }

    @Override
    public List<String> getPackageIds() {
        return packageIds;
    }

    @Override
    public void setPackageIds(List<String> packageIds) {
        this.packageIds = packageIds;
    }

    @Override
    public Map<String, String> getExternalIds() {
        return externalIds;
    }

    @Override
    public void setExternalIds(Map<String, String> externalIds) {
        this.externalIds = externalIds;
    }

    @Override
    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    @Override
    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    @Override
    public void setBusinessUnit(String businessUnit) {
        this.businessUnit = businessUnit;
    }

    public String getBusinessUnit() {
        return businessUnit;
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
    public Map<String, String> getExternalUrls() {
        return externalUrls;
    }

    @Override
    public void setExternalUrls(Map<String, String> externalUrls) {
        this.externalUrls = externalUrls;
    }

    public List<ComponentAPI> getComponents() {
        return components.stream().map(component -> {
            ComponentAPI componentAPI = new ComponentAPI();
            componentAPI.setId(component.getId());
            componentAPI.setName(component.getName());
            // componentAPI.
            return componentAPI;
        }).toList();
    }

    public List<ComponentPG> getComponentsPG() {
        return components;
    }

    public void setComponents(List<ComponentAPI> components) {
        this.components = components.stream().map(ComponentPG::new).toList();
    }

    public ProjectPG() {
        super();
    }

    public ProjectPG(ProjectAPI project) {
        this.revision = project.getRevision();
        this.name = project.getName();
        this.description = project.getDescription();
        this.version = project.getVersion();
        this.domain = project.getDomain();
        this.createdOn = project.getCreatedOn();
        this.businessUnit = project.getBusinessUnit();
        this.state = project.getState() != null ? StateEnum.valueOf(project.getState().name())
                : StateEnum.UNKNOWN;
        this.projectType = project.getProjectType() != null
                ? ProjectTypeEnum.valueOf(project.getProjectType().name())
                : ProjectTypeEnum.INTERNAL;
        this.tag = project.getTag();
        this.clearingState = project.getClearingState() != null
                ? ClearingStateEnum.valueOf(project.getClearingState().name())
                : ClearingStateEnum.OPEN;
        this.createdBy = project.getCreatedBy();
        this.projectResponsible = project.getProjectResponsible();
        this.leadArchitect = project.getLeadArchitect();
        this.moderators = new ArrayList<>(project.getModerators());
        this.contributors = new ArrayList<>(project.getContributors());
        this.visibility = project.getVisibility() != null
                ? VisibilityEnum.valueOf(project.getVisibility().name())
                : VisibilityEnum.RESTRICTED;
        this.roles.putAll(project.getRoles());
        this.securityResponsibles = new ArrayList<>(project.getSecurityResponsibles());
        this.projectOwner = project.getProjectOwner();
        this.ownerAccountingUnit = project.getOwnerAccountingUnit();
        this.ownerGroup = project.getOwnerGroup();
        this.ownerCountry = project.getOwnerCountry();
        this.releaseIdToUsage.putAll(project.getReleaseIdToUsage());
        this.packageIds.addAll(project.getPackageIds());
        this.clearingTeam = project.getClearingTeam();
        this.preevaluationDeadline = project.getPreevaluationDeadline();
        this.systemTestStart = project.getSystemTestStart();
        this.systemTestEnd = project.getSystemTestEnd();
        this.deliveryStart = project.getDeliveryStart();
        this.phaseOutSince = project.getPhaseOutSince();
        this.externalIds.putAll(project.getExternalIds());
        this.additionalData.putAll(project.getAdditionalData());
        this.components = project.getComponents().stream().map(ComponentPG::new).toList();
    }
}
