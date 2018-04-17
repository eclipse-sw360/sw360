/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.exporter;

import java.util.ArrayList;
import java.util.List;

public class SubTable {

    List<List<String>> elements;

    public int getnRows() {
        return elements.size();
    }

    List<String> getRow(int rowNumber) {
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
