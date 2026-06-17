/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter.utils;

import java.util.ArrayList;
import java.util.List;

public class SubTable {

    List<List<String>> elements;

    public int getnRows() {
        return elements.size();
    }

    public List<String> getRow(int rowNumber) {
        return elements.get(rowNumber);
    }

    public SubTable(List<String> row) {
        elements = new ArrayList<>();
        elements.add(row);
    }

    public SubTable() {
        elements = new ArrayList<>();
    }

    public void addRow(List<String> row) {
        elements.add(row);
    }
}
