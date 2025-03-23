/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

 package org.eclipse.sw360;

 import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceTrackerCloudant;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import jakarta.servlet.ServletContextListener;
 import jakarta.servlet.ServletContextEvent;
 
 /**
  * @author daniele.fognini@tngtech.com
  */
 public class SW360ServiceContextListener implements ServletContextListener {
     // Add SLF4J logger
     private static final Logger LOGGER = LoggerFactory.getLogger(SW360ServiceContextListener.class);
 
     @Override
     public void contextInitialized(ServletContextEvent sce) {
         LOGGER.info("SW360 service context is initializing");
         // Future initialization logic can be added here with logging
     }
 
     @Override
     public void contextDestroyed(ServletContextEvent sce) {
         LOGGER.info("SW360 service context is being destroyed");
         try {
             DatabaseInstanceTrackerCloudant.destroy();
             LOGGER.info("Cloudant database instances successfully destroyed");
         } catch (Exception e) {
             LOGGER.error("Failed to destroy Cloudant database instances: {}", e.getMessage(), e);
         }
     }
 }