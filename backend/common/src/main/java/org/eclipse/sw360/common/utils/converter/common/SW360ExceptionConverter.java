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

import org.eclipse.sw360.datahandler.services.common.SW360Exception;

public final class SW360ExceptionConverter {

    private SW360ExceptionConverter() {}

    public static SW360Exception fromThrift(org.eclipse.sw360.datahandler.thrift.SW360Exception thrift) {
        if (thrift == null) {
            return null;
        }
        return new SW360Exception(thrift.getWhy(), thrift.getErrorCode());
    }

    public static org.eclipse.sw360.datahandler.thrift.SW360Exception toThrift(SW360Exception pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.SW360Exception thrift =
                new org.eclipse.sw360.datahandler.thrift.SW360Exception();
        thrift.setWhy(pojo.getWhy());
        if (pojo.getErrorCode() != null) {
            thrift.setErrorCode(pojo.getErrorCode());
        }
        return thrift;
    }
}
