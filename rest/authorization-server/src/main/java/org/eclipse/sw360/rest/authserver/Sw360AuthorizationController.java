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

package org.eclipse.sw360.rest.authserver;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.RestApiToken;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.authserver.security.Sw360AuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360AuthorizationController {

    @NonNull
    private Sw360AuthenticationProvider sw360AuthenticationProvider;

    // This service can only be called by localhost and should be protected
    @RequestMapping(value = "/generateToken", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public @ResponseBody
    RestApiToken generateToken(@RequestBody User sw360User) {
        RestApiToken response = new RestApiToken();
        OAuth2AccessToken accessToken = sw360AuthenticationProvider.generateAccessTokenByUser(sw360User);
        response.setValue(accessToken.getTokenType() + " " + accessToken.getValue());
        response.setScope(accessToken.getScope().stream().collect(Collectors.joining()));
        response.setAuthorities(accessToken.getAdditionalInformation().get("authorities").toString());
        response.setExpiration(accessToken.getExpiration().toString());
        return response;
    }

}
