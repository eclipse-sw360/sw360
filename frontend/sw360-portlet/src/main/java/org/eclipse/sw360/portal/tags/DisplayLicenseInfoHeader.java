/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.sw360.datahandler.thrift.projects.Project;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

public class DisplayLicenseInfoHeader extends SimpleTagSupport {

    private Project project;
    private String defaultText = "";

    public void setProject(Project project) {
        this.project = project;
    }

    public void setDefaultText(String defaultText) {
        this.defaultText = defaultText;
    }


    public void doTag() throws JspException, IOException {
        String output = project.isSetLicenseInfoHeaderText() ? StringEscapeUtils.escapeHtml(project.licenseInfoHeaderText) : StringEscapeUtils.escapeHtml(defaultText);

        getJspContext().getOut().print(output);
    }
}