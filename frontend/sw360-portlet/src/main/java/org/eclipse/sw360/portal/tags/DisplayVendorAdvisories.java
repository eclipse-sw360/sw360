/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VendorAdvisory;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Set;

/**
 * This displays a set of CVEReferences
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class DisplayVendorAdvisories extends SimpleTagSupport {

    private Set<VendorAdvisory> value;
    private Set<VendorAdvisory> autoFillValue;

    public void setValue(Set<VendorAdvisory> value) {
        this.value = value;
    }
    public void setAutoFillValue(Set<VendorAdvisory> autoFillValue) {
        this.autoFillValue = autoFillValue;
    }

    public void doTag() throws JspException, IOException {
        Set<VendorAdvisory> fullValue;

        if (value == null)
            fullValue = autoFillValue;
        else {
            fullValue = value;
        }

        if (null != fullValue && !fullValue.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("<ul>");
            fullValue.stream().forEach(a -> sb.append("<li>"+toString(a)+"</li>"));
            sb.append("</ul>");
            getJspContext().getOut().print(sb.toString());
        }
    }

    private String toString(VendorAdvisory advisory){
        return "<b>vendor: </b>"+ advisory.getVendor()
                +", <b>name: </b>"+ advisory.getName()
                +", <b>url: </b>"+ advisory.getUrl()+"<br/>";
    }
}
