/*
 * Copyright Siemens AG, 2021.
 * With modifications by Bosch Software Innovations GmbH, 2016
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

public class CommonUtils {

    public static final String SYSTEM_CONFIGURATION_PATH = "/etc/sw360";

    public static Properties loadProperties(Class<?> clazz, String propertiesFilePath) {
        return loadProperties(clazz, propertiesFilePath, true);
    }

    public static Properties loadProperties(Class<?> clazz, String propertiesFilePath, boolean useSystemConfig) {
        Properties props = new Properties();

        try (InputStream resourceAsStream = clazz.getResourceAsStream(propertiesFilePath)) {
            if (resourceAsStream == null)
                throw new IOException("cannot open " + propertiesFilePath);

            props.load(resourceAsStream);
        } catch (IOException e) {
        }

        if (useSystemConfig) {
            File systemPropertiesFile = new File(SYSTEM_CONFIGURATION_PATH, propertiesFilePath);
            if (systemPropertiesFile.exists()) {
                try (InputStream resourceAsStream = new FileInputStream(systemPropertiesFile.getPath())) {
                    if (resourceAsStream == null)
                        throw new IOException("cannot open " + systemPropertiesFile.getPath());

                    props.load(resourceAsStream);
                } catch (IOException e) {
                }
            }
        }
        return props;
    }

    public static Optional<byte[]> loadResource(Class<?> clazz, String resourceFilePath) {
        return loadResource(clazz, resourceFilePath, true);
    }

    public static Optional<byte[]> loadResource(Class<?> clazz, String resourceFilePath, boolean useSystemResourses) {
        if (resourceFilePath == null || resourceFilePath.isBlank()) {
            return Optional.empty();
        }
        if (useSystemResourses) {
            File systemResourceFile = new File(SYSTEM_CONFIGURATION_PATH, resourceFilePath);
            if (systemResourceFile.exists()) {
                try (InputStream resourceAsStream = new FileInputStream(systemResourceFile.getPath())) {
                    if (resourceAsStream == null) {
                        throw new IOException("cannot open " + systemResourceFile.getPath());
                    }
                    return Optional.of(IOUtils.toByteArray(resourceAsStream));
                } catch (IOException e) {
                }
            }
        }

        try (InputStream resourceAsStream = clazz.getResourceAsStream(resourceFilePath)) {
            if (resourceAsStream == null)
                throw new IOException("cannot open " + resourceFilePath);
            return Optional.of(IOUtils.toByteArray(resourceAsStream));
        } catch (IOException e) {
        }
        return Optional.empty();
    }

}
