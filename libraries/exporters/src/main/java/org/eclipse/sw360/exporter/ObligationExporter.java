/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exporter for project obligations (license, project, organisation and component level),
 * following the same structure as {@link ProjectExporter}/{@link LicenseExporter}: a dedicated
 * {@link ExcelExporter} whose {@link ObligationHelper} renders a single {@link ObligationStatusInfo}
 * into a {@link SubTable} row.
 */
public class ObligationExporter extends ExcelExporter<ObligationStatusInfo, ObligationExporter.ObligationHelper> {

    public static final List<String> HEADERS = List.of(
            "Obligation Level", "Obligation Id", "Obligation Text", "Obligation Type",
            "Status", "Comment", "License Ids", "Releases");

    public ObligationExporter() {
        super(new ObligationHelper());
    }

    public static class ObligationHelper implements ExporterHelper<ObligationStatusInfo> {

        @Override
        public int getColumns() {
            return HEADERS.size();
        }

        @Override
        public List<String> getHeaders() {
            return HEADERS;
        }

        @Override
        public SubTable makeRows(ObligationStatusInfo osi) {
            List<String> row = new ArrayList<>();
            row.add(obligationLevelLabel(osi));
            row.add(CommonUtils.nullToEmptyString(osi.getId()));
            row.add(CommonUtils.nullToEmptyString(osi.getText()));
            row.add(osi.isSetObligationType() ? osi.getObligationType().name() : "");
            row.add(osi.isSetStatus() ? osi.getStatus().name() : "");
            row.add(CommonUtils.nullToEmptyString(osi.getComment()));
            row.add(osi.isSetLicenseIds() ? String.join(", ", osi.getLicenseIds()) : "");
            row.add(osi.isSetReleases()
                    ? osi.getReleases().stream().map(Release::getName).collect(Collectors.joining(", "))
                    : "");
            return new SubTable(row);
        }

        private static String obligationLevelLabel(ObligationStatusInfo osi) {
            if (!osi.isSetObligationLevel()) {
                return "";
            }
            return switch (osi.getObligationLevel()) {
                case ORGANISATION_OBLIGATION -> "Organisation";
                case PROJECT_OBLIGATION -> "Project";
                case COMPONENT_OBLIGATION -> "Component";
                case LICENSE_OBLIGATION -> "License";
            };
        }
    }
}


