/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.components;

import com.google.common.collect.Lists;

import org.eclipse.sw360.datahandler.common.CommonUtils;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;

import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import static org.eclipse.sw360.portal.common.PortalConstants.PROPERTIES_FILE_PATH;

@Component
public class DynamicComponentManager extends LoggingComponent {
    @Reference
    private ConfigurationAdmin configurationAdmin;

    private List<Configuration> activatedConfigurations = Lists.newArrayList();

    @Activate
    protected synchronized void activate() {
        super.activate();

        Properties properties = CommonUtils.loadProperties(DynamicComponentManager.class, PROPERTIES_FILE_PATH);

        String components = properties.getProperty("components.activate");
        activateComponents(components.split("\\s*,\\s*"));

        String portlets = properties.getProperty("portlets.activate");
        activateComponents(portlets.split("\\s*,\\s*"));
    }

    @Deactivate
    protected synchronized void deactivate() {
        deactivateComponents();
        super.deactivate();
    }

    private void activateComponents(String[] components) {
        for (String component : components) {
            if(component.trim().isEmpty()) {
                continue;
            }

            log.info("Enable component [" + component + "]");
            try {
                Configuration configuration = configurationAdmin.getConfiguration(component);
                if (configuration != null) {
                    Hashtable<String, Object> componentProperties = new Hashtable<>();
                    componentProperties.put("enabled", true);
                    configuration.update(componentProperties);

                    activatedConfigurations.add(configuration);
                } else {
                    log.error("Cannot enable component [" + component + "]. No configuration found!");
                }
            } catch (Exception exception) {
                log.error("Cannot enable component [" + component + "].", exception);
            }
        }
    }

    private void deactivateComponents() {
        for (Configuration configuration : activatedConfigurations) {
            log.info("Disable component [" + configuration.getPid() + "]");

            try {
                configuration.delete();
            } catch (Exception exception) {
                log.error("Cannot disable component [" + configuration.getPid() + "].", exception);
            }
        }
    }
}
