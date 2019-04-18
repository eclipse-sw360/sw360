/*
 * Copyright Siemens AG, 2014-2015.
 * Copyright Bosch Software Innovations GmbH, 2016,2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

        fileNameToStreams.put(LicsArchive.RISK_CATEGORY_FILE, getCsvStream(serialize(licenseClient.getRiskCategories(), riskCategorySerializer())));

        fileNameToStreams.put(LicsArchive.RISK_FILE, getCsvStream(serialize(licenseClient.getRisks(), riskSerializer())));
        fileNameToStreams.put(LicsArchive.OBLIGATION_FILE, getCsvStream(serialize(licenseClient.getObligations(), obligationSerializer())));

        final List<Todo> todos = licenseClient.getTodos();
        List<ConvertRecord.PropertyWithValueAndId> customProperties = new ArrayList<>();
        SetMultimap<String, Integer> todoCustomPropertyMap = HashMultimap.create();
        ConvertRecord.fillTodoCustomPropertyInfo(todos, customProperties, todoCustomPropertyMap);
        fileNameToStreams.put(LicsArchive.TODO_CUSTOM_PROPERTIES_FILE, getCsvStream(serialize(todoCustomPropertyMap, ImmutableList.of("T_ID", "P_ID"))));
        fileNameToStreams.put(LicsArchive.CUSTOM_PROPERTIES_FILE, getCsvStream(serialize(customProperties, customPropertiesSerializer())));
        fileNameToStreams.put(LicsArchive.OBLIGATION_TODO_FILE, getCsvStream(serialize(getTodoToObligationMap(todos), ImmutableList.of("O_ID", "T_ID"))));
        fileNameToStreams.put(LicsArchive.TODO_FILE, getCsvStream(serialize(todos, todoSerializer())));

        fileNameToStreams.put(LicsArchive.LICENSETYPE_FILE, getCsvStream(serialize(licenseClient.getLicenseTypes(), licenseTypeSerializer())));

        final List<License> licenses = licenseClient.getLicenses();
        fileNameToStreams.put(LicsArchive.LICENSE_TODO_FILE, getCsvStream(serialize(getLicenseToTodoMap(licenses), ImmutableList.of("Identifier", "ID"))));
        fileNameToStreams.put(LicsArchive.LICENSE_RISK_FILE, getCsvStream(serialize(getLicenseToRiskMap(licenses), ImmutableList.of("Identifier", "ID"))));
        fileNameToStreams.put(LicsArchive.LICENSE_FILE, getCsvStream(serialize(licenses, licenseSerializer())));
        return fileNameToStreams;
    }


}
