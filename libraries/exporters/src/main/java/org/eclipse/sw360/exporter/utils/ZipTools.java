/*
 * Copyright Siemens AG, 2014-2015.
 * Copyright Bosch Software Innovations GmbH, 2016,2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter.utils;

import java.io.*;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author johannes.najjar@tngtech.com
 */
public class ZipTools {
    public static void addToZip(ZipOutputStream zipOutputStream, String filename, InputStream bit) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(filename));
        byte[] buffer = new byte[1024];
        int length;
        while ((length = bit.read(buffer)) > 0) {
            zipOutputStream.write(buffer, 0, length);
        }
        zipOutputStream.closeEntry();
    }

    public static void extractZipToInputStreamMap(InputStream in, HashMap<String, InputStream> inputMap) throws IOException {
        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(in));
        /** I assume that the license Zip will not be so big and I can keep it in memory with impunity**/
        /** Nevertheless I do not want to go through the file twice, so read first and then check :) **/
        final int BUFFER = 2048;
        byte data[] = new byte[BUFFER];
        ZipEntry entry;
        int count;
        while ((entry = zis.getNextEntry()) != null) {
            final ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
            while ((count = zis.read(data, 0, BUFFER)) != -1) {
                bufferStream.write(data, 0, count);
            }
            bufferStream.flush();
            bufferStream.close();

            final ByteArrayInputStream inputStream = new ByteArrayInputStream(bufferStream.toByteArray());

            inputMap.put(entry.getName(), inputStream);
        }
    }


}
