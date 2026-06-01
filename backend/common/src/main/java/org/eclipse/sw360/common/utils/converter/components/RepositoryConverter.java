/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.components;

import org.eclipse.sw360.datahandler.services.components.Repository;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class RepositoryConverter {

    private RepositoryConverter() {}

    public static Repository fromThrift(org.eclipse.sw360.datahandler.thrift.components.Repository thrift) {
        if (thrift == null) {
            return null;
        }
        Repository pojo = new Repository();
        if (thrift.isSetRepositorytype()) {
            pojo.setRepositorytype(EnumConverter.fromThrift(thrift.getRepositorytype(), org.eclipse.sw360.datahandler.services.components.RepositoryType.class));
        }
        if (thrift.isSetUrl()) {
            pojo.setUrl(thrift.getUrl());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.Repository toThrift(Repository pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.Repository thrift = new org.eclipse.sw360.datahandler.thrift.components.Repository();
        if (pojo.getRepositorytype() != null) {
            thrift.setRepositorytype(EnumConverter.toThrift(pojo.getRepositorytype(), org.eclipse.sw360.datahandler.thrift.components.RepositoryType.class));
        }
        if (pojo.getUrl() != null) {
            thrift.setUrl(pojo.getUrl());
        }
        return thrift;
    }
}
