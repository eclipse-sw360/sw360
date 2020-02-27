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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
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

}