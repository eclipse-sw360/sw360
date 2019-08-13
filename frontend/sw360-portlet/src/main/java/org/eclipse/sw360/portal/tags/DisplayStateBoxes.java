/*
 * Copyright Siemens AG, 2017, 2019.
 * Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectState;
import org.eclipse.sw360.portal.common.PortalConstants;


import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;

import static org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState.*;

public class DisplayStateBoxes extends SimpleTagSupport {

    private Project project;

    public void setProject(Project project) {
        this.project = project;
    }

    public String makeBoxes() {
        // Add project state color to boxes if the project is active
        //   (default color is grey)
        String projectStateBackgroundColour = PortalConstants.PROJECT_STATE_INACTIVE__CSS;
        if( project.isSetState() && project.state == ProjectState.ACTIVE ) { // -> green
            projectStateBackgroundColour = PortalConstants.PROJECT_STATE_ACTIVE__CSS;
        }

        // Add clearingstate color to boxes in projects list
        //   (default color is grey)
        String clearingStateBackgroundColour = PortalConstants.CLEARING_STATE_UNKNOWN__CSS;

        if(project.isSetClearingState()) {
            switch(project.clearingState) {
                case CLOSED: // -> green
                    clearingStateBackgroundColour = PortalConstants.CLEARING_STATE_CLOSED__CSS;
                    break;
                case IN_PROGRESS: // -> yellow
                    clearingStateBackgroundColour = PortalConstants.CLEARING_STATE_INPROGRESS__CSS;
                    break;
                case OPEN: // -> red
                    clearingStateBackgroundColour = PortalConstants.CLEARING_STATE_OPEN__CSS;
                    break;
            }
        }

        String box_PS = "<div class=\"stateBox " + projectStateBackgroundColour + " capsuleLeft \" title=\"Project state: " + ThriftEnumUtils.enumToString(project.state) + "\"> PS </div>";
        String box_CS = "<div class=\"stateBox capsuleRight " + clearingStateBackgroundColour + "\" title=\"Project clearing state: " + ThriftEnumUtils.enumToString(project.clearingState) + "\"> CS </div>";

        return box_PS+box_CS;
    }


    public void doTag() throws JspException, IOException {
        getJspContext().getOut().print( makeBoxes() );
    }
}
