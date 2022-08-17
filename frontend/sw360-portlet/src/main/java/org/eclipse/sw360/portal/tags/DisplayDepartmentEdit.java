/**
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.tags;

import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

/**
 * This displays a department field that can be used as extension point for a department
 * search dialog via onclick listeners (see also
 * src/main/webapp/js/components/includes/departments/searchDepartment.js)
 */
public class DisplayDepartmentEdit extends NameSpaceAwareTag {

    private String namespace;
    private String id;
    private String departmentId = "";
    private String label = "";


    public int doStartTag() throws JspException {
        JspWriter jspWriter = pageContext.getOut();

        namespace = getNamespace();
        StringBuilder display = new StringBuilder();
        try {
            if (StringUtils.isNotEmpty(departmentId)) {
            	printFullDepartment(display);
            }else {
            	printEmptyDepartment(display);
            }
            jspWriter.print(display.toString());
        } catch (Exception e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private void printEmptyDepartment(StringBuilder display) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());

        display.append("<div class=\"form-group\">");
        printLabel(display);
        display.append(String.format("<input type=\"hidden\" value=\"\"  id=\"%s\" name=\"%s%s\"/>", id, namespace, id))
                .append("<div class=\"form-group has-feedback\">")
                .append(String.format(
                        "<input type=\"text\" class=\"form-control edit-department clickable\" placeholder=\"" + LanguageUtil.get(resourceBundle, "click.to.set.department") + "\" id=\"%sDisplay\" required=\"\"/>",
                        id))
                .append("<span class=\"glyphicon glyphicon-remove-circle form-control-feedback clearSelection\" id=\"clearDepartment\"/>")
                .append("</div>");
        display.append("</div>");
    }

    private void printFullDepartment(StringBuilder display) {
        display.append("<div class=\"form-group\">");
        printLabel(display);
        display.append(String.format("<input type=\"hidden\" value=\"%s\"  id=\"%s\" name=\"%s%s\"/>", departmentId, id, namespace, id))
                .append("<div class=\"form-group has-feedback\">")
                .append(String.format(
                        "<input type=\"text\" class=\"form-control edit-department clickable\" value=\"%s\" id=\"%sDisplay\" required=\"\"/>",
                        departmentId, id))
                .append("<span class=\"glyphicon glyphicon-remove-circle form-control-feedback clearSelection\" id=\"clearDepartment\"/>")
                .append("</div>");
        display.append("</div>");
    }

    private void printLabel(StringBuilder display) {
    	HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());

        if (StringUtils.isNotEmpty(label)) {
            display.append(
                    String.format("<label class=\"mandatory\" for=\"group\" >%s</label>", LanguageUtil.get(resourceBundle,label)));
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
