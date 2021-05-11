/*
 * Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import java.lang.reflect.Type;
import java.util.Map;

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.thrift.Source;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.LicenseInfoUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.ManuallySetUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.SourcePackageUsage;
import org.eclipse.sw360.datahandler.thrift.attachments.UsageData;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.internal.LinkedTreeMap;

public class CustomThriftDeserializer implements JsonDeserializer<TBase> {
    private static Map<UsageData._Fields, Class<?>> typeMap;
    static {
        typeMap = Maps.newHashMap();
        typeMap.put(UsageData._Fields.LICENSE_INFO, LicenseInfoUsage.class);
        typeMap.put(UsageData._Fields.SOURCE_PACKAGE, SourcePackageUsage.class);
        typeMap.put(UsageData._Fields.MANUALLY_SET, ManuallySetUsage.class);
    }

    private static class GenericUsageData {
        String setField_;
        LinkedTreeMap value_;
    }

    private static class GenericAttachmentUsage {
        String id;
        String revision;
        String type;
        Source owner;
        String attachmentContentId;
        Source usedBy;
        GenericUsageData usageData;
    }

    @Override
    public TBase deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonObject()) {
            JsonObject jObj = json.getAsJsonObject();
            if (jObj.has("issetBitfield")) {
                jObj.add("__isset_bitfield", jObj.get("issetBitfield"));
            }
            json = jObj.getAsJsonObject();
        }

        Gson gson = new Gson();
        if (typeOfT.getTypeName().equals("org.eclipse.sw360.datahandler.thrift.attachments.AttachmentUsage")) {
            AttachmentUsage tbase = gson.fromJson(json, typeOfT);
            GenericAttachmentUsage attachmntUsage = gson.fromJson(json, GenericAttachmentUsage.class);
            GenericUsageData genericUsageData = attachmntUsage.usageData;
            AttachmentUsage au = new AttachmentUsage(attachmntUsage.owner, attachmntUsage.attachmentContentId,
                    attachmntUsage.usedBy);
            if(genericUsageData != null) {
                UsageData._Fields type = null;
                if(genericUsageData != null) {
                    type = UsageData._Fields.valueOf(genericUsageData.setField_);
                }
                if (type == null) {
                    throw new IllegalArgumentException(
                            "Type " + genericUsageData.setField_ + " is not registered and cannot be deserialized.");
                }

                Class<?> valueType = typeMap.get(type);
                if (valueType == null) {
                    throw new IllegalArgumentException("No class registered for type " + genericUsageData.setField_
                            + ". Could not deserialize field.");
                }
                Object value = gson.fromJson(new Gson().toJson((LinkedTreeMap) genericUsageData.value_), valueType);
                UsageData ud = new UsageData(type, value);
                au.setUsageData(ud);
            }
            Map tbaseMap = gson.fromJson(json, Map.class);
            if (tbaseMap.get("_id").toString() != null) {
                au.setId(tbaseMap.get("_id").toString());
            }
            if (tbaseMap.get("_rev").toString() != null) {
                au.setRevision(tbaseMap.get("_rev").toString());
            }
            return au;
        }
        Map tbaseMap = gson.fromJson(json, Map.class);
        TBase tbase = gson.fromJson(json, typeOfT);
        TFieldIdEnum id = tbase.fieldForId(1);
        TFieldIdEnum rev = tbase.fieldForId(2);
        tbase.setFieldValue(id, tbaseMap.get("_id"));
        tbase.setFieldValue(rev, tbaseMap.get("_rev"));
        return tbase;
    }
}
