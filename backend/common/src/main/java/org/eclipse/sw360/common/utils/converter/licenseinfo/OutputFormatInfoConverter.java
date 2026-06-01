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

import org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatInfo;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class OutputFormatInfoConverter {

    private OutputFormatInfoConverter() {}

    public static OutputFormatInfo fromThrift(org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo thrift) {
        if (thrift == null) {
            return null;
        }
        OutputFormatInfo pojo = new OutputFormatInfo();
        if (thrift.isSetDescription()) {
            pojo.setDescription(thrift.getDescription());
        }
        if (thrift.isSetFileExtension()) {
            pojo.setFileExtension(thrift.getFileExtension());
        }
        if (thrift.isSetGeneratorClassName()) {
            pojo.setGeneratorClassName(thrift.getGeneratorClassName());
        }
        if (thrift.isSetIsOutputBinary()) {
            pojo.setIsOutputBinary(thrift.isIsOutputBinary());
        }
        if (thrift.isSetMimeType()) {
            pojo.setMimeType(thrift.getMimeType());
        }
        if (thrift.isSetVariant()) {
            pojo.setVariant(EnumConverter.fromThrift(thrift.getVariant(), org.eclipse.sw360.datahandler.services.licenseinfo.OutputFormatVariant.class));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo toThrift(OutputFormatInfo pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo thrift = new org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo();
        if (pojo.getDescription() != null) {
            thrift.setDescription(pojo.getDescription());
        }
        if (pojo.getFileExtension() != null) {
            thrift.setFileExtension(pojo.getFileExtension());
        }
        if (pojo.getGeneratorClassName() != null) {
            thrift.setGeneratorClassName(pojo.getGeneratorClassName());
        }
        if (pojo.getIsOutputBinary() != null) {
            thrift.setIsOutputBinary(pojo.getIsOutputBinary());
        }
        if (pojo.getMimeType() != null) {
            thrift.setMimeType(pojo.getMimeType());
        }
        if (pojo.getVariant() != null) {
            thrift.setVariant(EnumConverter.toThrift(pojo.getVariant(), org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatVariant.class));
        }
        return thrift;
    }
}
