/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RestExceptionHandlerTest {

    private final RestExceptionHandler restExceptionHandler = new RestExceptionHandler();

    @Test
    public void isClientAbortException_returnsTrue_forNestedBrokenPipe() {
        HttpMessageNotWritableException exception =
                new HttpMessageNotWritableException("write failed", new IOException("Broken pipe"));

        assertThat(RestExceptionHandler.isClientAbortException(exception)).isTrue();
    }

    @Test
    public void handleMessageNotWritableException_returnsNoContent_forClientAbort() {
        HttpMessageNotWritableException exception =
                new HttpMessageNotWritableException("write failed", new IOException("Broken pipe"));

        ResponseEntity<RestExceptionHandler.ErrorMessage> response =
                restExceptionHandler.handleMessageNotWritableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    @Test
    public void handleMessageNotWritableException_returnsInternalServerError_forNonClientAbort() {
        HttpMessageNotWritableException exception =
                new HttpMessageNotWritableException("serialization failed", new IllegalStateException("boom"));

        ResponseEntity<RestExceptionHandler.ErrorMessage> response =
                restExceptionHandler.handleMessageNotWritableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    public void handleSw360Exception_mapsErrorCodeToBadRequest_for400Code() {
        SW360Exception exception = new SW360Exception();
        exception.setWhy("Invalid component parameters");
        exception.setErrorCode(400);

        ResponseEntity<RestExceptionHandler.ErrorMessage> response =
                restExceptionHandler.handleSw360Exception(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid component parameters");
    }

    @Test
    public void handleSw360Exception_mapsErrorCodeToNotFound_for404Code() {
        SW360Exception exception = new SW360Exception();
        exception.setWhy("Component not found");
        exception.setErrorCode(404);

        ResponseEntity<RestExceptionHandler.ErrorMessage> response =
                restExceptionHandler.handleSw360Exception(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(404);
        assertThat(response.getBody().getMessage()).isEqualTo("Component not found");
    }

    @Test
    public void handleSw360Exception_mapsErrorCodeToForbidden_for403Code() {
        SW360Exception exception = new SW360Exception();
        exception.setWhy("Access forbidden for project");
        exception.setErrorCode(403);

        ResponseEntity<RestExceptionHandler.ErrorMessage> response =
                restExceptionHandler.handleSw360Exception(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("Access forbidden for project");
    }

    @Test
    public void handleSw360Exception_defaultsToInternalServerError_whenErrorCodeUnsetOrZero() {
        SW360Exception exception = new SW360Exception();
        exception.setWhy("Unexpected backend error");

        ResponseEntity<RestExceptionHandler.ErrorMessage> response =
                restExceptionHandler.handleSw360Exception(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Unexpected backend error");
    }
}
