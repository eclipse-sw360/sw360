/*
 * Copyright Siemens AG, 2017.
 * Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.apache.thrift.TEnum;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.portal.common.PortalConstants;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

public class DisplayProjectStateBox extends SimpleTagSupport {

    private ProjectState state;

    public void setValue(ProjectState state) {
        this.state = state;
    }

    public String makeBox(ProjectState state) {
        String projectStateBackgroundColour = PortalConstants.PROJECT_STATE_INACTIVE__CSS;
        if( state == ProjectState.ACTIVE ) {
            projectStateBackgroundColour = PortalConstants.PROJECT_STATE_ACTIVE__CSS;
        }
        return "<div class=\"stateBox capsuleLeft capsuleRight " + projectStateBackgroundColour + "\" title=\"Project state: " + state + "\"> PS </div>";
    }


    public void doTag() throws JspException, IOException {
        getJspContext().getOut().print( makeBox( state ) );
    }
}
