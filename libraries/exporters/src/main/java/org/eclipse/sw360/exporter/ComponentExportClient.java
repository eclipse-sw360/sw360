/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.users.User;

import java.util.List;
import java.util.Set;

/**
 * The component lookups an Excel export needs. Callers that already hold service-api
 * documents pass those straight through; callers that still speak Thrift convert here.
 */
public interface ComponentExportClient {

    Component getComponentById(String id, User user) throws TException;

    List<Component> getComponentsShort(Set<String> ids) throws TException;

    List<Release> getReleasesWithAccessibilityByIdsForExport(Set<String> ids, User user) throws TException;
}
