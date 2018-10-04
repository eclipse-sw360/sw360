/*
 * Copyright (c) Verifa Oy, 2018. Part of the SW360 Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

