/*
 * Copyright (c) Bosch Software Innovations GmbH 2015.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.projectimport;

import com.liferay.portal.kernel.json.JSONObject;
import junit.framework.TestCase;
import org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.portlet.PortletSession;

import static org.mockito.Mockito.verify;


@RunWith(MockitoJUnitRunner.class)
public class BdpImportPortletTest extends TestCase {
    private final String name = "name";
    private final String password = "password";
    private final String newURL = "newURL";

    @Mock
    private PortletSession session;

    @Mock
    private JSONObject responseData;

    @Test
    public void testUpdateInputSourceWithoutUrl() throws Exception {

        BdpImportPortlet.LoginState loginState = new BdpImportPortlet.LoginState();
        RemoteCredentials remoteCredentials = new RemoteCredentials();

        new BdpImportPortlet().setNewImportSource(
                remoteCredentials,
                session,
                responseData,
                loginState);
        verify(responseData).put(ProjectImportConstants.RESPONSE__STATUS, ProjectImportConstants.RESPONSE__DB_URL_NOT_SET);
    }

    @Test
    public void testUpdateInputSourceWithUrlUpdatesResponse() throws Exception {

        BdpImportPortlet.LoginState loginState = new BdpImportPortlet.LoginState();
        RemoteCredentials remoteCredentials = new RemoteCredentials();
        remoteCredentials.setUsername(name);
        remoteCredentials.setPassword(password);
        remoteCredentials.setServerUrl(newURL);

        new BdpImportPortlet().setNewImportSource(
                remoteCredentials,
                session,
                responseData,
                loginState);

        verify(responseData).put(ProjectImportConstants.RESPONSE__STATUS, ProjectImportConstants.RESPONSE__DB_CHANGED);
        verify(responseData).put(ProjectImportConstants.RESPONSE__DB_URL, newURL);
    }

    @Test
    public void testUpdateInputSourceWithUrlUpdatesSession() throws Exception {

        BdpImportPortlet.LoginState loginState = new BdpImportPortlet.LoginState();
        RemoteCredentials remoteCredentials = new RemoteCredentials();
        remoteCredentials.setUsername(name);
        remoteCredentials.setPassword(password);
        remoteCredentials.setServerUrl(newURL);

        new BdpImportPortlet().setNewImportSource(
                remoteCredentials,
                session,
                responseData,
                loginState);

        verify(session).setAttribute(ProjectImportConstants.USERNAME, name);
        verify(session).setAttribute(ProjectImportConstants.PASSWORD, password);
        verify(session).setAttribute(ProjectImportConstants.SERVER_URL, newURL);
    }

}
