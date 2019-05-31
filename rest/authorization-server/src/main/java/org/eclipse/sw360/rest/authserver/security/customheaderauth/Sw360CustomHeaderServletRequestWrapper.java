/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.rest.authserver.security.customheaderauth;

import org.apache.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.savedrequest.Enumerator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import java.util.*;

/**
 * This {@link HttpServletRequestWrapper} is able to extend the parameter list
 * of the wrapped request object (which is not possible on standard
 * {@link HttpServletRequest}s because normally one wants to have exactly the
 * request params as they are coming from the client / proxy). This is necessary
 * because we want to be part of the standard oauth 2 workflow of spring with
 * our custom header pre authentication chain. And in this workflow we need to
 * pass additional information to our
 * {@link Sw360CustomHeaderAuthenticationProvider} from our
 * {@link Sw360CustomHeaderAuthenticationFilter} which is only possible via
 * request params that are added by the standard flow to the authentication
 * details in {@link Authentication#getDetails()}. This additional parameters
 * come in handy for normal pre-authenticated requests as well.
 *
 * Think about if this class should return only unmodifiable values.
 */
public class Sw360CustomHeaderServletRequestWrapper extends HttpServletRequestWrapper {

    private final Logger log = Logger.getLogger(this.getClass());

    private final Map<String, String[]> addableParameterMap;

    public Sw360CustomHeaderServletRequestWrapper(HttpServletRequest request) {
        super(request);

        addableParameterMap = new HashMap<String, String[]>(request.getParameterMap());
    }

    public void addParameter(String name, String[] values) {
        log.debug("Added parameter with key " + name + " to parameter map of request " + getRequest());

        addableParameterMap.put(name, values);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return addableParameterMap;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return new Enumerator<>(addableParameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return addableParameterMap.get(name);
    }

    @Override
    public String getParameter(String name) {
        String[] allValues = addableParameterMap.get(name);

        return Arrays.stream(allValues)
                .findFirst()
                .orElse(null);
    }

}
