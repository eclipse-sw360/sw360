/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.actuator;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Endpoint(id = "config")
public class SW360ConfigActuator {

    private final Map<String, String> properties = new ConcurrentHashMap<>();

    public SW360ConfigActuator() {
        properties.put("admin.private.project.access.enabled", String.valueOf(SW360Constants.IS_ADMIN_PRIVATE_ACCESS_ENABLED));
        properties.put("clearing.teams", SW360Constants.CLEARING_TEAMS);
        properties.put("clearing.team.unknown.enabled", String.valueOf(SW360Constants.CLEARING_TEAM_UNKNOWN_ENABLED));
        properties.put("component.categories", StringUtils.join(SW360Constants.COMPONENT_CATEGORIES, ","));
        properties.put("component.externalkeys", StringUtils.join(SW360Constants.COMPONENT_EXTERNAL_ID_KEYS, ","));
        properties.put("component.visibility.restriction.enabled", String.valueOf(SW360Constants.IS_COMPONENT_VISIBILITY_RESTRICTION_ENABLED));
        properties.put("components.activate", StringUtils.join(SW360Constants.COMPONENTS_ACTIVATE, ","));
        properties.put("custommap.component.roles", StringUtils.join(SW360Constants.COMPONENT_ROLES, ","));
        properties.put("custommap.project.roles", StringUtils.join(SW360Constants.PROJECT_ROLES, ","));
        properties.put("custommap.release.externalIds", StringUtils.join(SW360Constants.RELEASE_EXTERNAL_IDS, ","));
        properties.put("custommap.release.roles", StringUtils.join(SW360Constants.RELEASE_ROLES, ","));
        properties.put("custom.welcome.page.guideline", String.valueOf(SW360Constants.CUSTOM_WELCOME_PAGE_GUIDELINE));
        properties.put("disable.clearing.fossology.report.download", String.valueOf(SW360Constants.DISABLE_CLEARING_FOSSOLOGY_REPORT_DOWNLOAD));
        properties.put("domain", StringUtils.join(SW360Constants.DOMAIN, ", "));
        properties.put("enable.add.license.info.to.release.button", String.valueOf(SW360Constants.ENABLE_ADD_LIC_INFO_TO_RELEASE));
        properties.put("enable.security.vulnerability.monitoring", String.valueOf(SW360Constants.IS_SVM_ENABLED));
        properties.put("license.identifiers", StringUtils.join(SW360Constants.LICENSE_IDENTIFIERS, ","));
        properties.put("mainline.state.enabled.for.user", String.valueOf(SW360Constants.MAINLINE_STATE_ENABLED_FOR_USER));
        properties.put("operating.systems", StringUtils.join(SW360Constants.OPERATING_SYSTEMS, ","));
        properties.put("org.eclipse.sw360.disable.clearing.request.for.project.group", String.valueOf(SW360Constants.DISABLE_CLEARING_REQUEST_FOR_PROJECT_WITH_GROUPS));
        properties.put("org.eclipse.sw360.licensinfo.header.by.group", SW360Constants.LICENSE_INFO_HEADER_TEXT_FILE_NAME_BY_PROJECT_GROUP);
        properties.put("org.eclipse.sw360.licensinfo.projectclearing.templateformat", SW360Constants.CLEARING_REPORT_TEMPLATE_FORMAT);
        properties.put("org.eclipse.sw360.licensinfo.projectclearing.templatemapping", SW360Constants.REST_REPORT_FILENAME_MAPPING);
        properties.put("programming.languages", StringUtils.join(SW360Constants.PROGRAMMING_LANGUAGES, ","));
        properties.put("project.externalkeys", StringUtils.join(SW360Constants.PROJECT_EXTERNAL_ID_KEYS, ","));
        properties.put("project.externalurls", StringUtils.join(SW360Constants.PROJECT_EXTERNAL_URL_KEYS, ","));
        properties.put("projectimport.hosts", SW360Constants.PROJECTIMPORT_HOSTS);
        properties.put("project.obligation.actions", StringUtils.join(SW360Constants.PROJECT_OBLIGATIONS_ACTION_SET, ","));
        properties.put("project.obligations.enabled", String.valueOf(SW360Constants.IS_PROJECT_OBLIGATIONS_ENABLED));
        properties.put("project.tag", StringUtils.join(SW360Constants.PREDEFINED_TAGS, ","));
        properties.put("project.type", StringUtils.join(SW360Constants.PROJECT_TYPE, ","));
        properties.put("relationship.type", StringUtils.join(SW360Constants.SET_RELATIONSHIP_TYPE, ","));
        properties.put("release.externalkeys", StringUtils.join(SW360Constants.RELEASE_EXTERNAL_ID_KEYS, ","));
        properties.put("rest.apitoken.generator.enable", String.valueOf(SW360Constants.REST_API_TOKEN_GENERATOR_ENABLE));
        properties.put("rest.apitoken.read.validity.days", SW360Constants.REST_API_TOKEN_MAX_VALIDITY_READ_IN_DAYS);
        properties.put("rest.apitoken.write.validity.days", SW360Constants.REST_API_TOKEN_MAX_VALIDITY_WRITE_IN_DAYS);
        properties.put("rest.api.write.access.token.in.preferences.enabled", String.valueOf(SW360Constants.REST_API_WRITE_ACCESS_TOKEN_ENABLE));
        properties.put("rest.force.update.enabled", String.valueOf(SW360Constants.IS_FORCE_UPDATE_ENABLED));
        properties.put("rest.write.access.usergroup", SW360Constants.CONFIG_WRITE_ACCESS_USERGROUP.name());
        properties.put("send.component.spreadsheet.export.to.mail.enabled", String.valueOf(SW360Constants.MAIL_REQUEST_FOR_COMPONENT_REPORT));
        properties.put("send.project.spreadsheet.export.to.mail.enabled", String.valueOf(SW360Constants.MAIL_REQUEST_FOR_PROJECT_REPORT));
        properties.put("software.platforms", StringUtils.join(SW360Constants.SOFTWARE_PLATFORMS, ","));
        properties.put("state", StringUtils.join(SW360Constants.STATE, ","));
        properties.put("user.role.allowed.to.merge.or.split.component", SW360Constants.USER_ROLE_ALLOWED_TO_MERGE_OR_SPLIT_COMPONENT.name());
    }

    @ReadOperation
    public Map<String, String> config() {
        return properties;
    }

    @ReadOperation(produces = MediaType.TEXT_PLAIN_VALUE)
    public String config(@Selector String name) {
        return properties.get(name);
    }
}
