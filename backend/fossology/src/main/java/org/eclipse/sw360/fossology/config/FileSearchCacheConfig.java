/*
 * Copyright Ritankar Saha<ritankar.saha786@gmail.com>, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.fossology.config;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Cache configuration for file search operations
 */
@Configuration
@EnableCaching
public class FileSearchCacheConfig {

    @Autowired
    private FossologyRestConfig fossologyRestConfig;

    @Bean
    public CacheManager fileSearchCacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        
        // Set up cache names
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "fileSearchBySha1",
                "fileSearchByName", 
                "fileSearchByContent",
                "filesByUpload"
        ));
        
        return cacheManager;
    }

    /**
     * Get maximum cache size from configuration
     */
    private long getMaxCacheSize() {
        String maxSize = fossologyRestConfig.getFileSearchMaxResults();
        if (!CommonUtils.isNullEmptyOrWhitespace(maxSize)) {
            try {
                return Long.parseLong(maxSize) * 10; // Cache 10x the max results
            } catch (NumberFormatException e) {
                // Ignore and use default
            }
        }
        return 1000; // Default maximum cache size
    }

    /**
     * Get cache TTL from configuration
     */
    private long getCacheTtl() {
        String cacheTtl = fossologyRestConfig.getFileSearchCacheTtl();
        if (!CommonUtils.isNullEmptyOrWhitespace(cacheTtl)) {
            try {
                return Long.parseLong(cacheTtl);
            } catch (NumberFormatException e) {
                // Ignore and use default
            }
        }
        return 30; // Default TTL of 30 minutes
    }
}