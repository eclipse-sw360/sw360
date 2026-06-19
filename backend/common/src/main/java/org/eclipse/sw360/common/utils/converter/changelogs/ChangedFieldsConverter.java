/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.changelogs;

import org.eclipse.sw360.datahandler.services.changelogs.ChangedFields;

public final class ChangedFieldsConverter {

    private ChangedFieldsConverter() {}

    public static ChangedFields fromThrift(org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields thrift) {
        if (thrift == null) {
            return null;
        }
        ChangedFields pojo = new ChangedFields();
        if (thrift.isSetFieldName()) {
            pojo.setFieldName(thrift.getFieldName());
        }
        if (thrift.isSetFieldValueNew()) {
            pojo.setFieldValueNew(thrift.getFieldValueNew());
        }
        if (thrift.isSetFieldValueOld()) {
            pojo.setFieldValueOld(thrift.getFieldValueOld());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields toThrift(ChangedFields pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields thrift = new org.eclipse.sw360.datahandler.thrift.changelogs.ChangedFields();
        if (pojo.getFieldName() != null) {
            thrift.setFieldName(pojo.getFieldName());
        }
        if (pojo.getFieldValueNew() != null) {
            thrift.setFieldValueNew(pojo.getFieldValueNew());
        }
        if (pojo.getFieldValueOld() != null) {
            thrift.setFieldValueOld(pojo.getFieldValueOld());
        }
        return thrift;
    }
}
