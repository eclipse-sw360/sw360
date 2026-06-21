/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport;

import org.eclipse.sw360.wsimport.rest.WsImportService;
import org.eclipse.sw360.wsimport.thrift.ThriftUploader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class WsImportHandlerTest {

    @Test
    void getIdNameReturnsWsId() {
        WsImportHandler handler = new WsImportHandler(mock(WsImportService.class), mock(ThriftUploader.class));
        assertEquals("wsId", handler.getIdName());
    }
}
