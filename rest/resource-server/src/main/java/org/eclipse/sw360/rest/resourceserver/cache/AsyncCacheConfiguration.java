/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuration for asynchronous cache operations.
 * Provides a dedicated thread pool for background cache building.
 *
 * <p><b>Purpose:</b> When a cache miss occurs, the cache can be built asynchronously
 * in the background without blocking the user's request. The user gets their response
 * via standard (slow) processing, but subsequent requests will benefit from the cached response.</p>
 *
 * <p><b>Thread Pool Configuration:</b></p>
 * <ul>
 *   <li>Core pool size: 1 - Only one cache build at a time</li>
 *   <li>Max pool size: 2 - Allow one additional thread for peak load</li>
 *   <li>Queue capacity: 10 - Queue up to 10 cache build requests</li>
 *   <li>Rejection policy: CallerRunsPolicy - If queue full, run in caller thread (graceful degradation)</li>
 * </ul>
 */
@Configuration
@EnableAsync
public class AsyncCacheConfiguration {

    private static final Logger log = LogManager.getLogger(AsyncCacheConfiguration.class);

    /**
     * Dedicated executor for cache building operations.
     * Uses a small thread pool to avoid overwhelming the server during cache builds.
     *
     * @return Configured ThreadPoolTaskExecutor for async cache operations
     */
    @Bean(name = "cacheExecutor")
    public Executor cacheExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Core pool size - number of threads to keep alive
        executor.setCorePoolSize(1);

        // Maximum pool size - max threads during peak load
        executor.setMaxPoolSize(2);

        // Queue capacity - how many tasks can be queued
        executor.setQueueCapacity(10);

        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("cache-async-");

        // Rejection policy - what to do when queue is full
        // CallerRunsPolicy: run in caller thread (graceful degradation)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Initialized cache executor with corePoolSize=1, maxPoolSize=2, queueCapacity=10");
        return executor;
    }
}
