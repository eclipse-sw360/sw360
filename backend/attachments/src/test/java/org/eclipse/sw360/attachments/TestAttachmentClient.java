/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.attachments;

import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentService;
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
public class TestAttachmentClient {

    public static void main(String[] args) {
        try {
            THttpClient thriftClient = new THttpClient("http://127.0.0.1:8080/attachmentservice/thrift");
            TProtocol protocol = new TCompactProtocol(thriftClient);
            AttachmentService.Iface client = new AttachmentService.Client(protocol);
        } catch (Exception e) {
            assert(false);
        }
    }

}
