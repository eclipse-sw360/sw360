/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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