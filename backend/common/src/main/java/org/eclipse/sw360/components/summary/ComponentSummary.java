/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import java.util.List;

import static org.eclipse.sw360.datahandler.thrift.ThriftUtils.copyField;

/**
 * Created by bodet on 17/02/15.
 *
 * @author cedric.bodet@tngtech.com
 */
public class ComponentSummary extends DocumentSummary<Component> {

    @Override
    protected Component summary(SummaryType type, Component document) {
        Component copy = new Component();
        if (type == SummaryType.EXPORT_SUMMARY) {
            throw new IllegalStateException("Export summaries must be built with preloaded releases.");
        } else if (type == SummaryType.DETAILED_EXPORT_SUMMARY) {
            throw new IllegalStateException("Detailed export summaries must be built with preloaded releases.");
        } else if (type == SummaryType.HOME) {
            copyField(document, copy, Component._Fields.ID);
            copyField(document, copy, Component._Fields.DESCRIPTION);
        }

        copyField(document, copy, Component._Fields.ID);
        copyField(document, copy, Component._Fields.NAME);
        copyField(document, copy, Component._Fields.VENDOR_NAMES);
        copyField(document, copy, Component._Fields.COMPONENT_TYPE);
        copyField(document, copy, Component._Fields.CATEGORIES);

        if (type == SummaryType.SUMMARY) {
            for (Component._Fields field : Component.metaDataMap.keySet()) {
                copyField(document, copy, field);
            }
        }

        return copy;
    }

    public Component makeDetailedExportSummary(Component document, List<Release> releases) {
        document.setReleases(releases);
        return document;
    }

    public Component makeExportSummary(Component document, List<Release> releases) {
        Component copy = new Component();

        copyField(document, copy, Component._Fields.ID);
        copyField(document, copy, Component._Fields.NAME);
        copyField(document, copy, Component._Fields.LANGUAGES);
        copyField(document, copy, Component._Fields.OPERATING_SYSTEMS);
        copyField(document, copy, Component._Fields.SOFTWARE_PLATFORMS);
        copyField(document, copy, Component._Fields.CREATED_BY);
        copyField(document, copy, Component._Fields.CREATED_ON);
        copyField(document, copy, Component._Fields.VENDOR_NAMES);
        copyField(document, copy, Component._Fields.MAIN_LICENSE_IDS);
        copyField(document, copy, Component._Fields.COMPONENT_TYPE);
        copyField(document, copy, Component._Fields.DEFAULT_VENDOR_ID);
        copyField(document, copy, Component._Fields.VCS);
        copyField(document, copy, Component._Fields.HOMEPAGE);
        copyField(document, copy, Component._Fields.EXTERNAL_IDS);

        for (Release release : releases) {
            Release exportRelease = new Release();
            copyField(release, exportRelease, Release._Fields.NAME);
            copyField(release, exportRelease, Release._Fields.VERSION);
            exportRelease.setComponentId("");
            copy.addToReleases(exportRelease);
        }

        return copy;
    }
}
