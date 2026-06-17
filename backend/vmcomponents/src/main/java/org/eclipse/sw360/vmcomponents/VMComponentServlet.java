/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents;

import jakarta.annotation.PreDestroy;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponentService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.apache.thrift.protocol.TCompactProtocol;
import org.eclipse.sw360.vmcomponents.process.VMProcessHandler;

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

    @PreDestroy
    public void cleanup() {
        VMProcessHandler.shutdown();
    }
}
