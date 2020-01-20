/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.components;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingComponent {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been ENABLED.");
    }

    @Modified
    protected void modified() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been MODIFIED.");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Component [" + getClass().getCanonicalName() + "] has been DISABLED.");
    }
}
