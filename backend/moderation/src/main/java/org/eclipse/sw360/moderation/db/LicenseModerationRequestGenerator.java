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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;

import com.google.common.collect.Maps;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;

/**
 * Class for comparing a document with its counterpart in the database
 * Writes the difference (= additions and deletions) to the moderation request
 *
 * @author birgit.heydenreicht@tngtech.com
 */
public class LicenseModerationRequestGenerator extends ModerationRequestGenerator<License> {

    @Override
    public ModerationRequest setAdditionsAndDeletions(ModerationRequest request, License updateLicense, License actualLicense) {
        updateDocument = updateLicense;
        actualDocument = actualLicense;

        documentAdditions = new License();
        documentDeletions = new License();
        documentAdditions.setFullname(updateLicense.getFullname());
        documentAdditions.setId(actualLicense.getId());
        documentDeletions.setFullname(actualLicense.getFullname());
        documentDeletions.setId(actualLicense.getId());

        Map<String, Obligation> actualTodos = Maps.uniqueIndex(nullToEmptyList(actualLicense.getObligations()), Obligation::getId);

        for (Obligation updateTodo : nullToEmptyList(updateLicense.getObligations())) {
            if (!actualTodos.containsKey(updateTodo.getId())) {
                if (documentAdditions.getObligations() == null) {
                    documentAdditions.setObligations(new ArrayList<>());
                }
                documentAdditions.getObligations().add(updateTodo);
            } else {
                Obligation actualTodo = actualTodos.get(updateTodo.getId());
                Set<String> actualWhitelist = actualTodo != null && actualTodo.getWhitelist() != null
                        ? actualTodo.getWhitelist() : new HashSet<>();
                Set<String> updateWhitelist = updateTodo.getWhitelist() != null ? updateTodo.getWhitelist() : new HashSet<>();
                String departement = request.getRequestingUserDepartment();
                if (updateWhitelist.contains(departement) && !actualWhitelist.contains(departement)) {
                    if (documentAdditions.getObligations() == null) {
                        documentAdditions.setObligations(new ArrayList<>());
                    }
                    documentAdditions.getObligations().add(updateTodo);
                } else if (!updateWhitelist.contains(departement) && actualWhitelist.contains(departement)) {
                    if (documentDeletions.getObligations() == null) {
                        documentDeletions.setObligations(new ArrayList<>());
                    }
                    if (actualTodo != null) {
                        documentDeletions.getObligations().add(actualTodo);
                    }
                }
            }
        }

        request.setLicenseAdditions(documentAdditions);
        request.setLicenseDeletions(documentDeletions);
        return request;
    }
}
