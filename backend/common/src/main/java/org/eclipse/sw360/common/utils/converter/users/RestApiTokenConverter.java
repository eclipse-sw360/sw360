/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.users;

import org.eclipse.sw360.datahandler.services.users.RestApiToken;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class RestApiTokenConverter {

    private RestApiTokenConverter() {}

    public static RestApiToken fromThrift(org.eclipse.sw360.datahandler.thrift.users.RestApiToken thrift) {
        if (thrift == null) {
            return null;
        }
        RestApiToken pojo = new RestApiToken();
        if (thrift.isSetAuthorities()) {
            pojo.setAuthorities(ThriftCollectionConverter.mapSet(thrift.getAuthorities(), e -> e));
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetNumberOfDaysValid()) {
            pojo.setNumberOfDaysValid(thrift.getNumberOfDaysValid());
        }
        if (thrift.isSetToken()) {
            pojo.setToken(thrift.getToken());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.users.RestApiToken toThrift(RestApiToken pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.RestApiToken thrift = new org.eclipse.sw360.datahandler.thrift.users.RestApiToken();
        if (pojo.getAuthorities() != null) {
            thrift.setAuthorities(ThriftCollectionConverter.mapSet(pojo.getAuthorities(), e -> e));
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getNumberOfDaysValid() != null) {
            thrift.setNumberOfDaysValid(pojo.getNumberOfDaysValid());
        }
        if (pojo.getToken() != null) {
            thrift.setToken(pojo.getToken());
        }
        return thrift;
    }
}
