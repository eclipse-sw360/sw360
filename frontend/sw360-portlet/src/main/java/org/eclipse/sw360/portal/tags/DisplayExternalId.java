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

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This displays comma separated externalId
 *
 */
public class DisplayExternalId extends SimpleTagSupport {

	private String value;
	private String autoFillValue;

	public void setValue(String value) {
		this.value = value;
	}

	public void setAutoFillValue(String autoFillValue) {
		this.autoFillValue = autoFillValue;
	}

	public void doTag() throws JspException, IOException {
		String fullValue;

		if (value == null) {
			fullValue = autoFillValue;
		} else {
			fullValue = value;
		}

		if (null != fullValue && !fullValue.isEmpty()) {
			String result = getExternalIdsString(fullValue);
			getJspContext().getOut().print(result);
		}
	}

	private static String getExternalIdsString(String externalIdValues) {
		StringBuilder sb = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();
		Set<String> externalIdValueSet = new TreeSet<>();
		try {
			externalIdValueSet = mapper.readValue(externalIdValues, Set.class);
		} catch (IOException e) {
			externalIdValueSet.add(externalIdValues);
		}

		return String.join(", ", externalIdValueSet);
	}
}
