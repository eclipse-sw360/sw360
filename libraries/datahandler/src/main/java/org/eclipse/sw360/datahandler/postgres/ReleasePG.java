package org.eclipse.sw360.datahandler.postgres;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_EMPTY, content = Include.NON_NULL)
@Entity
@Table(name = "release")
@Transactional
public class ReleasePG extends org.eclipse.sw360.datahandler.componentsApi.model.ReleaseAPI
        implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String version;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String createdBy;

    @Column(nullable = true)
    private Date createdOn;

    @Column(nullable = true)
    private String modifiedBy;

    @Column(nullable = true)
    private LocalDate modifiedOn;

    @ManyToOne
    @JoinColumn(name = "component_id")
    private ComponentPG component;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "release")
    @Fetch(FetchMode.JOIN)
    private java.util.Set<VulnerabilityReleaseRelationPG> vulnerabilities;

    public ComponentPG getComponent() {
        return component;
    }

    public Set<VulnerabilityReleaseRelationPG> getVulnerabilityRelation() {
        return vulnerabilities;
    }

    public void setVulnerabilityRelation(Set<VulnerabilityReleaseRelationPG> vulnerabilities) {
        this.vulnerabilities = vulnerabilities;
    }

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

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
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
    public LocalDate getModifiedOn() {
        return modifiedOn;
    }

    @Override
    public void setModifiedOn(LocalDate modifiedOn) {
        this.modifiedOn = modifiedOn;
    }

    @Override
    public String toString() {
        return "Release{" + "id=" + id + ", name='" + name + '\'' + ", version='" + version + '\''
                + ", description='" + ", createdBy='" + createdBy + '\'' + ", createdOn="
                + createdOn + ", modifiedBy='" + modifiedBy + '\'' + ", modifiedOn=" + modifiedOn
                + '}';
    }

    public void setComponent(ComponentPG new_Component) {
        this.component = new_Component;
    }

    public ReleasePG() {
        super();
    }

    public ReleasePG(ReleaseAPI release) {
        this.id = release.getId();
        this.name = release.getName();
        this.version = release.getVersion();
        this.modifiedBy = release.getModifiedBy();
        this.modifiedOn = release.getModifiedOn();

    }

}
