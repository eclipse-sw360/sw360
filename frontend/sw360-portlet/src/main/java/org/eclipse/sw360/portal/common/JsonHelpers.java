/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.common;

import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.io.IOException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.users.UserCacheHolder.getUserFromEmail;

/**
 * @author daniele.fognini@tngtech.com
 */
public class JsonHelpers {
    public static JSONObject getProjectResponsible(ThriftJsonSerializer thriftJsonSerializer, Project project) throws JSONException, IOException {
        JSONObject responsible = JSONFactoryUtil.createJSONObject();
        if (project.isSetProjectResponsible()) {
            String projectResponsible = project.getProjectResponsible();
            if (!isNullOrEmpty(projectResponsible)) {
                User userFromEmail=null;
                try {
                    userFromEmail = getUserFromEmail(projectResponsible);
                } catch(Exception ignored) {
                }
                if(userFromEmail!=null) {
                    responsible = toJson(userFromEmail, thriftJsonSerializer);
                }
            }
        }
        return responsible;
    }

    public static JSONObject toJson(Object thriftObject, ThriftJsonSerializer thriftJsonSerializer) throws JSONException, IOException {
        return JSONFactoryUtil.createJSONObject(thriftJsonSerializer.toJson(thriftObject));
    }
}
