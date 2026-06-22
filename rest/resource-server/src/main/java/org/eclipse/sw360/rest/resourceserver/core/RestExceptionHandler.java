/*
 * Copyright Siemens AG, 2017-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.core;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.resourcelists.PaginationParameterException;
import org.eclipse.sw360.datahandler.resourcelists.ResourceClassNotFoundException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.rest.resourceserver.core.serializer.Json3InstantSerializer;
import org.eclipse.sw360.rest.resourceserver.core.serializer.JsonInstantSerializer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;

@ControllerAdvice
public class RestExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger(RestExceptionHandler.class);

    @ExceptionHandler({Exception.class, TException.class, ResourceClassNotFoundException.class})
    public ResponseEntity<ErrorMessage> handleException(Exception e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorMessage> handleAccessDeniedException(AccessDeniedException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.FORBIDDEN), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler({ResourceNotFoundException.class, PaginationParameterException.class})
    public ResponseEntity<ErrorMessage> handleResourceNotFound(Exception e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, BadRequestClientException.class})
    public ResponseEntity<ErrorMessage> handleMessageNotReadableException(RuntimeException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<ErrorMessage> handleMessageNotWritableException(HttpMessageNotWritableException e) {
        if (isClientAbortException(e)) {
            logClientAbort(e);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<ErrorMessage> handleAsyncRequestNotUsableException(AsyncRequestNotUsableException e) {
        if (isClientAbortException(e)) {
            logClientAbort(e);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ErrorMessage> handleMissingServletRequestParameter(Exception e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.BAD_REQUEST), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessage> handleRuntimeException(RuntimeException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.INTERNAL_SERVER_ERROR), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorMessage> handleRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.METHOD_NOT_ALLOWED), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorMessage> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.UNSUPPORTED_MEDIA_TYPE), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorMessage> handleClientError(HttpClientErrorException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.valueOf(e.getStatusCode().value())), HttpStatus.valueOf(e.getStatusCode().value()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorMessage> handleNoHandlerFound(NoResourceFoundException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.NOT_FOUND), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ResponseEntity<ErrorMessage> handleConflict(Exception e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.CONFLICT), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<ErrorMessage> handleInvalidApiToken(AuthenticationException e) {
        return new ResponseEntity<>(new ErrorMessage(e, HttpStatus.UNAUTHORIZED), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({SW360Exception.class})
    public ResponseEntity<ErrorMessage> handleSw360Exception(SW360Exception e) {
        HttpStatus httpStatus = resolveHttpStatus(e);
        return new ResponseEntity<>(new ErrorMessage(e, httpStatus), httpStatus);
    }

    private static HttpStatus resolveHttpStatus(SW360Exception e) {
        if (e.isSetErrorCode()) {
            int errorCode = e.getErrorCode();
            if (errorCode >= 400 && errorCode <= 599) {
                HttpStatus resolved = HttpStatus.resolve(errorCode);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    static boolean isClientAbortException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof AsyncRequestNotUsableException) {
                return true;
            }
            if (current instanceof IOException) {
                String message = current.getMessage();
                if (message != null) {
                    String normalized = message.toLowerCase(Locale.ROOT);
                    if (normalized.contains("broken pipe") || normalized.contains("connection reset by peer")) {
                        return true;
                    }
                }
            }
            if (current.getClass().getName().endsWith("ClientAbortException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void logClientAbort(Exception e) {
        LOGGER.warn("Client disconnected while writing response: {}", e.getMessage());
        LOGGER.debug("Client abort details", e);
    }

    @Data
    @RequiredArgsConstructor
    public static class ErrorMessage {

        @JsonSerialize(using = JsonInstantSerializer.class)
        @tools.jackson.databind.annotation.JsonSerialize(using = Json3InstantSerializer.class)
        private Instant timestamp = Instant.now();
        private final int status;
        private final String error;
        private final String message;

        public ErrorMessage(Exception e, HttpStatus httpStatus) {
            this.status = httpStatus.value();
            this.error = httpStatus.getReasonPhrase();
            this.message = e.getMessage();
            LOGGER.log(
                    httpStatus.is5xxServerError() ? Level.ERROR : Level.WARN,
                    "Response ({}): {}", this.status, this.message, e
            );
        }
    }
}
