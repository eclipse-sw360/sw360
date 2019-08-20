/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * Copyright Siemens AG, 2016-2019.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.users;

import com.liferay.portal.kernel.bean.PortalBeanLocatorUtil;
import com.liferay.portal.kernel.exception.NoSuchOrganizationException;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.*;
import com.liferay.portal.kernel.security.membershippolicy.OrganizationMembershipPolicyUtil;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.OrganizationLocalServiceUtil;
import com.liferay.portal.kernel.service.UserLocalServiceUtil;

import org.eclipse.sw360.datahandler.common.CommonUtils;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * Helper class to map organization from identity provider to sw360 internal org names and create
 * organizations in Liferay if necessary
 *
 * Adapted from https://github.com/sw360/ldapOrganizationMappingExt/blob/master/ldapAdapterEXT-ext-impl/src/main/java/com/bosch/osmi/sw360/ldapAdapter/CustomPortalLDAPImporterImpl.java
 */
public class OrganizationHelper {
    private static final String MAPPING_KEYS_PREFIX = "mapping.";
    private static final String MAPPING_VALUES_SUFFIX = ".target";
    private static boolean matchPrefix = false;
    private static List<Map.Entry<String, String>> sortedOrganizationMappings;
    private static boolean customMappingEnabled = false;
    private static Logger log = Logger.getLogger(OrganizationHelper.class);
    private static final String MATCH_PREFIX_KEY = "match.prefix";
    private static final String ENABLE_CUSTOM_MAPPING_KEY = "enable.custom.mapping";
    private static final String PROPERTIES_FILE_PATH = "/orgmapping.properties";
    private static final String TYPE_REGULAR_ORGANIZATION = "organization";

    static {
        loadOrganizationHelperSettings();
    }

    public void reassignUserToOrganizationIfNecessary(User user, Organization organization) throws PortalException, SystemException {
        if (organization != null && userIsNotInOrganization(user, organization.getOrganizationId())) {
            removeUserFromOtherOrganizations(user);
            log.info("OrganizationHelper adds user " + user.getEmailAddress() + " to the organization " + organization.getName());
            UserLocalServiceUtil.addOrganizationUsers(organization.getOrganizationId(), Collections.singletonList(user));
        }
    }

    private boolean userIsNotInOrganization(User user, long organizationId) throws SystemException, PortalException {
        return LongStream.of(user.getOrganizationIds()).noneMatch(x -> x == organizationId);
    }

    private void removeUserFromOtherOrganizations(User user) throws SystemException, PortalException {
        long[] usersOrganizations = user.getOrganizationIds();
        for (long usersOrganization : usersOrganizations) {
            log.info("remove user " + user.getEmailAddress() + " from organization with id: " + usersOrganization);
            UserLocalServiceUtil.deleteOrganizationUser(usersOrganization, user);
        }
    }

    public Organization addOrGetOrganization(String organizationName, long companyId) throws PortalException, SystemException {
        Organization organization;
        try {
            organization = OrganizationLocalServiceUtil.getOrganization(companyId, organizationName);
            log.info(String.format("Organization %s already exists", organizationName));
        } catch (NoSuchOrganizationException e) {
            User defaultUser = UserLocalServiceUtil.loadGetDefaultUser(companyId);
            organization = addOrganization(organizationName, defaultUser);
        }
        return organization;
    }

    private Organization addOrganization(String organizationName, User user) throws SystemException {
        log.info("OrganizationHelper adds the organization " + organizationName);
        Organization organization = null;
        try {
            OrganizationLocalService organizationLocalService = (OrganizationLocalService) PortalBeanLocatorUtil.locate(OrganizationLocalService.class.getName());
            organization = organizationLocalService
                    .addOrganization(
                            user.getUserId(),
                            OrganizationConstants.DEFAULT_PARENT_ORGANIZATION_ID,
                            organizationName,
                            TYPE_REGULAR_ORGANIZATION,
                            RegionConstants.DEFAULT_REGION_ID,
                            CountryConstants.DEFAULT_COUNTRY_ID,
                            ListTypeConstants.ORGANIZATION_STATUS_DEFAULT,
                            "Automatically created during LDAP import",
                            false,
                            null
                    );

            OrganizationMembershipPolicyUtil.verifyPolicy(organization);

            log.info("added organization with name: " + organization.getName() + " and id:" + organization.getOrganizationId());
        } catch (PortalException e) {
            log.error("A creator or parent organization with the primary key could not be found or the organization's information was invalid", e);
        }
        return organization;
    }

    public String mapOrganizationName(String name) {
        if (!customMappingEnabled) {
            return name;
        }

        final Predicate<Map.Entry<String, String>> matcher;
        if (matchPrefix) {
            matcher = e -> name.startsWith(e.getKey());
        } else { // match complete name
            matcher = e -> name.equals(e.getKey());
        }
        return sortedOrganizationMappings.stream()
                .filter(matcher)
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(name);
    }

    private static void loadOrganizationHelperSettings() {
        log.info("begin initialization");
        Properties sw360Properties = CommonUtils.loadProperties(OrganizationHelper.class, PROPERTIES_FILE_PATH);
        matchPrefix = Boolean.parseBoolean(sw360Properties.getProperty(MATCH_PREFIX_KEY, "false"));
        customMappingEnabled = Boolean.parseBoolean(sw360Properties.getProperty(ENABLE_CUSTOM_MAPPING_KEY, "false"));

        List<Object> mappingSourceKeys = sw360Properties
                .keySet()
                .stream()
                .filter(p -> ((String) p).startsWith(MAPPING_KEYS_PREFIX) && !((String) p).endsWith(MAPPING_VALUES_SUFFIX))
                .collect(Collectors.toList());

        Map<String, String> tempOrgMappings = new HashMap<>();
        for (Object sourceKey : mappingSourceKeys) {
            String sourceOrg = sw360Properties.getProperty((String) sourceKey);
            String targetOrg = sw360Properties.getProperty(sourceKey + MAPPING_VALUES_SUFFIX);
            if (sourceOrg != null && targetOrg != null && sourceOrg.length() > 0 && targetOrg.length() > 0) {
                tempOrgMappings.put(sourceOrg, targetOrg);
            }
        }
        sortedOrganizationMappings = tempOrgMappings
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> o) -> o.getKey().length())
                        .reversed())
                .collect(Collectors.toList());
        log.info(String.format("initialized with %d mappings", sortedOrganizationMappings.size()));
    }
}
