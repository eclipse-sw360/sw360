/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.licenseinfo;

import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatVariant;

public final class OutputFormatVariantConverter {

    private OutputFormatVariantConverter() {}

    public static OutputFormatVariant fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant thrift) {
        return EnumConverter.fromThrift(thrift, OutputFormatVariant.class);
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant toThrift(OutputFormatVariant pojo) {
        return EnumConverter.toThrift(pojo, org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant.class);
    }
}
