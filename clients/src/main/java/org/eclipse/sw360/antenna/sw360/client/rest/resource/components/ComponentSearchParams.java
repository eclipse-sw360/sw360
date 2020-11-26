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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A data class that allows specifying parameters for a search for SW360
 * components.
 * </p>
 * <p>
 * The SW360 components endpoint supports a number of parameters to restrict
 * the components returned by a search. This class makes the search criteria
 * available explicit; so a search can be defined by setting the corresponding
 * properties on an instance. Instances are created using a builder class.
 * </p>
 */
public final class ComponentSearchParams {
    /**
     * Constant for a {@code ComponentSearchParams} instance that triggers an
     * unrestricted search on all components. This should be used with care, as
     * the search might yield a huge result set.
     */
    public static final ComponentSearchParams ALL_COMPONENTS = builder().build();

    /**
     * Constant for ascending sort order.
     */
    private static final String SORT_ASC = ",ASC";

    /**
     * Constant for descending sort order.
     */
    private static final String SORT_DESC = ",DESC";

    /**
     * Criterion to search by component name.
     */
    private final String name;

    /**
     * Criterion to search by component type.
     */
    private final String componentType;

    /**
     * The parameter for the page to request if using paging.
     */
    private final String pageIndex;

    /**
     * The parameter for the number of elements on a page if using paging.
     */
    private final String pageSize;

    /**
     * The order clauses to sort the result.
     */
    private final List<String> orderClauses;

    /**
     * The names of the fields to be contained in the result.
     */
    private final List<String> fields;

    /**
     * Creates a new instance of {@code ComponentSearchParams} that is
     * initialized from the passed in builder object.
     *
     * @param builder the builder
     */
    private ComponentSearchParams(Builder builder) {
        name = builder.name;
        componentType = builder.componentType != null ? builder.componentType.name() : null;
        pageIndex = builder.pageIndex >= 0 ? String.valueOf(builder.pageIndex) : null;
        pageSize = builder.pageSize > 0 ? String.valueOf(builder.pageSize) : null;
        orderClauses = Collections.unmodifiableList(new ArrayList<>(builder.orderClauses));
        fields = Collections.unmodifiableList(new ArrayList<>(builder.fields));
    }

    /**
     * Returns a new, uninitialized {@code Builder} object that can be used to
     * create a new instance of this class. Initially, all search criteria are
     * disabled. By setting properties of the builder, filter criteria for the
     * search can be specified.
     *
     * @return the new builder for {@code ComponentSearchParams} objects
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the component name to search for. A <strong>null</strong>
     * value or empty string means that this search criterion is disabled.
     *
     * @return the component name to search for
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the component type to search for or <strong>null</strong> if
     * this search criterion is disabled.
     *
     * @return the component type to search for
     */
    public String getComponentType() {
        return componentType;
    }

    /**
     * Returns the parameter for the page index to be requested from the
     * server. A <strong>null</strong> or empty value means that paging is not
     * used for the search.
     *
     * @return the parameter for the page index
     */
    public String getPageIndex() {
        return pageIndex;
    }

    /**
     * Returns the parameter for the page size. This determines the number of
     * components to be contained on a result page. A <strong>null</strong> or
     * empty value means that paging is disabled or the default page size
     * should be used.
     *
     * @return the parameter for the page size
     */
    public String getPageSize() {
        return pageSize;
    }

    /**
     * Returns a (unmodifiable) list with the clauses defining the order of the
     * search result. Each clause consists of a field name and an optional
     * direction (ASC for ascending or DESC for descending).
     *
     * @return the list with order clauses
     */
    public List<String> getOrderClauses() {
        return orderClauses;
    }

    /**
     * Returns a (unmodifiable) list with the names of fields to be returned in
     * the search result. This allows to restrict the search result to a set of
     * fields relevant for the current use case.
     *
     * @return the list with the fields to request from the server
     */
    public List<String> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentSearchParams params = (ComponentSearchParams) o;
        return Objects.equals(getName(), params.getName()) &&
                Objects.equals(getComponentType(), params.getComponentType()) &&
                Objects.equals(getPageIndex(), params.getPageIndex()) &&
                Objects.equals(getPageSize(), params.getPageSize()) &&
                getOrderClauses().equals(params.getOrderClauses()) &&
                getFields().equals(params.getFields());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getComponentType(), getPageIndex(), getPageSize(), getOrderClauses(), getFields());
    }

    /**
     * A builder class for creating instances of {@link ComponentSearchParams}.
     */
    public static class Builder {
        /**
         * Criterion to search by component name.
         */
        private String name;

        /**
         * The component type to search for.
         */
        private SW360ComponentType componentType;

        /**
         * Index of the page to request if using paging.
         */
        private int pageIndex;

        /**
         * Number of elements on a page when using paging.
         */
        private int pageSize;

        /**
         * Stores the order clauses for sorting the result.
         */
        private final List<String> orderClauses;

        /**
         * The names of the fields to load from the server.
         */
        private final List<String> fields;

        /**
         * Creates a new instance of {@code Builder} that is initialized with
         * some default values. All search criteria are disabled per default.
         */
        private Builder() {
            orderClauses = new ArrayList<>();
            fields = new ArrayList<>();
            pageIndex = -1;
            pageSize = -1;
        }

        /**
         * Sets the search criterion for the component name. The search result
         * will contain all the components whose name includes this string.
         *
         * @param name a component name to search for
         * @return this builder
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the search criterion for the component type. This limits the
         * search result to components with this type.
         *
         * @param componentType the component type to search for
         * @return this builder
         */
        public Builder withComponentType(SW360ComponentType componentType) {
            this.componentType = componentType;
            return this;
        }

        /**
         * Sets the index of the page to be requested from the server. Setting
         * a value &gt;= 0 enables paging of the result set. Using this
         * mechanism, large result sets can be processed in multiple chunks.
         *
         * @param pageIndex the index of the page to be requested
         * @return this builder
         */
        public Builder withPage(int pageIndex) {
            this.pageIndex = pageIndex;
            return this;
        }

        /**
         * Sets the number of entries contained on a result page. This property
         * is evaluated only if paging is enabled. It then allows controlling
         * the size of the chunks that are queried.
         *
         * @param pageSize the number of elements of a result page
         * @return this builder
         */
        public Builder withPageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * Specifies a field for an ascending order. The {@code order()}
         * methods can be called multiple times to define the sort order of the
         * search result.
         *
         * @param field the name of the field to sort
         * @return this builder
         */
        public Builder orderAscending(String field) {
            orderClauses.add(field + SORT_ASC);
            return this;
        }

        /**
         * Specifies a field for a descending order. The {@code order()}
         * methods can be called multiple times to define the sort order of the
         * search result.
         *
         * @param field the name of the field to sort
         * @return this builder
         */
        public Builder orderDescending(String field) {
            orderClauses.add(field + SORT_DESC);
            return this;
        }

        /**
         * Adds the fields provided to the list of fields to query from the
         * server. Using this method, it is possible to restrict the data that
         * is queried. The embedded components in the result will contain only
         * the fields listed here. (If this method is not called, all fields
         * available are retrieved.) Unknown fields are ignored. Note that not
         * all fields of component entities are supported by this mechanism.
         *
         * @param fields a set of fields to retrieve from the server
         * @return this builder
         */
        public Builder retrieveFields(String... fields) {
            this.fields.addAll(Arrays.asList(fields));
            return this;
        }

        /**
         * Returns a new {@code ComponentSearchParams} instance that is
         * initialized from the properties set for this builder.
         *
         * @return the newly created {@code ComponentSearchParams} object
         */
        public ComponentSearchParams build() {
            return new ComponentSearchParams(this);
        }
    }
}
