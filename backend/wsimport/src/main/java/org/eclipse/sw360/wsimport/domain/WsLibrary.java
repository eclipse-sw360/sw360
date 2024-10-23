/*
 * Copyright (c) Verifa Oy, 2018. Part of the SW360 Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.wsimport.domain;

/**
 * @author: ksoranko@verifa.io
 */
public class WsLibrary {

    private int keyId;
    private String filename;
    private String name;
    private String version;
    private String type;
    private WsReference references;
    private WsLicense[] licenses;

    public int getKeyId() {
        return keyId;
    }

    public void setKeyId(int keyId) {
        this.keyId = keyId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public WsReference getReferences() {
        return references;
    }

    public void setReferences(WsReference references) {
        this.references = references;
    }

    public WsLicense[] getLicenses() {
        return licenses;
    }

    public void setLicenses(WsLicense[] licenses) {
        this.licenses = licenses;
    }

}

