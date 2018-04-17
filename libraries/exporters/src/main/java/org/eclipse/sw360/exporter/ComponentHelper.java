/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import java.util.*;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.SW360Utils.fieldValueAsString;
import static org.eclipse.sw360.datahandler.common.SW360Utils.getReleaseNames;
import static org.eclipse.sw360.exporter.ComponentExporter.COMPONENT_RENDERED_FIELDS;
import static org.eclipse.sw360.exporter.ComponentExporter.HEADERS;
import static org.eclipse.sw360.exporter.ComponentExporter.HEADERS_EXTENDED_BY_RELEASES;

class ComponentHelper implements ExporterHelper<Component> {

    private ReleaseHelper releaseHelper;
    private boolean extendedByReleases;

    ComponentHelper(boolean extendedByReleases, ReleaseHelper releaseHelper) {
        this.extendedByReleases = extendedByReleases;
        this.releaseHelper = releaseHelper;
    }

    @Override
    public int getColumns() {
        return getHeaders().size();
    }

    @Override
    public List<String> getHeaders() {
        return extendedByReleases ? HEADERS_EXTENDED_BY_RELEASES : HEADERS;
    }

    @Override
    public SubTable makeRows(Component component) throws SW360Exception {
        return extendedByReleases ? makeRowsWithReleases(component) : makeRowForComponentOnly(component);
    }

    private SubTable makeRowsWithReleases(Component component) throws SW360Exception {
        List<Release> releases = getReleases(component);
        SubTable table = new SubTable();

        if (releases.size() > 0) {
            for (Release release : releases) {
                List<String> currentRow = makeRowForComponent(component);
                currentRow.addAll(releaseHelper.makeRows(release).elements.get(0));
                table.addRow(currentRow);
            }
        } else {
            List<String> componentRowWithEmptyReleaseFields = makeRowForComponent(component);
            for (int i = 0; i < releaseHelper.getColumns(); i++) {
                componentRowWithEmptyReleaseFields.add("");
            }
            table.addRow(componentRowWithEmptyReleaseFields);
        }
        return table;
    }

    private List<String> makeRowForComponent(Component component) throws SW360Exception {
        if (!component.isSetAttachments()) {
            component.setAttachments(Collections.emptySet());
        }
        List<String> row = new ArrayList<>(getColumns());
        for (Component._Fields renderedField : COMPONENT_RENDERED_FIELDS) {
            addFieldValueToRow(row, renderedField, component);
        }
        return row;
    }

    private void addFieldValueToRow(List<String> row, Component._Fields field, Component component) throws SW360Exception {
        if (component.isSet(field)) {
            Object fieldValue = component.getFieldValue(field);
            switch (field) {
                case RELEASE_IDS:
                    row.add(fieldValueAsString(getReleaseNames(getReleases(component))));
                    break;
                case ATTACHMENTS:
                    row.add(component.attachments.size() + "");
                    break;
                default:
                    row.add(fieldValueAsString(fieldValue));
            }
        } else {
            row.add("");
        }
    }

    private SubTable makeRowForComponentOnly(Component component) throws SW360Exception {
        return new SubTable(makeRowForComponent(component));
    }

    private List<Release> getReleases(Component component) throws SW360Exception {
        return getReleases(nullToEmptySet(component.releaseIds));
    }

    List<Release> getReleases(Set<String> ids) throws SW360Exception {
        return releaseHelper.getReleases(ids);
    }

    public void setPreloadedLinkedReleases(Map<String, Release> preloadedLinkedReleases, boolean componentsNeeded)
            throws SW360Exception {
        releaseHelper.setPreloadedLinkedReleases(preloadedLinkedReleases, componentsNeeded);
    }
}
