/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.spdx;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnippetInformation {

    private String SPDXID;

    private String snippetFromFile;

    private Set<SnippetRange> snippetRanges;

    private String licenseConcluded;

    private Set<String> licenseInfoInSnippets;

    private String licenseComments;

    private String copyrightText;

    private String comment;

    private String name;

    private String snippetAttributionText;

    private Integer index;
}
