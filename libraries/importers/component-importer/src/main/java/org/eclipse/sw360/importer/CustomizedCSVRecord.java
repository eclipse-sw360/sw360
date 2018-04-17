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

/**
 * TODO mcj erase this?
 *
 * @author johannes.najjar@tngtech.com
 */
public interface CustomizedCSVRecord {

    //Methods
  Iterable<String> getCSVIterable();

    // Static abstract methods don't exist, nevertheless you should implement those:
//    public static Iterable<String> getCSVHeaderIterable() {
//        return null;
//    }
//
//    public static Iterable<String> getSampleInputIterable() {
//        return null;
//    }

//    public static T builder(){
//        return new T();
//    }
//
//    public static T builder( CSVRecord in){
//        return new T( in );
//    }

}
