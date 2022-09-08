/*
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.datahandler.resourcelists;

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLinkJSON;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;

import java.util.*;

public class ResourceComparatorGenerator<T> {

    private static final Map<Component._Fields, Comparator<Component>> componentMap = generateComponentMap();
    private static final Map<Project._Fields, Comparator<Project>> projectMap = generateProjectMap();
    private static final Map<Release._Fields, Comparator<Release>> releaseMap = generateReleaseMap();
    private static final Map<SearchResult._Fields, Comparator<SearchResult>> searchResultMap = generateSearchResultMap();
    private static final Map<ChangeLogs._Fields, Comparator<ChangeLogs>> changeLogMap = generateChangeLogMap();
    private static final Map<VulnerabilityDTO._Fields, Comparator<VulnerabilityDTO>> vDtoMap = generateVulDtoMap();
    private static final Map<ReleaseLinkJSON._Fields, Comparator<ReleaseLinkJSON>> releaseLinkJSONMap = generateReleaseLinkJSONMap();
    private static Map<Component._Fields, Comparator<Component>> generateComponentMap() {
        Map<Component._Fields, Comparator<Component>> componentMap = new HashMap<>();
        componentMap.put(Component._Fields.NAME, Comparator.comparing(Component::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.CREATED_ON, Comparator.comparing(Component::getCreatedOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.CREATED_BY, Comparator.comparing(Component::getCreatedBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        componentMap.put(Component._Fields.COMPONENT_TYPE, Comparator.comparing(c -> Optional.ofNullable(c.getComponentType()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(componentMap);
    }

    private static Map<Project._Fields, Comparator<Project>> generateProjectMap() {
        Map<Project._Fields, Comparator<Project>> projectMap = new HashMap<>();
        projectMap.put(Project._Fields.NAME, Comparator.comparing(Project::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        projectMap.put(Project._Fields.CREATED_ON, Comparator.comparing(Project::getCreatedOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        projectMap.put(Project._Fields.CREATED_BY, Comparator.comparing(Project::getCreatedBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        projectMap.put(Project._Fields.CLEARING_STATE, Comparator.comparing(p -> Optional.ofNullable(p.getClearingState()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        projectMap.put(Project._Fields.PROJECT_TYPE, Comparator.comparing(p -> Optional.ofNullable(p.getProjectType()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(projectMap);
    }

    private static Map<Release._Fields, Comparator<Release>> generateReleaseMap() {
        Map<Release._Fields, Comparator<Release>> releaseMap = new HashMap<>();
        releaseMap.put(Release._Fields.NAME, Comparator.comparing(Release::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap.put(Release._Fields.CLEARING_STATE, Comparator.comparing(p -> Optional.ofNullable(p.getClearingState()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(releaseMap);
    }

    private static Map<SearchResult._Fields, Comparator<SearchResult>> generateSearchResultMap() {
        Map<SearchResult._Fields, Comparator<SearchResult>> searchResultMap = new HashMap<>();
        searchResultMap.put(SearchResult._Fields.NAME, Comparator.comparing(SearchResult::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        searchResultMap.put(SearchResult._Fields.TYPE, Comparator.comparing(SearchResult::getType, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(searchResultMap);
    }

    private static Map<ChangeLogs._Fields, Comparator<ChangeLogs>> generateChangeLogMap() {
        Map<ChangeLogs._Fields, Comparator<ChangeLogs>> changeLogMap = new HashMap<>();
        changeLogMap.put(ChangeLogs._Fields.CHANGE_TIMESTAMP, Comparator.comparing(ChangeLogs::getChangeTimestamp, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        changeLogMap.put(ChangeLogs._Fields.USER_EDITED, Comparator.comparing(ChangeLogs::getUserEdited, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(changeLogMap);
    }

    private static Map<VulnerabilityDTO._Fields, Comparator<VulnerabilityDTO>> generateVulDtoMap() {
        Map<VulnerabilityDTO._Fields, Comparator<VulnerabilityDTO>> vulDTOMap = new HashMap<>();
        vulDTOMap.put(VulnerabilityDTO._Fields.EXTERNAL_ID, Comparator.comparing(VulnerabilityDTO::getExternalId, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        vulDTOMap.put(VulnerabilityDTO._Fields.TITLE, Comparator.comparing(VulnerabilityDTO::getTitle, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        vulDTOMap.put(VulnerabilityDTO._Fields.PRIORITY, Comparator.comparing(VulnerabilityDTO::getPriority, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        vulDTOMap.put(VulnerabilityDTO._Fields.PROJECT_RELEVANCE, Comparator.comparing(VulnerabilityDTO::getProjectRelevance, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(vulDTOMap);
    }

    private static Map<ReleaseLinkJSON._Fields, Comparator<ReleaseLinkJSON>> generateReleaseLinkJSONMap() {
        Map<ReleaseLinkJSON._Fields, Comparator<ReleaseLinkJSON>> releaseLinkJSONMap = new HashMap<>();
        releaseLinkJSONMap.put(ReleaseLinkJSON._Fields.RELEASE_ID, Comparator.comparing(ReleaseLinkJSON::getReleaseId, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseLinkJSONMap.put(ReleaseLinkJSON._Fields.RELEASE_LINK, null);
        releaseLinkJSONMap.put(ReleaseLinkJSON._Fields.RELEASE_RELATIONSHIP, Comparator.comparing(ReleaseLinkJSON::getReleaseRelationship));
        releaseLinkJSONMap.put(ReleaseLinkJSON._Fields.COMMENT, Comparator.comparing(ReleaseLinkJSON::getComment, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseLinkJSONMap.put(ReleaseLinkJSON._Fields.MAINLINE_STATE, Comparator.comparing(ReleaseLinkJSON::getMainlineState, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseLinkJSONMap.put(ReleaseLinkJSON._Fields.CREATE_BY, Comparator.comparing(ReleaseLinkJSON::getCreateBy, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseLinkJSONMap.put(ReleaseLinkJSON._Fields.CREATE_ON, Comparator.comparing(ReleaseLinkJSON::getCreateOn, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(releaseLinkJSONMap);
    }

    public Comparator<T> generateComparator(String type) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                return (Comparator<T>)defaultComponentComparator();
            case SW360Constants.TYPE_PROJECT:
                return (Comparator<T>)defaultProjectComparator();
            case SW360Constants.TYPE_RELEASE:
                return (Comparator<T>)defaultReleaseComparator();
            case SW360Constants.TYPE_SEARCHRESULT:
                return (Comparator<T>)defaultSearchResultComparator();
            case SW360Constants.TYPE_CHANGELOG:
                return (Comparator<T>)defaultChangeLogComparator();
            case SW360Constants.TYPE_VULNERABILITYDTO:
                return (Comparator<T>)defaultVulDtoComparator();
            default:
                throw new ResourceClassNotFoundException("No default comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateComparator(String type, String property) throws ResourceClassNotFoundException {
        return generateComparator(type, Collections.singletonList(property));
    }

    public Comparator<T> generateComparator(String type, List<String> properties) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                List<Component._Fields> fields = new ArrayList<>();
                for(String property:properties) {
                    Component._Fields field = Component._Fields.findByName(property);
                    if (field != null) {
                        fields.add(field);
                    }
                }
                return generateComparatorWithFields(type, fields);
            case SW360Constants.TYPE_PROJECT:
                List<Project._Fields> projectFields = new ArrayList<>();
                for(String property:properties) {
                    Project._Fields field = Project._Fields.findByName(property);
                    if (field != null) {
                        projectFields.add(field);
                    }
                }
                return generateProjectComparatorWithFields(type, projectFields);
            case SW360Constants.TYPE_RELEASE:
                List<Release._Fields> releaeFields = new ArrayList<>();
                for(String property:properties) {
                    Release._Fields field = Release._Fields.findByName(property);
                    if (field != null) {
                        releaeFields.add(field);
                    }
                }
                return generateReleaseComparatorWithFields(type, releaeFields);
            case SW360Constants.TYPE_SEARCHRESULT:
                List<SearchResult._Fields> searchReult = new ArrayList<>();
                for(String property:properties) {
                    SearchResult._Fields field = SearchResult._Fields.findByName(property);
                    if (field != null) {
                        searchReult.add(field);
                    }
                }
                return generateSearchResultComparatorWithFields(type, searchReult);
            case SW360Constants.TYPE_CHANGELOG:
                List<ChangeLogs._Fields> changeLogs = new ArrayList<>();
                for(String property : properties) {
                    ChangeLogs._Fields field = ChangeLogs._Fields.findByName(property);
                    if (field != null) {
                        changeLogs.add(field);
                    }
                }
                return generateChangeLogComparatorWithFields(type, changeLogs);
            case SW360Constants.TYPE_VULNERABILITYDTO:
                List<VulnerabilityDTO._Fields> vulDtos = new ArrayList<>();
                for(String property : properties) {
                    VulnerabilityDTO._Fields field = VulnerabilityDTO._Fields.findByName(property);
                    if (field != null) {
                        vulDtos.add(field);
                    }
                }
                return generateVulDTOComparatorWithFields(type, vulDtos);
            case SW360Constants.TYPE_RELEASE_LINK_JSON:
                List<ReleaseLinkJSON._Fields> releaseLinked = new ArrayList<>();
                for(String property : properties) {
                    ReleaseLinkJSON._Fields field = ReleaseLinkJSON._Fields.findByName(property);
                    if (field != null) {
                        releaseLinked.add(field);
                    }
                }
                return generateReleaseLinkJSONComparatorWithFields(type, releaseLinked);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateComparatorWithFields(String type, List<Component._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                return (Comparator<T>)componentComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateProjectComparatorWithFields(String type, List<Project._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_PROJECT:
                return (Comparator<T>)projectComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateReleaseComparatorWithFields(String type, List<Release._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_RELEASE:
                return (Comparator<T>)releaseComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateSearchResultComparatorWithFields(String type, List<SearchResult._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_SEARCHRESULT:
                return (Comparator<T>)searchResultComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateChangeLogComparatorWithFields(String type, List<ChangeLogs._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_CHANGELOG:
                return (Comparator<T>)changeLogComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateVulDTOComparatorWithFields(String type, List<VulnerabilityDTO._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_VULNERABILITYDTO:
                return (Comparator<T>)vulnDtoComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    private Comparator<Component> componentComparator(List<Component._Fields> fields) {
        Comparator<Component> comparator = Comparator.comparing(x -> true);
        for (Component._Fields field:fields) {
            Comparator<Component> fieldComparator = componentMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultComponentComparator());
        return comparator;
    }

    private Comparator<Project> projectComparator(List<Project._Fields> fields) {
        Comparator<Project> comparator = Comparator.comparing(x -> true);
        for (Project._Fields field:fields) {
            Comparator<Project> fieldComparator = projectMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultProjectComparator());
        return comparator;
    }

    private Comparator<Release> releaseComparator(List<Release._Fields> fields) {
        Comparator<Release> comparator = Comparator.comparing(x -> true);
        for (Release._Fields field:fields) {
            Comparator<Release> fieldComparator = releaseMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultReleaseComparator());
        return comparator;
    }

    private Comparator<SearchResult> searchResultComparator(List<SearchResult._Fields> fields) {
        Comparator<SearchResult> comparator = Comparator.comparing(x -> true);
        for (SearchResult._Fields field:fields) {
            Comparator<SearchResult> fieldComparator = searchResultMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultSearchResultComparator());
        return comparator;
    }

    private Comparator<ChangeLogs> changeLogComparator(List<ChangeLogs._Fields> fields) {
        Comparator<ChangeLogs> comparator = Comparator.comparing(x -> true);
        for (ChangeLogs._Fields field:fields) {
            Comparator<ChangeLogs> fieldComparator = changeLogMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultChangeLogComparator());
        return comparator;
    }

    private Comparator<VulnerabilityDTO> vulnDtoComparator(List<VulnerabilityDTO._Fields> fields) {
        Comparator<VulnerabilityDTO> comparator = Comparator.comparing(x -> true);
        for (VulnerabilityDTO._Fields field:fields) {
            Comparator<VulnerabilityDTO> fieldComparator = vDtoMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultVulDtoComparator());
        return comparator;
    }

    private Comparator<Component> defaultComponentComparator() {
        return componentMap.get(Component._Fields.NAME);
    }

    private Comparator<Project> defaultProjectComparator() {
        return projectMap.get(Project._Fields.NAME);
    }

    private Comparator<Release> defaultReleaseComparator() {
        return releaseMap.get(Release._Fields.NAME);
    }

    private Comparator<SearchResult> defaultSearchResultComparator() {
        return searchResultMap.get(SearchResult._Fields.NAME);
    }

    private Comparator<ChangeLogs> defaultChangeLogComparator() {
        return changeLogMap.get(ChangeLogs._Fields.CHANGE_TIMESTAMP);
    }

    private Comparator<VulnerabilityDTO> defaultVulDtoComparator() {
        return vDtoMap.get(VulnerabilityDTO._Fields.EXTERNAL_ID);
    }

    public Comparator<T> generateReleaseLinkJSONComparatorWithFields(String type, List<ReleaseLinkJSON._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_RELEASE_LINK_JSON:
                return (Comparator<T>)releaseLinkJSONComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    private Comparator<ReleaseLinkJSON> releaseLinkJSONComparator(List<ReleaseLinkJSON._Fields> fields) {
        Comparator<ReleaseLinkJSON> comparator = Comparator.comparing(x -> true);
        for (ReleaseLinkJSON._Fields field:fields) {
            Comparator<ReleaseLinkJSON> fieldComparator = releaseLinkJSONMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultReleaseLinkJSONComparator());
        return comparator;
    }

    private Comparator<ReleaseLinkJSON> defaultReleaseLinkJSONComparator() {
        return releaseLinkJSONMap.get(ReleaseLinkJSON._Fields.RELEASE_ID);
    }
}
