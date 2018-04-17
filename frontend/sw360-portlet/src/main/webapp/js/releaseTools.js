/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

function createReleaseUrl( paramId, paramVal) {
    var portletURL = Liferay.PortletURL.createURL( baseUrl ).setParameter(pageName,pageDetail)
                                .setParameter(compIdInURL,componentId).setParameter(paramId,paramVal);
    return portletURL.toString();
}

function createDetailURLfromReleaseId (id ) {
    return createReleaseUrl(releaseIdInURL,id );
}

