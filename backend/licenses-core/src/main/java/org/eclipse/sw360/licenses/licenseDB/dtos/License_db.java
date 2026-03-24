/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
 * Copyright Mahmoud Elsheemy<mahmoudalshemy.3@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */


package org.eclipse.sw360.licenses.licenseDB.dtos;

import java.util.List;

public class License_db {
    private boolean OSIapproved;
    private boolean active;
    private String add_date;
    private boolean copyleft;
    private CreatedBy created_by;
    private ExternalRef external_ref;
    private String fullname;
    private String id;
    private String notes;
    private List<String> obligation_ids;
    private int risk;
    private String shortname;
    private String source;
    private String spdx_id;
    private String text;
    private boolean text_updatable;
    private String url;

    public License_db() {
        // Default constructor for deserialization
    }

    public boolean isOSIapproved() {
        return OSIapproved;
    }

    public void setOSIapproved(boolean OSIapproved) {
        this.OSIapproved = OSIapproved;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAdd_date() {
        return add_date;
    }

    public void setAdd_date(String add_date) {
        this.add_date = add_date;
    }

    public boolean isCopyleft() {
        return copyleft;
    }

    public void setCopyleft(boolean copyleft) {
        this.copyleft = copyleft;
    }

    public CreatedBy getCreated_by() {
        return created_by;
    }

    public void setCreated_by(CreatedBy created_by) {
        this.created_by = created_by;
    }

    public ExternalRef getExternal_ref() {
        return external_ref;
    }

    public void setExternal_ref(ExternalRef external_ref) {
        this.external_ref = external_ref;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<String> getObligation_ids() {
        return obligation_ids;
    }

    public void setObligation_ids(List<String> obligation_ids) {
        this.obligation_ids = obligation_ids;
    }

    public int getRisk() {
        return risk;
    }

    public void setRisk(int risk) {
        this.risk = risk;
    }

    public String getShortname() {
        return shortname;
    }

    public void setShortname(String shortname) {
        this.shortname = shortname;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSpdx_id() {
        return spdx_id;
    }

    public void setSpdx_id(String spdx_id) {
        this.spdx_id = spdx_id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isText_updatable() {
        return text_updatable;
    }

    public void setText_updatable(boolean text_updatable) {
        this.text_updatable = text_updatable;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public static class CreatedBy {
        private String display_name;
        private String id;
        private boolean subscribed;
        private String user_email;
        private String user_level;
        private String user_name;

        public CreatedBy() {
        }

        public String getDisplay_name() {
            return display_name;
        }

        public void setDisplay_name(String display_name) {
            this.display_name = display_name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isSubscribed() {
            return subscribed;
        }

        public void setSubscribed(boolean subscribed) {
            this.subscribed = subscribed;
        }

        public String getUser_email() {
            return user_email;
        }

        public void setUser_email(String user_email) {
            this.user_email = user_email;
        }

        public String getUser_level() {
            return user_level;
        }

        public void setUser_level(String user_level) {
            this.user_level = user_level;
        }

        public String getUser_name() {
            return user_name;
        }

        public void setUser_name(String user_name) {
            this.user_name = user_name;
        }
    }

    public static class ExternalRef {
        private String license_explanation;
        private String license_suffix;

        public ExternalRef() {
        }

        public String getLicense_explanation() {
            return license_explanation;
        }

        public void setLicense_explanation(String license_explanation) {
            this.license_explanation = license_explanation;
        }

        public String getLicense_suffix() {
            return license_suffix;
        }

        public void setLicense_suffix(String license_suffix) {
            this.license_suffix = license_suffix;
        }
    }
}
