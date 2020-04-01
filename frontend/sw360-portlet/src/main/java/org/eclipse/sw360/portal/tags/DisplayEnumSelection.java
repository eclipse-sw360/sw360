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

import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;

import org.apache.thrift.TEnum;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Iterator;
import java.util.ResourceBundle;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.Iterables.all;

/**
 * @author daniele.fognini@tngtech.com
 */
public class DisplayEnumSelection extends SimpleTagSupport {

    private Class type;
    private TEnum selected;
    private String selectedName;
    private Boolean useStringValues = false;
    private Boolean inQuotes = false;
    private Iterable<? extends TEnum> options;

    public void setType(Class type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public void setOptions(Iterable options) throws JspException {
        if (!all(options, instanceOf(TEnum.class))) {
            throw new JspException("given type options are not of class TEnum");
        }

        this.options = (Iterable<? extends TEnum>) options;
    }

    public void setSelected(TEnum selected) {
        this.selected = selected;
    }

    public void setSelectedName(String selectedName) {
        this.selectedName = selectedName;
    }

    public void setUseStringValues(Boolean useStringValues) {
        this.useStringValues = useStringValues;
    }

    public void setInQuotes(Boolean inQuotes) { this.inQuotes = inQuotes;}

    public void doTag() throws JspException, IOException {
        if (options != null) {
            doEnumValues(options);
        } else if (type != null) {
            doEnumValues(ThriftEnumUtils.MAP_ENUMTYPE_MAP.get(type).keySet());
        } else {
            throw new JspException("you must select either a TEnum type or a collection of values");
        }
    }

    private void doEnumValues(Iterable<? extends TEnum> enums) throws IOException {
        PageContext pageContext = (PageContext) getJspContext();
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ResourceBundle resourceBundle = ResourceBundleUtil.getBundle("content.Language", request.getLocale(), getClass());

        JspWriter jspWriter = getJspContext().getOut();

        Iterator<? extends TEnum> iterator = enums.iterator();
        while (iterator.hasNext()){
            TEnum enumItem = iterator.next();
            String enumItemDescription = ThriftEnumUtils.enumToString(enumItem);

            boolean selected = enumItem.equals(this.selected) || enumItem.toString().equals(this.selectedName);
            String value = useStringValues ? enumItem.toString() : "" + enumItem.getValue();
            String result = String.format(
                    "<option value=\"%s\" class=\"textlabel stackedLabel\" " +
                            (selected ? "selected=\"selected\" " : "") +
                             ">%s</option>",
                    value, LanguageUtil.get(resourceBundle, enumItemDescription.replace(' ','.').toLowerCase()));
            if (inQuotes && iterator.hasNext()){
                jspWriter.write("\'"+ result+ "\' +");
            } else if (inQuotes) {
                jspWriter.write("\'"+ result+ "\'");
            } else {
                jspWriter.write(result);
            }
        }
    }
}
