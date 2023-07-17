/*
SPDX-FileCopyrightText: © 2023 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.rest.resourceserver.report;

import java.nio.ByteBuffer;
import java.util.List;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SW360ReportService {
	
	ThriftClients thriftClients = new ThriftClients();
    ProjectService.Iface projectclient = thriftClients.makeProjectClient();
    ComponentService.Iface componentclient = thriftClients.makeComponentClient();

    public ByteBuffer getProjectBuffer(User user, boolean extendedByReleases)
            throws TException {
        return projectclient.getReportDataStream(user, extendedByReleases);
    }

    public String getUploadedProjectPath(User user, boolean extendedByReleases) throws TException{
        return projectclient.getReportInEmail(user, extendedByReleases);
    }
    
    public ByteBuffer getReportStreamFromURl(User user,boolean extendedByReleases, String token) 
            throws TException{
        return projectclient.downloadExcel(user,extendedByReleases, token);
    }
    public void sendExportSpreadsheetSuccessMail(String emailURL, String email) throws TException{
    	projectclient.sendExportSpreadsheetSuccessMail(emailURL, email);
	}

	public String getUploadedComponentPath(User sw360User, boolean withLinkedReleases) throws TException{
		return componentclient.getComponentReportInEmail(sw360User, withLinkedReleases);
	}

	public ByteBuffer getComponentBuffer(User sw360User, boolean withLinkedReleases) throws TException{
		return componentclient.getComponentReportDataStream(sw360User, withLinkedReleases);
	}
	
	public ByteBuffer getComponentReportStreamFromURl(User user,boolean extendedByReleases, String token) 
            throws TException{
        return componentclient.downloadExcel(user,extendedByReleases, token);
    }
    public void sendComponentExportSpreadsheetSuccessMail(String emailURL, String email) throws TException{
    	componentclient.sendExportSpreadsheetSuccessMail(emailURL, email);
	}
}