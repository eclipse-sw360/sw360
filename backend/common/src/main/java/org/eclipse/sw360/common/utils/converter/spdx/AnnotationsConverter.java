/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.common.utils.converter.spdx;

import org.eclipse.sw360.datahandler.services.spdx.Annotations;

public final class AnnotationsConverter {

    private AnnotationsConverter() {}

    public static Annotations fromThrift(org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations thrift) {
        if (thrift == null) {
            return null;
        }
        Annotations pojo = new Annotations();
        if (thrift.isSetAnnotationComment()) {
            pojo.setAnnotationComment(thrift.getAnnotationComment());
        }
        if (thrift.isSetAnnotationDate()) {
            pojo.setAnnotationDate(thrift.getAnnotationDate());
        }
        if (thrift.isSetAnnotationType()) {
            pojo.setAnnotationType(thrift.getAnnotationType());
        }
        if (thrift.isSetAnnotator()) {
            pojo.setAnnotator(thrift.getAnnotator());
        }
        if (thrift.isSetIndex()) {
            pojo.setIndex(thrift.getIndex());
        }
        if (thrift.isSetSpdxIdRef()) {
            pojo.setSpdxIdRef(thrift.getSpdxIdRef());
        }
        return pojo;
    }

    public static org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations toThrift(Annotations pojo) {
        if (pojo == null) {
            return null;
        }
        org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations thrift = new org.eclipse.sw360.datahandler.thrift.spdx.annotations.Annotations();
        if (pojo.getAnnotationComment() != null) {
            thrift.setAnnotationComment(pojo.getAnnotationComment());
        }
        if (pojo.getAnnotationDate() != null) {
            thrift.setAnnotationDate(pojo.getAnnotationDate());
        }
        if (pojo.getAnnotationType() != null) {
            thrift.setAnnotationType(pojo.getAnnotationType());
        }
        if (pojo.getAnnotator() != null) {
            thrift.setAnnotator(pojo.getAnnotator());
        }
        if (pojo.getIndex() != null) {
            thrift.setIndex(pojo.getIndex());
        }
        if (pojo.getSpdxIdRef() != null) {
            thrift.setSpdxIdRef(pojo.getSpdxIdRef());
        }
        return thrift;
    }
}
