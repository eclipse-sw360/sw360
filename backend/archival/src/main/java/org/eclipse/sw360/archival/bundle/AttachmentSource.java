/*
 * Copyright Taanvi Khevaria, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.archival.bundle;

import org.eclipse.sw360.services.archival.AttachmentMetadata;

import java.io.IOException;
import java.io.InputStream;

/** Streamed handle to one attachment binary
 *  open() is called when the bundler is ready to copy bytes.
*/

public interface AttachmentSource {

    AttachmentMetadata metadata();

    long sizeBytes();

    InputStream open() throws IOException;
}
