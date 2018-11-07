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

window.onload = function () {
    if (!isSupportedBrowser()) {
        var browserWarning = document.getElementById('unsupported-browser');
        browserWarning.style.display = 'block';
    }
};

// Verify user browser to detect IE (IE6-11 + Edge)
function isSupportedBrowser() {
    const ua_ie6_10 = 'MSIE ';
    const ua_ie11 = 'Trident/';
    const ua_ie12 = 'Edge/';

    var userAgent = window.navigator.userAgent;
    var ie6_10 = userAgent.indexOf(ua_ie6_10);
    var ie11 = userAgent.indexOf(ua_ie11);
    var ie12 = userAgent.indexOf(ua_ie12);

    if (ie6_10 > 0) {
        return !parseInt(userAgent.substring(ie6_10 + 5, userAgent.indexOf('.', ie6_10)), 10);
    }
    if (ie11 > 0) {
        var releaseVersion = userAgent.indexOf('rv:');
        return !parseInt(userAgent.substring(releaseVersion + 3, userAgent.indexOf('.', releaseVersion)), 10);
    }
    if (ie12 > 0) {
        return !parseInt(userAgent.substring(ie12 + 5, userAgent.indexOf('.', ie12)), 10);
    }

    return true;
}
