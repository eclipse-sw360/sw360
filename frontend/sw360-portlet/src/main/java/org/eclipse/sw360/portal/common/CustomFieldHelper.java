/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2017.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common;

import com.liferay.expando.kernel.model.*;
import com.liferay.expando.kernel.service.ExpandoColumnLocalServiceUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.model.ResourceConstants;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.RoleConstants;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.kernel.service.RoleLocalServiceUtil;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.users.UserUtils;

import org.apache.log4j.Logger;

import javax.portlet.PortletRequest;

import java.io.Serializable;
import java.util.Optional;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;
import static org.eclipse.sw360.portal.common.PortalConstants.CUSTOM_FIELD_COMPONENTS_VIEW_SIZE;
import static org.eclipse.sw360.portal.common.PortalConstants.CUSTOM_FIELD_PROJECT_GROUP_FILTER;
import static org.eclipse.sw360.portal.common.PortalConstants.CUSTOM_FIELD_VULNERABILITIES_VIEW_SIZE;

public class CustomFieldHelper {

    private static final Logger log = Logger.getLogger(CustomFieldHelper.class);

    private static final int DEFAULT_VIEW_SIZE = 200;

    public static int loadAndStoreStickyViewSize(PortletRequest request, User user, String fieldName) {
        String view_size = request.getParameter(PortalConstants.VIEW_SIZE);
        int limit;
        if (isNullEmptyOrWhitespace(view_size)) {
            limit = CustomFieldHelper
                    .loadField(Integer.class, request, user, fieldName)
                    .orElse(DEFAULT_VIEW_SIZE);
        } else {
            limit = Integer.parseInt(view_size);
            CustomFieldHelper.saveField(request, user, fieldName, limit);
        }
        if (limit == 0) {
            limit = DEFAULT_VIEW_SIZE;
        }
        request.setAttribute(PortalConstants.VIEW_SIZE, limit);
        return limit;
    }

    public static <T extends Serializable> void saveField(PortletRequest request, User user, String field, T value) {
        try {
            ExpandoBridge exp = getUserExpandoBridge(request, user);
            exp.setAttribute(field, value);
        } catch (PortalException | SystemException e) {
            log.error("Could not save custom field " + field, e);
        }
    }

    public static <T extends Serializable> Optional<T> loadField(Class<T> type, PortletRequest request, User user, String field) {
        try {
            ExpandoBridge exp = getUserExpandoBridge(request, user);
            T viewSize = type.cast(exp.getAttribute(field));
            return Optional.ofNullable(viewSize);
        } catch (PortalException | SystemException e) {
            log.error("Could not load custom field " + field, e);
            return Optional.empty();
        }
    }

    private static ExpandoBridge getUserExpandoBridge(PortletRequest request, User user) throws PortalException, SystemException {
        com.liferay.portal.kernel.model.User liferayUser = UserUtils.findLiferayUser(request, user);
        ensureUserCustomFieldExists(liferayUser, CUSTOM_FIELD_PROJECT_GROUP_FILTER, ExpandoColumnConstants.STRING);
        ensureUserCustomFieldExists(liferayUser, CUSTOM_FIELD_COMPONENTS_VIEW_SIZE, ExpandoColumnConstants.INTEGER);
        ensureUserCustomFieldExists(liferayUser, CUSTOM_FIELD_VULNERABILITIES_VIEW_SIZE, ExpandoColumnConstants.INTEGER);
        return liferayUser.getExpandoBridge();
    }

    private static void ensureUserCustomFieldExists(com.liferay.portal.kernel.model.User liferayUser, String customFieldName, int customFieldType) throws PortalException, SystemException {
        ExpandoBridge exp = liferayUser.getExpandoBridge();
        if (!exp.hasAttribute(customFieldName)) {
            exp.addAttribute(customFieldName, customFieldType, false);
            long companyId = liferayUser.getCompanyId();

            ExpandoColumn column = ExpandoColumnLocalServiceUtil.getColumn(companyId, exp.getClassName(), ExpandoTableConstants.DEFAULT_TABLE_NAME, customFieldName);

            String[] roleNames = new String[]{RoleConstants.USER, RoleConstants.POWER_USER};
            for (String roleName : roleNames) {
                Role role = RoleLocalServiceUtil.getRole(companyId, roleName);
                if (role != null && column != null) {
                    ResourcePermissionLocalServiceUtil.setResourcePermissions(companyId,
                            ExpandoColumn.class.getName(),
                            ResourceConstants.SCOPE_INDIVIDUAL,
                            String.valueOf(column.getColumnId()),
                            role.getRoleId(),
                            new String[]{ActionKeys.VIEW, ActionKeys.UPDATE});
                }
            }
        }
    }
}
