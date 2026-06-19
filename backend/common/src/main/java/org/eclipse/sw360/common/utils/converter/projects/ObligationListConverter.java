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

import org.eclipse.sw360.datahandler.services.projects.ObligationList;
import org.eclipse.sw360.common.utils.converter.common.ThriftCollectionConverter;

public final class ObligationListConverter {

    private ObligationListConverter() {}

    public static ObligationList fromThrift(org.eclipse.sw360.datahandler.thrift.projects.ObligationList thrift) {
        if (thrift == null) {
            return null;
        }
        ObligationList pojo = new ObligationList();
        if (thrift.isSetId()) {
            pojo.setId(thrift.getId());
        }
        if (thrift.isSetLinkedObligationStatus()) {
            pojo.setLinkedObligationStatus(ThriftCollectionConverter.mapMap(thrift.getLinkedObligationStatus(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter.fromThrift(mapValue)));
        }
        if (thrift.isSetProjectId()) {
            pojo.setProjectId(thrift.getProjectId());
        }
        if (thrift.isSetRevision()) {
            pojo.setRevision(thrift.getRevision());
        }
        if (thrift.isSetType()) {
            pojo.setType(thrift.getType());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.projects.ObligationList toThrift(ObligationList pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.projects.ObligationList thrift = new org.eclipse.sw360.datahandler.thrift.projects.ObligationList();
        if (pojo.getId() != null) {
            thrift.setId(pojo.getId());
        }
        if (pojo.getLinkedObligationStatus() != null) {
            thrift.setLinkedObligationStatus(ThriftCollectionConverter.mapMap(pojo.getLinkedObligationStatus(), mapKey -> mapKey, mapValue -> org.eclipse.sw360.common.utils.converter.projects.ObligationStatusInfoConverter.toThrift(mapValue)));
        }
        if (pojo.getProjectId() != null) {
            thrift.setProjectId(pojo.getProjectId());
        }
        if (pojo.getRevision() != null) {
            thrift.setRevision(pojo.getRevision());
        }
        if (pojo.getType() != null) {
            thrift.setType(pojo.getType());
        }
        return thrift;
    }
}
