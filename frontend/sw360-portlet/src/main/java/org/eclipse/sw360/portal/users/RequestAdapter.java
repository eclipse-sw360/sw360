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
package org.eclipse.sw360.portal.users;

import com.liferay.portal.kernel.service.ServiceContext;

import java.util.Optional;
import java.util.function.Consumer;

interface RequestAdapter {
    long getCompanyId();
    Consumer<String> getErrorMessagesConsumer();
    Optional<ServiceContext> getServiceContext();
}
