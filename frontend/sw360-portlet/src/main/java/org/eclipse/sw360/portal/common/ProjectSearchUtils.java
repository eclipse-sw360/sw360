/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.common;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;

import java.util.List;

public class ProjectSearchUtils {
    protected final Logger logger = LogManager.getLogger(this.getClass());

    private final ThriftClients thriftClients;

    public ProjectSearchUtils(ThriftClients thriftClients) {
        this.thriftClients = thriftClients;
    }

    public List<Project> searchProjects(User user, String searchTerm) {
        List<Project> searchResults;

        try {
            ProjectService.Iface client = thriftClients.makeProjectClient();
            if (!Strings.isNullOrEmpty(searchTerm)) {
                searchResults = client.search(searchTerm);
            } else {
                searchResults = client.getAccessibleProjectsSummary(user);
            }
        } catch (TException exception) {
            logger.error("Cannot search for projects", exception);
            searchResults = Lists.newArrayList();
        }

        return searchResults;
    }
}
