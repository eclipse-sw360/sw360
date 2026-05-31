/*
 *  Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 * 
 *  This program and the accompanying materials are made
 *  available under the terms of the Eclipse Public License 2.0
 *  which is available at https://www.eclipse.org/legal/epl-2.0/
 * 
 *  SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.services.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(chain = true)
public class VerificationStateInfo {
    
    @JsonProperty(required = true)
    private String checkedOn;

    @JsonProperty(required = true)
    private String checkedBy;

    @JsonProperty(required = true)
    private VerificationState verificationState;

    private String comment;
    
}
