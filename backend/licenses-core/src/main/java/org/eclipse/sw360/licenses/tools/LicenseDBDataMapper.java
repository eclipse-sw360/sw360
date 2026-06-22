/*
 * Copyright Sandip Mandal, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.tools;

import org.eclipse.sw360.datahandler.thrift.Quadratic;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class LicenseDBDataMapper {

    public static final String EXTERNAL_ID_LICENSEDB = "licensedb-id";
    public static final String EXTERNAL_ID_LICENSEDB_OB = "licensedb-ob-id";

    private LicenseDBDataMapper() {
    }

    public static Obligation toObligation(LicenseDBObligationDTO dto) {
        return new Obligation()
                .setText(dto.getText())
                .setTitle(dto.getTopic())
                .setComments(dto.getComment())
                .setObligationLevel(ObligationLevel.LICENSE_OBLIGATION)
                .setObligationType(mapObligationType(dto.getType()))
                .setExternalIds(Collections.singletonMap(EXTERNAL_ID_LICENSEDB_OB, dto.getId()))
                .setWhitelist(new HashSet<>());
    }

    public static License toLicense(LicenseDBLicenseDTO dto) {
        Map<String, String> externalIds = new HashMap<>();
        externalIds.put(EXTERNAL_ID_LICENSEDB, dto.getId());
        if (dto.getSpdxId() != null && !dto.getSpdxId().isEmpty()) {
            externalIds.put("SPDX-License-Identifier", dto.getSpdxId());
        }

        String fullname = (dto.getFullname() != null && !dto.getFullname().isEmpty())
                ? dto.getFullname()
                : dto.getShortname();

        return new License()
                .setId(dto.getShortname())
                .setShortname(dto.getShortname())
                .setFullname(fullname)
                .setText(dto.getText())
                .setExternalLicenseLink(dto.getUrl())
                .setNote(dto.getNotes())
                .setOSIApproved(dto.isOsiApproved() ? Quadratic.YES : Quadratic.NA)
                .setExternalIds(externalIds)
                .setChecked(false)
                .setObligationDatabaseIds(new HashSet<>());
    }

    public static ObligationType mapObligationType(String type) {
        if (type == null) return ObligationType.OBLIGATION;
        return switch (type.toUpperCase()) {
            case "RISK" -> ObligationType.RISK;
            case "RESTRICTION" -> ObligationType.RESTRICTION;
            case "RIGHT" -> ObligationType.PERMISSION;
            default -> ObligationType.OBLIGATION;
        };
    }
}
