/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.search;

import org.eclipse.sw360.datahandler.services.search.ResultDetail;

public final class ResultDetailConverter {

    private ResultDetailConverter() {}

    public static ResultDetail fromThrift(org.eclipse.sw360.datahandler.thrift.search.ResultDetail thrift) {
        if (thrift == null) {
            return null;
        }
        ResultDetail pojo = new ResultDetail();
        if (thrift.isSetKey()) {
            pojo.setKey(thrift.getKey());
        }
        if (thrift.isSetValue()) {
            pojo.setValue(thrift.getValue());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.search.ResultDetail toThrift(ResultDetail pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.search.ResultDetail thrift = new org.eclipse.sw360.datahandler.thrift.search.ResultDetail();
        if (pojo.getKey() != null) {
            thrift.setKey(pojo.getKey());
        }
        if (pojo.getValue() != null) {
            thrift.setValue(pojo.getValue());
        }
        return thrift;
    }
}
