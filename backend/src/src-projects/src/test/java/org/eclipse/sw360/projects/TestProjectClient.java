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
package org.eclipse.sw360.projects;

import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;

import java.io.IOException;

/**
 * Small client for testing a service
 *
 * @author cedric.bodet@tngtech.com
 */
public class TestProjectClient {

    public static void main(String[] args) throws TException, IOException {
        THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080/projects/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        ProjectService.Iface client = new ProjectService.Client(protocol);

//        User cedric = new User().setEmail("cedric.bodet@tngtech.com").setDepartment("CT BE OSS TNG CB");
//        Project myProject = new Project().setName("First project").setDescription("My first project");
//        client.addProject(myProject, cedric);

    //    List<Project> projects = client.getBUProjectsSummary("CT BE OSS");

//        System.out.println("Fetched " + projects.size() + " from project service");
//        for (Project project : projects) {
//            System.out.println(project.toString());
//        }
    }

}
