/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.components;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

import org.eclipse.sw360.datahandler.services.attachments.Attachment;
import org.eclipse.sw360.datahandler.services.common.CycloneDxComponentType;
import org.eclipse.sw360.datahandler.services.common.DocumentState;
import org.eclipse.sw360.datahandler.services.common.Visibility;
import org.eclipse.sw360.datahandler.services.users.RequestedAction;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Component {

    private String id;
    private String revision;
    private String type;
    @JsonProperty(required = true)
    private String name;
    private String description;
    private Set<Attachment> attachments;
    private String createdOn;
    private ComponentType componentType;
    private String createdBy;
    private Set<String> subscribers;
    private Set<String> moderators;
    private String componentOwner;
    private String ownerAccountingUnit;
    private String ownerGroup;
    private String ownerCountry;
    private Map<String, Set<String>> roles;
    private Visibility visbility;
    private String businessUnit;
    private CycloneDxComponentType cdxComponentType;
    private Map<String, String> externalIds;
    private Map<String, String> additionalData;
    private List<Release> releases;
    private Set<String> releaseIds;
    private Set<String> mainLicenseIds;
    private Vendor defaultVendor;
    private String defaultVendorId;
    private Set<String> categories;
    private Set<String> languages;
    private Set<String> softwarePlatforms;
    private Set<String> operatingSystems;
    private Set<String> vendorNames;
    private String homepage;
    private String mailinglist;
    private String wiki;
    private String blog;
    private String wikipedia;
    private String openHub;
    private String vcs;
    private DocumentState documentState;
    private Map<RequestedAction, Boolean> permissions;
    private String modifiedBy;
    private String modifiedOn;
}
