/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This displays a list
 *
 * @author Cedric.Bodet@tngtech.com Johannes.Najjar@tngtech.com
 */
public class DisplayCollection extends SimpleTagSupport {

    private Collection<String> value;
    private Collection<String> autoFillValue;

    public void setValue(Collection<String> value) {
        this.value = value;
    }

    public void setAutoFillValue(Collection<String> autoFillValue) {
        this.autoFillValue = autoFillValue;
    }

    public void doTag() throws JspException, IOException {
        Collection<String> fullValue;

        if (value == null)
            fullValue = autoFillValue;
        else {
            fullValue = value;
        }


        if (null != fullValue && !fullValue.isEmpty()) {
            List<String> valueList = new ArrayList<>(fullValue);
            Collections.sort(valueList, String.CASE_INSENSITIVE_ORDER);
            valueList = valueList.stream().map(StringEscapeUtils::escapeXml)
                    .collect(Collectors.toList());
            getJspContext().getOut().print(CommonUtils.COMMA_JOINER.join(valueList));
        }
    }
}
