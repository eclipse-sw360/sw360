/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver;

public class StringTransformer {
    
    /**
     * Depending on the first parameter this method returns:
     * <ul>
     *   <li>null: null</li>
     *   <li>String[] with at least one element: first element</li>
     *   <li>String[] with no element: ""</li>
     *   <li>other: string value of parameter</li>
     * </ul>
     * 
     * @param object object to transform into a single string
     * 
     * @return the transformed string
     */
    public static String transformIntoString(Object object) {
        if(object == null) {
            return null;
        }

        if(object instanceof String[]) {
            if(((String[]) object).length > 0) {
                return ((String[])object)[0];
            } else {
                return "";
            }
        }

        return object.toString();
    }
}