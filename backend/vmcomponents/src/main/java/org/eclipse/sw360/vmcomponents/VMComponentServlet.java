/*
SPDX-FileCopyrightText: © 2022 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.vmcomponents;

import org.apache.thrift.protocol.TProtocolFactory;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.VMComponentService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.apache.thrift.protocol.TCompactProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Thrift Servlet instantiation
 *
 * @author stefan.jaeger@evosoft.com
 */
@Component
public class VMComponentServlet extends Sw360ThriftServlet {

    @Autowired
    public VMComponentServlet(VMComponentHandler vmComponentHandler, TProtocolFactory thriftProtocolFactory) {
        // Create a service processor using the provided handler
        super(new VMComponentService.Processor<>(vmComponentHandler), thriftProtocolFactory);
    }
}
