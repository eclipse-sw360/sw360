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

import com.google.gson.annotations.SerializedName;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * The result of the lucene query.
 */
public class NouveauResult extends CommonNouveauResult {

    @SerializedName("hits")
    private List<Hits> hits;

    public List<Hits> getHits() {
        return hits;
    }

    public void setHits(List<Hits> hits) {
        this.hits = hits;
    }

    public static class Hits extends CommonHits {

        @SerializedName("doc")
        private LinkedHashMap<String, Object> doc;

        /**
         * The stored contents of the document (when include_docs=true)
         */
        public LinkedHashMap<String, Object> getDoc() {
            return doc;
        }

        public void setDoc(LinkedHashMap<String, Object> doc) {
            this.doc = doc;
        }
    }
}
