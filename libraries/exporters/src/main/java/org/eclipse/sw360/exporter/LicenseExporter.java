/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.ConvertRecord;
import org.eclipse.sw360.exporter.utils.SubTable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.eclipse.sw360.exporter.utils.ConvertRecord.licenseSerializer;


/**
 * Created by bodet on 10/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class LicenseExporter extends ExcelExporter<License, LicenseExporter.LicenseHelper> {
    private static final Logger log = LogManager.getLogger(LicenseExporter.class);

    public LicenseExporter(Function<Logger, List<LicenseType>> getLicenseTypes) {
        super(new LicenseHelper(() -> getLicenseTypes.apply(log)));
    }

    static class LicenseHelper implements ExporterHelper<License> {
        private final ConvertRecord.Serializer<License> converter;
        private Supplier<List<LicenseType>> getLicenseTypes;
        private HashMap<String, String> formattedStringToTypeId = new HashMap<>();
        int indexOfTypeOrId;

        public LicenseHelper(Supplier<List<LicenseType>> getLicenseTypes) {
            this.getLicenseTypes = getLicenseTypes;
            converter = licenseSerializer();
            indexOfTypeOrId = converter.headers().indexOf("Type");
        }

        public void fillLicenseTypeIdToFormattedString() {
            formattedStringToTypeId.put("","");
            List<LicenseType> licenseTypes = getLicenseTypes.get();
            for (LicenseType licenseType: licenseTypes) {
                String formattedLicenseType = getFormattedStringForLicenseType(licenseType);
                formattedStringToTypeId.put(String.valueOf(licenseType.getLicenseTypeId()),
                        formattedLicenseType);
                formattedStringToTypeId.put(String.valueOf(licenseType.getId()),
                        formattedLicenseType);
            }
        }

        private String getFormattedStringForLicenseType(LicenseType licenseType) {
            return licenseType.getLicenseTypeId() + ": " + licenseType.getLicenseType();
        }

        @Override
        public int getColumns() {
            return converter.headers().size();
        }

        @Override
        public List<String> getHeaders() {
            return converter.headers();
        }

        private List<String> formatRow(List<String> row) {
            if(formattedStringToTypeId.size() == 0) {
                fillLicenseTypeIdToFormattedString();
            }

            row.set(indexOfTypeOrId, formattedStringToTypeId.get(row.get(indexOfTypeOrId)));
            return row;
        }

        @Override
        public SubTable makeRows(License license) {
            return new SubTable(
                    formatRow(converter.transformer().apply(license))
            );
        }
    }

}
