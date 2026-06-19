/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.attachments;

import org.eclipse.sw360.datahandler.services.attachments.UsageData;

public final class UsageDataConverter {

    private UsageDataConverter() {}

    public static UsageData fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.UsageData thrift) {
        if (thrift == null) {
            return null;
        }
        UsageData pojo = new UsageData();

        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.UsageData toThrift(UsageData pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.UsageData thrift = new org.eclipse.sw360.datahandler.thrift.attachments.UsageData();

        return thrift;
    }
}
