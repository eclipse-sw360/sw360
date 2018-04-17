/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.couchdb;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.ektorp.CouchDbConnector;
import org.ektorp.impl.ObjectMapperFactory;
import org.ektorp.impl.jackson.EktorpJacksonModule;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Mapper factory to bridge between Thrift generated objects and CouchDB serialization requirements
 *
 * @author cedric.bodet@tngtech.com
 */
public class MapperFactory implements ObjectMapperFactory {

    private final List<Class<?>> classes;
    private final List<Class<?>> nestedClasses;
    private final Map<Class<?>, JsonDeserializer<?>> customDeserializer;


    /**
     * Create a mapper factory with mix-in for the thrift generated classes (defined in ThriftUtils)
     */
    public MapperFactory() {
        this(ThriftUtils.THRIFT_CLASSES, ThriftUtils.THRIFT_NESTED_CLASSES, ThriftUtils.CUSTOM_DESERIALIZER);
    }

    /**
     * Create a mapper factory with mix-in for the specified classes
     *
     * @param classes List of classes to add mix-ins to in the object mapper
     */
    public MapperFactory(List<Class<?>> classes, List<Class<?>> nestedClasses, Map<Class<?>, JsonDeserializer<?>> customDeserializer) {
        this.classes = classes;
        this.nestedClasses = nestedClasses;
        this.customDeserializer = customDeserializer;
    }

    /**
     * Creates an object mapper with the given personalization
     *
     * @return the personalized object mapper
     */
    @Override
    @SuppressWarnings("unchecked")
    public ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // General settings
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY); // auto-detect all member fields
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE); // but only public getters
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE); // and none of "is-setters"

        // Do not include null
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleModule module = new SimpleModule();
        for (Entry<Class<?>, JsonDeserializer<?>> entry : customDeserializer.entrySet()) {
            module.addDeserializer((Class<Object>) entry.getKey(), entry.getValue());
        }
        mapper.registerModule(module);

        // Classes mix-in
        for (Class<?> type : classes) {
            mapper.addMixInAnnotations(type, DatabaseMixIn.class);
        }

        // Nested classes mix-in
        for (Class<?> type : nestedClasses) {
            mapper.addMixInAnnotations(type, DatabaseNestedMixIn.class);
        }

        return mapper;
    }

    @Override
    public ObjectMapper createObjectMapper(CouchDbConnector connector) {
        ObjectMapper mapper = createObjectMapper();
        mapper.registerModule(new EktorpJacksonModule(connector, mapper));
        return mapper;
    }

}
