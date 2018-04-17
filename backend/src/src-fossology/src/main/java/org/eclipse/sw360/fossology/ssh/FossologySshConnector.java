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
package org.eclipse.sw360.fossology.ssh;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.fossology.config.FossologySettings;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.eclipse.sw360.datahandler.common.SW360Assert.fail;
import static java.lang.System.currentTimeMillis;
import static org.apache.log4j.Logger.getLogger;

/**
 * @author daniele.fognini@tngtech.com
 */
@Component
public class FossologySshConnector {
    private static final Logger log = getLogger(FossologySshConnector.class);

    private static final OutputStream BLACK_HOLE = new OutputStream() {
        @Override
        public void write(int b) throws IOException {
        }
    };
    private static final InputStream EMPTY_INPUT = new InputStream() {
        @Override
        public int read() throws IOException {
            return -1;
        }
    };

    private final JSchSessionProvider jSchSessionProvider;
    private final int connectionTimeout;
    private final long executionTimeout;

    @Autowired
    public FossologySshConnector(JSchSessionProvider jSchSessionProvider, FossologySettings fossologySettings) {
        this.jSchSessionProvider = jSchSessionProvider;
        executionTimeout = fossologySettings.getFossologyExecutionTimeout();
        connectionTimeout = fossologySettings.getFossologyConnectionTimeout();
    }

    protected void waitCompletion(Channel channel, long timeout) throws SW360Exception {
        long startTime = currentTimeMillis();
        while (!channel.isClosed() && (currentTimeMillis() - startTime < timeout)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw fail(e, "interrupted connection to Fossology");
            }
        }
        if (!channel.isClosed()) {
            throw fail("timeout while waiting for completion of connection to Fossology");
        }
    }

    public int runInFossologyViaSsh(String command) {
        return runInFossologyViaSsh(command, EMPTY_INPUT, BLACK_HOLE);
    }

    public int runInFossologyViaSsh(String command, InputStream stdin) {
        return runInFossologyViaSsh(command, stdin, BLACK_HOLE);
    }

    public int runInFossologyViaSsh(String command, OutputStream stdout) {
        return runInFossologyViaSsh(command, EMPTY_INPUT, stdout);
    }

    public int runInFossologyViaSsh(String command, InputStream stdin, OutputStream stdout) {
        ChannelExec channel = null;
        Session session = null;
        int exitCode = -1;
        try {
            session = jSchSessionProvider.getSession(connectionTimeout);

            channel = (ChannelExec) session.openChannel("exec");

            channel.setOutputStream(stdout);
            channel.setInputStream(stdin);

            channel.setCommand(command);

            channel.connect(connectionTimeout);
            waitCompletion(channel, executionTimeout);
            channel.disconnect();

            exitCode = channel.getExitStatus();
        } catch (JSchException | SW360Exception | NullPointerException | ClassCastException e) {
            log.error("error executing remote command on Fossology Server " + jSchSessionProvider.getServerString(), e);
        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            jSchSessionProvider.closeSession(session);
        }
        return exitCode;
    }

}
