/*
 * Copyright Siemens AG, 2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.Map;
import java.util.Set;

public interface AwareOfRestServices<T> {

    Set<T> searchByExternalIds(Map<String, Set<String>> externalIds, User user) throws TException;

    T convertToEmbeddedWithExternalIds(T sw360Object);

}
