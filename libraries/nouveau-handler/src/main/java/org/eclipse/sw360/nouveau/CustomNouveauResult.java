/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.nouveau;

import java.util.List;

/**
 * The result of the lucene query with hits containing the document.
 * @param <T> Type of the document expected to be contained in the hit.
 */
public class CustomNouveauResult<T> extends CommonNouveauResult {

    private List<Hits<T>> hits;

    public void setHits(List<Hits<T>> hits) {
        this.hits = hits;
    }

    public List<Hits<T>> getHits() {
        return hits;
    }

    public static class Hits<T> extends CommonHits {
        private T doc;
    }
}
