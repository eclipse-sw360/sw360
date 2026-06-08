/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.changelogs;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class ChangeLogs {

    private String id;

    private String revision;

    private String type;

    private String parentDocId;

    @JsonProperty(required = true)
    private String documentId;

    @JsonProperty(required = true)
    private String documentType;

    @JsonProperty(required = true)
    private String dbName;

    private Set<ChangedFields> changes;

    @JsonProperty(required = true)
    private Operation operation;

    @JsonProperty(required = true)
    private String userEdited;

    @JsonProperty(required = true)
    private String changeTimestamp;

    private Set<ReferenceDocData> referenceDoc;

    private Map<String,String> info;

    public ChangeLogs(){
        this.type = "changeLogs";
    }
    
}
