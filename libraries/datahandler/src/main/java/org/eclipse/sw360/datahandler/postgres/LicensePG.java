package org.eclipse.sw360.datahandler.postgres;

import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import java.io.Serializable;
import java.util.*;
import org.eclipse.sw360.datahandler.componentsApi.model.LicenseAPI;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_EMPTY, content = Include.NON_NULL)
@Entity
@Table(name = "licenses")
@Transactional
public class LicensePG extends LicenseAPI implements Serializable {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private UUID id;

        @Column(name = "revision")
        private String revision;

        @Column(name = "type")
        private String type = "license";

        @Column(name = "shortname")
        private String shortname;

        @Column(name = "fullname")
        private String fullname;

        @Column(name = "license_type")
        private String licenseType;

        @Column(name = "license_type_database_id")
        private String licenseTypeDatabaseId;

        @Column(name = "external_license_link")
        private String externalLicenseLink;

        @Column(name = "note")
        private String note;

        @JsonProperty("externalIds")
        private Map<String, String> externalIds = new HashMap<>();

        @JsonProperty("additionalData")
        private Map<String, String> additionalData = new HashMap<>();

        @Column(name = "review_date")
        private String reviewdate;

        @Column(name = "osi_approved")
        private Integer osIApproved;

        @Column(name = "fsf_libre")
        private Integer fsFLibre;

        @JsonProperty("obligationDatabaseIds")
        private List<String> obligationDatabaseIds = new ArrayList<>();

        @Column(name = "obligation_list_id")
        private String obligationListId;

        @Column(name = "text")
        private String text;

        @Column(name = "checked")
        private Boolean checked;

        @JsonProperty("permissions")
        private Map<String, Boolean> permissions = new HashMap<>();

        @Override
        public UUID getId() {
                return id;
        }

        @Override
        public void setId(UUID id) {
                this.id = id;
        }

        @Override
        public String getRevision() {
                return revision;
        }

        @Override
        public void setRevision(String revision) {
                this.revision = revision;
        }

        public String getType() {
                return type;
        }

        public void setType(String type) {
                this.type = type;
        }

        @Override
        public String getShortname() {
                return shortname;
        }

        @Override
        public void setShortname(String shortname) {
                this.shortname = shortname;
        }

        @Override
        public String getFullname() {
                return fullname;
        }

        @Override
        public void setFullname(String fullname) {
                this.fullname = fullname;
        }

        @Override
        public String getLicenseType() {
                return licenseType;
        }

        public void setLicenseType(String licenseType) {
                this.licenseType = licenseType;
        }

        @Override
        public String getLicenseTypeDatabaseId() {
                return licenseTypeDatabaseId;
        }

        @Override
        public void setLicenseTypeDatabaseId(String licenseTypeDatabaseId) {
                this.licenseTypeDatabaseId = licenseTypeDatabaseId;
        }

        @Override
        public String getExternalLicenseLink() {
                return externalLicenseLink;
        }

        @Override
        public void setExternalLicenseLink(String externalLicenseLink) {
                this.externalLicenseLink = externalLicenseLink;
        }

        @Override
        public String getNote() {
                return note;
        }

        @Override
        public void setNote(String note) {
                this.note = note;
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
        public String getReviewdate() {
                return reviewdate;
        }

        @Override
        public void setReviewdate(String reviewdate) {
                this.reviewdate = reviewdate;
        }

        @Override
        public Integer getOsIApproved() {
                return osIApproved;
        }

        @Override
        public void setOsIApproved(Integer osIApproved) {
                this.osIApproved = osIApproved;
        }

        @Override
        public Integer getFsFLibre() {
                return fsFLibre;
        }

        @Override
        public void setFsFLibre(Integer fsFLibre) {
                this.fsFLibre = fsFLibre;
        }

        @Override
        public List<String> getObligationDatabaseIds() {
                return obligationDatabaseIds;
        }

        @Override
        public void setObligationDatabaseIds(List<String> obligationDatabaseIds) {
                this.obligationDatabaseIds = obligationDatabaseIds;
        }

        @Override
        public String getObligationListId() {
                return obligationListId;
        }

        @Override
        public void setObligationListId(String obligationListId) {
                this.obligationListId = obligationListId;
        }

        @Override
        public String getText() {
                return text;
        }

        @Override
        public void setText(String text) {
                this.text = text;
        }

        @Override
        public Boolean getChecked() {
                return checked;
        }

        @Override
        public void setChecked(Boolean checked) {
                this.checked = checked;
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
        public boolean equals(Object o) {
                if (this == o)
                        return true;
                if (o == null || getClass() != o.getClass())
                        return false;
                LicensePG licensePG = (LicensePG) o;
                return Objects.equals(id, licensePG.id)
                                && Objects.equals(revision, licensePG.revision)
                                && Objects.equals(shortname, licensePG.shortname)
                                && Objects.equals(fullname, licensePG.fullname)
                                && Objects.equals(licenseTypeDatabaseId,
                                                licensePG.licenseTypeDatabaseId)
                                && Objects.equals(externalLicenseLink,
                                                licensePG.externalLicenseLink)
                                && Objects.equals(note, licensePG.note)
                                && Objects.equals(externalIds, licensePG.externalIds)
                                && Objects.equals(additionalData, licensePG.additionalData)
                                && Objects.equals(reviewdate, licensePG.reviewdate)
                                && Objects.equals(osIApproved, licensePG.osIApproved)
                                && Objects.equals(fsFLibre, licensePG.fsFLibre)
                                && Objects.equals(obligationDatabaseIds,
                                                licensePG.obligationDatabaseIds)
                                && Objects.equals(obligationListId, licensePG.obligationListId)
                                && Objects.equals(text, licensePG.text)
                                && Objects.equals(checked, licensePG.checked)
                                && Objects.equals(permissions, licensePG.permissions);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id, revision, shortname, fullname, licenseTypeDatabaseId,
                                externalLicenseLink, note, externalIds, additionalData, reviewdate,
                                osIApproved, fsFLibre, obligationDatabaseIds, obligationListId,
                                text, checked, permissions);
        }

        @Override
        public String toString() {
                return "LicensePG{" + "id=" + id + ", revision='" + revision + '\'' + ", type='"
                                + type + '\'' + ", shortname='" + shortname + '\'' + ", fullname='"
                                + fullname + '\'' + ", licenseType='" + licenseType + '\''
                                + ", licenseTypeDatabaseId='" + licenseTypeDatabaseId + '\''
                                + ", externalLicenseLink='" + externalLicenseLink + '\''
                                + ", note='" + note + '\'' + ", externalIds=" + externalIds
                                + ", additionalData=" + additionalData + ", reviewdate='"
                                + reviewdate + '\'' + ", osIApproved=" + osIApproved + ", fsFLibre="
                                + fsFLibre;
        }

        public LicensePG() {}

        public LicensePG(LicenseAPI license) {
                this.id = license.getId();
                this.revision = license.getRevision();
                this.shortname = license.getShortname();
                this.fullname = license.getFullname();
                this.licenseTypeDatabaseId = license.getLicenseTypeDatabaseId();
                this.externalLicenseLink = license.getExternalLicenseLink();
                this.note = license.getNote();
                this.externalIds = new HashMap<>(license.getExternalIds());
                this.additionalData = new HashMap<>(license.getAdditionalData());
                this.reviewdate = license.getReviewdate();
                this.osIApproved = license.getOsIApproved();
                this.fsFLibre = license.getFsFLibre();
                this.obligationDatabaseIds = new ArrayList<>(license.getObligationDatabaseIds());
                this.obligationListId = license.getObligationListId();
                this.text = license.getText();
                this.checked = license.getChecked();
                this.permissions = new HashMap<>(license.getPermissions());
        }
}
