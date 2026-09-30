/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses;

import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.Assert.assertEquals;

public class LicenseRestExceptionHandlerTest {

    private final LicenseRestExceptionHandler handler = new LicenseRestExceptionHandler();

    @Test
    public void mapsMissingLicenseToNotFound() {
        ResponseEntity<String> response = handler.handleSw360Exception(
                new SW360Exception("No license details found in the database for id missing.", 404));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("No license details found in the database for id missing.", response.getBody());
    }

    @Test
    public void mapsForbiddenAndBadRequest() {
        assertEquals(HttpStatus.FORBIDDEN,
                handler.handleSw360Exception(new SW360Exception("denied", 403)).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                handler.handleSw360Exception(new SW360Exception("bad", 400)).getStatusCode());
    }

    @Test
    public void mapsMissingErrorCodeToInternalServerError() {
        ResponseEntity<String> response = handler.handleSw360Exception(new SW360Exception("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("boom", response.getBody());
    }
}
