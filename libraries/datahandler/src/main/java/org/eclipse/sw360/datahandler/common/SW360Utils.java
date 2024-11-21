/*
 * Copyright Siemens AG, 2014-2017, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.google.gson.Gson;
import org.eclipse.sw360.datahandler.couchdb.DatabaseMixInForChangeLog.ProjectProjectRelationshipMixin;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.permissions.PermissionUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel;
import org.eclipse.sw360.datahandler.thrift.packages.Package;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.ReleaseVulnerabilityRelation;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxdocument.SPDXDocument;
import org.eclipse.sw360.datahandler.thrift.spdx.documentcreationinformation.DocumentCreationInformation;
import org.eclipse.sw360.datahandler.thrift.spdx.spdxpackageinfo.PackageInformation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.thrift.TEnum;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VulnerabilityService;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

/**
 * @author Cedric.Bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */
public class SW360Utils {

    private final static Logger log = LogManager.getLogger(SW360Utils.class);

    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final Comparator<ReleaseLink> RELEASE_LINK_COMPARATOR = Comparator.comparing(rl -> getReleaseFullname(rl.getVendor(), rl.getName(), rl.getVersion()).toLowerCase());
    private static final ObjectMapper objectMapper;
    private static final String DIGIT_AND_DECIMAL_REGEX = "[^\\d.]";
    private static final Comparator<Entry<String, ObligationStatusInfo>> COMPARE_BY_OBLIGATIONTYPE = Comparator
            .comparing(Entry<String, ObligationStatusInfo>::getValue, (osi1, osi2) -> {
                String type1 = "", type2 = "";
                if (Objects.nonNull(osi1) && Objects.nonNull(osi1.getObligationType())) {
                    type1 = osi1.getObligationType().name();
                }
                if (Objects.nonNull(osi2) && Objects.nonNull(osi2.getObligationType())) {
                    type2 = osi2.getObligationType().name();
                }
                return type1.compareToIgnoreCase(type2);
            });

    public static Joiner spaceJoiner = Joiner.on(" ");
    public static Joiner commaJoiner = Joiner.on(", ");

    public static String INACCESSIBLE_RELEASE ="RestrictedRelease";

    private SW360Utils() {
        // Utility class with only static functions
    }

    static{
        objectMapper = new ObjectMapper();
        SimpleModule customModule = new SimpleModule("SW360 serializers");
        customModule.setMixInAnnotation(ProjectProjectRelationship.class, ProjectProjectRelationshipMixin.class);
        customModule.addSerializer(TEnum.class, new TEnumSerializer());
        customModule.addSerializer(ProjectReleaseRelationship.class, new ProjectReleaseRelationshipSerializer());
        objectMapper.registerModule(customModule);
    }

    /**
     * Returns a string for the current date in the form "yyyy-MM-dd"
     */
    public static String getCreatedOn() {
        return new SimpleDateFormat(FORMAT_DATE).format(new Date());
    }

    /**
     * Returns a string for the current date in the form "yyyy-MM-dd HH:mm:ss"
     */
    public static String getCreatedOnTime() {
        return new SimpleDateFormat(FORMAT_DATE_TIME).format(new Date());
    }

    /**
     * Tries to parse a given date in format "yyyy-MM-dd HH:mm:ss" to a Date, returns null if it fails
     * @param date in format "yyyy-MM-dd HH:mm:ss"
     * @return Date
     */
    public static Date getDateFromTimeString(String date){
        try {
            return new SimpleDateFormat(FORMAT_DATE_TIME).parse(date);
        } catch (ParseException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * returns a string of a given date in the form "yyyy-MM-dd HH:mm:ss"
     * @param date
     * @return {@link String}
     */
    public static String getDateTimeString(Date date){
        return new SimpleDateFormat(FORMAT_DATE_TIME).format(date);
    }

    /**
     * Filter BU to first three blocks
     */
    public static String getBUFromOrganisation(String organisation) {
        if(Strings.isNullOrEmpty(organisation)) return "";

        List<String> parts = Arrays.asList(organisation.toUpperCase().split("\\s"));

        int maxIndex = Math.min(parts.size(), 3);

        return spaceJoiner.join(parts.subList(0, maxIndex)).toUpperCase();
    }

    public static Set<String> filterBUSet(String organisation, Set<String> strings) {
        if (strings == null || isNullOrEmpty(organisation)) {
            return new HashSet<String>();
        }
        String bu = getBUFromOrganisation(organisation);
        return strings
                .stream()
                .filter(string-> bu.equals(string))
                .collect(Collectors.toSet());
    }

    /**
     * Set the vendor id if the vendor object is set
     */
    public static void setVendorId(Release release) {
        // Save the vendor ID, not its contents
        if (release.isSetVendor()) {
            Vendor vendor = release.getVendor();
            release.setVendorId(vendor.getId());
            release.unsetVendor();
        }
        if (isNullOrEmpty(release.getVendorId())){
            release.unsetVendorId();
        }
    }

    public static Set<String> getReleaseIds(Collection<Release> in) {
        return in.stream().map(Release::getId).collect(Collectors.toSet());
    }

    public static Set<String> getComponentIds(Collection<Component> in) {
        return in.stream().map(Component::getId).collect(Collectors.toSet());
    }

    public static Set<String> getProjectIds(Collection<Project> in) {
        return in.stream().map(Project::getId).collect(Collectors.toSet());
    }

    public static String getVersionedName(String name, String version) {
        if (isNullOrEmpty(version)) {
            return name;
        } else {
            return name + " (" + version + ")";
        }
    }

    public static String printName(Component component) {
        if (component == null || isNullOrEmpty(component.getName())) {
            return "New Component";
        }

        return component.getName();
    }

    public static String printName(Release release) {
        if (release == null || isNullOrEmpty(release.getName())) {
            return "New Release";
        }

        return getVersionedName(release.getName(), release.getVersion());
    }

    public static List<Attachment> getApprovedClxAttachmentForRelease(Release release) {
        Predicate<Attachment> isApprovedCLI = attachment -> CheckStatus.ACCEPTED.equals(attachment.getCheckStatus())
                && AttachmentType.COMPONENT_LICENSE_INFO_XML.equals(attachment.getAttachmentType());
        if (release.getAttachments() == null) {
            return new ArrayList<Attachment>();
        }
        return release.getAttachments().stream().filter(isApprovedCLI).collect(Collectors.toList());
    }

    public static List<Attachment> getClxAttachmentForRelease(Release release) {
        Predicate<Attachment> isCLI = attachment -> AttachmentType.COMPONENT_LICENSE_INFO_XML.equals(attachment.getAttachmentType());
        if (release.getAttachments() == null) {
            return new ArrayList<Attachment>();
        }
        return release.getAttachments().stream().filter(isCLI).collect(Collectors.toList());
    }

    public static Map<String, String> getReleaseIdtoAcceptedCLIMappings(Map<String, ObligationStatusInfo> obligationStatusMap) {
        if (null == obligationStatusMap) {
            return Maps.newHashMap();
        }
        return obligationStatusMap.values().stream().flatMap(e -> (e.getReleaseIdToAcceptedCLI() != null ? e.getReleaseIdToAcceptedCLI() : new HashMap<String,String>()).entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue));
    }

    public static String dropCommentedLine(Class<?> clazz, String TEMPLATE_FILE) {
        String text = new String(CommonUtils.loadResource(clazz, TEMPLATE_FILE).orElse(new byte[0]));
        return text.replaceAll("(?m)^#.*(?:\r?\n)?", ""); // ignore comments in template file
    }

    public static boolean isValidDate(String date, DateTimeFormatter format, Long greaterThanDays) {
        try {
            LocalDate selectedDate = LocalDate.parse(date, format);
            LocalDate currentDate = LocalDate.now();
            long difference = ChronoUnit.DAYS.between(currentDate, selectedDate);
            if (null != greaterThanDays) {
                return difference >= greaterThanDays;
            }
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isValidDate(String currRequestedClearingDate, String newRequestedClearingDate, DateTimeFormatter format) {
        try {
            LocalDate currLocalDate = LocalDate.parse(currRequestedClearingDate, format);
            LocalDate requestedLocalDate = LocalDate.parse(newRequestedClearingDate, format);

            return requestedLocalDate.isAfter(currLocalDate);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static String printFullname(Release release) {
        if (release == null || isNullOrEmpty(release.getName())) {
            return "New Release";
        }
        String vendorName = Optional.ofNullable(release.getVendor()).map(Vendor::getShortname).orElse(null);
        return getReleaseFullname(vendorName, release.getName(), release.getVersion());
    }

    @NotNull
    public static String getReleaseFullname(String vendorName, String releaseName, String version) {
        StringBuilder sb = new StringBuilder();
        if (!isNullOrEmpty(vendorName)){
            sb.append(vendorName).append(" ");
        }
        sb.append(releaseName);
        if (!isNullOrEmpty(version)){
            sb.append(" ").append(version);
        }
        return sb.toString();
    }

    public static String printName(Package pkg) {
        if (pkg == null || isNullOrEmpty(pkg.getName())) {
            return "New Package";
        }
        return getVersionedName(pkg.getName(), pkg.getVersion());
    }

    public static String printName(Project project) {
        if (project == null || isNullOrEmpty(project.getName())) {
            return "New Project";
        }
        return getVersionedName(project.getName(), project.getVersion());
    }

    public static String printName(License license) {
        if (license == null || isNullOrEmpty(license.getId())) {
            return "New License";
        }
        return license.getId();
    }

    public static String printName(Vulnerability vulnerability) {
        if (vulnerability == null || isNullOrEmpty(vulnerability.getId())) {
            return "";
        }
        return vulnerability.getExternalId();
    }

    public static String printName(User user) {
        if (user == null || isNullOrEmpty(user.getEmail())) {
            return "New User";
        }
        return user.getEmail();
    }

    public static String printName(SPDXDocument spdx) {
        if (spdx == null || isNullOrEmpty(spdx.getId())) {
            return "New SPDX Document";
        }
        return spdx.getId();
    }

    public static String printName(DocumentCreationInformation documentCreationInfo) {
        if (documentCreationInfo == null || isNullOrEmpty(documentCreationInfo.getName())) {
            return "New SPDX Document Creation Info";
        }
        return documentCreationInfo.getName();
    }

    public static String printName(PackageInformation packageInfo) {
        if (packageInfo == null || isNullOrEmpty(packageInfo.getName())) {
            return "New SPDX Package Info";
        }
        return packageInfo.getName();
    }

    public static String printFullname(User user) {
        if (user == null || isNullOrEmpty(user.getEmail())) {
            return "New User";
        }
        StringBuilder sb = new StringBuilder(CommonUtils.nullToEmptyString(user.getFullname())).append(" (")
                .append(user.getEmail()).append(")");
        return sb.toString();
    }

    public static boolean isWriteAccessUser(String writeAccessUserEmail, User loggedInUser, UserGroup userGroup) {
        if (CommonUtils.isNullEmptyOrWhitespace(writeAccessUserEmail)) {
            return false;
        }
        if (writeAccessUserEmail.equalsIgnoreCase(loggedInUser.getEmail())) {
            return true;
        }
        return isUserAtleastDesiredRoleInPrimaryOrSecondaryGroup(loggedInUser, userGroup);
    }

    public static boolean isUserAtleastDesiredRoleInPrimaryOrSecondaryGroup(User user, UserGroup userGroup) {
        if (PermissionUtils.isUserAtLeast(userGroup, user)) {
            return true;
        }
        if (CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())) {
            return false;
        }
        return (PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(userGroup,
                user.getSecondaryDepartmentsAndRoles().values().stream().flatMap(Set::stream).collect(Collectors.toSet())));
    }

    public static String getSW360Version() {
        final MavenXpp3Reader reader = new MavenXpp3Reader();
        try (InputStreamReader iStreamReader = new InputStreamReader(
                SW360Utils.class.getResourceAsStream(SW360Constants.DATA_HANDLER_POM_FILE_PATH))) {
            Model model = reader.read(iStreamReader);
            return model.getVersion();
        } catch (Exception e) {
            log.error("Error while getting SW360 version information: "+ e);
            return SW360Constants.NA;
        }
    }

    public static Collection<ProjectLink> getLinkedProjects(Project project, boolean deep, ThriftClients thriftClients, Logger log, User user) {
        if (project != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                List<ProjectLink> linkedProjects = client.getLinkedProjectsOfProject(project, deep, user);
                return linkedProjects;
            } catch (TException e) {
                log.error("Could not get linked projects", e);
            }
        }
        return Collections.emptyList();
    }

    public static Collection<ProjectLink> getLinkedProjects(String id, boolean deep, ThriftClients thriftClients, Logger log, User user) {
        if (id != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                List<ProjectLink> linkedProjects = client.getLinkedProjectsById(id, deep, user);
                return linkedProjects;
            } catch (TException e) {
                log.error("Could not get linked projects", e);
            }
        }
        return Collections.emptyList();
    }

    public static Map<String, Set<String>> getLinkedReleaseIdsOfAllSubProjectsAsFlatList(Project project, Set<String> projectIds, Set<String> releaseIds, Set<String> packageIds, ProjectService.Iface client, User user) {
        for (String projId : CommonUtils.getNullToEmptyKeyset(project.getLinkedProjects())) {
            if (!projectIds.contains(projId)) {
                try {
                    projectIds.add(projId);
                    project = client.getProjectById(projId, user);
                    if (project.getReleaseIdToUsageSize() > 0) {
                        releaseIds.addAll(project.getReleaseIdToUsage().keySet());
                    }
                    if (project.getPackageIdsSize() > 0) {
                        packageIds.addAll(project.getPackageIds());
                    }
                    if (project.getLinkedProjectsSize() > 0) {
                        getLinkedReleaseIdsOfAllSubProjectsAsFlatList(project, projectIds, releaseIds, packageIds, client, user);
                    }
                } catch (TException e) {
                    log.error("Could not get linked projects while exporting SBOM: ", e);
                }
            }
        }
        Map<String, Set<String>> idsMap = new HashMap<>();
        idsMap.put(SW360Constants.RELEASE_IDS, releaseIds);
        idsMap.put(SW360Constants.PACKAGE_IDS, packageIds);
        return idsMap;
    }

    public static Collection<ProjectLink> getLinkedProjectsAsFlatList(Project project, boolean deep, ThriftClients thriftClients, Logger log, User user) {
        return flattenProjectLinkTree(getLinkedProjects(project, deep, thriftClients, log, user));
    }

    public static Collection<ProjectLink> getLinkedProjectsAsFlatList(String id, boolean deep, ThriftClients thriftClients, Logger log, User user) {
        return flattenProjectLinkTree(getLinkedProjects(id, deep, thriftClients, log, user));
    }

    public static Collection<ProjectLink> flattenProjectLinkTree(Collection<ProjectLink> linkedProjects) {
        List<ProjectLink> result = new ArrayList<>();

        for (ProjectLink projectLink : linkedProjects) {
            result.add(projectLink);
            if (projectLink.isSetSubprojects()){
                result.addAll(flattenProjectLinkTree(projectLink.getSubprojects()));
            }
        }

        return result;
    }

    public static Collection<ProjectLink> getLinkedProjectsAsFlatList(Project project, boolean deep,
            ThriftClients thriftClients, Logger log, User user,
            Set<ProjectRelationship> selectedProjectRelationAsList) {
        Collection<ProjectLink> linkedProjects = getLinkedProjects(project, deep, thriftClients, log, user);
        if (CommonUtils.isNotEmpty(linkedProjects)) {
            filteredListOfLinkedProject(linkedProjects.iterator().next().getSubprojects(),
                    selectedProjectRelationAsList);
        }
        return flattenProjectLinkTree(linkedProjects);
    }

    public static void filteredListOfLinkedProject(Collection<ProjectLink> linkedProjects,
            Set<ProjectRelationship> selectedProjectRelationAsList) {
        if (CommonUtils.isNotEmpty(linkedProjects)) {
            Collection<ProjectLink> linkedProjectsFiltered=linkedProjects.stream()
                    .filter(projectLink -> selectedProjectRelationAsList.contains(projectLink.getRelation())).collect(Collectors.toSet());
            linkedProjects.retainAll(linkedProjectsFiltered);
            linkedProjects.forEach(projectLink -> filteredListOfLinkedProject(projectLink.getSubprojects(),
                            selectedProjectRelationAsList));
        }
    }

    public static List<ReleaseLink> getLinkedReleases(Project project, ThriftClients thriftClients, Logger log) {
        if (project != null && project.getReleaseIdToUsage() != null) {
            try {
                ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                return componentClient.getLinkedReleases(project.getReleaseIdToUsage());
            } catch (TException e) {
                log.error("Could not get linked releases", e);
            }
        }
        return Collections.emptyList();
    }

    public static List<ReleaseLink> getLinkedReleasesWithAccessibility(Project project, ThriftClients thriftClients, Logger log, User user) {
        if (project != null && project.getReleaseIdToUsage() != null) {
            try {
                ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                return componentClient.getLinkedReleasesWithAccessibility(project.getReleaseIdToUsage(), user);
            } catch (TException e) {
                log.error("Could not get linked releases", e);
            }
        }
        return Collections.emptyList();
    }

    public static List<ReleaseLink> getLinkedReleaseRelations(Release release, ThriftClients thriftClients, Logger log) {
        if (release != null && release.getReleaseIdToRelationship() != null) {
            try {
                ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                return componentClient.getLinkedReleaseRelations(release.getReleaseIdToRelationship());
            } catch (TException e) {
                log.error("Could not get linked releases", e);
            }
        }
        return Collections.emptyList();
    }

    public static List<ReleaseLink> getLinkedReleaseRelationsWithAccessibility(Release release, ThriftClients thriftClients, Logger log, User user) {
        if (release != null && release.getReleaseIdToRelationship() != null) {
            try {
                ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                return componentClient.getLinkedReleaseRelationsWithAccessibility(release.getReleaseIdToRelationship(), user);
            } catch (TException e) {
                log.error("Could not get linked releases", e);
            }
        }
        return Collections.emptyList();
    }

    public static Predicate<String> startsWith(final String prefix) {
        return new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input != null && input.startsWith(prefix);
            }
        };
    }

    public static List<String> getLicenseNamesFromIds(Collection<String> ids, String department) throws TException {
        final List<License> licenseList = getLicenses(ids, department);

        return getLicenseNamesFromLicenses(licenseList);
    }

    public static List<License> getLicenses(Collection<String> ids, String department) throws TException {
        if (ids != null && ids.size() > 0) {
            LicenseService.Iface client = new ThriftClients().makeLicenseClient();
            return client.getByIds(new HashSet<>(ids), department);
        } else return Collections.emptyList();
    }

    @NotNull
    public static List<String> getLicenseNamesFromLicenses(List<License> licenseList) {
        List<String> resultList = new ArrayList<>();
        for (License license : licenseList) {
            resultList.add(license.getFullname());
        }
        return resultList;
    }

    public static Map<String, License> getStringLicenseMap(User user, Set<String> licenseIds) {
        Map<String, License> idToLicense;

        try {
            final List<License> licenses = getLicenses(licenseIds, user.getDepartment());
            idToLicense = ThriftUtils.getIdMap(licenses);
        } catch (TException e) {
            idToLicense = Collections.emptyMap();
        }
        return idToLicense;
    }

    public static String fieldValueAsString(Object fieldValue) throws SW360Exception {
        if(fieldValue == null){
            return "";
        }
        if (fieldValue instanceof TEnum) {
            return nullToEmpty(ThriftEnumUtils.enumToString((TEnum) fieldValue));
        }
        if (fieldValue instanceof String) {
            return nullToEmpty((String) fieldValue);
        }
        if (fieldValue instanceof Map) {
            Map<String, Object> originalMap = nullToEmptyMap(((Map<String, Object>) fieldValue));
            // cannot use CommonUtils.nullToEmptyString here, because it calls toString() on non-null objects,
            // which destroys the chance for ObjectMapper to serialize values according to its configuration
            Map<String, Object> map = Maps.transformValues(originalMap, v -> v == null ? "" : v);
            return serializeToJson(map);
        }
        if (fieldValue instanceof Iterable){
            return serializeToJson(fieldValue);
        }
        return fieldValue.toString();
    }

    private static String serializeToJson(Object value) throws SW360Exception{
        ObjectMapper mapper = getObjectMapper();
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            String msg = String.format("Cannot serialize field value %s to JSON", value);
            log.error(msg, e);
            throw new SW360Exception(msg);
        }
    }

    @NotNull
    private static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public static void initializeMailNotificationsPreferences(User user) {
        if(!user.isSetWantsMailNotification()) {
            user.setWantsMailNotification(true);
        }
        if (!user.isSetNotificationPreferences()){
            user.setNotificationPreferences(Maps.newHashMap(SW360Constants.DEFAULT_NOTIFICATION_PREFERENCES));
        }
    }

    public static String notificationPreferenceKey(String notificationClass, String roleName){
        return notificationClass + roleName;
    }

    private static class TEnumSerializer extends JsonSerializer<TEnum>{
        @Override
        public void serialize(TEnum value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString(nullToEmpty(ThriftEnumUtils.enumToString(value)));
        }
    }

    private static class ProjectReleaseRelationshipSerializer extends JsonSerializer<ProjectReleaseRelationship>{
        @Override
        public void serialize(ProjectReleaseRelationship value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeStartObject();
            jgen.writeObjectField("releaseRelation", value.getReleaseRelation());
            jgen.writeObjectField("mainlineState", value.getMainlineState());
            jgen.writeObjectField("comment", nullToEmpty(value.getComment()));
            jgen.writeObjectField("createdBy", nullToEmpty(value.getCreatedBy()));
            jgen.writeObjectField("createdOn", nullToEmpty(value.getCreatedOn()));
            jgen.writeEndObject();
        }
    }

    public static String displayNameFor(String name, Map<String, String> nameToDisplayName){
        return nameToDisplayName.containsKey(name)? nameToDisplayName.get(name) : name;
    }

    public static <T> Map<String, T> putReleaseNamesInMap(Map<String, T> map, List<Release> releases) {
        if(map == null || releases == null) {
            return Collections.emptyMap();
        }
        Map<String, T> releaseNamesMap = new HashMap<>();
        releases.stream()
                .forEach(r -> releaseNamesMap.put(printName(r),map.get(r.getId())));
        return releaseNamesMap;
    }

    public static <T> Map<String, T> putAccessibleReleaseNamesInMap(Map<String, T> map, List<Release> releases, User user, T inaccessibleValue) {
        if (map == null || releases == null) {
            return Collections.emptyMap();
        }
        Map<String, T> releaseNamesMap = new HashMap<>();
        int inaccessbileReleaseCount = 0;
        if (!CommonUtils.isNullOrEmptyCollection(releases)) {
            for (Release release : releases) {
                if (release.isSetPermissions() && release.getPermissions().get(RequestedAction.READ)) {
                    releaseNamesMap.put(printName(release), map.get(release.getId()));
                } else {
                    inaccessbileReleaseCount++;
                    releaseNamesMap.put(String.format("%s%d", INACCESSIBLE_RELEASE, inaccessbileReleaseCount), inaccessibleValue);
                }
            }
        }
        return releaseNamesMap;
    }

    public static <T> Map<String, T> putProjectNamesInMap(Map<String, T> map, List<Project> projects) {
        if(map == null || projects == null) {
            return Collections.emptyMap();
        }
        Map<String, T> projectNamesMap = new HashMap<>();
        projects.stream()
                .forEach(p -> projectNamesMap.put(printName(p),map.get(p.getId())));
        return projectNamesMap;
    }

    public static List<String> getReleaseNames(List<Release> releases) {
        if (releases == null) return Collections.emptyList();
        return releases.stream().map(SW360Utils::printName).collect(Collectors.toList());
    }

    public static EccInformation newDefaultEccInformation(){
        return new EccInformation().setEccStatus(ECCStatus.OPEN);
    }

    @NotNull
    public static <T> Set<T> unionValues(Map<?, Set<T>>map){
        return nullToEmptyMap(map).values().stream().filter(Objects::nonNull).reduce(Sets::union).orElse(Sets.newHashSet());
    }

    public static Set<ExternalToolProcess> getExternalToolProcessesForTool(Release release, ExternalTool et) {
        if (release == null || et == null) {
            return new HashSet<>();
        }

        return nullToEmptySet(release.getExternalToolProcesses()) //
                .stream() //
                .filter(etp -> et.equals(etp.getExternalTool())) //
                .collect(Collectors.toSet());
    }

    public static Set<ExternalToolProcess> getNotOutdatedExternalToolProcessesForTool(Release release,
            ExternalTool et) {
        return getExternalToolProcessesForTool(release, et) //
                .stream() //
                .filter(etp -> !ExternalToolProcessStatus.OUTDATED.equals(etp.getProcessStatus())) //
                .collect(Collectors.toSet());
    }

    public static int getTotalReleaseCount(ReleaseClearingStateSummary clearingSummary) {
        return clearingSummary.getNewRelease() + clearingSummary.getReportAvailable() + clearingSummary.getUnderClearing()
                + clearingSummary.getSentToClearingTool()+ clearingSummary.getApproved();
    }

    public static int getOpenReleaseCount(ReleaseClearingStateSummary clearingSummary) {
        return getTotalReleaseCount(clearingSummary) - (clearingSummary.getApproved() + clearingSummary.getReportAvailable());
    }

    public static String convertEpochTimeToDate(long timestamp) {
        LocalDate date = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("UTC")).toLocalDate();
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public static ClearingRequestSize determineCRSize(int totalReleaseCount) {
        if (totalReleaseCount <= CLEARING_REQUEST_SIZE_MAP.get(ClearingRequestSize.VERY_SMALL)) {
            return ClearingRequestSize.VERY_SMALL;
        } else if (totalReleaseCount <= CLEARING_REQUEST_SIZE_MAP.get(ClearingRequestSize.SMALL)) {
            return ClearingRequestSize.SMALL;
        } else if (totalReleaseCount <= CLEARING_REQUEST_SIZE_MAP.get(ClearingRequestSize.MEDIUM)) {
            return ClearingRequestSize.MEDIUM;
        } else if (totalReleaseCount <= CLEARING_REQUEST_SIZE_MAP.get(ClearingRequestSize.LARGE)) {
            return ClearingRequestSize.LARGE;
        } else {
            return ClearingRequestSize.VERY_LARGE;
        }
    }

    public static final HashMap<ClearingRequestSize, Integer> CLEARING_REQUEST_SIZE_MAP = new HashMap<>() {{
        put(ClearingRequestSize.VERY_SMALL, 20);
        put(ClearingRequestSize.SMALL, 50);
        put(ClearingRequestSize.MEDIUM, 75);
        put(ClearingRequestSize.LARGE, 150);
    }};

    /**
     * Assumes that the process exists.
     */
    public static ExternalToolProcessStep getExternalToolProcessStepOfFirstProcessForTool(Release release,
            ExternalTool et, String stepName) {
        if (release == null || et == null || CommonUtils.isNullEmptyOrWhitespace(stepName)) {
            return null;
        }

        return getNotOutdatedExternalToolProcessesForTool(release, et) //
                .stream() //
                .findFirst() //
                .map(ExternalToolProcess::getProcessSteps) //
                .orElseGet(Lists::newArrayList)//
                .stream() //
                .filter(etps -> stepName.equals(etps.getStepName())) //
                .findFirst() //
                .orElse(null);
    }

    public static Integer parseStringToNumber(String input) {
        final String digitsOnly = input.replaceAll(DIGIT_AND_DECIMAL_REGEX, "");

        if ("".equals(digitsOnly))
            return 0;
        try {
            return Integer.parseInt(digitsOnly);
        } catch (NumberFormatException nfe) {
            log.error("Number Format Exception while parsing clearing request Id: "+digitsOnly);
            return 0;
        }
    }

    public static Map<String, ObligationStatusInfo> getProjectComponentOrganisationLicenseObligationToDisplay(
            Map<String, ObligationStatusInfo> obligationStatusMap, List<Obligation> obligations,
            ObligationLevel oblLevel, boolean addFromDB) {
        Map<String, ObligationStatusInfo> obligationAlreadyPresent = obligationStatusMap.entrySet().stream()
                .filter(Objects::nonNull).filter(e -> Objects.nonNull(e.getValue()))
                .filter(e -> Objects.nonNull(e.getValue().getObligationLevel()))
                .filter(e -> e.getValue().getObligationLevel().equals(oblLevel)).collect(Collectors.toMap(
                        e -> e.getKey(), e -> e.getValue(), (oldValue, newValue) -> oldValue));
        obligationAlreadyPresent.entrySet().stream().forEach(e -> obligationStatusMap.remove(e.getKey()));

        Map<String, ObligationStatusInfo> mapOfObligations = obligations.stream().filter(Objects::nonNull)
                .filter(o -> Objects.nonNull(o.getObligationLevel()) && oblLevel.equals(o.getObligationLevel()))
                .filter(o -> addFromDB || obligationAlreadyPresent
                        .containsKey(CommonUtils.nullToEmptyString(o.getTitle()).replaceAll("\r\n", " ")))
                .collect(Collectors.toMap(
                        o -> CommonUtils.nullToEmptyString(o.getTitle()).replaceAll("\r\n", " "), o -> {
                            String key = CommonUtils.nullToEmptyString(o.getTitle()).replaceAll("\r\n", " ");
                            if (obligationAlreadyPresent.containsKey(key)) {
                                return obligationAlreadyPresent.remove(key);
                            } else {
                                return new ObligationStatusInfo().setComment(o.getComments())
                                        .setObligationLevel(oblLevel).setObligationType(o.getObligationType())
                                        .setReleaseIdToAcceptedCLI(new HashMap<>()).setText(o.getText());
                            }
                        }, (oldValue, newValue) -> oldValue));

        obligationAlreadyPresent.entrySet().stream().forEach(e -> mapOfObligations.put(e.getKey(), e.getValue()));

        return mapOfObligations;
    }

    public static List<Obligation> getObligations() {
        final LicenseService.Iface licenseClient = new ThriftClients().makeLicenseClient();
        List<Obligation> obligations = new ArrayList<>();

        try {
            obligations = licenseClient.getObligations();
        } catch (TException e) {
            log.error("Could not get Obligations from Admin Section!", e);
        }
        return obligations;
    }

    public static Map<String, ObligationStatusInfo> sortMapOfObligationOnType(
            Map<String, ObligationStatusInfo> mapOfObligations) {
        return mapOfObligations.entrySet().stream().sorted(COMPARE_BY_OBLIGATIONTYPE)
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (oldValue, newValue) -> oldValue,
                        LinkedHashMap<String, ObligationStatusInfo>::new));
    }

    public static boolean isUserAllowedToEditClosedProject(Project project, User user) {
        Set<String> users = new HashSet<String>();
        Set<String> moderators = project.getModerators();
        users.addAll(moderators);
        users.add(project.getCreatedBy());
        users.add(project.getProjectResponsible());
        Set<String> contributors = project.getContributors();
        users.addAll(contributors);
        users.add(project.getLeadArchitect());
        return PermissionUtils.isUserAtLeast(UserGroup.CLEARING_ADMIN, user) || users.contains(user.getEmail());
    }

    public static void copyLinkedObligationsForClonedProject(Project newProject, Project sourceProject, ProjectService.Iface client, User user) {
        try {
            ObligationList obligation = client.getLinkedObligations(sourceProject.getLinkedObligationId(), user);
            Set<String> newLinkedReleaseIds = newProject.getReleaseIdToUsage().keySet();
            Set<String> sourceLinkedReleaseIds = sourceProject.getReleaseIdToUsage().keySet();
            Map<String, ObligationStatusInfo> linkedObligations = obligation.getLinkedObligationStatus();
            if (!newLinkedReleaseIds.equals(sourceLinkedReleaseIds)) {
                linkedObligations = obligation.getLinkedObligationStatus().entrySet().stream().filter(entry -> {
                    Set<String> releaseIds = entry.getValue().getReleaseIdToAcceptedCLI().keySet();
                    releaseIds.retainAll(newLinkedReleaseIds);
                    if (releaseIds.isEmpty()) {
                        return false;
                    }
                    return true;
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            if (!linkedObligations.isEmpty()) {
                client.addLinkedObligations(new ObligationList().setProjectId(newProject.getId()).setLinkedObligationStatus(linkedObligations), user);
            }
        } catch (TException e) {
            log.error("Error duplicating obligations for project: " + newProject.getId(), e);
        }
    }

    public static void removeReleaseVulnerabilityRelation(String releaseId, User user){
        VulnerabilityService.Iface vulnerabilityService = new ThriftClients().makeVulnerabilityClient();
        try {
            List<ReleaseVulnerabilityRelation> releaseVulnerabilityRelations = vulnerabilityService.getReleaseVulnerabilityRelationsByReleaseId(releaseId, user);
            for (ReleaseVulnerabilityRelation relation : releaseVulnerabilityRelations) {
                vulnerabilityService.deleteReleaseVulnerabilityRelation(relation, user);
            }
        } catch (TException e) {
            log.error(e.getMessage());
        }
    }

    public static Set<String> getReleaseIdsLinkedWithProject(Project project) {
        Set<String> releasesIds = new HashSet<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<ReleaseNode> releaseLinkJSONS = new ArrayList<>();
        if (project.getReleaseRelationNetwork() != null) {
            try {
                releaseLinkJSONS = objectMapper.readValue(project.getReleaseRelationNetwork(), new TypeReference<List<ReleaseNode>>() {
                });
                for (ReleaseNode release : releaseLinkJSONS) {
                    releasesIds.addAll(flattenReleaseIdInNetwork(release));
                }
            } catch (JsonProcessingException e) {
                log.error(e.getMessage());
            }
        }

        return releasesIds;
    }

    private static Set<String> flattenReleaseIdInNetwork(ReleaseNode node) {
        Set<String> setReleaseIds = new HashSet<>();
        setReleaseIds.add(node.getReleaseId());
        List<ReleaseNode> children = node.getReleaseLink();
        if (!CommonUtils.isNullOrEmptyCollection(children)) {
            for (ReleaseNode child : children) {
                setReleaseIds.addAll(flattenReleaseIdInNetwork(child));
            }
        }

        return setReleaseIds;
    }

    public static ProjectReleaseRelationship extractProjectReleaseRelationShipFromReleaseNode(ReleaseNode node) {
        ProjectReleaseRelationship projectReleaseRelationship = new ProjectReleaseRelationship();
        projectReleaseRelationship.setReleaseRelation(ReleaseRelationship.valueOf(node.getReleaseRelationship()));
        projectReleaseRelationship.setMainlineState(MainlineState.valueOf(node.getMainlineState()));
        projectReleaseRelationship.setCreatedBy(node.getCreateBy());
        projectReleaseRelationship.setCreatedOn(node.getCreateOn());
        projectReleaseRelationship.setComment(node.getComment());
        return projectReleaseRelationship;
    }

    public static List<Project> getUsingProjectByReleaseIds(Set<String> releaseIds, User user) {
        ProjectService.Iface projectClient = new ThriftClients().makeProjectClient();
        Map<String, Set<String>> filterMap = getFilterMapForSetReleaseIds(releaseIds);
        List<Project> projectsUsings;
        try {
            if (user == null) {
                projectsUsings = projectClient.refineSearchWithoutUser(null, filterMap);
            } else {
                projectsUsings = projectClient.refineSearch(null, filterMap, user);
            }
        } catch (TException e) {
            log.error("Could not fetch projects");
            projectsUsings = Collections.emptyList();
        }
        return projectsUsings;
    }

    private static Map<String, Set<String>> getFilterMapForSetReleaseIds(Set<String> releaseIds) {
        Map<String, Set<String>> filterMap = new HashMap<>();
        Set<String> values = new HashSet<>();
        for(String releaseId : releaseIds) {
            values.add("\"releaseId\":\"" + releaseId + "\"");
            values.add("\"releaseId\": \"" + releaseId + "\"");
        }
        values = values.stream().map(NouveauLuceneAwareDatabaseConnector::prepareWildcardQuery).collect(Collectors.toSet());
        filterMap.put(Project._Fields.RELEASE_RELATION_NETWORK.getFieldName(), values);
        return filterMap;
    }

    public static Collection<ProjectLink> getLinkedProjectWithoutReleases(Project project, boolean deep, ThriftClients thriftClients, Logger log, User user) {
        if (project != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                return client.getLinkedProjectsOfProjectWithoutReleases(project, deep, user);
            } catch (TException e) {
                log.error("Could not get linked projects", e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Sync data between releaseRelationNetwork and releaseIdToUsage (enable.flexible.project.release.relationship = true)
     * @param project
     * @return void
     */
    public static void syncReleaseRelationNetworkAndReleaseIdToUsage(Project project, User user) throws TException {
        if (project.getReleaseRelationNetwork() != null && project.getReleaseIdToUsage() == null) {
            createReleaseIdToUsages(project);
        } else if (project.getReleaseIdToUsage() != null && project.getReleaseRelationNetwork() == null) {
            createDefaultDependenciesNetwork(project, user);
        }
    }

    /**
     * Create default dependency network (enable.flexible.project.release.relationship = true)
     * @param project Project
     * @param user   sw360 User
     * @return void
     */
    private static void createDefaultDependenciesNetwork(Project project, User user) throws TException {
        List<ReleaseNode> dependencyNetwork = new ArrayList<>();
        ComponentService.Iface sw360ComponentService = new ThriftClients().makeComponentClient();
        if (project.getReleaseIdToUsage() != null) {
            Map<String, ProjectReleaseRelationship> oriReleaseIdToUsage = project.getReleaseIdToUsage();
            for (Map.Entry<String, ProjectReleaseRelationship> entry : oriReleaseIdToUsage.entrySet()) {
                Release release = sw360ComponentService.getAccessibleReleaseById(entry.getKey(), user);
                ReleaseNode node = sw360ComponentService.getReleaseRelationNetworkOfRelease(release, user).get(0);
                node.setMainlineState(entry.getValue().getMainlineState().toString());
                node.setReleaseRelationship(entry.getValue().getReleaseRelation().toString());
                node.setComment(entry.getValue().getComment());
                node.setCreateBy(user.getEmail());
                node.setCreateOn(SW360Utils.getCreatedOn());
                dependencyNetwork.add(node);
            }
        }
        project.setReleaseRelationNetwork(new Gson().toJson(dependencyNetwork));
    }

    /**
     * Create releaseIdToUsage form releaseRelationNetwork (enable.flexible.project.release.relationship = true)
     * @param project Project
     * @return void
     */
    private static void createReleaseIdToUsages(Project project) {
        if (CommonUtils.isNullEmptyOrWhitespace(project.getReleaseRelationNetwork())) {
            return;
        }

        List<ReleaseNode> dependencyNetwork = new ArrayList<>();
        try {
            dependencyNetwork = objectMapper.readValue(project.getReleaseRelationNetwork(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Error when parsing dependency network from json to array");
        }

        Map<String, ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();

        for (ReleaseNode node : dependencyNetwork) {
            ProjectReleaseRelationship projectReleaseRelationship = SW360Utils.extractProjectReleaseRelationShipFromReleaseNode(node);
            releaseIdToUsage.put(node.getReleaseId(), projectReleaseRelationship);
        }
        project.setReleaseIdToUsage(releaseIdToUsage);
    }

    public static Collection<ProjectLink> getLinkedProjectsWithAllReleasesAsFlatList(Project project, boolean deep, ThriftClients thriftClients, Logger log, User user) {
        return flattenProjectLinkTree(getLinkedProjectsWithAllReleases(project, deep, thriftClients, log, user));
    }

    public static Collection<ProjectLink> getLinkedProjectsWithAllReleases(Project project, boolean deep, ThriftClients thriftClients, Logger log, User user) {
        if (project != null) {
            try {
                ProjectService.Iface client = thriftClients.makeProjectClient();
                return client.getLinkedProjectsOfProjectWithAllReleases(project, deep, user);
            } catch (TException e) {
                log.error("Could not get linked projects", e);
            }
        }
        return Collections.emptyList();
    }

}
