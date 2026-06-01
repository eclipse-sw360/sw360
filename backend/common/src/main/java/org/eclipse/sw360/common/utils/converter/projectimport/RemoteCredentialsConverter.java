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

import org.eclipse.sw360.datahandler.services.projectimport.RemoteCredentials;

public final class RemoteCredentialsConverter {

    private RemoteCredentialsConverter() {}

    public static RemoteCredentials fromThrift(org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials thrift) {
        if (thrift == null) {
            return null;
        }
        RemoteCredentials pojo = new RemoteCredentials();
        if (thrift.isSetPassword()) {
            pojo.setPassword(thrift.getPassword());
        }
        if (thrift.isSetServerUrl()) {
            pojo.setServerUrl(thrift.getServerUrl());
        }
        if (thrift.isSetUsername()) {
            pojo.setUsername(thrift.getUsername());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials toThrift(RemoteCredentials pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials thrift = new org.eclipse.sw360.datahandler.thrift.projectimport.RemoteCredentials();
        if (pojo.getPassword() != null) {
            thrift.setPassword(pojo.getPassword());
        }
        if (pojo.getServerUrl() != null) {
            thrift.setServerUrl(pojo.getServerUrl());
        }
        if (pojo.getUsername() != null) {
            thrift.setUsername(pojo.getUsername());
        }
        return thrift;
    }
}
