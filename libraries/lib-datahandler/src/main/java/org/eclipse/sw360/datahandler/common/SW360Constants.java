/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.projects.Project;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.equalTo;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Sets.newHashSet;

/**
 * Utility class with definitions for the CouchDB connection
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class SW360Constants {

    public static final String KEY_ID = "_id";
    public static final String KEY_REV = "_rev";

    public static final String PROJECT_VULNERABILITY_RATING_ID_PREFIX = "pvrating_";
    public static final String LICENSE_TYPE_GLOBAL = "Global";
    public static final String LICENSE_TYPE_OTHERS = "Others";
    public static final String NA = "n/a";
    public static final String LICENSE_NAME_UNKNOWN = "License name unknown";
    public static final String OBLIGATION_TOPIC_UNKNOWN = "Obligation topic unknown";
    // Proper values of the "type" member to deserialize to CouchDB
    public static final String TYPE_OBLIGATION = "obligation";
    public static final String TYPE_TODO = "todo";
    public static final String TYPE_RISKCATEGORY = "riskCategory";
    public static final String TYPE_RISK = "risk";
    public static final String TYPE_LICENSETYPE = "licenseType";
    public static final String TYPE_LICENSE = "license";
    public static final String TYPE_VENDOR = "vendor";
    public static final String TYPE_USER = "user";
    public static final String TYPE_COMPONENT = "component";
    public static final String TYPE_RELEASE = "release";
    public static final String TYPE_ATTACHMENT = "attachment";
    public static final String TYPE_PROJECT = "project";
    public static final String TYPE_MODERATION = "moderation";

    /**
     * Hashmap containing the name field for each type.
     * Used by the search service to fill the search results
     */
    public static final Map<String, String> MAP_FULLTEXT_SEARCH_NAME =
            ImmutableMap.<String, String>builder()
                    .put(TYPE_LICENSE, "fullname")
                    .put(TYPE_TODO, "text")
                    .put(TYPE_OBLIGATION, "name")
                    .put(TYPE_USER, "email")
                    .put(TYPE_VENDOR, "fullname")
                    .put(TYPE_COMPONENT, "name")
                    .put(TYPE_RELEASE, "name version")
                    .put(TYPE_PROJECT, "name")
                    .build();

    public static final Collection<AttachmentType> LICENSE_INFO_ATTACHMENT_TYPES = Arrays.asList(AttachmentType.COMPONENT_LICENSE_INFO_XML, AttachmentType.COMPONENT_LICENSE_INFO_COMBINED);
    public static final Collection<AttachmentType> SOURCE_CODE_ATTACHMENT_TYPES = Arrays.asList(AttachmentType.SOURCE, AttachmentType.SOURCE_SELF);
    public static final String CONTENT_TYPE_OPENXML_SPREADSHEET = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static final String NOTIFICATION_CLASS_RELEASE = "release";
    public static final String NOTIFICATION_CLASS_MODERATION_REQUEST = "moderation";
    public static final String NOTIFICATION_CLASS_COMPONENT = "component";
    public static final String NOTIFICATION_CLASS_PROJECT = "project";
    public static final Map<String, List<Map.Entry<String, String>>> NOTIFIABLE_ROLES_BY_OBJECT_TYPE = ImmutableMap.<String, List<Map.Entry<String, String>>>builder()
            .put(NOTIFICATION_CLASS_PROJECT, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(Project._Fields.PROJECT_RESPONSIBLE, "Project Responsible"),
                            pair(Project._Fields.PROJECT_OWNER, "Project Owner"),
                            pair(Project._Fields.LEAD_ARCHITECT, "Lead Architect"),
                            pair(Project._Fields.MODERATORS, "Moderator"),
                            pair(Project._Fields.CONTRIBUTORS, "Contributor"),
                            pair(Project._Fields.SECURITY_RESPONSIBLES, "Security Responsible"),
                            pair(Project._Fields.ROLES, "Additional Role"))
                    .build())
            .put(NOTIFICATION_CLASS_COMPONENT, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(Component._Fields.CREATED_BY, "Creator"),
                            pair(Component._Fields.COMPONENT_OWNER, "Component Owner"),
                            pair(Component._Fields.MODERATORS, "Moderator"),
                            pair(Component._Fields.SUBSCRIBERS, "Subscriber"),
                            pair(Component._Fields.ROLES, "Additional Role"))
                    .build())
            .put(NOTIFICATION_CLASS_RELEASE, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(Release._Fields.CREATED_BY, "Creator"),
                            pair(Release._Fields.CONTRIBUTORS, "Contributor"),
                            pair(Release._Fields.MODERATORS, "Moderator"),
                            pair(Release._Fields.SUBSCRIBERS, "Subscriber"),
                            pair(Release._Fields.ROLES, "Additional Role"))
                    .build())
            .put(NOTIFICATION_CLASS_MODERATION_REQUEST, ImmutableList.<Map.Entry<String, String>>builder()
                    .add(pair(ModerationRequest._Fields.REQUESTING_USER, "Requesting User"),
                            pair(ModerationRequest._Fields.MODERATORS, "Moderator"))
                    .build())
            .build();
    public static final List<String> NOTIFICATION_EVENTS_KEYS = NOTIFIABLE_ROLES_BY_OBJECT_TYPE.entrySet()
            .stream()
            .map(notificationClassEntry -> notificationClassEntry.getValue()
                    .stream()
                    .map(roleEntry -> SW360Utils.notificationPreferenceKey(notificationClassEntry.getKey(), roleEntry.getKey())))
            .flatMap(Function.identity())
            .collect(Collectors.toList());

    static final Map<String, Boolean> DEFAULT_NOTIFICATION_PREFERENCES = NOTIFICATION_EVENTS_KEYS.stream().collect(Collectors.toMap(s -> s, s -> Boolean.FALSE));

    public static Collection<AttachmentType> allowedAttachmentTypes(String documentType) {
        Set<AttachmentType> types = newHashSet(AttachmentType.values());

        if (TYPE_COMPONENT.equals(documentType)) {
            return Sets.filter(types, not(equalTo(AttachmentType.CLEARING_REPORT)));
        } else {
            return types;
        }
    }

    private SW360Constants() {
        // Utility class with only static functions
    }

    private static Map.Entry<String, String> pair(TFieldIdEnum field, String displayName){
        return new AbstractMap.SimpleImmutableEntry<>(field.toString(), displayName);
    }

}
