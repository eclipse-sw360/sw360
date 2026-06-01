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

import org.eclipse.sw360.datahandler.services.users.DepartmentConfigDTO;

public final class DepartmentConfigDTOConverter {

    private DepartmentConfigDTOConverter() {}

    public static DepartmentConfigDTO fromThrift(org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO thrift) {
        if (thrift == null) {
            return null;
        }
        DepartmentConfigDTO pojo = new DepartmentConfigDTO();
        if (thrift.isSetLastRunningTime()) {
            pojo.setLastRunningTime(thrift.getLastRunningTime());
        }
        if (thrift.isSetPathFolder()) {
            pojo.setPathFolder(thrift.getPathFolder());
        }
        if (thrift.isSetPathFolderLog()) {
            pojo.setPathFolderLog(thrift.getPathFolderLog());
        }
        if (thrift.isSetShowFileLogFrom()) {
            pojo.setShowFileLogFrom(thrift.getShowFileLogFrom());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO toThrift(DepartmentConfigDTO pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO thrift = new org.eclipse.sw360.datahandler.thrift.users.DepartmentConfigDTO();
        if (pojo.getLastRunningTime() != null) {
            thrift.setLastRunningTime(pojo.getLastRunningTime());
        }
        if (pojo.getPathFolder() != null) {
            thrift.setPathFolder(pojo.getPathFolder());
        }
        if (pojo.getPathFolderLog() != null) {
            thrift.setPathFolderLog(pojo.getPathFolderLog());
        }
        if (pojo.getShowFileLogFrom() != null) {
            thrift.setShowFileLogFrom(pojo.getShowFileLogFrom());
        }
        return thrift;
    }
}
