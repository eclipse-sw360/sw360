/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.antenna.http.utils.HttpUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * A class to represent an HTTP multi-status response.
 * </p>
 * <p>
 * For some operations affecting multiple entities, SW360 sends responses of
 * this type. From a response clients can find out whether the operation was
 * successful or failed for some of the entities.
 * </p>
 */
public final class MultiStatusResponse {
    /**
     * The property containing the status of an operation.
     */
    private static final String PROP_STATUS = "status";

    /**
     * The property containing the resource ID in JSON.
     */
    private static final String PROP_RES_ID = "resourceId";

    /**
     * Reference to parse a JSON representation with the correct data types.
     */
    private static final TypeReference<List<Map<String, Object>>> REF_MULTI_RESPONSE =
            new TypeReference<List<Map<String, Object>>>() {
            };

    /**
     * Stores the map with the responses managed by this instance.
     */
    private final Map<String, Integer> responses;

    /**
     * Creates a new instance of {@code MultiStatusResponse} with the given map
     * of single response codes. The map contains the IDs of resources as keys
     * and the corresponding status codes as values.
     *
     * @param responses the map with status codes
     */
    public MultiStatusResponse(Map<String, Integer> responses) {
        this.responses = Collections.unmodifiableMap(new HashMap<>(responses));
    }

    /**
     * Creates a new instance of {@code MultiStatusResponse} that is
     * initialized from a JSON document. This method can be used to parse a
     * response received from SW360.
     *
     * @param mapper the object mapper to parse the JSON stream
     * @param stream the stream with the data
     * @return the newly created instance
     * @throws IOException if an error occurs
     */
    public static MultiStatusResponse fromJson(ObjectMapper mapper, InputStream stream) throws IOException {
        List<Map<String, Object>> responses = mapper.readValue(stream, REF_MULTI_RESPONSE);
        try {
            Map<String, Integer> responseMap = responses.stream()
                    .map(map -> new AbstractMap.SimpleEntry<>(extractResourceId(map), extractStatus(map)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            return new MultiStatusResponse(responseMap);
        } catch (IllegalArgumentException e) {
            // Status extraction failed from JSON
            throw new IOException("Failed to parse JSON response", e);
        }
    }

    /**
     * Returns an unmodifiable map with the single responses stored in this
     * object. The keys of the map are the IDs of the resources that were
     * manipulated. The values are the status codes for the single operations.
     *
     * @return a map with the responses stored in this object
     */
    public Map<String, Integer> getResponses() {
        return responses;
    }

    /**
     * Returns the number of responses stored in this object.
     *
     * @return the number of responses
     */
    public int responseCount() {
        return responses.size();
    }

    /**
     * Returns a flag whether all the responses managed by this object have
     * been successful.
     *
     * @return <strong>true</strong> if there are only success responses;
     * <strong>false</strong> if there is at least one failure status code
     */
    public boolean isAllSuccess() {
        return responses.values().stream()
                .allMatch(HttpUtils::isSuccessStatus);
    }

    /**
     * Returns a flag whether this object contains information about the
     * resource ID specified.
     *
     * @param resId the resource ID in question
     * @return a flag whether a status about this resource is stored in this
     * object
     */
    public boolean hasResourceId(String resId) {
        return responses.containsKey(resId);
    }

    /**
     * Returns the status code for the resource with the given ID. The ID
     * must be present, otherwise a {@code NoSuchElementException} exception is
     * thrown.
     *
     * @param resId the ID of the resource in question
     * @return the status code for this resource
     * @throws java.util.NoSuchElementException if the resource is unknown
     */
    public int getStatus(String resId) {
        Integer status = responses.get(resId);
        if (status == null) {
            throw new NoSuchElementException("Unknown resource ID: " + resId +
                    "; not contained in this multi-status response.");
        }
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MultiStatusResponse response = (MultiStatusResponse) o;
        return responses.equals(response.responses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(responses);
    }

    @Override
    public String toString() {
        return "MultiStatusResponse{" +
                "responses=" + responses +
                '}';
    }

    /**
     * Extracts the resource ID from the given object map. Throws an exception
     * if the property does not exist.
     *
     * @param map the map
     * @return the resource ID extracted from the map
     * @throws IllegalArgumentException if the resource ID cannot be resolved
     */
    private static String extractResourceId(Map<String, Object> map) {
        Object resourceId = map.get(PROP_RES_ID);
        if (!(resourceId instanceof String)) {
            throw new IllegalArgumentException("Unexpected JSON response. Expected resource ID, but found " +
                    resourceId);
        }
        return (String) resourceId;
    }

    /**
     * Extracts the status code property from the given object map. Throws an
     * exception if this property is invalid or cannot be resolved.
     *
     * @param map the map
     * @return the status code extracted from the map
     * @throws IllegalArgumentException if the status code could not be extracted
     */
    private static Integer extractStatus(Map<String, Object> map) {
        Object status = map.get(PROP_STATUS);
        if (!(status instanceof Integer)) {
            throw new IllegalArgumentException("Unexpected JSON response. Expected status code, but found " +
                    status);
        }
        return (Integer) status;
    }
}
