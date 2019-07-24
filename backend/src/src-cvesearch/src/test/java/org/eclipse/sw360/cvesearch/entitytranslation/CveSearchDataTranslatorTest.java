/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * With modifications by Siemens AG, 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.cvesearch.entitytranslation;

import org.eclipse.sw360.cvesearch.TestWithCveSearchConnection;
import org.eclipse.sw360.cvesearch.datasource.CveSearchApiImpl;
import org.eclipse.sw360.cvesearch.datasource.CveSearchData;
import org.eclipse.sw360.cvesearch.service.CveSearchHandler;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.cvesearch.datasource.CveSearchDataTestHelper.isUrlReachable;

public class CveSearchDataTranslatorTest extends TestWithCveSearchConnection {

    private CveSearchData cveSearchData;

    private String ID = "1";
    private String CVEYEAR = "2007";
    private String CVENUMBER = "5432";
    private String CVE = "CVE-" + CVEYEAR + "-" + CVENUMBER;

    private CveSearchDataTranslator cveSearchDataTranslator;
    private CveSearchDataToVulnerabilityTranslator cveSearchDataToVulnerabilityTranslator;

    private CveSearchData generateCveSearchData(String id) {
        return new CveSearchData() {
            @Override
            public Map<String, String> getAccess() {
                Map<String,String> access = new HashMap<>();
                access.put("access_key_a" + id, "access_value_a" + id);
                access.put("access_key_b" + id, "access_value_b" + id);
                return access;
            }

            @Override
            public String getId() {
                return "CVE-" + CVEYEAR + "-" + CVENUMBER + id;
            }

            @Override
            public String getModified() {
                return "Modified" + id;
            }

            @Override
            public Set<String> getReferences() {
                Set<String> references = new HashSet<>();
                references.add("references_a" + id);
                references.add("references_b" + id);
                return references;
            }

            @Override
            public String getPublished() {
                return "Published" + id;
            }

            @Override
            public Double getCvss() {
                return 7.5;
            }

            @Override
            public Map<String, String> getImpact() {
                Map<String,String> impact = new HashMap<>();
                impact.put("impact_key_a" + id, "impact_value_a" + id);
                impact.put("impact_key_b" + id, "impact_value_b" + id);
                return impact;
            }

            @Override
            public String getSummary() {
                return "summary" + id;
            }
        };
    }

    @Before
    public void setUp() {
        cveSearchData = generateCveSearchData("1");
        cveSearchDataTranslator = new CveSearchDataTranslator();
        cveSearchDataToVulnerabilityTranslator = new CveSearchDataToVulnerabilityTranslator();
    }

    @Test
    public void getCVEReferenceForCveSearchdataTest(){
        Set<CVEReference> cveReferences = cveSearchDataToVulnerabilityTranslator.getCVEReferencesForCVE(CVE);

        assert(1 == cveReferences.size());
        CVEReference cveReference = cveReferences.stream()
                .findAny()
                .get();
        assert(CVEYEAR.equals(cveReference.getYear()));
        assert(CVENUMBER.equals(cveReference.getNumber()));
    }

    @Test
    public void testIdTranslation() {
        Vulnerability v = cveSearchDataTranslator.apply(cveSearchData).vulnerability;

        Set<CVEReference> cveReferences = v.getCveReferences();

        assert(cveReferences.size() == 1);

        CVEReference cveReference = cveReferences.stream()
                .findAny()
                .get();

        assert(CVEYEAR.equals(cveReference.getYear()));
        assert((CVENUMBER+ID).equals(cveReference.getNumber()));
    }

    @Test
    public void testSummaryTranslation() {
        Vulnerability v = cveSearchDataTranslator.apply(cveSearchData).vulnerability;
        assert(v.getDescription().equals(cveSearchData.getSummary()));
    }

    @Test
    public void testWithRealData() throws IOException {
        List<CveSearchData> cveSearchDatas = cveSearchApi.search("zyxel","zywall");
        List<CveSearchDataTranslator.VulnerabilityWithRelation> vms = cveSearchDatas.stream()
                .map(cveSearchData -> cveSearchDataTranslator.apply(cveSearchData))
                .collect(Collectors.toList());
    }

    @Test
    public void testWithApacheData() throws IOException {
        List<CveSearchData> cveSearchDatas = cveSearchApi.search("apache",".*");
        List<CveSearchDataTranslator.VulnerabilityWithRelation> vms = cveSearchDatas.stream()
                .map(cveSearchData -> cveSearchDataTranslator.apply(cveSearchData))
                .collect(Collectors.toList());
        List<Vulnerability> vs = vms.stream()
                .map(vm -> vm.vulnerability)
                .collect(Collectors.toList());
        assert(vs.size() > 700);
    }
}
