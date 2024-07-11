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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.changelogs.ChangeLogs;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.EccInformation;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityDTO;

public class ResourceComparatorGenerator<T> {

    private static final Map<Component._Fields, Comparator<Component>> componentMap = generateComponentMap();
    private static final Map<Project._Fields, Comparator<Project>> projectMap = generateProjectMap();
    private static final Map<User._Fields, Comparator<User>> userMap = generateUserMap();
    private static final Map<Release._Fields, Comparator<Release>> releaseMap = generateReleaseMap();
    private static final Map<Release._Fields, Comparator<Release>> releaseMapForEcc = generateReleaseMapForEccComparator();
    private static final Map<EccInformation._Fields, Comparator<Release>> eccInfoMap = generateEccInfoMap();
    private static final Map<Vendor._Fields, Comparator<Vendor>> vendorMap = generateVendorMap();
    private static final Map<License._Fields, Comparator<License>> licenseMap = generateLicenseMap();
    private static final Map<Obligation._Fields, Comparator<Obligation>> obligationMap = generateObligationMap();
    private static final Map<Package._Fields, Comparator<Package>> packageMap = generatePackageMap();
    private static final Map<SearchResult._Fields, Comparator<SearchResult>> searchResultMap = generateSearchResultMap();
    private static final Map<ChangeLogs._Fields, Comparator<ChangeLogs>> changeLogMap = generateChangeLogMap();
    private static final Map<VulnerabilityDTO._Fields, Comparator<VulnerabilityDTO>> vDtoMap = generateVulDtoMap();
    private static final Map<Vulnerability._Fields, Comparator<Vulnerability>> vMap = generateVulMap();
    private static final Map<ModerationRequest._Fields, Comparator<ModerationRequest>> moderationRequestMap = generateModerationRequestMap();

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

    private static Map<User._Fields, Comparator<User>> generateUserMap() {
        Map<User._Fields, Comparator<User>> userMap = new HashMap<>();
        userMap.put(User._Fields.FULLNAME, Comparator.comparing(User::getFullname, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        userMap.put(User._Fields.EMAIL, Comparator.comparing(User::getEmail, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(userMap);
    }

    private static Map<License._Fields, Comparator<License>> generateLicenseMap() {
        Map<License._Fields, Comparator<License>> licenseMap = new HashMap<>();
        licenseMap.put(License._Fields.FULLNAME, Comparator.comparing(License::getShortname, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(licenseMap);
    }

    private static Map<Release._Fields, Comparator<Release>> generateReleaseMap() {
        Map<Release._Fields, Comparator<Release>> releaseMap = new HashMap<>();
        releaseMap.put(Release._Fields.NAME, Comparator.comparing(Release::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMap.put(Release._Fields.CLEARING_STATE, Comparator.comparing(p -> Optional.ofNullable(p.getClearingState()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(releaseMap);
    }

    private static Map<Obligation._Fields, Comparator<Obligation>> generateObligationMap() {
        Map<Obligation._Fields, Comparator<Obligation>> obligationMap = new HashMap<>();
        obligationMap.put(Obligation._Fields.TITLE, Comparator.comparing(Obligation::getTitle, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(obligationMap);
    }

    private static Map<Release._Fields, Comparator<Release>> generateReleaseMapForEccComparator() {
        Map<Release._Fields, Comparator<Release>> releaseMapForEcc = new HashMap<>();
        releaseMapForEcc.put(Release._Fields.NAME,
                Comparator.comparing(Release::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMapForEcc.put(Release._Fields.VERSION,
                Comparator.comparing(Release::getVersion, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        releaseMapForEcc.put(Release._Fields.CREATOR_DEPARTMENT, Comparator.comparing(Release::getCreatorDepartment,
                Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(releaseMapForEcc);
    }

    private static Map<EccInformation._Fields, Comparator<Release>> generateEccInfoMap() {
        Map<EccInformation._Fields, Comparator<Release>> eccInfoMap = new HashMap<>();
        eccInfoMap.put(EccInformation._Fields.ASSESSMENT_DATE,
                Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getEccInformation().getAssessmentDate()),
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        eccInfoMap.put(EccInformation._Fields.ASSESSOR_CONTACT_PERSON,
                Comparator.comparing(
                        r -> CommonUtils.nullToEmptyString(r.getEccInformation().getAssessorContactPerson()),
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        eccInfoMap.put(EccInformation._Fields.ASSESSOR_DEPARTMENT,
                Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getEccInformation().getAssessorDepartment()),
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        eccInfoMap.put(EccInformation._Fields.ECC_STATUS,
                Comparator.comparing(r -> CommonUtils.nullToEmptyString(r.getEccInformation().getEccStatus()),
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(eccInfoMap);
    }

    private static Map<Vendor._Fields, Comparator<Vendor>> generateVendorMap() {
        Map<Vendor._Fields, Comparator<Vendor>> vendorMap = new HashMap<>();
        vendorMap.put(Vendor._Fields.FULLNAME, Comparator.comparing(Vendor::getFullname, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        vendorMap.put(Vendor._Fields.SHORTNAME, Comparator.comparing(Vendor::getShortname, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(vendorMap);
    }

    private static Map<Package._Fields, Comparator<Package>> generatePackageMap() {
        Map<Package._Fields, Comparator<Package>> packageMap = new HashMap<>();
        packageMap.put(Package._Fields.NAME, Comparator.comparing(Package::getName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(packageMap);
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

    private static Map<Vulnerability._Fields, Comparator<Vulnerability>> generateVulMap() {
        Map<Vulnerability._Fields, Comparator<Vulnerability>> vulMap = new HashMap<>();
        vulMap.put(Vulnerability._Fields.EXTERNAL_ID, Comparator.comparing(Vulnerability::getExternalId, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        vulMap.put(Vulnerability._Fields.TITLE, Comparator.comparing(Vulnerability::getTitle, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        vulMap.put(Vulnerability._Fields.PRIORITY, Comparator.comparing(Vulnerability::getPriority, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(vulMap);
    }

    private static Map<ModerationRequest._Fields, Comparator<ModerationRequest>> generateModerationRequestMap() {
        Map<ModerationRequest._Fields, Comparator<ModerationRequest>> moderationRequestMap = new HashMap<>();
        moderationRequestMap.put(ModerationRequest._Fields.TIMESTAMP,
                Comparator.comparingLong(ModerationRequest::getTimestamp));
        moderationRequestMap.put(ModerationRequest._Fields.REVISION,
                Comparator.comparing(ModerationRequest::getRevision, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        moderationRequestMap.put(ModerationRequest._Fields.REQUESTING_USER,
                Comparator.comparing(ModerationRequest::getRequestingUser, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        moderationRequestMap.put(ModerationRequest._Fields.DOCUMENT_TYPE,
                Comparator.comparing(c -> Optional.ofNullable(c.getDocumentType()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        moderationRequestMap.put(ModerationRequest._Fields.DOCUMENT_NAME,
                Comparator.comparing(ModerationRequest::getDocumentName, Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        moderationRequestMap.put(ModerationRequest._Fields.MODERATION_STATE,
                Comparator.comparing(c -> Optional.ofNullable(c.getModerationState()).map(Object::toString).orElse(null), Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER)));
        return Collections.unmodifiableMap(moderationRequestMap);
    }

    public Comparator<T> generateComparator(String type) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_COMPONENT:
                return (Comparator<T>)defaultComponentComparator();
            case SW360Constants.TYPE_PROJECT:
                return (Comparator<T>)defaultProjectComparator();
            case SW360Constants.TYPE_USER:
                return (Comparator<T>)defaultUserComparator();
            case SW360Constants.TYPE_RELEASE:
                return (Comparator<T>)defaultReleaseComparator();
            case SW360Constants.TYPE_VENDOR:
                return (Comparator<T>)defaultVendorComparator();
            case SW360Constants.TYPE_SEARCHRESULT:
                return (Comparator<T>)defaultSearchResultComparator();
            case SW360Constants.TYPE_CHANGELOG:
                return (Comparator<T>)defaultChangeLogComparator();
            case SW360Constants.TYPE_VULNERABILITYDTO:
                return (Comparator<T>)defaultVulDtoComparator();
            case SW360Constants.TYPE_VULNERABILITY:
                return (Comparator<T>)defaultVulComparator();
            case SW360Constants.TYPE_MODERATION:
                return (Comparator<T>)defaultModerationRequestComparator();
            case SW360Constants.TYPE_PACKAGE:
                return (Comparator<T>)defaultPackageComparator();
            case SW360Constants.TYPE_ECC:
                return (Comparator<T>)defaultReleaseComparator();
            case SW360Constants.TYPE_LICENSE:
                return (Comparator<T>)defaultLicenseComparator();
            case SW360Constants.TYPE_OBLIGATION:
                return (Comparator<T>)defaultObligationComparator();
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
            case SW360Constants.TYPE_USER:
                List<User._Fields> userFields = new ArrayList<>();
                for(String property:properties) {
                    User._Fields field = User._Fields.findByName(property);
                    if (field != null) {
                        userFields.add(field);
                    }
                }
                return generateUserComparatorWithFields(type, userFields);
            case SW360Constants.TYPE_RELEASE:
                List<Release._Fields> releaeFields = new ArrayList<>();
                for(String property:properties) {
                    Release._Fields field = Release._Fields.findByName(property);
                    if (field != null) {
                        releaeFields.add(field);
                    }
                }
                return generateReleaseComparatorWithFields(type, releaeFields);
            case SW360Constants.TYPE_VENDOR:
                List<Vendor._Fields> vendorFields = new ArrayList<>();
                for(String property:properties) {
                    Vendor._Fields field = Vendor._Fields.findByName(property);
                    if (field != null) {
                        vendorFields.add(field);
                    }
                }
                return generateVendorComparatorWithFields(type, vendorFields);
            case SW360Constants.TYPE_PACKAGE:
                List<Package._Fields> packageFields = new ArrayList<>();
                for (String property:properties) {
                    Package._Fields field = Package._Fields.findByName(property);
                    if (field != null) {
                        packageFields.add(field);
                    }
                }
                return generatePackageComparatorWithFields(type, packageFields);
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
            case SW360Constants.TYPE_VULNERABILITY:
                List<Vulnerability._Fields> vul = new ArrayList<>();
                for(String property : properties) {
                    Vulnerability._Fields field = Vulnerability._Fields.findByName(property);
                    if (field != null) {
                        vul.add(field);
                    }
                }
                return generateVulComparatorWithFields(type, vul);
            case SW360Constants.TYPE_ECC:
                List<Release._Fields> releaseFields = new ArrayList<>();
                List<EccInformation._Fields> eccInfoFields = new ArrayList<>();
                for (String property : properties) {
                    Release._Fields field = Release._Fields.findByName(property);
                    EccInformation._Fields eccField = EccInformation._Fields.findByName(property);
                    if (field != null) {
                        releaseFields.add(field);
                    } else if (eccField != null) {
                        eccInfoFields.add(eccField);
                    }
                }
                return generateReleaseEccComparatorWithFields(type, releaseFields, eccInfoFields);
            case SW360Constants.TYPE_MODERATION:
                List<ModerationRequest._Fields> modFields = new ArrayList<>();
                for (String property : properties) {
                    ModerationRequest._Fields field = ModerationRequest._Fields.findByName(property);
                    if (field != null) {
                        modFields.add(field);
                    }
                }
                return generateModerationRequestComparatorWithFields(type, modFields);
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

    public Comparator<T> generateUserComparatorWithFields(String type, List<User._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_USER:
                return (Comparator<T>)userComparator(fields);
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

    public Comparator<T> generateReleaseEccComparatorWithFields(String type, List<Release._Fields> fields,
            List<EccInformation._Fields> eccInfoFields) throws ResourceClassNotFoundException {
        switch (type) {
        case SW360Constants.TYPE_ECC:
            return (Comparator<T>) releaseEccComparator(fields, eccInfoFields);
        default:
            throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateVendorComparatorWithFields(String type, List<Vendor._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_VENDOR:
                return (Comparator<T>)vendorComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generatePackageComparatorWithFields(String type, List<Package._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_PACKAGE:
                return (Comparator<T>)packageComparator(fields);
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

    public Comparator<T> generateVulComparatorWithFields(String type, List<Vulnerability._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_VULNERABILITY:
                return (Comparator<T>)vulnComparator(fields);
            default:
                throw new ResourceClassNotFoundException("No comparator for resource class with name " + type);
        }
    }

    public Comparator<T> generateModerationRequestComparatorWithFields(
            String type, List<ModerationRequest._Fields> fields) throws ResourceClassNotFoundException {
        switch (type) {
            case SW360Constants.TYPE_MODERATION:
                return (Comparator<T>) moderationComparator(fields);
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

    private Comparator<User> userComparator(List<User._Fields> fields) {
        Comparator<User> comparator = Comparator.comparing(x -> true);
        for (User._Fields field:fields) {
            Comparator<User> fieldComparator = userMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultUserComparator());
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

    private Comparator<?> releaseEccComparator(List<Release._Fields> fields,
            List<EccInformation._Fields> eccInfoFields) {
        Comparator<Release> comparator = Comparator.comparing(x -> true);
        Comparator<Release> eccComparator = Comparator.comparing(x -> true);
        if (!fields.isEmpty()) {
            for (Release._Fields field : fields) {
                Comparator<Release> fieldComparator = releaseMapForEcc.get(field);
                if (fieldComparator != null) {
                    comparator = comparator.thenComparing(fieldComparator);
                }
            }
            comparator = comparator.thenComparing(defaultReleaseComparator());
        } else if (!eccInfoFields.isEmpty()) {
            for (EccInformation._Fields eccField : eccInfoFields) {
                Comparator<Release> eccFieldComparator = eccInfoMap.get(eccField);
                if (eccFieldComparator != null) {
                    eccComparator = eccComparator.thenComparing(eccFieldComparator);
                }
            }
            eccComparator = eccComparator.thenComparing(defaultEccComparator());
            return eccComparator;
        }
        return comparator;
    }

    private Comparator<Vendor> vendorComparator(List<Vendor._Fields> fields) {
        Comparator<Vendor> comparator = Comparator.comparing(x -> true);
        for (Vendor._Fields field:fields) {
            Comparator<Vendor> fieldComparator = vendorMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultVendorComparator());
        return comparator;
    }

    private Comparator<Package> packageComparator(List<Package._Fields> fields) {
        Comparator<Package> comparator = Comparator.comparing(x -> true);
        for (Package._Fields field:fields) {
            Comparator<Package> fieldComparator = packageMap.get(field);
            if (fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultPackageComparator());
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

    private Comparator<Vulnerability> vulnComparator(List<Vulnerability._Fields> fields) {
        Comparator<Vulnerability> comparator = Comparator.comparing(x -> true);
        for (Vulnerability._Fields field:fields) {
            Comparator<Vulnerability> fieldComparator = vMap.get(field);
            if(fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultVulComparator());
        return comparator;
    }

    private Comparator<ModerationRequest> moderationComparator(List<ModerationRequest._Fields> fields) {
        Comparator<ModerationRequest> comparator = Comparator.comparing(x -> true);
        for (ModerationRequest._Fields field : fields) {
            Comparator<ModerationRequest> fieldComparator = moderationRequestMap.get(field);
            if (fieldComparator != null) {
                comparator = comparator.thenComparing(fieldComparator);
            }
        }
        comparator = comparator.thenComparing(defaultModerationRequestComparator());
        return comparator;
    }

    private Comparator<Component> defaultComponentComparator() {
        return componentMap.get(Component._Fields.NAME);
    }

    private Comparator<Project> defaultProjectComparator() {
        return projectMap.get(Project._Fields.NAME);
    }

    private Comparator<User> defaultUserComparator() {
        return userMap.get(User._Fields.EMAIL);
    }

    private Comparator<Release> defaultReleaseComparator() {
        return releaseMap.get(Release._Fields.NAME);
    }

    private Comparator<Vendor> defaultVendorComparator() {
        return vendorMap.get(Vendor._Fields.FULLNAME);
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

    private Comparator<Vulnerability> defaultVulComparator() {
        return vMap.get(Vulnerability._Fields.EXTERNAL_ID);
    }

    private Comparator<ModerationRequest> defaultModerationRequestComparator() {
        return moderationRequestMap.get(ModerationRequest._Fields.TIMESTAMP);
    }

    private Comparator<Package> defaultPackageComparator() {
        return packageMap.get(Package._Fields.NAME);
    }

    private Comparator<Release> defaultEccComparator() {
        return eccInfoMap.get(EccInformation._Fields.ASSESSMENT_DATE);
    }

    private Comparator<License> defaultLicenseComparator() {
        return licenseMap.get(License._Fields.FULLNAME);
    }

    private Comparator<Obligation> defaultObligationComparator() {
        return obligationMap.get(Obligation._Fields.TITLE);
    }
}
