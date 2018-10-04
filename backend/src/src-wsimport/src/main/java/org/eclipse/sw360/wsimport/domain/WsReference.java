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
public class WsReference {

    private String url;
    private String issueUrl;
    private String pomUrl;
    private String scmUrl;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIssueUrl() {
        return issueUrl;
    }

    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }

    public String getPomUrl() {
        return pomUrl;
    }

    public void setPomUrl(String pomUrl) {
        this.pomUrl = pomUrl;
    }

    public String getScmUrl() {
        return scmUrl;
    }

    public void setScmUrl(String scmUrl) {
        this.scmUrl = scmUrl;
    }
}

