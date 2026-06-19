/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.projects;

import org.eclipse.sw360.datahandler.services.projects.ClearingRequest;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ClearingRequestConverter {

    private ClearingRequestConverter() {}

    public static ClearingRequest fromThrift(org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest thrift) {
        if (thrift == null) {
            return null;
        }
        ClearingRequest pojo = new ClearingRequest();
        if (thrift.isSetAgreedClearingDate()) {
            pojo.setAgreedClearingDate(thrift.getAgreedClearingDate());
        }
        if (thrift.isSetClearingSize()) {
            pojo.setClearingSize(EnumConverter.fromThrift(thrift.getClearingSize(), org.eclipse.sw360.datahandler.services.common.ClearingRequestSize.class));
        }
        if (thrift.isSetClearingState()) {
            pojo.setClearingState(EnumConverter.fromThrift(thrift.getClearingState(), org.eclipse.sw360.datahandler.services.common.ClearingRequestState.class));
        }
        if (thrift.isSetClearingTeam()) {
            pojo.setClearingTeam(thrift.getClearingTeam());
        }
        if (thrift.isSetClearingType()) {
            pojo.setClearingType(EnumConverter.fromThrift(thrift.getClearingType(), org.eclipse.sw360.datahandler.services.common.ClearingRequestType.class));
        }
        if (thrift.isSetComments()) {
            pojo.setComments(ThriftCollectionConverter.mapList(thrift.getComments(), e -> org.eclipse.sw360.common.utils.converter.common.CommentConverter.fromThrift(e)));
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetModifiedOn()) {
            pojo.setModifiedOn(thrift.getModifiedOn());
        }
        if (thrift.isSetPriority()) {
            pojo.setPriority(EnumConverter.fromThrift(thrift.getPriority(), org.eclipse.sw360.datahandler.services.common.ClearingRequestPriority.class));
        }
        if (thrift.isSetProjectBU()) {
            pojo.setProjectBU(thrift.getProjectBU());
        }
        if (thrift.isSetProjectId()) {
            pojo.setProjectId(thrift.getProjectId());
        }
        if (thrift.isSetReOpenOn()) {
            pojo.setReOpenOn(ThriftCollectionConverter.mapList(thrift.getReOpenOn(), e -> e));
        }
        if (thrift.isSetRequestedClearingDate()) {
            pojo.setRequestedClearingDate(thrift.getRequestedClearingDate());
        }
        if (thrift.isSetRequestingUser()) {
            pojo.setRequestingUser(thrift.getRequestingUser());
        }
        if (thrift.isSetRequestingUserComment()) {
            pojo.setRequestingUserComment(thrift.getRequestingUserComment());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetTimestamp()) {
            pojo.setTimestamp(thrift.getTimestamp());
        }
        if (thrift.isSetTimestampOfDecision()) {
            pojo.setTimestampOfDecision(thrift.getTimestampOfDecision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest toThrift(ClearingRequest pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest thrift = new org.eclipse.sw360.datahandler.thrift.projects.ClearingRequest();
        if (pojo.getAgreedClearingDate() != null) {
            thrift.setAgreedClearingDate(pojo.getAgreedClearingDate());
        }
        if (pojo.getClearingSize() != null) {
            thrift.setClearingSize(EnumConverter.toThrift(pojo.getClearingSize(), org.eclipse.sw360.datahandler.thrift.ClearingRequestSize.class));
        }
        if (pojo.getClearingState() != null) {
            thrift.setClearingState(EnumConverter.toThrift(pojo.getClearingState(), org.eclipse.sw360.datahandler.thrift.ClearingRequestState.class));
        }
        if (pojo.getClearingTeam() != null) {
            thrift.setClearingTeam(pojo.getClearingTeam());
        }
        if (pojo.getClearingType() != null) {
            thrift.setClearingType(EnumConverter.toThrift(pojo.getClearingType(), org.eclipse.sw360.datahandler.thrift.ClearingRequestType.class));
        }
        if (pojo.getComments() != null) {
            thrift.setComments(ThriftCollectionConverter.mapList(pojo.getComments(), e -> org.eclipse.sw360.common.utils.converter.common.CommentConverter.toThrift(e)));
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getModifiedOn() != null) {
            thrift.setModifiedOn(pojo.getModifiedOn());
        }
        if (pojo.getPriority() != null) {
            thrift.setPriority(EnumConverter.toThrift(pojo.getPriority(), org.eclipse.sw360.datahandler.thrift.ClearingRequestPriority.class));
        }
        if (pojo.getProjectBU() != null) {
            thrift.setProjectBU(pojo.getProjectBU());
        }
        if (pojo.getProjectId() != null) {
            thrift.setProjectId(pojo.getProjectId());
        }
        if (pojo.getReOpenOn() != null) {
            thrift.setReOpenOn(ThriftCollectionConverter.mapList(pojo.getReOpenOn(), e -> e));
        }
        if (pojo.getRequestedClearingDate() != null) {
            thrift.setRequestedClearingDate(pojo.getRequestedClearingDate());
        }
        if (pojo.getRequestingUser() != null) {
            thrift.setRequestingUser(pojo.getRequestingUser());
        }
        if (pojo.getRequestingUserComment() != null) {
            thrift.setRequestingUserComment(pojo.getRequestingUserComment());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getTimestamp() != null) {
            thrift.setTimestamp(pojo.getTimestamp());
        }
        if (pojo.getTimestampOfDecision() != null) {
            thrift.setTimestampOfDecision(pojo.getTimestampOfDecision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
