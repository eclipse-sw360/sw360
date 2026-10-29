/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;

/**
 * Created by jn on 03.03.15.
 *
 * @author Johannes.Najjar@tngtech.com
 */
public class VendorExporter extends ExcelExporter<Vendor, VendorExporter.VendorHelper> {

    public static final List<FieldAccessor<Vendor>> RENDERED_FIELDS = ExporterFields.inOrder(
            Vendor.class, "fullname", "shortname", "url");

    private static final List<String> HEADERS = List.of(
            "Vendor Fullname",
            "Vendor Shortname",
            "URL");

    public VendorExporter() {
        super(new VendorHelper());
    }

    static class VendorHelper implements ExporterHelper<Vendor> {

        @Override
        public int getColumns() {
            return HEADERS.size();
        }

        @Override
        public List<String> getHeaders() {
            return HEADERS;
        }

        @Override
        public SubTable makeRows(Vendor vendor) throws SW360Exception {
            List<String> row = new ArrayList<>(getColumns());

            for (FieldAccessor<Vendor> renderedField : RENDERED_FIELDS) {
                row.add(fieldValueAsString(renderedField.get(vendor)));
            }
            return new SubTable(row);
        }
    }
}
