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

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.taglib.TagSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.sw360.datahandler.common.CommonUtils;

public class DisplayLicenseCollection extends TagSupport {
    private Long scopeGroupId;
    private Collection<String> licenseIds;
    private String icon;
    private String releaseId;
    private boolean main = true;
    private String title;

    public void setLicenseIds(Collection<String> licenseIds) {
        this.licenseIds = licenseIds;
    }
    public void setScopeGroupId(Long scopeGroupId) {
        if(scopeGroupId != null && scopeGroupId.longValue() != 0) {
            this.scopeGroupId = scopeGroupId;
        }
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }
    public void setMain(Boolean main) {
        this.main = main;
    }
    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }
    public String getTitle() {
        return title;
    }

    @Override
    public int doStartTag() throws JspException {
        Long scopeGroupIdAttribute = (Long) pageContext.getAttribute("scopeGroupId");
        if (scopeGroupIdAttribute != null && scopeGroupIdAttribute != 0 && (scopeGroupId == null || scopeGroupId == 0)) {
            this.scopeGroupId = scopeGroupIdAttribute;
        }
        try {
            JspWriter jspWriter = pageContext.getOut();
            if (CommonUtils.isNotEmpty(licenseIds)) {
                if (CommonUtils.isNullEmptyOrWhitespace(icon)) {
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
                } else {
                    List<String> licenseList = new ArrayList<>(licenseIds);
                    Collections.sort(licenseList, String.CASE_INSENSITIVE_ORDER);
                    final List<String> finalValueList = licenseList.stream().map(StringEscapeUtils::escapeXml).collect(Collectors.toList());
                    if (CommonUtils.isNullEmptyOrWhitespace(title)) {
                        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
                        final ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
                        title = LanguageUtil.get(resourceBundle, "view.file.list");
                    }
                    if (CommonUtils.isNotNullEmptyOrWhitespace(icon) && CommonUtils.isNotNullEmptyOrWhitespace(releaseId)) {
                        final String tag = main ? "-ml-" : "-ol-";
                        licenseList = IntStream.range(0, licenseList.size())
                            .mapToObj(i -> new StringBuilder(finalValueList.get(i)).append("&nbsp;<svg class='cursor lexicon-icon' data-tag='").append(releaseId).append(tag).append(i)
                            .append("'><title>").append(title).append("</title><use href='/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#").append(icon).append("'/></svg> ").toString())
                        .collect(Collectors.toList());
                    }
                    jspWriter.write(CommonUtils.NEW_LINE_JOINER.join(licenseList));
                }
            }
        } catch (IOException e) {
            throw new JspException("cannot write", e);
        }

        return SKIP_BODY;
    }
}
