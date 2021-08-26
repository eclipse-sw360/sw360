/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.taglibs.standard.tag.common.core.OutSupport;

import javax.servlet.jsp.JspException;

import java.io.IOException;

import static org.eclipse.sw360.portal.tags.TagUtils.escapeAttributeValue;

/**
 * Util to display multiline strings also in javascript
 *
 * @author Daniele.Fognini@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 * @author thomas.maier@evosoft.com
 */
public class OutTag extends OutSupport {
    private boolean jsQuoting = false;
    private boolean stripNewlines = true;
    private boolean hashSet = false;
    private boolean bare = false;
    private Integer maxChar = -1;

    public OutTag() {
    }

    @Override
    public int doStartTag() throws JspException {
        if (value instanceof String) {
            boolean abbreviated = false;
            String candidate = (String) this.value;
            String originalValue = candidate;

            if (maxChar > 4) {
                candidate = StringUtils.abbreviate(candidate, maxChar);
                if (!originalValue.equals(candidate)) {
                    abbreviated = true;
                }
            }

            if (jsQuoting) {
                candidate = StringEscapeUtils.escapeJavaScript(candidate);
            }

            if (stripNewlines) {
                candidate = candidate.replaceAll("[\r\n]+", " ");
            }

            if (hashSet) {
                candidate = StringUtils.removeStart(candidate, "[");
                candidate = StringUtils.chop(candidate);
            }

            this.value = candidate;

            if (!bare && abbreviated) {
                try {
                    this.pageContext.getOut().write("<span title=\"" + prepareTitleAttribute(originalValue) + "\">");
                    int i = super.doStartTag();
                    this.pageContext.getOut().write("</span>");
                    return i;
                } catch (IOException e) {
                    throw new JspException(e.toString(), e);
                }
            }
        }

        return super.doStartTag();
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setDefault(String def) {
        this.def = def;
    }

    public void setJsQuoting(boolean jsQuoting) {
        this.jsQuoting = jsQuoting;
    }

    public void setMaxChar(Integer maxChar) {
        this.maxChar = maxChar;
    }

    public void setStripNewlines(boolean stripNewlines) {
        this.stripNewlines = stripNewlines;
    }

    public void setHashSet(boolean hashSet) {
        this.hashSet = hashSet;
    }

    public void setBare(boolean bare) { this.bare = bare; }

    private String prepareTitleAttribute(String value) {
        return escapeAttributeValue(value).replaceAll("[\r\n]+", "&#013;&#010;");
    }
}


