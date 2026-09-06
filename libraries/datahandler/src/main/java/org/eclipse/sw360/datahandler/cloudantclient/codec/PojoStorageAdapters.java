/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.cloudantclient.codec;

import org.eclipse.sw360.datahandler.services.attachments.UsageData;
import org.eclipse.sw360.datahandler.services.common.Source;

import com.google.gson.GsonBuilder;

/**
 * Registers Gson adapters that implement mentor-approved lazy migration:
 * read thrift-shaped CouchDB JSON into clean service-api POJOs; write POJO JSON
 * so documents clean up on the next save.
 *
 * <p>Thrift {@code TBase} types keep using {@code CustomThriftSerializer} /
 * {@code CustomThriftDeserializer}. These adapters apply only to service-api types.
 */
public final class PojoStorageAdapters {

    private PojoStorageAdapters() {}

    public static void register(GsonBuilder gsonBuilder) {
        gsonBuilder.registerTypeAdapter(Source.class, new SourceTypeAdapter());
        gsonBuilder.registerTypeAdapter(UsageData.class, new UsageDataTypeAdapter());
    }
}
