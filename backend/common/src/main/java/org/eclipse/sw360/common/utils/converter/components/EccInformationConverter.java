/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.components;

import org.eclipse.sw360.datahandler.services.components.EccInformation;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;

public final class EccInformationConverter {

    private EccInformationConverter() {}

    public static EccInformation fromThrift(org.eclipse.sw360.datahandler.thrift.components.EccInformation thrift) {
        if (thrift == null) {
            return null;
        }
        EccInformation pojo = new EccInformation();
        if (thrift.isSetAl()) {
            pojo.setAl(thrift.getAl());
        }
        if (thrift.isSetAssessmentDate()) {
            pojo.setAssessmentDate(thrift.getAssessmentDate());
        }
        if (thrift.isSetAssessorContactPerson()) {
            pojo.setAssessorContactPerson(thrift.getAssessorContactPerson());
        }
        if (thrift.isSetAssessorDepartment()) {
            pojo.setAssessorDepartment(thrift.getAssessorDepartment());
        }
        if (thrift.isSetContainsCryptography()) {
            pojo.setContainsCryptography(thrift.isContainsCryptography());
        }
        if (thrift.isSetEccComment()) {
            pojo.setEccComment(thrift.getEccComment());
        }
        if (thrift.isSetEccStatus()) {
            pojo.setEccStatus(EnumConverter.fromThrift(thrift.getEccStatus(), org.eclipse.sw360.datahandler.services.components.ECCStatus.class));
        }
        if (thrift.isSetEccn()) {
            pojo.setEccn(thrift.getEccn());
        }
        if (thrift.isSetMaterialIndexNumber()) {
            pojo.setMaterialIndexNumber(thrift.getMaterialIndexNumber());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.components.EccInformation toThrift(EccInformation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.components.EccInformation thrift = new org.eclipse.sw360.datahandler.thrift.components.EccInformation();
        if (pojo.getAl() != null) {
            thrift.setAl(pojo.getAl());
        }
        if (pojo.getAssessmentDate() != null) {
            thrift.setAssessmentDate(pojo.getAssessmentDate());
        }
        if (pojo.getAssessorContactPerson() != null) {
            thrift.setAssessorContactPerson(pojo.getAssessorContactPerson());
        }
        if (pojo.getAssessorDepartment() != null) {
            thrift.setAssessorDepartment(pojo.getAssessorDepartment());
        }
        if (pojo.getContainsCryptography() != null) {
            thrift.setContainsCryptography(pojo.getContainsCryptography());
        }
        if (pojo.getEccComment() != null) {
            thrift.setEccComment(pojo.getEccComment());
        }
        if (pojo.getEccStatus() != null) {
            thrift.setEccStatus(EnumConverter.toThrift(pojo.getEccStatus(), org.eclipse.sw360.datahandler.thrift.components.ECCStatus.class));
        }
        if (pojo.getEccn() != null) {
            thrift.setEccn(pojo.getEccn());
        }
        if (pojo.getMaterialIndexNumber() != null) {
            thrift.setMaterialIndexNumber(pojo.getMaterialIndexNumber());
        }
        return thrift;
    }
}
