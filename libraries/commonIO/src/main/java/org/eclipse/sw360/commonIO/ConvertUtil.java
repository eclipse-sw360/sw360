/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.commonIO;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by bodet on 18/11/14.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ConvertUtil {

    private ConvertUtil() {
        // Utility class with only static functions
    }

    public static String parseDate(String input) {
        if ("NULL".equals(input)) {
            return null;
        }

        if (input.length() == 24 && input.startsWith("CAST")) {
            String text = input.substring(7, 7 + 8);

            StringBuilder invertedBuilder =  new StringBuilder();
            for (int i = 3; i >= 0; i--) {
                invertedBuilder.append(text.substring(2 * i, 2 * i + 2) );
            }

            long daysSinceRataDieEpoch = Long.parseLong(invertedBuilder.toString(), 16);

            long daysSinceLinuxEpoch = daysSinceRataDieEpoch - 719163L; // Epoch is 1.1.1 for rata die, 1970-01-01 corresponds to 719163L

            long miliSecondsSinceLinuxEpoch = daysSinceLinuxEpoch * 24 * 3600 * 1000;
            Date date = new Date(miliSecondsSinceLinuxEpoch);
            return new SimpleDateFormat("yyyy-MM-dd").format(date);
        }

        else {
            return input;
        }
    }

}
