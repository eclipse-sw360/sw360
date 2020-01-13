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
package org.eclipse.sw360.cvesearch.datasource;

import java.io.IOException;
import java.util.List;

public interface CveSearchApi {

    List<CveSearchData> search(String vendor, String product) throws IOException;
    List<CveSearchData> cvefor(String cpe) throws IOException;
    CveSearchData cve(String cve) throws IOException;

    List<String> allVendorNames() throws IOException;
    List<String> allProductsOfVendor(String vendorName) throws IOException;

}
