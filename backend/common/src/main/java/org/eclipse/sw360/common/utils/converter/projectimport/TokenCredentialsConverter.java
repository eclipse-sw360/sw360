/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.projectimport;

import org.eclipse.sw360.datahandler.services.projectimport.TokenCredentials;

public final class TokenCredentialsConverter {

    private TokenCredentialsConverter() {}

    public static TokenCredentials fromThrift(org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials thrift) {
        if (thrift == null) {
            return null;
        }
        TokenCredentials pojo = new TokenCredentials();
        if (thrift.isSetServerUrl()) {
            pojo.setServerUrl(thrift.getServerUrl());
        }
        if (thrift.isSetToken()) {
            pojo.setToken(thrift.getToken());
        }
        if (thrift.isSetUserKey()) {
            pojo.setUserKey(thrift.getUserKey());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials toThrift(TokenCredentials pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials thrift = new org.eclipse.sw360.datahandler.thrift.projectimport.TokenCredentials();
        if (pojo.getServerUrl() != null) {
            thrift.setServerUrl(pojo.getServerUrl());
        }
        if (pojo.getToken() != null) {
            thrift.setToken(pojo.getToken());
        }
        if (pojo.getUserKey() != null) {
            thrift.setUserKey(pojo.getUserKey());
        }
        return thrift;
    }
}
