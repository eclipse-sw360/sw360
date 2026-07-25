/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.common;

import org.eclipse.sw360.datahandler.services.common.Source;

/**
 * Maps thrift {@code Source} (a union) to/from the service-api POJO.
 * Only the currently-set union arm may be read; Jackson {@code convertValue} is unsafe here.
 */
public final class SourceConverter {

    private SourceConverter() {}

    public static Source fromThrift(org.eclipse.sw360.datahandler.thrift.Source thrift) {
        if (thrift == null) {
            return null;
        }
        Source pojo = new Source();
        if (thrift.isSetProjectId()) {
            pojo.setProjectId(thrift.getProjectId());
        } else if (thrift.isSetComponentId()) {
            pojo.setComponentId(thrift.getComponentId());
        } else if (thrift.isSetReleaseId()) {
            pojo.setReleaseId(thrift.getReleaseId());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.Source toThrift(Source pojo) {
        if (pojo == null) {
            return null;
        }
        if (pojo.getProjectId() != null) {
            return org.eclipse.sw360.datahandler.thrift.Source.projectId(pojo.getProjectId());
        }
        if (pojo.getComponentId() != null) {
            return org.eclipse.sw360.datahandler.thrift.Source.componentId(pojo.getComponentId());
        }
        if (pojo.getReleaseId() != null) {
            return org.eclipse.sw360.datahandler.thrift.Source.releaseId(pojo.getReleaseId());
        }
        return new org.eclipse.sw360.datahandler.thrift.Source();
    }
}
