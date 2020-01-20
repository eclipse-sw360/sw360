/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags.links;

import org.apache.taglibs.standard.tag.common.core.OutSupport;

import javax.servlet.jsp.JspException;
import java.io.IOException;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DisplayLink extends DisplayLinkAbstract {
    private String target;
    private String text;

    public void setTarget(String target) {
        this.target = target;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    protected void writeUrl() throws JspException {
        try {
            OutSupport.out(pageContext, true, target);
        } catch (IOException e) {
            throw new JspException("cannot write", e);
        }
    }

    @Override
    protected String getTextDisplay() {
        return text != null ? text : target;
    }
}
