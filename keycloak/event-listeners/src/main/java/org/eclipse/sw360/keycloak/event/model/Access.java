/*
SPDX-FileCopyrightText: © 2024 Siemens AG
SPDX-License-Identifier: EPL-2.0
*/
package org.eclipse.sw360.keycloak.event.model;

public class Access {
    private boolean manageGroupMembership;
    private boolean view;
    private boolean mapRoles;
    private boolean impersonate;
    private boolean manage;

    public boolean isManageGroupMembership() {
        return manageGroupMembership;
    }

    public void setManageGroupMembership(boolean manageGroupMembership) {
        this.manageGroupMembership = manageGroupMembership;
    }

    public boolean isView() {
        return view;
    }

    public void setView(boolean view) {
        this.view = view;
    }

    public boolean isMapRoles() {
        return mapRoles;
    }

    public void setMapRoles(boolean mapRoles) {
        this.mapRoles = mapRoles;
    }

    public boolean isImpersonate() {
        return impersonate;
    }

    public void setImpersonate(boolean impersonate) {
        this.impersonate = impersonate;
    }

    public boolean isManage() {
        return manage;
    }

    public void setManage(boolean manage) {
        this.manage = manage;
    }

}
