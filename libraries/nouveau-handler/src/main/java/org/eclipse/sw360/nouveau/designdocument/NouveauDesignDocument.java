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

import com.cloudant.client.api.model.DesignDocument;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * CouchDB Design document that contains nouveau indexes.
 */
public class NouveauDesignDocument extends DesignDocument {

    private JsonObject nouveau;

    public JsonObject getNouveau() {
        return this.nouveau;
    }

    public void setNouveau(JsonObject nouveau) {
        this.nouveau = nouveau;
    }

    public void addNouveau(NouveauIndexDesignDocument indexDesign,
                           @NotNull Gson gson) {
        if (this.nouveau == null) {
            this.nouveau = new JsonObject();
        }
        this.nouveau.add(indexDesign.getIndexName(), gson.toJsonTree(indexDesign.getIndexFunction()));
    }

    public int hashCode() {
        return 31 * super.hashCode() + (this.nouveau != null ? this.nouveau.hashCode() : 0);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            if (super.equals(o)) {
                return true;
            } else {
                NouveauDesignDocument that = (NouveauDesignDocument) o;
                return Objects.equals(this.nouveau, that.nouveau);
            }
        } else {
            return false;
        }
    }
}
