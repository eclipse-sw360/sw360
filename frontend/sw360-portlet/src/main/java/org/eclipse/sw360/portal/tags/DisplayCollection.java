/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.CommonUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
            getJspContext().getOut().print(CommonUtils.COMMA_JOINER.join(valueList));
        }
    }
}
