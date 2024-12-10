/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TConfiguration;

public class ThriftConstants extends SW360Constants {

    private final static Logger log = LogManager.getLogger(ThriftConstants.class);

    public static final String BACKEND_URL;
    public static final String BACKEND_PROXY_URL;
    public static final int THRIFT_CONNECTION_TIMEOUT;
    public static final int THRIFT_READ_TIMEOUT;
    public static final int THRIFT_MAX_MESSAGE_SIZE;
    public static final int THRIFT_MAX_FRAME_SIZE;

    static {
        BACKEND_URL = props.getProperty("backend.url", "http://127.0.0.1:8080");
        //Proxy can be set e.g. with "http://localhost:3128". if set all request to the thrift backend are routed through the proxy
        BACKEND_PROXY_URL = props.getProperty("backend.proxy.url", null);
        // maximum timeout for connecting and reading
        THRIFT_CONNECTION_TIMEOUT = Integer.valueOf(props.getProperty("backend.timeout.connection", "5000"));
        THRIFT_READ_TIMEOUT = Integer.valueOf(props.getProperty("backend.timeout.read", "600000"));

        THRIFT_MAX_MESSAGE_SIZE = Integer.valueOf(props.getProperty("backend.thrift.max.message.size", String.valueOf(TConfiguration.DEFAULT_MAX_MESSAGE_SIZE)));
        THRIFT_MAX_FRAME_SIZE = Integer.valueOf(props.getProperty("backend.thrift.max.frame.size", String.valueOf(TConfiguration.DEFAULT_MAX_FRAME_SIZE)));

        log.info("The following configuration will be used for connections to the backend:\n" +
                "\tURL                      : " + BACKEND_URL + "\n" +
                "\tProxy                    : " + BACKEND_PROXY_URL + "\n" +
                "\tTimeout Connecting (ms)  : " + THRIFT_CONNECTION_TIMEOUT + "\n" +
                "\tTimeout Read (ms)        : " + THRIFT_READ_TIMEOUT + "\n");
    }
}
