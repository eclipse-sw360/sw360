/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.licenses;

import org.eclipse.sw360.datahandler.services.licenses.Obligation;
import org.eclipse.sw360.common.utils.converter.common.EnumConverter;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ObligationConverter {

    private ObligationConverter() {}

    public static Obligation fromThrift(org.eclipse.sw360.datahandler.thrift.licenses.Obligation thrift) {
        if (thrift == null) {
            return null;
        }
        Obligation pojo = new Obligation();
        if (thrift.isSetAdditionalData()) {
            pojo.setAdditionalData(thrift.getAdditionalData());
        }
        if (thrift.isSetComments()) {
            pojo.setComments(thrift.getComments());
        }
        if (thrift.isSetCustomPropertyToValue()) {
            pojo.setCustomPropertyToValue(thrift.getCustomPropertyToValue());
        }
        if (thrift.isSetDevelopment()) {
            pojo.setDevelopment(thrift.isDevelopment());
        }
        if (thrift.isSetDevelopmentString()) {
            pojo.setDevelopmentString(thrift.getDevelopmentString());
        }
        if (thrift.isSetDistribution()) {
            pojo.setDistribution(thrift.isDistribution());
        }
        if (thrift.isSetDistributionString()) {
            pojo.setDistributionString(thrift.getDistributionString());
        }
        if (thrift.isSetExternalIds()) {
            pojo.setExternalIds(thrift.getExternalIds());
        }
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetNode()) {
            pojo.setNode(thrift.getNode());
        }
        if (thrift.isSetObligationLevel()) {
            pojo.setObligationLevel(EnumConverter.fromThrift(thrift.getObligationLevel(), org.eclipse.sw360.datahandler.services.licenses.ObligationLevel.class));
        }
        if (thrift.isSetObligationType()) {
            pojo.setObligationType(EnumConverter.fromThrift(thrift.getObligationType(), org.eclipse.sw360.datahandler.services.licenses.ObligationType.class));
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetText()) {
            pojo.setText(thrift.getText());
        }
        if (thrift.isSetTitle()) {
            pojo.setTitle(thrift.getTitle());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        if (thrift.isSetWhitelist()) {
            pojo.setWhitelist(ThriftCollectionConverter.mapSet(thrift.getWhitelist(), e -> e));
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.licenses.Obligation toThrift(Obligation pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.licenses.Obligation thrift = new org.eclipse.sw360.datahandler.thrift.licenses.Obligation();
        if (pojo.getAdditionalData() != null) {
            thrift.setAdditionalData(pojo.getAdditionalData());
        }
        if (pojo.getComments() != null) {
            thrift.setComments(pojo.getComments());
        }
        if (pojo.getCustomPropertyToValue() != null) {
            thrift.setCustomPropertyToValue(pojo.getCustomPropertyToValue());
        }
        if (pojo.getDevelopment() != null) {
            thrift.setDevelopment(pojo.getDevelopment());
        }
        if (pojo.getDevelopmentString() != null) {
            thrift.setDevelopmentString(pojo.getDevelopmentString());
        }
        if (pojo.getDistribution() != null) {
            thrift.setDistribution(pojo.getDistribution());
        }
        if (pojo.getDistributionString() != null) {
            thrift.setDistributionString(pojo.getDistributionString());
        }
        if (pojo.getExternalIds() != null) {
            thrift.setExternalIds(pojo.getExternalIds());
        }
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getNode() != null) {
            thrift.setNode(pojo.getNode());
        }
        if (pojo.getObligationLevel() != null) {
            thrift.setObligationLevel(EnumConverter.toThrift(pojo.getObligationLevel(), org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel.class));
        }
        if (pojo.getObligationType() != null) {
            thrift.setObligationType(EnumConverter.toThrift(pojo.getObligationType(), org.eclipse.sw360.datahandler.thrift.licenses.ObligationType.class));
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getText() != null) {
            thrift.setText(pojo.getText());
        }
        if (pojo.getTitle() != null) {
            thrift.setTitle(pojo.getTitle());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        if (pojo.getWhitelist() != null) {
            thrift.setWhitelist(ThriftCollectionConverter.mapSet(pojo.getWhitelist(), e -> e));
        }
        return thrift;
    }
}
