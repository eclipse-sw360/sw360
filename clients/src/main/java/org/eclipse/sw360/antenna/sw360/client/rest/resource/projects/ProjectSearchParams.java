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
package org.eclipse.sw360.antenna.sw360.client.rest.resource.projects;

import java.util.Objects;

/**
 * <p>
 * A data class that allows specifying parameters for a search for SW360
 * projects.
 * </p>
 * <p>
 * The SW360 projects endpoint supports a number of parameters to restrict the
 * projects returned by a search. This class makes the search criteria
 * available explicit; so a search can be defined by setting the corresponding
 * properties on an instance. Instances are created using a builder class.
 * </p>
 */
public final class ProjectSearchParams {
    /**
     * Constant for a {@code ProjectSearchParams} instance that does not define
     * any search criteria. When using this instance, all projects that can be
     * accessed by the current user are returned.
     */
    public static final ProjectSearchParams ALL_PROJECTS = builder().build();

    /**
     * The name criterion for the search.
     */
    private final String name;

    /**
     * The type criterion for the search.
     */
    private final SW360ProjectType type;

    /**
     * The business unit criterion for the search.
     */
    private final String businessUnit;

    /**
     * The tag criterion for the search.
     */
    private final String tag;

    private ProjectSearchParams(Builder builder) {
        name = builder.name;
        type = builder.type;
        businessUnit = builder.businessUnit;
        tag = builder.tag;
    }

    /**
     * Returns the name of the projects to search for.
     *
     * @return the name search criterion (may be <strong>null</strong>)
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the type of the projects to search for.
     *
     * @return the type search criterion (may be <strong>null</strong>)
     */
    public SW360ProjectType getType() {
        return type;
    }

    /**
     * Returns the business unit of the projects to search for.
     *
     * @return the business unit search criterion (may be
     * <strong>null</strong>)
     */
    public String getBusinessUnit() {
        return businessUnit;
    }

    /**
     * Returns the tag of the projects to search for.
     *
     * @return the tag search criterion (may be <strong>null</strong>)
     */
    public String getTag() {
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectSearchParams params = (ProjectSearchParams) o;
        return Objects.equals(getName(), params.getName()) &&
                getType() == params.getType() &&
                Objects.equals(getBusinessUnit(), params.getBusinessUnit()) &&
                Objects.equals(getTag(), params.getTag());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getType(), getBusinessUnit(), getTag());
    }

    @Override
    public String toString() {
        return "ProjectSearchParams{" +
                "name='" + name + '\'' +
                ", type=" + type +
                ", businessUnit='" + businessUnit + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }

    /**
     * Returns a {@code Builder} object for creating new instances of this
     * class.
     *
     * @return the new builder for search parameters
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * A builder class for creating instances of {@link ProjectSearchParams}.
     * </p>
     * <p>
     * Using this builder class, the criteria for the project search can be
     * defined. Criteria that are not set are not taken into account for the
     * search.
     * </p>
     */
    public static final class Builder {
        /**
         * Project name to search for.
         */
        private String name;

        /**
         * Project type to search for.
         */
        private SW360ProjectType type;

        /**
         * Business unit to search for.
         */
        private String businessUnit;

        /**
         * Tag to search for.
         */
        private String tag;

        /**
         * Private constructor to prevent direct instantiation.
         */
        private Builder() {
        }

        /**
         * Sets the search criterion for the project name. This is a contains
         * search; all projects are matched whose name contains the string
         * provided here.
         *
         * @param name the project name to search for
         * @return this builder
         */
        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the search criterion for the project type. Only projects with
         * this exact type are matched.
         *
         * @param type the desired project type
         * @return this builder
         */
        public Builder withType(SW360ProjectType type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the search criterion for the projects' business unit. Only
         * projects assigned to this exact unit are matched.
         *
         * @param unit the desired business unit
         * @return this builder
         */
        public Builder withBusinessUnit(String unit) {
            this.businessUnit = unit;
            return this;
        }

        /**
         * Sets the search criterion for the project tag. Only projects with
         * this exact tag are matched.
         *
         * @param tag the desired tag
         * @return this builder
         */
        public Builder withTag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Creates a new {@code ProjectSearchParams} instance based on the
         * properties set so far on this builder instance.
         *
         * @return the {@code ProjectSearchParams} constructed by this builder
         */
        public ProjectSearchParams build() {
            return new ProjectSearchParams(this);
        }
    }
}
