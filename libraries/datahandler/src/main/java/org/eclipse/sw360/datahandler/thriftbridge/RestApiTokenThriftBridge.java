/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thriftbridge;

import org.eclipse.sw360.datahandler.services.users.RestApiToken;

public final class RestApiTokenThriftBridge {

    private RestApiTokenThriftBridge() {}

    public static RestApiToken toPojo(org.eclipse.sw360.datahandler.thrift.users.RestApiToken thrift) {
        if (thrift == null) {
            return null;
        }
        RestApiToken pojo = new RestApiToken();
        if (thrift.isSetToken()) {
            pojo.setToken(thrift.getToken());
        }
        if (thrift.isSetName()) {
            pojo.setName(thrift.getName());
        }
        if (thrift.isSetCreatedOn()) {
            pojo.setCreatedOn(thrift.getCreatedOn());
        }
        if (thrift.isSetNumberOfDaysValid()) {
            pojo.setNumberOfDaysValid(thrift.getNumberOfDaysValid());
        }
        if (thrift.isSetAuthorities()) {
            pojo.setAuthorities(thrift.getAuthorities());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.users.RestApiToken toThrift(RestApiToken pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.RestApiToken thrift =
                new org.eclipse.sw360.datahandler.thrift.users.RestApiToken();
        if (pojo.getToken() != null) {
            thrift.setToken(pojo.getToken());
        }
        if (pojo.getName() != null) {
            thrift.setName(pojo.getName());
        }
        if (pojo.getCreatedOn() != null) {
            thrift.setCreatedOn(pojo.getCreatedOn());
        }
        if (pojo.getNumberOfDaysValid() != null) {
            thrift.setNumberOfDaysValid(pojo.getNumberOfDaysValid());
        }
        if (pojo.getAuthorities() != null) {
            thrift.setAuthorities(pojo.getAuthorities());
        }
        return thrift;
    }
}
