/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.utils;

public class SW360ClientException extends RuntimeException {
    public SW360ClientException(String s) {
        super(s);
    }

    public SW360ClientException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
