/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.entitlement;

import com.google.common.collect.Maps;
import org.eclipse.sw360.common.utils.converter.users.UserConverter;
import org.eclipse.sw360.datahandler.moderation.ModerationClients;
import org.eclipse.sw360.datahandler.services.common.RequestStatus;
import org.eclipse.sw360.datahandler.services.common.SW360Exception;
import org.eclipse.sw360.datahandler.services.licenses.License;
import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.datahandler.services.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.eclipse.sw360.datahandler.common.CommonUtils.TMP_OBLIGATION_ID_PREFIX;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;

/**
 * Moderation for the license service (POJO License / Obligation).
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class LicenseModerator {

    private static final Logger log = LogManager.getLogger(LicenseModerator.class);

    public void notifyModeratorOnDelete(String documentId) {
        try {
            ModerationClients.get().deleteRequestsOnDocument(documentId);
        } catch (SW360Exception e) {
            log.error("Could not notify moderation client, that I delete document with id " + documentId, e);
        }
    }

    public List<ModerationRequest> getModerationRequestsForDocumentId(String documentId) {
        try {
            return ModerationClients.get().getModerationRequestByDocumentId(documentId);
        } catch (SW360Exception e) {
            log.error("Could not get moderations for Document " + documentId, e);
        }
        return Collections.emptyList();
    }

    public RequestStatus updateLicense(License license, User user) {
        try {
            ModerationClients.get().createLicenseRequest(license, UserConverter.fromThrift(user));
            return RequestStatus.SENT_TO_MODERATOR;
        } catch (SW360Exception e) {
            log.error("Could not moderate license " + license.getId() + " for User " + user.getEmail(), e);
            return RequestStatus.FAILURE;
        }
    }

    public License updateLicenseFromModerationRequest(License license,
                                                      License licenseAdditions,
                                                      License licenseDeletions,
                                                      String department) {
        Map<String, Obligation> actualTodoMap = Maps.uniqueIndex(nullToEmptyList(license.getObligations()), Obligation::getId);

        for (Obligation added : nullToEmptyList(licenseAdditions != null ? licenseAdditions.getObligations() : null)) {
            if (added.getId() == null) {
                log.error("Obligation id not set in licenseAdditions.");
                continue;
            }
            if (isTemporaryObligation(added)) {
                if (license.getObligations() == null) {
                    license.setObligations(new ArrayList<>());
                }
                license.getObligations().add(added);
            } else {
                Obligation actual = actualTodoMap.get(added.getId());
                if (actual != null && added.getWhitelist() != null && added.getWhitelist().contains(department)) {
                    if (actual.getWhitelist() == null) {
                        actual.setWhitelist(new HashSet<>());
                    }
                    actual.getWhitelist().add(department);
                }
            }
        }
        for (Obligation deleted : nullToEmptyList(licenseDeletions != null ? licenseDeletions.getObligations() : null)) {
            if (deleted.getId() == null) {
                log.error("Obligation id is not set in licenseDeletions.");
                continue;
            }
            Obligation actual = actualTodoMap.get(deleted.getId());
            if (actual == null) {
                log.info("Obligation from licenseDeletions does not exist (any more) in license.");
                continue;
            }
            if (deleted.getWhitelist() != null && deleted.getWhitelist().contains(department)) {
                if (actual.getWhitelist() != null && actual.getWhitelist().contains(department)) {
                    actual.getWhitelist().remove(department);
                }
            }
        }
        return license;
    }

    private static boolean isTemporaryObligation(Obligation oblig) {
        return oblig.getId() != null && oblig.getId().startsWith(TMP_OBLIGATION_ID_PREFIX);
    }
}
