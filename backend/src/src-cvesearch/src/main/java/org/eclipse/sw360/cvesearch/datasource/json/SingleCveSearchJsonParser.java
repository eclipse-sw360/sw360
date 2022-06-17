/*
 * Copyright (c) Bosch Software Innovations GmbH 2019.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.datasource.json;

import com.google.gson.reflect.TypeToken;
import org.eclipse.sw360.cvesearch.datasource.CveSearchData;

import java.lang.reflect.Type;

public class SingleCveSearchJsonParser extends CveSearchJsonParser<CveSearchData> {

    @Override
    public Type getType() {
        return new TypeToken<CveSearchData>(){}.getType();
    }
}
