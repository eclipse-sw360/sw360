/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import org.eclipse.sw360.datahandler.couchdb.DatabaseRepository;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.ektorp.DocumentOperationResult;

import java.util.*;

/**
 * @author johannes.najjar@tngtech.com
 */
public class RepositoryUtils {

    //This works with any repository
    public static RequestSummary doBulk(Collection<?> objects, User user, DatabaseRepository<?> repository){

        RequestSummary requestSummary =  new RequestSummary();
        if(PermissionUtils.isAdmin(user)) {
            // Prepare component for database
            final List<DocumentOperationResult> documentOperationResults = repository.executeBulk(objects);

            requestSummary.setTotalElements(objects.size() );
            requestSummary.setTotalAffectedElements(objects.size() - documentOperationResults.size());

            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        } else {
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
        return requestSummary;
    }

    public Set<String> searchByExternalIds(DatabaseRepository<?> repository, String queryName, Map<String, Set<String>> externalIds) {
        Set<String> searchIds = new HashSet<>();
        for (Iterator<Map.Entry<String, Set<String>>> it = externalIds.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Set<String>> externalId = it.next();
            Set<String> externalValues = externalId.getValue();
            if (externalValues.isEmpty() || (externalValues.size() == 1 && externalValues.iterator().next().isEmpty())) {
                searchIds.addAll(repository.queryForIdsOnlyComplexKey(queryName, externalId.getKey()));
                it.remove();
            }
        }

        if (!externalIds.isEmpty()) {
            searchIds.addAll(repository.queryForIdsAsComplexValues(queryName, externalIds));
        }

        return searchIds;
    }
}
