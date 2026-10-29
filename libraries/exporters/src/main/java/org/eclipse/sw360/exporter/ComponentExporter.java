/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.services.common.FieldAccessor;
import org.eclipse.sw360.datahandler.services.components.Component;
import org.eclipse.sw360.datahandler.services.components.Release;
import org.eclipse.sw360.datahandler.services.users.User;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.exporter.helper.ComponentHelper;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.helper.ReleaseHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ComponentExporter extends ExcelExporter<Component, ComponentHelper> {

    private static final Map<String, String> nameToDisplayName;

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put("name", "component name");
        nameToDisplayName.put("createdOn", "creation date");
        nameToDisplayName.put("componentType", "component type");
        nameToDisplayName.put("createdBy", "created by");
        nameToDisplayName.put("releaseIds", "releases");
        nameToDisplayName.put("mainLicenseIds", "main license IDs");
        nameToDisplayName.put("softwarePlatforms", "software platforms");
        nameToDisplayName.put("operatingSystems", "operating systems");
        nameToDisplayName.put("vendorNames", "vendor names");
        nameToDisplayName.put("componentOwner", "component owner");
        nameToDisplayName.put("ownerAccountingUnit", "owner accounting unit");
        nameToDisplayName.put("ownerGroup", "owner group");
        nameToDisplayName.put("ownerCountry", "owner country");
    }

    public static final List<FieldAccessor<Component>> COMPONENT_RENDERED_FIELDS = ExporterFields.except(
            Component.class, "id", "revision", "documentState", "permissions", "releases");

    public static List<String> HEADERS = COMPONENT_RENDERED_FIELDS
            .stream()
            .map(FieldAccessor::name)
            .map(n -> SW360Utils.displayNameFor(n, nameToDisplayName))
            .collect(Collectors.toList());

    public static List<String> HEADERS_EXTENDED_BY_RELEASES = ExporterHelper.addSubheadersWithPrefixesAsNeeded(HEADERS, ReleaseExporter.HEADERS, "release: ");

    public ComponentExporter(ComponentExportClient componentClient, List<Component> components, User user,
            boolean extendedByReleases) throws SW360Exception {
        super(new ComponentHelper(extendedByReleases, new ReleaseHelper(componentClient, user)));
        preloadLinkedReleasesFor(components, extendedByReleases);
    }

    public ComponentExporter(ComponentExportClient componentClient, User user,
            boolean extendedByReleases) throws SW360Exception {
        super(new ComponentHelper(extendedByReleases, new ReleaseHelper(componentClient, user)));
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
