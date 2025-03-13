/*
 * Copyright Ankush1oo8, 2025.
 * Copyright NEXT Ankush1oo8, 2025.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 */
package org.eclipse.sw360.common.utils;

import java.util.Comparator;

public class VersionComparator implements Comparator<String> {
    @Override
    public int compare(String v1, String v2) {
        if (v1 == null || v2 == null) {
            return (v1 == null) ? ((v2 == null) ? 0 : -1) : 1;
        }
        String[] parts1 = v1.split("[.-]"); // Handle both '.' and '-' separators
        String[] parts2 = v2.split("[.-]");
        
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = 0, num2 = 0;
            try {
                num1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            } catch (NumberFormatException e) {
                num1 = handleNonNumeric(parts1[i]);
            }
            
            try {
                num2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;
            } catch (NumberFormatException e) {
                num2 = handleNonNumeric(parts2[i]);
            }
            
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    private int handleNonNumeric(String part) {
        // Assign a lower priority to non-numeric parts
        return part.matches("\\d+") ? Integer.parseInt(part) : -1;
    }
}