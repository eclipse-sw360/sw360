/*
 * Copyright (c) 2026 GSiC Project, Bosch Software Innovations GmbH 2018.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.clients.cli;

/**
 * CLI tool for LicenseDB sync operations.
 * 
 * Usage:
 *   java -jar sw360-client-cli.jar --test-connection
 *   java -jar sw360-client-cli.jar --sync
 *   java -jar sw360-client-cli.jar --status
 * 
 * This CLI provides commands to interact with LicenseDB integration in SW360.
 * The actual LicenseDB sync functionality is handled by the SW360LicenseClient.
 */
public class LicenseDbCli {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0];
        
        try {
            switch (command) {
                case "--test-connection":
                    testConnection();
                    break;
                case "--sync":
                    syncFromLicenseDB();
                    break;
                case "--status":
                    getSyncStatus();
                    break;
                case "--help":
                    printUsage();
                    break;
                default:
                    System.out.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("LicenseDB CLI Tool");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("  java -jar sw360-client-cli.jar [command]");
        System.out.println("");
        System.out.println("Commands:");
        System.out.println("  --test-connection    Test connection to LicenseDB");
        System.out.println("  --sync               Trigger license sync from LicenseDB");
        System.out.println("  --status             Get current sync status");
        System.out.println("  --help               Show this help message");
        System.out.println("");
        System.out.println("Examples:");
        System.out.println("  java -jar sw360-client-cli.jar --test-connection");
        System.out.println("  java -jar sw360-client-cli.jar --sync");
        System.out.println("  java -jar sw360-client-cli.jar --status");
    }

    private static void testConnection() throws Exception {
        System.out.println("Testing connection to LicenseDB...");
        System.out.println("This command tests the connection to LicenseDB using the configured credentials.");
        System.out.println("Configure LicenseDB in sw360.properties before using this command.");
        System.out.println("");
        System.out.println("To test connection programmatically, use:");
        System.out.println("  SW360LicenseClient.testLicenseDBConnection()");
        System.out.println("");
        System.out.println("Connection test would be executed here.");
    }

    private static void syncFromLicenseDB() throws Exception {
        System.out.println("Starting license sync from LicenseDB...");
        System.out.println("This command triggers a sync of licenses from LicenseDB to SW360.");
        System.out.println("Configure LicenseDB in sw360.properties before using this command.");
        System.out.println("");
        System.out.println("To sync programmatically, use:");
        System.out.println("  SW360LicenseClient.importFromLicenseDB()");
        System.out.println("");
        System.out.println("License sync would be executed here.");
    }

    private static void getSyncStatus() throws Exception {
        System.out.println("Getting sync status from LicenseDB...");
        System.out.println("This command gets the current sync status from LicenseDB.");
        System.out.println("");
        System.out.println("To get status programmatically, use:");
        System.out.println("  SW360LicenseClient.getLicenseDBSyncStatus()");
        System.out.println("");
        System.out.println("Sync status would be shown here.");
    }
}
