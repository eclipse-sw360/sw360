/*
 * Copyright Siemens AG, 2014-2017, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.Attachment;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.attachments.CheckStatus;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.licenses.Todo;
import org.eclipse.sw360.datahandler.thrift.projects.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TEnum;
import org.apache.thrift.TException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

/**
 * @author Cedric.Bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author stefan.jaeger@evosoft.com
 * @author alex.borodin@evosoft.com
 */
public class SW360Utils {

    private final static Logger log = getLogger(SW360Utils.class);

    public static final String FORMAT_DATE = "yyyy-MM-dd";
    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";
    public static final Comparator<ReleaseLink> RELEASE_LINK_COMPARATOR = Comparator.comparing(rl -> getReleaseFullname(rl.getVendor(), rl.getName(), rl.getVersion()).toLowerCase());

    private static final ObjectMapper objectMapper;

    private static Joiner spaceJoiner = Joiner.on(" ");

    private SW360Utils() {
        // Utility class with only static functions
    }

    public static class TodoInfo {
        public final boolean fulfilled;
        public final String timestamp;
        public final String user;

        public TodoInfo(ProjectTodo projectTodo) {
            final UserService.Iface userClient = new ThriftClients().makeUserClient();

            String userString = "";

            User user;
            try {
                if (projectTodo.userId != null) {
                    user = userClient.getUser(projectTodo.userId);
                    if (user != null) {
                        userString = String.format("%s <%s>", user.getFullname(), user.getEmail());
                    }
                }
            } catch (TException te) {
                log.error("Could not load user from backend.", te);
            }

            this.user = userString;
            this.fulfilled = projectTodo.fulfilled;
            this.timestamp = Strings.nullToEmpty(projectTodo.updated);
        }

        public String getModificationHint() {
            return String.format("%s %s",this.user, this.timestamp);
        }

        public boolean isFulfilled() {
            return fulfilled;
        }
    }

    static{
        objectMapper = new ObjectMapper();
        SimpleModule customModule = new SimpleModule("SW360 serializers");
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
        Predicate<Attachment> isApprovedCLI = attachment -> attachment.getCheckStatus().equals(CheckStatus.ACCEPTED)
                && AttachmentType.COMPONENT_LICENSE_INFO_XML.equals(attachment.getAttachmentType());

        return release.getAttachments().stream().filter(isApprovedCLI).collect(Collectors.toList());
    }

    public static Map<String, String> getReleaseIdtoAcceptedCLIMappings(Map<String, ObligationStatusInfo> obligationStatusMap) {
        return obligationStatusMap.values().stream().flatMap(e -> e.getReleaseIdToAcceptedCLI().entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue));
    }

    public static String printFullname(Release release) {
        if (release == null || isNullOrEmpty(release.getName())) {
            return "New Release";
        }
        String vendorName = Optional.ofNullable(release.getVendor()).map(Vendor::getShortname).orElse(null);
        return getReleaseFullname(vendorName, release.getName(), release.getVersion());
    }

    public static Map<Todo, TodoInfo> getProjectObligations(Project project) {
        final LicenseService.Iface licenseClient = new ThriftClients().makeLicenseClient();

        Set<ProjectTodo> projectTodos = project.getTodosSize() > 0 ? project.getTodos() : Collections.emptySet();
        try {
            return licenseClient.getTodos().stream()
                    .filter(o -> o.isValidForProject())
                    .collect(Collectors.toMap(
                            todo -> todo,
                            todo -> new TodoInfo(projectTodos.stream()
                                        .filter(projectTodo -> projectTodo.getTodoId().equals(todo.getId()))
                                        .findFirst()
                                        .orElseGet(ProjectTodo::new))
                            )
                    );
        } catch (TException te) {
            return Collections.emptyMap();
        }
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

    /**
     * Assumes that the process exists.
     */
    public static ExternalToolProcessStep getExternalToolProcessStepOfFirstProcessForTool(Release release,
            ExternalTool et, String stepName) {
        if (release == null || et == null || StringUtils.isEmpty(stepName)) {
            return null;
        }

        return getNotOutdatedExternalToolProcessesForTool(release, et) //
                .stream() //
                .findFirst() //
                .map(ExternalToolProcess::getProcessSteps) //
                .get() //
                .stream() //
                .filter(etps -> stepName.equals(etps.getStepName())) //
                .findFirst() //
                .orElse(null);
    }
}
