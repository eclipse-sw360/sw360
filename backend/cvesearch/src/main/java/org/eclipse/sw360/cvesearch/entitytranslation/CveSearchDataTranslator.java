/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.entitytranslation;

import org.eclipse.sw360.cvesearch.datasource.CveSearchData;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;

public class CveSearchDataTranslator implements EntityTranslator<CveSearchData, CveSearchDataTranslator.VulnerabilityWithRelation> {

    public class VulnerabilityWithRelation{
        public Vulnerability vulnerability;
        public ReleaseVulnerabilityRelation relation;

        public VulnerabilityWithRelation(Vulnerability vulnerability, ReleaseVulnerabilityRelation relation) {
            this.vulnerability = vulnerability;
            this.relation = relation;
        }
    }

    @Override
    public VulnerabilityWithRelation apply(CveSearchData cveSearchData) {
        Vulnerability vulnerability = new CveSearchDataToVulnerabilityTranslator().apply(cveSearchData);
        ReleaseVulnerabilityRelation relation = new CveSearchDataToReleaseVulnerabilityRelationTranslator().apply(cveSearchData);
        return new VulnerabilityWithRelation(vulnerability,relation);
    }
}
