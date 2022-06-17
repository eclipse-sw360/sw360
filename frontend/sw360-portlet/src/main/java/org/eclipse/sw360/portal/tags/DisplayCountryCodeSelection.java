/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import java.io.IOException;
import java.util.*;

public class DisplayCountryCodeSelection extends SimpleTagSupport {

    private String selected;
    private String preferredCountryCodes;
    private Map<String, Locale> countryCodeMap;

    public void setSelected(String selected) {
        this.selected = selected;
    }

    public void setPreferredCountryCodes(String preferredCountryCodes) {
        this.preferredCountryCodes = preferredCountryCodes;
    }

    public void doTag() throws JspException, IOException {
        populateCountryCodeMap();
        writeOptions(selected, preferredCountryCodes);
    }

    private void writeOptions(String selected, String preferredCountryCodes) throws IOException {
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());
        JspWriter jspWriter = getJspContext().getOut();

        if (selected == null || selected.isEmpty()) {
            jspWriter.write("<option value=\"\">"+LanguageUtil.get(resourceBundle,"select.a.country")+"</option>");
        }

        List<String> validCountryCodeList = applyCountryCodes(preferredCountryCodes);
        if (!validCountryCodeList.isEmpty()) {
            for (String countryCode : validCountryCodeList) {
                writeOption(jspWriter, countryCodeMap.get(countryCode));
            }
            jspWriter.write("<option disabled>───────────────</option>");
        }

        for (Locale locale : countryCodeMap.values()) {
            if (!validCountryCodeList.contains(locale.getCountry())) {
                writeOption(jspWriter, locale);
            }
        }
    }

    private void writeOption(JspWriter jspWriter, Locale locale) throws IOException {
        boolean selected = locale.getCountry().equalsIgnoreCase(this.selected);
        jspWriter.write(String.format(
                "<option value=\"%s\" class=\"textlabel stackedLabel\" " +
                        (selected ? "selected=\"selected\" " : "") + ">%s</option>",
                locale.getCountry(), locale.getDisplayCountry()));
    }

    private List<String> applyCountryCodes(String countryCodes) {
        List<String> result = new ArrayList<>();
        if (countryCodes != null && !countryCodes.isEmpty()) {
            for (String countryCode : countryCodes.split(",")) {
                if (countryCodeMap.get(countryCode) != null) {
                    result.add(countryCode);
                }
            }
        }
        return result;
    }

    private void populateCountryCodeMap() {
        String[] countryCodes = Locale.getISOCountries();
        List<Locale> locales = new ArrayList<>();
        countryCodeMap = new LinkedHashMap<>();
        for (String countryCode : countryCodes) {
            locales.add(new Locale("", countryCode));
        }
        locales.sort(Comparator.comparing(Locale::getDisplayCountry));
        for (Locale locale : locales) {
            countryCodeMap.put(locale.getCountry(), locale);
        }
    }
}
