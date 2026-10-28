/*
 * Copyright Shivamrut<gshivamrut@gmail.com>, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.core.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.eclipse.sw360.datahandler.services.common.Visibility;
import org.eclipse.sw360.datahandler.services.projects.ProjectState;
import org.eclipse.sw360.datahandler.services.projects.ProjectType;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.io.IOException;
import java.util.Set;

/**
 * Writes the values Thrift used to emit when a project field was left unset.
 * Service-api fields are null in that case, and the class-level NON_NULL
 * include would otherwise drop them from the JSON.
 */
public final class ProjectWireDefaults {

    private ProjectWireDefaults() {}

    public static class State extends JsonSerializer<ProjectState> {
        @Override
        public void serialize(ProjectState value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value == null ? "ACTIVE" : value.name());
        }
    }

    public static class State3 extends ValueSerializer<ProjectState> {
        @Override
        public void serialize(ProjectState value, tools.jackson.core.JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writeString(value == null ? "ACTIVE" : value.name());
        }
    }

    public static class Type extends JsonSerializer<ProjectType> {
        @Override
        public void serialize(ProjectType value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value == null ? "PRODUCT" : value.name());
        }
    }

    public static class Type3 extends ValueSerializer<ProjectType> {
        @Override
        public void serialize(ProjectType value, tools.jackson.core.JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writeString(value == null ? "PRODUCT" : value.name());
        }
    }

    public static class VisibilityDefault extends JsonSerializer<Visibility> {
        @Override
        public void serialize(Visibility value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value == null ? "EVERYONE" : value.name());
        }
    }

    public static class VisibilityDefault3 extends ValueSerializer<Visibility> {
        @Override
        public void serialize(Visibility value, tools.jackson.core.JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writeString(value == null ? "EVERYONE" : value.name());
        }
    }

    public static class FalseBoolean extends JsonSerializer<Boolean> {
        @Override
        public void serialize(Boolean value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeBoolean(Boolean.TRUE.equals(value));
        }
    }

    public static class FalseBoolean3 extends ValueSerializer<Boolean> {
        @Override
        public void serialize(Boolean value, tools.jackson.core.JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writeBoolean(Boolean.TRUE.equals(value));
        }
    }

    public static class ProjectTypeName extends JsonSerializer<String> {
        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value == null ? "project" : value);
        }
    }

    public static class ProjectTypeName3 extends ValueSerializer<String> {
        @Override
        public void serialize(String value, tools.jackson.core.JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writeString(value == null ? "project" : value);
        }
    }

    public static class EmptyStringSet extends JsonSerializer<Set<String>> {
        @Override
        public void serialize(Set<String> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeObject(value == null ? Set.of() : value);
        }
    }

    public static class EmptyStringSet3 extends ValueSerializer<Set<String>> {
        @Override
        public void serialize(Set<String> value, tools.jackson.core.JsonGenerator gen, SerializationContext ctxt)
                throws JacksonException {
            gen.writePOJO(value == null ? Set.of() : value);
        }
    }
}
