/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 * With modifications by Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.moderation.db;

import com.google.common.collect.Maps;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;

import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.uniqueIndex;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class LicenseModerationRequestGenerator extends ModerationRequestGenerator<License._Fields, License> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, License updateLicense, License actualLicense){
        updateDocument = updateLicense;
        actualDocument = actualLicense;

        documentAdditions = new License();
        documentDeletions = new License();
        //required fields:
        documentAdditions.setFullname(updateLicense.getFullname());
        documentAdditions.setId(actualLicense.getId());
        documentDeletions.setFullname(actualLicense.getFullname());
        documentDeletions.setId(actualLicense.getId());

        Map<String, Obligation> actualTodos = Maps.uniqueIndex(nullToEmptyList(actualLicense.getObligations()), Obligation::getId);

        for (Obligation updateTodo : updateLicense.getObligations()) {
            if(!actualTodos.containsKey(updateTodo.getId())){
                if(!documentAdditions.isSetObligations()) {
                    documentAdditions.setObligations(new ArrayList<>());
                }
                documentAdditions.getObligations().add(updateTodo);
            } else {
                Obligation actualTodo = actualTodos.get(updateTodo.getId());
                Set<String> actualWhitelist = actualTodo.whitelist != null ? actualTodo.whitelist : new HashSet<String>();
                Set<String> updateWhitelist = updateTodo.whitelist != null ? updateTodo.whitelist : new HashSet<String>();
                String departement = request.getRequestingUserDepartment();
                if(updateWhitelist.contains(departement) && !actualWhitelist.contains(departement)){
                    if(!documentAdditions.isSetObligations()) {
                        documentAdditions.setObligations(new ArrayList<>());
                    }
                    documentAdditions.getObligations().add(updateTodo);
                } else if (!updateWhitelist.contains(departement) && actualWhitelist.contains(departement)) {
                    if(!documentDeletions.isSetObligations()) {
                        documentDeletions.setObligations(new ArrayList<>());
                    }
                    documentDeletions.getObligations().add(actualTodo);
                }
            }
        }

        request.setLicenseAdditions(documentAdditions);
        request.setLicenseDeletions(documentDeletions);
        return request;
    }
}
