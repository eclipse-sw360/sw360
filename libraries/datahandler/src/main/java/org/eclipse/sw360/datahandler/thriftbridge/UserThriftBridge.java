/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.thriftbridge;

import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.services.users.UserGroup;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts between thrift and service-api {@link User} at Thrift {@code *Service.Iface}
 * adapter boundaries. Remove when all Iface adapters are deleted.
 */
public final class UserThriftBridge {

    private UserThriftBridge() {}

    public static User toPojo(org.eclipse.sw360.datahandler.thrift.users.User thrift) {
        if (thrift == null) {
            return null;
        }
        User pojo = new User();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetEmail()) {
            pojo.setEmail(thrift.getEmail());
        }
        if (thrift.isSetUserGroup()) {
            pojo.setUserGroup(UserGroup.valueOf(thrift.getUserGroup().name()));
        }
        if (thrift.isSetExternalid()) {
            pojo.setExternalid(thrift.getExternalid());
        }
        if (thrift.isSetFullname()) {
            pojo.setFullname(thrift.getFullname());
        }
        if (thrift.isSetGivenname()) {
            pojo.setGivenname(thrift.getGivenname());
        }
        if (thrift.isSetLastname()) {
            pojo.setLastname(thrift.getLastname());
        }
        if (thrift.isSetDepartment()) {
            pojo.setDepartment(thrift.getDepartment());
        }
        if (thrift.isSetWantsMailNotification()) {
            pojo.setWantsMailNotification(thrift.isWantsMailNotification());
        }
        if (thrift.isSetCommentMadeDuringModerationRequest()) {
            pojo.setCommentMadeDuringModerationRequest(thrift.getCommentMadeDuringModerationRequest());
        }
        if (thrift.isSetNotificationPreferences()) {
            pojo.setNotificationPreferences(thrift.getNotificationPreferences());
        }
        if (thrift.isSetFormerEmailAddresses()) {
            pojo.setFormerEmailAddresses(thrift.getFormerEmailAddresses());
        }
        if (thrift.isSetRestApiTokens()) {
            pojo.setRestApiTokens(thrift.getRestApiTokens() == null ? null : thrift.getRestApiTokens().stream()
                    .map(RestApiTokenThriftBridge::toPojo)
                    .collect(Collectors.toList()));
        }
        if (thrift.isSetMyProjectsPreferenceSelection()) {
            pojo.setMyProjectsPreferenceSelection(thrift.getMyProjectsPreferenceSelection());
        }
        if (thrift.isSetSecondaryDepartmentsAndRoles()) {
            pojo.setSecondaryDepartmentsAndRoles(convertSecondaryRoles(thrift.getSecondaryDepartmentsAndRoles()));
        }
        if (thrift.isSetPrimaryRoles()) {
            pojo.setPrimaryRoles(thrift.getPrimaryRoles());
        }
        if (thrift.isSetDeactivated()) {
            pojo.setDeactivated(thrift.isDeactivated());
        }
        if (thrift.isSetOidcClientInfos()) {
            pojo.setOidcClientInfos(thrift.getOidcClientInfos() == null ? null : thrift.getOidcClientInfos().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> ClientMetadataThriftBridge.toPojo(e.getValue()))));
        }
        if (thrift.isSetPassword()) {
            pojo.setPassword(thrift.getPassword());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.users.User toThrift(User pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.User thrift =
                new org.eclipse.sw360.datahandler.thrift.users.User();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getEmail() != null) {
            thrift.setEmail(pojo.getEmail());
        }
        if (pojo.getUserGroup() != null) {
            thrift.setUserGroup(
                    org.eclipse.sw360.datahandler.thrift.users.UserGroup.valueOf(pojo.getUserGroup().name()));
        }
        if (pojo.getExternalid() != null) {
            thrift.setExternalid(pojo.getExternalid());
        }
        if (pojo.getFullname() != null) {
            thrift.setFullname(pojo.getFullname());
        }
        if (pojo.getGivenname() != null) {
            thrift.setGivenname(pojo.getGivenname());
        }
        if (pojo.getLastname() != null) {
            thrift.setLastname(pojo.getLastname());
        }
        if (pojo.getDepartment() != null) {
            thrift.setDepartment(pojo.getDepartment());
        }
        if (pojo.getWantsMailNotification() != null) {
            thrift.setWantsMailNotification(pojo.getWantsMailNotification());
        }
        if (pojo.getCommentMadeDuringModerationRequest() != null) {
            thrift.setCommentMadeDuringModerationRequest(pojo.getCommentMadeDuringModerationRequest());
        }
        if (pojo.getNotificationPreferences() != null) {
            thrift.setNotificationPreferences(pojo.getNotificationPreferences());
        }
        if (pojo.getFormerEmailAddresses() != null) {
            thrift.setFormerEmailAddresses(pojo.getFormerEmailAddresses());
        }
        if (pojo.getRestApiTokens() != null) {
            thrift.setRestApiTokens(pojo.getRestApiTokens().stream()
                    .map(RestApiTokenThriftBridge::toThrift)
                    .collect(Collectors.toList()));
        }
        if (pojo.getMyProjectsPreferenceSelection() != null) {
            thrift.setMyProjectsPreferenceSelection(pojo.getMyProjectsPreferenceSelection());
        }
        if (pojo.getSecondaryDepartmentsAndRoles() != null) {
            thrift.setSecondaryDepartmentsAndRoles(pojo.getSecondaryDepartmentsAndRoles().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                            .map(g -> org.eclipse.sw360.datahandler.thrift.users.UserGroup.valueOf(g.name()))
                            .collect(Collectors.toSet()))));
        }
        if (pojo.getPrimaryRoles() != null) {
            thrift.setPrimaryRoles(pojo.getPrimaryRoles());
        }
        if (pojo.getDeactivated() != null) {
            thrift.setDeactivated(pojo.getDeactivated());
        }
        if (pojo.getOidcClientInfos() != null) {
            thrift.setOidcClientInfos(pojo.getOidcClientInfos().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> ClientMetadataThriftBridge.toThrift(e.getValue()))));
        }
        if (pojo.getPassword() != null) {
            thrift.setPassword(pojo.getPassword());
        }
        return thrift;
    }

    private static Map<String, Set<UserGroup>> convertSecondaryRoles(
            Map<String, Set<org.eclipse.sw360.datahandler.thrift.users.UserGroup>> thriftMap) {
        return thriftMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                        .map(g -> UserGroup.valueOf(g.name()))
                        .collect(Collectors.toSet())));
    }
}
