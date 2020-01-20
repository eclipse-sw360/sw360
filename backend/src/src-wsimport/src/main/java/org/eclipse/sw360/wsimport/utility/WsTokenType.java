/*
 * Copyright (c) Verifa Oy, 2018. Part of the SW360 Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.utility;

/**
 * @author: ksoranko@verifa.io
 */
public enum WsTokenType {
    ORGANIZATION("orgToken"),
    PRODUCT("productToken"),
    PROJECT("projectToken");

    WsTokenType(final String text) {
        this.text = text;
    }

    private final String text;

    @Override
    public String toString() {
        return text;
    }
}
