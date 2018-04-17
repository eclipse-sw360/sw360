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

import com.google.common.collect.ImmutableList;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.thrift.components.Component._Fields.*;

public class ComponentExporter extends ExcelExporter<Component, ComponentHelper> {

    private static final Map<String, String> nameToDisplayName;

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put(Component._Fields.NAME.getFieldName(), "component name");
        nameToDisplayName.put(Component._Fields.CREATED_ON.getFieldName(), "creation date");
        nameToDisplayName.put(Component._Fields.COMPONENT_TYPE.getFieldName(), "component type");
        nameToDisplayName.put(Component._Fields.CREATED_BY.getFieldName(), "created by");
        nameToDisplayName.put(Component._Fields.RELEASE_IDS.getFieldName(), "releases");
        nameToDisplayName.put(Component._Fields.MAIN_LICENSE_IDS.getFieldName(), "main license IDs");
        nameToDisplayName.put(Component._Fields.SOFTWARE_PLATFORMS.getFieldName(), "software platforms");
        nameToDisplayName.put(Component._Fields.OPERATING_SYSTEMS.getFieldName(), "operating systems");
        nameToDisplayName.put(Component._Fields.VENDOR_NAMES.getFieldName(), "vendor names");
        nameToDisplayName.put(Component._Fields.COMPONENT_OWNER.getFieldName(), "component owner");
        nameToDisplayName.put(Component._Fields.OWNER_ACCOUNTING_UNIT.getFieldName(), "owner accounting unit");
        nameToDisplayName.put(Component._Fields.OWNER_GROUP.getFieldName(), "owner group");
        nameToDisplayName.put(Component._Fields.OWNER_COUNTRY.getFieldName(), "owner country");
    }

    private static final List<Component._Fields> COMPONENT_IGNORED_FIELDS = ImmutableList.<Component._Fields>builder()
            .add(ID)
            .add(REVISION)
            .add(DOCUMENT_STATE)
            .add(PERMISSIONS)
            .add(RELEASES)
            .build();

    static final List<Component._Fields> COMPONENT_RENDERED_FIELDS = Component.metaDataMap.keySet()
            .stream()
            .filter(k -> ! COMPONENT_IGNORED_FIELDS.contains(k))
            .collect(Collectors.toList());

    static List<String> HEADERS = COMPONENT_RENDERED_FIELDS
            .stream()
            .map(Component._Fields::getFieldName)
            .map(n -> SW360Utils.displayNameFor(n, nameToDisplayName))
            .collect(Collectors.toList());

    static List<String> HEADERS_EXTENDED_BY_RELEASES = ExporterHelper.addSubheadersWithPrefixesAsNeeded(HEADERS, ReleaseExporter.HEADERS, "release: ");

    public ComponentExporter(ComponentService.Iface componentClient, List<Component> components, User user,
            boolean extendedByReleases) throws SW360Exception {
        super(new ComponentHelper(extendedByReleases, new ReleaseHelper(componentClient, user)));
        preloadLinkedReleasesFor(components, extendedByReleases);
    }

    private void preloadLinkedReleasesFor(List<Component> components, boolean extendedByReleases)
            throws SW360Exception {
        Set<String> linkedReleaseIds = components
                .stream()
                .map(Component::getReleaseIds)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Map<String, Release> releasesById = ThriftUtils.getIdMap(helper.getReleases(linkedReleaseIds));
        helper.setPreloadedLinkedReleases(releasesById, extendedByReleases);
    }
}
