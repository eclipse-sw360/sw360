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

import org.eclipse.sw360.datahandler.services.attachments.ManuallySetUsage;

public final class ManuallySetUsageConverter {

    private ManuallySetUsageConverter() {}

    public static ManuallySetUsage fromThrift(org.eclipse.sw360.datahandler.thrift.attachments.ManuallySetUsage thrift) {
        if (thrift == null) {
            return null;
        }
        ManuallySetUsage pojo = new ManuallySetUsage();

        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.attachments.ManuallySetUsage toThrift(ManuallySetUsage pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.attachments.ManuallySetUsage thrift = new org.eclipse.sw360.datahandler.thrift.attachments.ManuallySetUsage();

        return thrift;
    }
}
