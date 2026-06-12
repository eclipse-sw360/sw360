/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.common;

import org.eclipse.sw360.datahandler.services.common.RestrictedResource;

public final class RestrictedResourceConverter {

    private RestrictedResourceConverter() {}

    public static RestrictedResource fromThrift(org.eclipse.sw360.datahandler.thrift.RestrictedResource thrift) {
        if (thrift == null) {
            return null;
        }
        RestrictedResource pojo = new RestrictedResource();
        if (thrift.isSetProjects()) {
            pojo.setProjects(thrift.getProjects());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.RestrictedResource toThrift(RestrictedResource pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.RestrictedResource thrift = new org.eclipse.sw360.datahandler.thrift.RestrictedResource();
        if (pojo.getProjects() != null) {
            thrift.setProjects(pojo.getProjects());
        }
        return thrift;
    }
}
