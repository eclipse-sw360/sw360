/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.users;

import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class UserConverter {

    private UserConverter() {}

    public static User fromThrift(org.eclipse.sw360.datahandler.thrift.users.User thrift) {
        if (thrift == null) {
            return null;
        }
        User pojo = new User();
        if (thrift.isSetCommentMadeDuringModerationRequest()) {
            pojo.setCommentMadeDuringModerationRequest(thrift.getCommentMadeDuringModerationRequest());
        }
        if (thrift.isSetDeactivated()) {
            pojo.setDeactivated(thrift.isDeactivated());
        }
        if (thrift.isSetDepartment()) {
            pojo.setDepartment(thrift.getDepartment());
        }
        if (thrift.isSetEmail()) {
            pojo.setEmail(thrift.getEmail());
        }
        if (thrift.isSetExternalid()) {
            pojo.setExternalid(thrift.getExternalid());
        }
        if (thrift.isSetFormerEmailAddresses()) {
            pojo.setFormerEmailAddresses(ThriftCollectionConverter.mapSet(thrift.getFormerEmailAddresses(), e -> e));
        }
        if (thrift.isSetFullname()) {
            pojo.setFullname(thrift.getFullname());
        }
        if (thrift.isSetGivenname()) {
            pojo.setGivenname(thrift.getGivenname());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLastname()) {
            pojo.setLastname(thrift.getLastname());
        }
        if (thrift.isSetMyProjectsPreferenceSelection()) {
            pojo.setMyProjectsPreferenceSelection(thrift.getMyProjectsPreferenceSelection());
        }
        if (thrift.isSetNotificationPreferences()) {
            pojo.setNotificationPreferences(thrift.getNotificationPreferences());
        }
        if (thrift.isSetOidcClientInfos()) {
            pojo.setOidcClientInfos(ThriftCollectionConverter.mapMap(thrift.getOidcClientInfos(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.users.ClientMetadataConverter.fromThrift(mapValue)));
        }
        if (thrift.isSetPassword()) {
            pojo.setPassword(thrift.getPassword());
        }
        if (thrift.isSetPrimaryRoles()) {
            pojo.setPrimaryRoles(ThriftCollectionConverter.mapList(thrift.getPrimaryRoles(), e -> e));
        }
        if (thrift.isSetRestApiTokens()) {
            pojo.setRestApiTokens(ThriftCollectionConverter.mapList(thrift.getRestApiTokens(), e -> org.eclipse.sw360.common.utils.converter.users.RestApiTokenConverter.fromThrift(e)));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetSecondaryDepartmentsAndRoles()) {
            pojo.setSecondaryDepartmentsAndRoles(ThriftCollectionConverter.mapMap(thrift.getSecondaryDepartmentsAndRoles(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> EnumConverter.fromThrift(e, org.eclipse.sw360.datahandler.services.users.UserGroup.class))));
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetUserGroup()) {
            pojo.setUserGroup(EnumConverter.fromThrift(thrift.getUserGroup(), org.eclipse.sw360.datahandler.services.users.UserGroup.class));
        }
        if (thrift.isSetWantsMailNotification()) {
            pojo.setWantsMailNotification(thrift.isWantsMailNotification());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.users.User toThrift(User pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.users.User thrift = new org.eclipse.sw360.datahandler.thrift.users.User();
        if (pojo.getCommentMadeDuringModerationRequest() != null) {
            thrift.setCommentMadeDuringModerationRequest(pojo.getCommentMadeDuringModerationRequest());
        }
        if (pojo.getDeactivated() != null) {
            thrift.setDeactivated(pojo.getDeactivated());
        }
        if (pojo.getDepartment() != null) {
            thrift.setDepartment(pojo.getDepartment());
        }
        if (pojo.getEmail() != null) {
            thrift.setEmail(pojo.getEmail());
        }
        if (pojo.getExternalid() != null) {
            thrift.setExternalid(pojo.getExternalid());
        }
        if (pojo.getFormerEmailAddresses() != null) {
            thrift.setFormerEmailAddresses(ThriftCollectionConverter.mapSet(pojo.getFormerEmailAddresses(), e -> e));
        }
        if (pojo.getFullname() != null) {
            thrift.setFullname(pojo.getFullname());
        }
        if (pojo.getGivenname() != null) {
            thrift.setGivenname(pojo.getGivenname());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLastname() != null) {
            thrift.setLastname(pojo.getLastname());
        }
        if (pojo.getMyProjectsPreferenceSelection() != null) {
            thrift.setMyProjectsPreferenceSelection(pojo.getMyProjectsPreferenceSelection());
        }
        if (pojo.getNotificationPreferences() != null) {
            thrift.setNotificationPreferences(pojo.getNotificationPreferences());
        }
        if (pojo.getOidcClientInfos() != null) {
            thrift.setOidcClientInfos(ThriftCollectionConverter.mapMap(pojo.getOidcClientInfos(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.users.ClientMetadataConverter.toThrift(mapValue)));
        }
        if (pojo.getPassword() != null) {
            thrift.setPassword(pojo.getPassword());
        }
        if (pojo.getPrimaryRoles() != null) {
            thrift.setPrimaryRoles(ThriftCollectionConverter.mapList(pojo.getPrimaryRoles(), e -> e));
        }
        if (pojo.getRestApiTokens() != null) {
            thrift.setRestApiTokens(ThriftCollectionConverter.mapList(pojo.getRestApiTokens(), e -> org.eclipse.sw360.common.utils.converter.users.RestApiTokenConverter.toThrift(e)));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getSecondaryDepartmentsAndRoles() != null) {
            thrift.setSecondaryDepartmentsAndRoles(ThriftCollectionConverter.mapMap(pojo.getSecondaryDepartmentsAndRoles(), mapKey -> mapKey, mapValue -> ThriftCollectionConverter.mapSet(mapValue, e -> EnumConverter.toThrift(e, org.eclipse.sw360.datahandler.thrift.users.UserGroup.class))));
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getUserGroup() != null) {
            thrift.setUserGroup(EnumConverter.toThrift(pojo.getUserGroup(), org.eclipse.sw360.datahandler.thrift.users.UserGroup.class));
        }
        if (pojo.getWantsMailNotification() != null) {
            thrift.setWantsMailNotification(pojo.getWantsMailNotification());
        }
        return thrift;
    }
}
