/*
 * Copyright Siemens AG, 2014-2019. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thrift;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.eclipse.sw360.datahandler.common.CustomThriftDeserializer;
import org.eclipse.sw360.datahandler.common.CustomThriftSerializer;
import org.eclipse.sw360.datahandler.couchdb.deserializer.UsageDataDeserializer;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationList;
import org.eclipse.sw360.datahandler.thrift.projects.UsedReleaseRelations;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.*;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.*;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.*;
import org.eclipse.sw360.datahandler.thrift.spdx.fileinformation.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import com.ibm.cloud.cloudant.v1.model.Document;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class to supplement the Thrift generated code
 *
 * @author cedric.bodet@tngtech.com
 */
public class ThriftUtils {
    private static final Logger log = LogManager.getLogger(ThriftUtils.class);

    public static final List<Class<?>> THRIFT_CLASSES = ImmutableList.<Class<?>>builder()
            .add(ConfigContainer.class) // general
            .add(AttachmentContent.class) // Attachment service
            .add(AttachmentUsage.class) // Attachment service
            .add(Component.class).add(Release.class) // Component service
            .add(License.class).add(Obligation.class)
            .add(ObligationElement.class)
            .add(ObligationNode.class)
            .add(LicenseType.class) // License service
            .add(CustomProperties.class) // License service
            .add(LicenseObligationList.class) // License service
            .add(Project.class).add(ObligationList.class).add(UsedReleaseRelations.class).add(ClearingRequest.class)  // Project service
            .add(User.class) // User service
            .add(Vendor.class) // Vendor service
            .add(ModerationRequest.class) // Moderation service‚
            .add(ExternalToolProcess.class, ExternalToolProcessStep.class) // external tools like Fossology service
            .add(Vulnerability.class, ReleaseVulnerabilityRelation.class, ProjectVulnerabilityRating.class) // Vulnerability Service
            .add(VMPriority.class, VMAction.class, VMComponent.class, VMProcessReporting.class, VMMatch.class) // Vulnerability Monitoring service
            .add(ChangeLogs.class) // Changelog Service
            .add(SPDXDocument.class ) // SPDX Document service
            .add(DocumentCreationInformation.class ) // Document Creation Information service
            .add(PackageInformation.class ) // Package Information service
            .add(FileInformation.class) // File Information Service
            .add(Package.class) // Package Service
            .build();

    public static final List<Class<?>> THRIFT_NESTED_CLASSES = ImmutableList.<Class<?>>builder()
            .add(Attachment.class) // Attachment service
            .add(Source.class)
            .add(UsageData.class)
            .add(LicenseInfoUsage.class)
            .add(SourcePackageUsage.class)
            .add(ManuallySetUsage.class)
            .add(Repository.class)
            .add(ClearingInformation.class) // Component service
            .add(CVEReference.class, VendorAdvisory.class, VulnerabilityCheckStatus.class) // Vulnerability Service
            .add(VerificationStateInfo.class)
            .add(VMMinPatchLevel.class)  // Vulnerability Monitoring service
            .build();

    public static final Map<Class<?>, JsonDeserializer<?>> CUSTOM_DESERIALIZER = ImmutableMap.of(
            UsageData.class, new UsageDataDeserializer()
    );

    public static final ImmutableList<Component._Fields> IMMUTABLE_OF_COMPONENT = ImmutableList.of(
            Component._Fields.CREATED_BY,
            Component._Fields.CREATED_ON);

    public static final ImmutableList<Release._Fields> IMMUTABLE_OF_RELEASE = ImmutableList.of(
            Release._Fields.CREATED_BY,
            Release._Fields.CREATED_ON,
            Release._Fields.EXTERNAL_TOOL_PROCESSES);


    public static final ImmutableList<Release._Fields> IMMUTABLE_OF_RELEASE_FOR_FOSSOLOGY = ImmutableList.of(
            Release._Fields.CREATED_BY,
            Release._Fields.CREATED_ON);

    private static Gson gson;


    private ThriftUtils() {
        // Utility class with only static functions
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum> void copyField(T src, T dest, F field) {
        if (src.isSet(field)) {
            dest.setFieldValue(field, src.getFieldValue(field));
        } else {
            dest.setFieldValue(field, null);
        }
    }


    public static <T extends TBase<T, F>, F extends TFieldIdEnum> void copyFields(T src, T dest, Iterable<F> fields) {
        for (F field : fields) {
            copyField(src, dest, field);
        }
    }

    public static <S extends TBase<S, FS>, FS extends TFieldIdEnum, D extends TBase<D, FD>, FD extends TFieldIdEnum> void copyField2(S src, D dest, FS srcField, FD destField) {
        if (src.isSet(srcField)) {
            dest.setFieldValue(destField, src.getFieldValue(srcField));
        } else {
            dest.setFieldValue(destField, null);
        }
    }

    public static <T> Map<String, T> getIdMap(Collection<T> in) {
        return Maps.uniqueIndex(in, value -> {
            if (value instanceof Document doc) {
                return doc.getId();
            }
            Gson gson = getGson();
            Type t = new TypeToken<Map<String, Object>>() {}.getType();

            Map<String, Object> map = gson.fromJson(gson.toJson(value), t);
            if (map.containsKey("id")) {
                return (String) map.get("id");
            }
            if (map.containsKey("_id")) {
                return (String) map.get("_id");
            }
            return "";
        });
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum> Function<T, Object> extractField(final F field) {
        return extractField(field, Object.class);
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum, R> Function<T, R> extractField(final F field, final Class<R> clazz) {
        return input -> {
            if (input.isSet(field)) {
                Object fieldValue = input.getFieldValue(field);
                if (clazz.isInstance(fieldValue)) {
                    return clazz.cast(fieldValue);
                } else {
                    log.error("field {} of {} cannot be cast to {}", field, input, clazz.getSimpleName());
                    return null;
                }
            } else {
                return null;
            }
        };
    }

    private static Gson getGson() {
        if (gson == null) {
            GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping();
            for (Class<?> c : THRIFT_CLASSES) {
                gsonBuilder.registerTypeAdapter(c, new CustomThriftDeserializer());
                gsonBuilder.registerTypeAdapter(c, new CustomThriftSerializer());
            }
            for (Class<?> c : THRIFT_NESTED_CLASSES) {
                gsonBuilder.registerTypeAdapter(c, new CustomThriftSerializer());
            }
            gson = gsonBuilder.create();
        }
        return gson;
    }
}
