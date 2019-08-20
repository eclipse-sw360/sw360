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

package org.eclipse.sw360.importer;

import org.apache.commons.csv.CSVRecord;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * @author johannes.najjar@tngtech.com
 */
public abstract  class  CustomizedCSVRecordBuilder  <T> {
    public static String alternative(String left, String right) {
        return isNullOrEmpty(left)?right:left;
    }

    CustomizedCSVRecordBuilder(CSVRecord record){
        //parse CSV Record
        //        int i = 0;
        //        member = record.get(i++);
    }

    CustomizedCSVRecordBuilder(){
        //set all members null
    }

    public abstract T build();
}
