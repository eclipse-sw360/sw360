/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * With modifications by Siemens AG, 2018.
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

import org.eclipse.sw360.datahandler.thrift.licenseinfo.OutputFormatInfo;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Collection;

/**
 * @author birgit.heydenreich@tngtech.com
 */
public class DisplayOutputFormats extends SimpleTagSupport {

    private Collection<OutputFormatInfo> options;
    private String selected;

    public void setOptions(Collection<OutputFormatInfo> options) throws JspException {
        this.options = options;
    }

    public void setSelected(String selected) {
        this.selected = selected;
    }

    public void doTag() throws JspException, IOException {
        writeOptions(options);
    }

    private void writeOptions(Collection<OutputFormatInfo> options) throws IOException {
        JspWriter jspWriter = getJspContext().getOut();
        boolean isChecked=true;
        for (OutputFormatInfo option : options) {
            String optionDescription = option.getDescription();
            String optionValue = option.getGeneratorClassName() + "::" + option.getVariant();
            String checked=isChecked?"checked":"";
            jspWriter.write(String.format(
                    ("<div class=\"radio form-check\"><label><input type=\"radio\" name=\"outputFormat\" value=\"%s\" "+checked+">%s</label></div>"),
                    optionValue, optionDescription));
            isChecked=false;
        }
    }
}
