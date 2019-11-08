/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.rest;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.InputStreamResource;

import java.io.InputStream;

/**
 * Spring offers a possibility to upload {@link InputStream}s in a multipart
 * HTTP request. But the used {@link InputStreamResource} does not overried the
 * {@link AbstractResource#getFilename()} so that no filename can be given for
 * the provided input stream. If one wants to use another Resource subtype, the
 * standard determination of the content-length causes issues because only the
 * {@link InputStreamResource} is handled in a special way.<br />
 * So we need our own implementation, extending the {@link InputStreamResource}
 * but providing a filename and a default content-length of -1 which triggers
 * the http container to determine the content length.
 */
public class FossologyInputStreamResource extends InputStreamResource {

    private String filename;

    public FossologyInputStreamResource(String filename, InputStream inputStream) {
        super(inputStream);

        this.filename = filename;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long contentLength() {
        return -1;
    }

}
