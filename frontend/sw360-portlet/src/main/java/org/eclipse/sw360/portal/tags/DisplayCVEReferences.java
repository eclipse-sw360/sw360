/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This displays a set of CVEReferences
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class DisplayCVEReferences extends SimpleTagSupport {

    private Set<CVEReference> value;
    private Set<CVEReference> autoFillValue;

    public void setValue(Set<CVEReference> value) {
        this.value = value;
    }
    public void setAutoFillValue(Set<CVEReference> autoFillValue) {
        this.autoFillValue = autoFillValue;
    }

    public void doTag() throws JspException, IOException {
        Set<CVEReference> fullValue;

        if (value == null) {
            fullValue = autoFillValue;
        } else {
            fullValue = value;
        }

        if (null != fullValue && !fullValue.isEmpty()) {
            String result = String.join(",", fullValue.stream().map(this::toString).collect(Collectors.toList()));
            getJspContext().getOut().print(result);
        }
    }

    private String toString(CVEReference reference){
        return "CVE-" + reference.getYear() + "-" + reference.getNumber();
    }
}
