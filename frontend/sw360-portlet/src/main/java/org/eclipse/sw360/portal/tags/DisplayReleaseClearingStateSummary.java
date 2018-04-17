
/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

/**
 * This wraps the ReleaseClearingStateSummary
 *
 * @author birgit.heydenreich@tngtech.com
 * @author alex.borodin@evosoft.com
 */

public class DisplayReleaseClearingStateSummary extends SimpleTagSupport {

    private ReleaseClearingStateSummary releaseClearingStateSummary;

    public void setReleaseClearingStateSummary(ReleaseClearingStateSummary releaseClearingStateSummary) {
        this.releaseClearingStateSummary = releaseClearingStateSummary;
    }

    public void doTag() throws JspException, IOException {
        String releaseCounts;
        if (releaseClearingStateSummary == null) {
            releaseCounts = "not available";
        } else {
            releaseCounts = releaseClearingStateSummary.newRelease + " " + releaseClearingStateSummary.underClearing + " " + releaseClearingStateSummary.underClearingByProjectTeam + " " + releaseClearingStateSummary.reportAvailable + " " + releaseClearingStateSummary.approved;
        }

        String s = "<span title=\"new release, under clearing, under clearing by the project clearing team, report available, approved\">" + releaseCounts + "</span>";
        getJspContext().getOut().print(s);

    }

}

