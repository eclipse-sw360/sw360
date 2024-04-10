/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.model;

import java.util.List;
import java.util.Map;

public class Group {
    private String id;
    private String name;
    private String path;
    private List<Object> subGroups;
    private Map<String, Object> attributes;
    private List<Object> realmRoles;
    private Map<String, Object> clientRoles;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<Object> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<Object> subGroups) {
        this.subGroups = subGroups;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public List<Object> getRealmRoles() {
        return realmRoles;
    }

    public void setRealmRoles(List<Object> realmRoles) {
        this.realmRoles = realmRoles;
    }

    public Map<String, Object> getClientRoles() {
        return clientRoles;
    }

    public void setClientRoles(Map<String, Object> clientRoles) {
        this.clientRoles = clientRoles;
    }
}
