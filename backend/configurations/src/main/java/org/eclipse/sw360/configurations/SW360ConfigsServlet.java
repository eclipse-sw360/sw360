/*
 * Copyright TOSHIBA CORPORATION, 2025. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.configurations;

import org.apache.thrift.protocol.TProtocolFactory;
import org.eclipse.sw360.datahandler.thrift.configurations.SW360ConfigsService;
import org.eclipse.sw360.projects.Sw360ThriftServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SW360ConfigsServlet extends Sw360ThriftServlet {
    @Autowired
    public SW360ConfigsServlet(SW360ConfigsHandler sw360ConfigsHandler, TProtocolFactory thriftProtocolFactory) {
        super(new SW360ConfigsService.Processor<>(sw360ConfigsHandler), thriftProtocolFactory);
    }
}
