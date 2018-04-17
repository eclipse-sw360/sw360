/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360;

import org.eclipse.sw360.attachments.db.RemoteAttachmentDownloader;
import org.apache.commons.cli.*;

import java.net.MalformedURLException;

/**
 * @author daniele.fognini@tngtech.com
 */
public class UtilsEntryPoint {

    private static final String OPTION_HELP = "h";
    private static final String OPTION_DOWNLOAD = "d";

    public static void main(String[] args) throws MalformedURLException {
        CommandLine cmd;

        try {
            cmd = parseArgs(args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelp();
            return;
        }

        String[] leftArgs = cmd.getArgs();

        if (cmd.hasOption(OPTION_HELP)) {
            printHelp();
            return;
        }

        if (cmd.hasOption(OPTION_DOWNLOAD)) {
            runRemoteAttachmentDownloader(leftArgs);
        } else {
            printHelp();
        }
    }

    private static void runRemoteAttachmentDownloader(String[] args) throws MalformedURLException {
        RemoteAttachmentDownloader.main(args);
    }

    private static CommandLine parseArgs(String[] args) throws ParseException {
        Options options = getOptions();

        return new BasicParser().parse(options, args, true);
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(UtilsEntryPoint.class.getCanonicalName(), getOptions());
    }

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(OPTION_DOWNLOAD, false, "download remote attachments");
        options.addOption(OPTION_HELP, false, "show this help");
        return options;
    }
}
