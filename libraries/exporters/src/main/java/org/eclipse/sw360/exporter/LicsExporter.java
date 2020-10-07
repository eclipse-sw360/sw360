/*
 * Copyright Siemens AG, 2014-2015.
 * Copyright Bosch Software Innovations GmbH, 2016,2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import org.apache.commons.csv.CSVPrinter;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.exporter.utils.ConvertRecord;
import org.eclipse.sw360.exporter.utils.LicsArchive;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.exporter.utils.ConvertRecord.*;

public class LicsExporter {
    final LicenseService.Iface licenseClient;

    public LicsExporter(LicenseService.Iface licenseClient) {
        this.licenseClient = licenseClient;
    }

    private ByteArrayInputStream getCsvStream(List<List<String>> listList) throws TException, IOException {
        return new ByteArrayInputStream(writeCsvStream(listList).toByteArray());
    }

    @NotNull
    private ByteArrayOutputStream writeCsvStream(List<List<String>> listList) throws TException, IOException {
        final ByteArrayOutputStream riskCategoryCsvStream = new ByteArrayOutputStream();
        Writer out = new BufferedWriter(new OutputStreamWriter(riskCategoryCsvStream));
        CSVPrinter csvPrinter = new CSVPrinter(out, CommonUtils.sw360CsvFormat);
        csvPrinter.printRecords(listList);
        csvPrinter.flush();
        csvPrinter.close();
        return riskCategoryCsvStream;
    }

    @NotNull
    public Map<String, InputStream> getFilenameToCSVStreams() throws TException, IOException {
        Map<String, InputStream> fileNameToStreams = new HashMap<>();

        final List<Obligation> obligations = licenseClient.getObligations();
        List<ConvertRecord.PropertyWithValueAndId> customProperties = new ArrayList<>();
        SetMultimap<String, Integer> obligCustomPropertyMap = HashMultimap.create();
        ConvertRecord.fillTodoCustomPropertyInfo(obligations, customProperties, obligCustomPropertyMap);
        fileNameToStreams.put(LicsArchive.TODO_CUSTOM_PROPERTIES_FILE, getCsvStream(serialize(obligCustomPropertyMap, ImmutableList.of("T_ID", "P_ID"))));
        fileNameToStreams.put(LicsArchive.CUSTOM_PROPERTIES_FILE, getCsvStream(serialize(customProperties, customPropertiesSerializer())));
        fileNameToStreams.put(LicsArchive.TODO_FILE, getCsvStream(serialize(obligations, obligSerializer())));

        fileNameToStreams.put(LicsArchive.LICENSETYPE_FILE, getCsvStream(serialize(licenseClient.getLicenseTypes(), licenseTypeSerializer())));

        final List<License> licenses = licenseClient.getLicenses();
        fileNameToStreams.put(LicsArchive.LICENSE_TODO_FILE, getCsvStream(serialize(getLicenseToTodoMap(licenses), ImmutableList.of("Identifier", "ID"))));
        fileNameToStreams.put(LicsArchive.LICENSE_FILE, getCsvStream(serialize(licenses, licenseSerializer())));
        return fileNameToStreams;
    }


}
