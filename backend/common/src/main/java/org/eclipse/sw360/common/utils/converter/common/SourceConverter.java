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

import org.eclipse.sw360.datahandler.services.common.Source;

public final class SourceConverter {

    private SourceConverter() {}

    public static Source fromThrift(org.eclipse.sw360.datahandler.thrift.Source thrift) {
        if (thrift == null) {
            return null;
        }
        Source pojo = new Source();

        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.Source toThrift(Source pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.Source thrift = new org.eclipse.sw360.datahandler.thrift.Source();

        return thrift;
    }
}
