/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.nouveau.designdocument;

import java.util.Objects;

/**
 * Represents a design document for a lucene index. Contains the index name
 * and the index function.
 */
public class NouveauIndexDesignDocument {
    private final String indexName;
    private final NouveauIndexFunction indexFunction;

    public NouveauIndexDesignDocument(String indexName, NouveauIndexFunction indexFunction) {
        this.indexName = indexName;
        this.indexFunction = indexFunction;
    }

    public String getIndexName() {
        return indexName;
    }

    public NouveauIndexFunction getIndexFunction() {
        return indexFunction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NouveauIndexDesignDocument that = (NouveauIndexDesignDocument) o;
        return Objects.equals(indexName, that.indexName) && Objects.equals(indexFunction, that.indexFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(indexName, indexFunction);
    }
}
