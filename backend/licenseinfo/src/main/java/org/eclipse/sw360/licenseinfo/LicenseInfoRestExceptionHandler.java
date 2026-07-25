/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenseinfo;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class LicenseInfoRestExceptionHandler {

    @ExceptionHandler(SW360Exception.class)
    public ResponseEntity<String> handleSw360Exception(SW360Exception exception) {
        // The thrift type carries its text in "why"; getMessage() is always null.
        String reason = exception.isSetWhy() ? exception.getWhy() : exception.getMessage();
        int code = exception.getErrorCode();
        if (code == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(reason);
        }
        if (code == 403) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(reason);
        }
        if (code == 400) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(reason);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(reason);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    /**
     * Plain {@link TException}s (for example an unsupported output generator) carry the
     * only diagnostic the caller gets, so keep the message in the response body.
     */
    @ExceptionHandler(TException.class)
    public ResponseEntity<String> handleTException(TException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
    }
}
