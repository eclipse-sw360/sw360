/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.tags.urlutils;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

/**
 * Helper class to render linked Releases
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class LinkedReleaseRenderer {

    private StringBuilder display;
    private String tableClasses;
    private String idPrefix;
    private User user;

    public LinkedReleaseRenderer(StringBuilder display, String tableClasses, String idPrefix, User user) {
        this.display = display;
        this.tableClasses = tableClasses;
        this.idPrefix = idPrefix;
        this.user = user;
    }


    public <T> void renderReleaseLinkList(StringBuilder display, Map<String, T> releaseRelationshipMap, Set<String> releaseIds, String msg, HttpServletRequest request) {
        if (releaseIds.isEmpty()) return;


        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        StringBuilder candidate = new StringBuilder();
        try {
            ComponentService.Iface componentClient = new ThriftClients().makeComponentClient();

            for (Release release : componentClient.getReleasesById(releaseIds, user)) {
                candidate.append(String.format("<tr><td>%s</td><td>%s</td></tr>", release.getName(), releaseRelationshipMap.get(release.getId())));
            }

        } catch (TException ignored) {
        }

        String tableContent = candidate.toString();
        if (!tableContent.isEmpty()) {

            display.append(String.format("<table class=\"%s\" id=\"%s%s\" >", tableClasses, idPrefix, msg));
            display.append(String.format("<thead><tr><th colspan=\"2\">%s</th></tr><tr><th>"+LanguageUtil.get(resourceBundle,"release.name")+"</th><th>"+LanguageUtil.get(resourceBundle,"release.relationship")+"</th></tr></thead><tbody>", msg));
            display.append(tableContent);
            display.append("</tbody></table>");
        }
    }

    public <T> void renderReleaseLinkListCompare(StringBuilder display, Map<String,T> oldReleaseRelationshipMap, Map<String, T> deleteReleaseRelationshipMap, Map<String, T> updateReleaseRelationshipMap, Set<String> releaseIds, HttpServletRequest request) {
        if (releaseIds.isEmpty()) return;


        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        StringBuilder candidate = new StringBuilder();
        try {
            ComponentService.Iface componentClient = new ThriftClients().makeComponentClient();

            final HashSet<String> changedIds = new HashSet<>();

            for (String releaseId : releaseIds) {
                T oldReleaseRelationship = oldReleaseRelationshipMap.get(releaseId);
                T updateReleaseRelationship = updateReleaseRelationshipMap.get(releaseId);

                if (!oldReleaseRelationship.equals(updateReleaseRelationship)) {
                    changedIds.add(releaseId);
                }
            }

            for (Release release : componentClient.getReleasesById(changedIds, user)) {
                String releaseId = release.getId();
                T oldReleaseRelationship = oldReleaseRelationshipMap.get(releaseId);
                T deleteReleaseRelationship = deleteReleaseRelationshipMap.get(releaseId);
                T updateReleaseRelationship = updateReleaseRelationshipMap.get(releaseId);
                candidate.append(String.format("<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>", release.getName(), oldReleaseRelationship, deleteReleaseRelationship, updateReleaseRelationship));
            }

        } catch (TException ignored) {
        }

        String tableContent = candidate.toString();
        if (!tableContent.isEmpty()) {
            display.append(String.format("<table class=\"%s\" id=\"%sUpdated\" >", tableClasses, idPrefix));
            display.append("<thead><tr><th colspan=\"4\">"+LanguageUtil.get(resourceBundle,"updated.release.links")+"</th></tr><tr><th>"+LanguageUtil.get(resourceBundle,"release.name")+"</th><th>"+LanguageUtil.get(resourceBundle,"current.release.relationship")+"</th><th>Deleted Release relationship</th><th>Suggested release relationship</th></tr></thead><tbody>");
            display.append(tableContent);
            display.append("</tbody></table>");
        }
    }
}
