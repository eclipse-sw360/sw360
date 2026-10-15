/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.component.cache;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Built-in {@link ComponentCachingCriteria} that only allows caching of
 * {@code GET /api/components} requests whose requested page size ({@code page_entries})
 * is greater than or equal to the configured threshold.
 *
 * <p>Threshold is sourced from {@code sw360.properties} key
 * {@code rest.cache.components-all.min.page.size} (default {@code 1000}).</p>
 */
@Component
public class PageSizeCriteria implements ComponentCachingCriteria {

    private static final Logger log = LogManager.getLogger(PageSizeCriteria.class);

    private static final String SW360_PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String CONFIG_KEY_MIN_PAGE_SIZE = "rest.cache.components-all.min.page.size";
    private static final int DEFAULT_MIN_PAGE_SIZE = 1000;
    private static final String PAGE_ENTRIES_PARAM = "page_entries";

    private final int minPageSize;

    public PageSizeCriteria() {
        this(loadConfiguredMinPageSize());
    }

    /**
     * Package-private constructor allowing explicit threshold injection —
     * primarily for tests (avoids depending on the system-wide
     * {@code sw360.properties} that
     * {@link CommonUtils#loadProperties(Class, String, boolean)} overlays from
     * {@code SYSTEM_CONFIGURATION_PATH}). Production wiring uses the no-arg
     * constructor, which Spring Boot 4 auto-wires as the single public
     * constructor.
     */
    PageSizeCriteria(int minPageSize) {
        this.minPageSize = minPageSize;
    }

    private static int loadConfiguredMinPageSize() {
        Properties properties = CommonUtils.loadProperties(PageSizeCriteria.class, SW360_PROPERTIES_FILE_PATH, true);
        return parseMinPageSize(properties.getProperty(CONFIG_KEY_MIN_PAGE_SIZE));
    }

    private static int parseMinPageSize(String configuredValue) {
        if (CommonUtils.isNullEmptyOrWhitespace(configuredValue)) {
            return DEFAULT_MIN_PAGE_SIZE;
        }
        try {
            return Integer.parseInt(configuredValue.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid value for {}: '{}' — falling back to default {}",
                    CONFIG_KEY_MIN_PAGE_SIZE, configuredValue, DEFAULT_MIN_PAGE_SIZE);
            return DEFAULT_MIN_PAGE_SIZE;
        }
    }

    @Override
    public String name() {
        return "PageSizeCriteria";
    }

    @Override
    public boolean shouldCache(HttpServletRequest request) {
        String pageEntries = request.getParameter(PAGE_ENTRIES_PARAM);
        if (CommonUtils.isNullEmptyOrWhitespace(pageEntries)) {
            // Missing page size — not eligible (no explicit large-page intent).
            return false;
        }
        try {
            int requestedPageSize = Integer.parseInt(pageEntries.trim());
            return requestedPageSize >= minPageSize;
        } catch (NumberFormatException e) {
            // Non-numeric page size — bypass cache rather than fail the request.
            return false;
        }
    }
}
