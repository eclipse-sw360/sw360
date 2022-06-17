/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * With modifications by Siemens AG, 2018-2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo.outputGenerators;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationAtProject;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationInfoRequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.ObligationParsingResult;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DocxGeneratorTest {

    private static List<ObligationParsingResult> obligationParsingResults;

    @BeforeClass
    public static void setUp() throws Exception {
        obligationParsingResults = new ArrayList<>();
        ObligationParsingResult opr = new ObligationParsingResult();
        opr.setStatus(ObligationInfoRequestStatus.SUCCESS);

        opr.setObligationsAtProject(IntStream.rangeClosed(1, 100).mapToObj(i ->
                    new ObligationAtProject("Topic" + i, "Text" + i,
                            IntStream.rangeClosed(1, i).mapToObj(j -> "License"+j).collect(Collectors.toList()))
                ).collect(Collectors.toList()));
        obligationParsingResults.add(opr);

    }


    @Test
    public void testExtractingMostCommonLicense() throws Exception {
        Set<String> mostCommonLicenses = DocxGenerator.extractMostCommonLicenses(obligationParsingResults, 100);
        assertThat(mostCommonLicenses.size(), is(1));
        assertThat(mostCommonLicenses.toArray(new String[1])[0], is("License1"));

        mostCommonLicenses = DocxGenerator.extractMostCommonLicenses(obligationParsingResults, 3);
        assertThat(mostCommonLicenses.size(), is(98));
    }
}