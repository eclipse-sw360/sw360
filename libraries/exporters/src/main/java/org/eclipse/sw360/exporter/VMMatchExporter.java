/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.exporter;

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch;
import org.apache.thrift.TEnum;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.utils.SubTable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.thrift.vmcomponents.VMMatch._Fields.*;

/**
 * Created by jn on 25.04.16.
 *
 * @author stefan.jaeger@evosoft.com
 */
public class VMMatchExporter extends ExcelExporter<VMMatch, VMMatchExporter.VMMacthExportHelper> {

    public static final List<VMMatch._Fields> RENDERED_FIELDS = ImmutableList.<VMMatch._Fields>builder()
            .add(VM_COMPONENT_VMID)
            .add(VM_COMPONENT_ID)
            .add(VM_COMPONENT_VENDOR)
            .add(VM_COMPONENT_NAME)
            .add(VM_COMPONENT_VERSION)
            .add(VM_COMPONENT_CPE)
            .add(MATCH_TYPES_UI)
            .add(RELEASE_ID)
            .add(VENDOR_NAME)
            .add(COMPONENT_NAME)
            .add(RELEASE_VERSION)
            .add(RELEASE_CPE)
            .add(STATE)
            .build();

    private static final List<String> HEADERS = ImmutableList.<String>builder()
            .add("VM Component ID")
            .add("VM Component internal ID")
            .add("VM Component Vendor")
            .add("VM Component Name")
            .add("VM Component Version")
            .add("VM Component CPE")
            .add("Matching Types")
            .add("SW360 Release internal ID")
            .add("SW360 Vendor Fullname")
            .add("SW360 Release Name")
            .add("SW360 Release Version")
            .add("SW360 Release CPE ID")
            .add("Matching Status")
            .build();

    public VMMatchExporter() {
        super(new VMMacthExportHelper());
    }

    static class VMMacthExportHelper implements ExporterHelper<VMMatch> {

        @Override
        public int getColumns() {
            return HEADERS.size();
        }

        @Override
        public List<String> getHeaders() {
            return HEADERS;
        }

        @Override
        public SubTable makeRows(VMMatch match) {
            List<String> row = new ArrayList<>(getColumns());

            for (VMMatch._Fields renderedField : RENDERED_FIELDS) {
                Object fieldValue = match.getFieldValue(renderedField);

                if (fieldValue instanceof TEnum) {
                    row.add(nullToEmpty(ThriftEnumUtils.enumToString((TEnum) fieldValue)));
                } else if (fieldValue instanceof String) {
                    row.add(nullToEmpty((String) fieldValue));
                } else {
                    row.add("");
                }
            }
            return new SubTable(row);
        }
    }
}
