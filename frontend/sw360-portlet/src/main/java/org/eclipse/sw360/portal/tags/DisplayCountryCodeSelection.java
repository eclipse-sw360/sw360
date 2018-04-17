/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;
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
        JspWriter jspWriter = getJspContext().getOut();

        if (selected == null || selected.isEmpty()) {
            jspWriter.write("<option value=\"\">Select a country</option>");
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
