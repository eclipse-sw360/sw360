/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
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
