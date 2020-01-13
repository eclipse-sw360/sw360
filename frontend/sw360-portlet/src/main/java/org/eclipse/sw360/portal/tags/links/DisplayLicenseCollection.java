/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
  * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.tags.links;

import com.liferay.taglib.TagSupport;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public class DisplayLicenseCollection extends TagSupport {
    private Long scopeGroupId;
    private Collection<String> licenseIds;

    public void setLicenseIds(Collection<String> licenseIds) {
        this.licenseIds = licenseIds;
    }
    public void setScopeGroupId(Long scopeGroupId) {
        if(scopeGroupId != null && scopeGroupId.longValue() != 0) {
            this.scopeGroupId = scopeGroupId;
        }
    }

    @Override
    public int doStartTag() throws JspException {
        Long scopeGroupIdAttribute = (Long) pageContext.getAttribute("scopeGroupId");
        if (scopeGroupIdAttribute != null && scopeGroupIdAttribute != 0 && (scopeGroupId == null || scopeGroupId == 0)) {
            this.scopeGroupId = scopeGroupIdAttribute;
        }
        try {
            JspWriter jspWriter = pageContext.getOut();
            if (licenseIds != null) {
                for (Iterator<String> iterator = licenseIds.iterator(); iterator.hasNext(); ) {
                    String licenseId = iterator.next();
                    DisplayLinkToLicense linkToLicense = new DisplayLinkToLicense();
                    linkToLicense.setPageContext(pageContext);
                    linkToLicense.setScopeGroupId(scopeGroupId);
                    linkToLicense.setLicenseId(licenseId);

                    linkToLicense.doStartTag();
                    linkToLicense.doEndTag();
                    if (iterator.hasNext()) {
                        jspWriter.write(", ");
                    }
                }
            }
        } catch (IOException e) {
            throw new JspException("cannot write", e);
        }

        return SKIP_BODY;
    }

}
