/*
 * Copyright (c) Verifa Oy, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.projectimport;

import com.liferay.portal.kernel.json.JSONObject;
import junit.framework.TestCase;
import org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.portlet.PortletSession;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class WsImportPortletTest extends TestCase {
    private final String token = "token";
    private final String userKey = "userKey";
    private final String newURL = "newURL";

    @Mock
    private PortletSession session;

    @Mock
    private JSONObject responseData;

    @Test
    public void testUpdateInputSourceWithoutUrl() throws Exception {

        TokenCredentials tokenCredentials = new TokenCredentials();

        new WsImportPortlet().setNewImportSource(
                tokenCredentials,
                session,
                responseData);
        verify(responseData).put(ProjectImportConstants.RESPONSE__STATUS, ProjectImportConstants.RESPONSE__DB_URL_NOT_SET);
    }

    @Test
    public void testUpdateInputSourceWithUrlUpdatesResponse() throws Exception {

        TokenCredentials tokenCredentials = new TokenCredentials();
        tokenCredentials.setToken(token);
        tokenCredentials.setUserKey(userKey);
        tokenCredentials.setServerUrl(newURL);

        new WsImportPortlet().setNewImportSource(
                tokenCredentials,
                session,
                responseData);

        verify(responseData).put(ProjectImportConstants.RESPONSE__STATUS, ProjectImportConstants.RESPONSE__DB_CHANGED);
        verify(responseData).put(ProjectImportConstants.RESPONSE__DB_URL, newURL);
    }

    @Test
    public void testUpdateInputSourceWithUrlUpdatesSession() throws Exception {

        TokenCredentials tokenCredentials = new TokenCredentials();
        tokenCredentials.setToken(token);
        tokenCredentials.setUserKey(userKey);
        tokenCredentials.setServerUrl(newURL);

        new WsImportPortlet().setNewImportSource(
                tokenCredentials,
                session,
                responseData);

        verify(session).setAttribute(ProjectImportConstants.TOKEN, token);
        verify(session).setAttribute(ProjectImportConstants.USER_KEY, userKey);
        verify(session).setAttribute(ProjectImportConstants.SERVER_URL, newURL);
    }

}
