/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.components.COTSDetails;
import org.eclipse.sw360.datahandler.thrift.components.ClearingInformation;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.users.User;

/**
 * @author smruti.sahoo@siemens.com
 *
 * Common class for database handlers to put the common/generic logic.
 */

public class DatabaseHandlerUtil {

    private static final String SEPARATOR = " -> ";

    private static <T, R> Object[] getCyclicLinkPresenceAndLastElementInCycle(T obj, R handler, User user,
            Map<String, String> linkedPath) throws TException {
        Map linkedElementsMap = null;
        if (obj instanceof Project) {
            Project proj = (Project) obj;
            linkedElementsMap = proj.getLinkedProjects();
        } else if (obj instanceof Release) {
            Release release = (Release) obj;
            linkedElementsMap = release.getReleaseIdToRelationship();
        }

        if (linkedElementsMap != null) {
            Iterator<String> linkedElementIterator = linkedElementsMap.keySet().iterator();
            while (linkedElementIterator.hasNext()) {
                String linkedElementId = linkedElementIterator.next();
                T linkedElement = null;
                String elementFullName = null;
                if (handler instanceof ProjectDatabaseHandler) {
                    ProjectDatabaseHandler projDBHandler = (ProjectDatabaseHandler) handler;
                    Project project = projDBHandler.getProjectById(linkedElementId, user);
                    elementFullName = SW360Utils.printName(project);
                    linkedElement = (T) project;
                } else if (handler instanceof ComponentDatabaseHandler) {
                    ComponentDatabaseHandler compDBHandler = (ComponentDatabaseHandler) handler;
                    Release release = compDBHandler.getRelease(linkedElementId, user);
                    elementFullName = SW360Utils.printName(release);
                    linkedElement = (T) release;
                }

                if (linkedPath.containsKey(linkedElementId)) {
                    return new Object[] { Boolean.TRUE, elementFullName };
                }

                linkedPath.put(linkedElementId, elementFullName);
                Object[] cyclicLinkPresenceAndLastElementInCycle = getCyclicLinkPresenceAndLastElementInCycle(
                        linkedElement, handler, user, linkedPath);
                boolean isCyclicLinkPresent = (Boolean) cyclicLinkPresenceAndLastElementInCycle[0];

                if (isCyclicLinkPresent) {
                    return cyclicLinkPresenceAndLastElementInCycle;
                }
                linkedPath.remove(linkedElementId);
            }
        }
        return new Object[] { Boolean.FALSE, null };
    }

    public static <T, R> String getCyclicLinkedPath(T obj, R handler, User user) throws TException {
        Map<String, String> linkedPath = new LinkedHashMap<>();
        String firstElementFullName = null;
        String id = null;
        if (obj instanceof Project) {
            Project proj = (Project) obj;
            firstElementFullName = SW360Utils.printName(proj);
            id = proj.getId();
        } else if (obj instanceof Release) {
            Release release = (Release) obj;
            firstElementFullName = SW360Utils.printName(release);
            id = release.getId();
        }
        linkedPath.put(id, firstElementFullName);
        Object[] cyclicLinkPresenceAndLastElementInCycle = getCyclicLinkPresenceAndLastElementInCycle(obj, handler,
                user, linkedPath);
        String cyclicHierarchy = "";
        boolean isCyclicLinkPresent = (Boolean) cyclicLinkPresenceAndLastElementInCycle[0];
        if (isCyclicLinkPresent) {
            String[] arrayOfLinkedPath = linkedPath.values().toArray(new String[0]);
            String lastElementInCycle = (String) cyclicLinkPresenceAndLastElementInCycle[1];
            cyclicHierarchy = String.join(SEPARATOR, arrayOfLinkedPath);
            cyclicHierarchy = cyclicHierarchy.concat(SEPARATOR).concat(lastElementInCycle);
        }
        return cyclicHierarchy;
    }

    public static <T, R extends DatabaseRepository> boolean isAllIdInSetExists(Set<String> setOfIds, R repository) {
        long nonExistingIdCount = 0;
        if (setOfIds != null) {
            nonExistingIdCount = setOfIds.stream()
                    .filter(id -> {
                        if(CommonUtils.isNullEmptyOrWhitespace(id))
                           return false;
                        T obj = (T) repository.get(id);
                        return Objects.isNull(obj);
                    }).count();
        }

        if (nonExistingIdCount > 0)
            return false;

        return true;
    }

    public static Set<String> trimSetOfString(Set<String> setOfString) {
        if (setOfString != null) {
            setOfString = setOfString.stream().filter(str -> CommonUtils.isNotNullEmptyOrWhitespace(str))
                    .map(str -> str.trim()).collect(Collectors.toSet());
        }
        return setOfString;
    }

    public static Set<Attachment> trimSetOfAttachement(Set<Attachment> setOfAttachment) {
        if (setOfAttachment != null) {
            setOfAttachment = setOfAttachment.stream().filter(attachment -> attachment != null).map(attachment -> {
                String createdComment = attachment.getCreatedComment();
                if (createdComment != null) {
                    attachment.setCreatedComment(createdComment.trim());
                }
                String checkedComment = attachment.getCheckedComment();
                if (checkedComment != null) {
                    attachment.setCheckedComment(checkedComment.trim());
                }
                return attachment;
            }).collect(Collectors.toSet());
        }
        return setOfAttachment;
    }

    public static Map<String, String> trimMapOfStringKeyStringValue(Map<String, String> mapOfStringKeyStringValue) {
        if (mapOfStringKeyStringValue != null) {
            mapOfStringKeyStringValue = mapOfStringKeyStringValue.entrySet().stream()
                    .filter(entry -> CommonUtils.isNotNullEmptyOrWhitespace(entry.getKey())
                            && CommonUtils.isNotNullEmptyOrWhitespace(entry.getValue()))
                    .collect(Collectors.toMap(entry -> entry.getKey().trim(), entry -> {
                        String value = entry.getValue();
                        if (value != null) {
                            value = value.trim();
                        }
                        return value;
                    }));
        }
        return mapOfStringKeyStringValue;
    }

    public static Map<String, Set<String>> trimMapOfStringKeySetValue(Map<String, Set<String>> mapOfStringKeySetValue) {
        if (mapOfStringKeySetValue != null) {
            mapOfStringKeySetValue = mapOfStringKeySetValue.entrySet().stream()
                    .filter(entry -> CommonUtils.isNotNullEmptyOrWhitespace(entry.getKey())).map(entry -> {
                        Set<String> value = entry.getValue();
                        if (value != null) {
                            Set<String> filteredValue = value.stream()
                                    .filter(valueItem -> CommonUtils.isNotNullEmptyOrWhitespace(valueItem))
                                    .map(valueItem -> valueItem.trim()).collect(Collectors.toSet());
                            entry.setValue(filteredValue);
                        }

                        return entry;
                    }).filter(entry -> entry.getValue() != null && !entry.getValue().isEmpty())
                    .collect(Collectors.toMap(entry -> entry.getKey().trim(), entry -> entry.getValue()));
        }
        return mapOfStringKeySetValue;
    }

    public static <T, R> void trimStringFields(T obj, List<R> listOfStrFields) {
        listOfStrFields.forEach(strField -> {
            if (obj instanceof Component) {
                Component._Fields compField = (Component._Fields) strField;
                Component comp = (Component) obj;
                Object fieldValueObj = comp.getFieldValue(compField);
                if (fieldValueObj instanceof String) {
                    comp.setFieldValue(compField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof Release) {
                Release._Fields releaseField = (Release._Fields) strField;
                Release release = (Release) obj;
                Object fieldValueObj = release.getFieldValue(releaseField);
                if (fieldValueObj instanceof String) {
                    release.setFieldValue(releaseField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof Project) {
                Project._Fields projField = (Project._Fields) strField;
                Project proj = (Project) obj;
                Object fieldValueObj = proj.getFieldValue(projField);
                if (fieldValueObj instanceof String) {
                    proj.setFieldValue(projField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof ClearingInformation) {
                ClearingInformation._Fields clearingInformationField = (ClearingInformation._Fields) strField;
                ClearingInformation clearingInformation = (ClearingInformation) obj;
                Object fieldValueObj = clearingInformation.getFieldValue(clearingInformationField);
                if (fieldValueObj instanceof String) {
                    clearingInformation.setFieldValue(clearingInformationField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof COTSDetails) {
                COTSDetails._Fields cotsDetailsField = (COTSDetails._Fields) strField;
                COTSDetails cotsDetails = (COTSDetails) obj;
                Object fieldValueObj = cotsDetails.getFieldValue(cotsDetailsField);
                if (fieldValueObj instanceof String) {
                    cotsDetails.setFieldValue(cotsDetailsField, fieldValueObj.toString().trim());
                }
            } else if (obj instanceof EccInformation) {
                EccInformation._Fields eccInformationField = (EccInformation._Fields) strField;
                EccInformation eccInformation = (EccInformation) obj;
                Object fieldValueObj = eccInformation.getFieldValue(eccInformationField);
                if (fieldValueObj instanceof String) {
                    eccInformation.setFieldValue(eccInformationField, fieldValueObj.toString().trim());
                }
            }
        });
    }
}