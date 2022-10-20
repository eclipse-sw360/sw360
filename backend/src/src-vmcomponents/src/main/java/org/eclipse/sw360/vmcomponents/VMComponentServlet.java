/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package org.eclipse.sw360.vmcomponents;

import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponentService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.apache.thrift.protocol.TCompactProtocol;

import java.io.IOException;

/**
 * Thrift Servlet instantiation
 *
 * @author stefan.jaeger@evosoft.com
 */
public class VMComponentServlet extends Sw360ThriftServlet {

    public VMComponentServlet() throws IOException, SW360Exception {
        // Create a service processor using the provided handler
        super(new VMComponentService.Processor<>(new VMComponentHandler()), new TCompactProtocol.Factory());
    }
}
