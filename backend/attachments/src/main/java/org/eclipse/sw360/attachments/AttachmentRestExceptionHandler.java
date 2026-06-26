/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.attachments;

import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class AttachmentRestExceptionHandler {

    @ExceptionHandler(SW360Exception.class)
    public ResponseEntity<String> handleSw360Exception(SW360Exception exception) {
        if (exception.getErrorCode() != null && exception.getErrorCode() == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getMessage());
    }

    @ExceptionHandler(org.eclipse.sw360.datahandler.thrift.SW360Exception.class)
    public ResponseEntity<String> handleThriftSw360Exception(org.eclipse.sw360.datahandler.thrift.SW360Exception exception) {
        if (exception.isSetErrorCode() && exception.getErrorCode() == 404) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getWhy());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exception.getWhy());
    }
}
