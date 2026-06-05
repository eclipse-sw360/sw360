/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.common;

public class SW360Exception extends RuntimeException {

    private final Integer errorCode;

    public SW360Exception(String why) {
        super(why);
        this.errorCode = null;
    }

    public SW360Exception(String why, Throwable cause) {
        super(why, cause);
        this.errorCode = null;
    }

    public SW360Exception(String why, Integer errorCode) {
        super(why);
        this.errorCode = errorCode;
    }

    public SW360Exception(String why, Integer errorCode, Throwable cause) {
        super(why, cause);
        this.errorCode = errorCode;
    }
    
    public Integer getErrorCode() {
        return errorCode;
    }

    public String getWhy(){
        return getMessage();
    }
}
