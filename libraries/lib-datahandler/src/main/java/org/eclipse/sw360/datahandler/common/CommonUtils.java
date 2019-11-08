/*
 * Copyright Siemens AG, 2014-2019.
 * With modifications by Bosch Software Innovations GmbH, 2016
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import com.google.common.base.*;
import com.google.common.collect.*;

import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.attachments.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.Todo;
import org.eclipse.sw360.datahandler.thrift.moderation.ModerationRequest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.ektorp.DocumentOperationResult;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.apache.log4j.LogManager.getLogger;

/**
 * @author Cedric.Bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 */
public class CommonUtils {

    public static final String SYSTEM_CONFIGURATION_PATH = "/etc/sw360";
    private static List<String> MULTIPLE_FILE_EXTENSIONS = Arrays.asList(".tar.gz", ".tar.bz2", ".tar.xz", ".tar.lz", ".tar.lzma");

    private CommonUtils() {
        // Utility class with only static functions
    }

    private static final Ordering<String> CASE_INSENSITIVE_ORDERING = Ordering.from(String.CASE_INSENSITIVE_ORDER);

    public static final CSVFormat sw360CsvFormat = CSVFormat.RFC4180.withQuote('\'').withEscape('\\').withIgnoreSurroundingSpaces(true).withQuoteMode(QuoteMode.ALL);

    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    public static final Joiner COMMA_JOINER = Joiner.on(", ");

    private static final Comparator<CheckStatus> CHECK_STATUS_COMPARATOR = Comparator.comparingInt(cs -> {
        switch (cs) {
            case ACCEPTED:
                return 2;
            case NOTCHECKED:
                return 1;
            case REJECTED:
                return 0;
        }
        throw new IllegalArgumentException("CheckStatus is unknown to this Comparator: " + cs.name());
    });


    public static final String TMP_TODO_ID_PREFIX = "tmp";

    private static final Predicate<String> NOT_EMPTY_OR_NULL = new Predicate<String>() {
        @Override
        public boolean apply(String input) {
            return !Strings.isNullOrEmpty(input);
        }
    };

    /**
     * Returns a sorted list containing the elements of the given collection.
     * The list is sorted alphabetically, ignoring case.
     */
    public static List<String> getSortedList(Collection<String> collection) {
        return collection != null ? CASE_INSENSITIVE_ORDERING.immutableSortedCopy(collection) : ImmutableList.<String>of();
    }

    public static String joinStrings(Iterable<String> strings) {
        return strings != null ? COMMA_JOINER.join(strings) : "";
    }

    public static Predicate<String> notEmptyOrNull() {
        return NOT_EMPTY_OR_NULL;
    }

    /**
     * Return true if and item is contained in a collection, false otherwise. Null objects make the function returns false
     */
    public static <T> boolean contains(T item, Collection<T> collection) {
        return item != null && collection != null && collection.contains(item);
    }

    public static <T> boolean contains(T item, T[] array) {
        return array != null && contains(item, ImmutableList.copyOf(array));
    }

    /**
     * Add a String to a set, if the string is not null
     */
    public static <T> void add(Collection<T> collection, T item) {
        if (collection != null && item != null) {
            collection.add(item);
        }
    }

    /**
     * Add all from right to left collection
     */
    public static <T> void addAll(Collection<T> left, Collection<T> right) {
        if (left != null && right != null) {
            left.addAll(right);
        }
    }

    public static <T> void removeAll(Collection<T> left, Collection<T> right) {
        if (left != null && right != null) {
            left.removeAll(right);
        }
    }

    /**
     * Gets the intersection of two sets, or the not null set.
     * Returns:
     * * the intersection of both sets if they are both not null
     * * the set that is not null, if only one is not null
     *
     * @throws NullPointerException if both sets are null
     */
    public static <T> Set<T> intersectionIfNotNull(Set<T> left, Set<T> right) {
        if (left != null && right != null) {
            return Sets.intersection(left, right);
        } else {
            return MoreObjects.firstNonNull(left, right);
        }
    }

    public static <T> List<T> nullToEmptyList(List<T> in) {
        return in != null ? in : ImmutableList.<T>of();
    }

    public static <T> List<T> arrayToList(T[] in) {
        return in==null ? Collections.emptyList() : Arrays.asList(in);
    }

    public static <T> Collection<T> nullToEmptyCollection(Collection<T> in) {
        return in != null ? in : ImmutableList.<T>of();
    }

    public static <T> Set<T> nullToEmptySet(Set<T> in) {
        return in != null ? in : ImmutableSet.<T>of();
    }

    public static <K, V> Map<K, V> nullToEmptyMap(Map<K, V> in) {
        return in != null ? in : ImmutableMap.<K, V>of();
    }

    public static <T> Set<T> toSingletonSet(T in) {
        return in != null ? ImmutableSet.of(in) : ImmutableSet.<T>of();
    }

    public static String nullToEmptyString(Object in) {
        return in != null ? in.toString() : "";
    }

    public static Set<String> splitToSet(String value) {
        return ImmutableSet.copyOf(COMMA_SPLITTER.split(value));
    }

    /**
     * @param string string to convert to int
     * @return int value if input positive, negative on error
     */
    public static int toUnsignedInt(String string) {
        int integer = -1;
        try {
            integer = Integer.valueOf(string);
        } catch (NullPointerException | NumberFormatException ignored) { // sic.
        }
        return integer;
    }

    public static boolean oneIsNull(Object... objects) {
        for (Object object : objects) {
            if (object == null)
                return true;
        }
        return false;
    }

    public static boolean isNullOrEmptyCollection(Collection collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection collection) {
        return !isNullOrEmptyCollection(collection);
    }

    public static boolean allAreEmptyOrNull(Collection... collections) {
        return !atLeastOneIsNotEmpty(collections);
    }

    public static boolean allAreEmptyOrNull(Map... maps) {
        return !atLeastOneIsNotEmpty(maps);
    }

    public static boolean allAreEmptyOrNull(String... strings) {
        return !atLeastOneIsNotEmpty(strings);
    }

    public static boolean atLeastOneIsNotEmpty(Collection... collections) {
        for (Collection collection : collections) {
            if (collection != null && !collection.isEmpty()) return true;
        }
        return false;
    }

    public static boolean atLeastOneIsNotEmpty(Map... maps) {
        for (Map map : maps) {
            if (map != null && !map.isEmpty()) return true;
        }
        return false;
    }

    public static boolean atLeastOneIsNotEmpty(String... strings) {
        for (String string : strings) {
            if (!Strings.isNullOrEmpty(string)) return true;
        }
        return false;
    }


    public static boolean allAreEmpty(Object... objects) {
        return !atLeastOneIsNotEmpty(objects);
    }

    public static boolean atLeastOneIsNotEmpty(Object... objects) {


        for (Object object : objects) {
            if (object instanceof Collection)
                if (!((Collection) object).isEmpty()) return true;
        }

        return false;
    }

    public static int compareAsNullsAreSmaller(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null ? 0 : -1;
        }
        if (o2 == null)
            return 1;

        return 0;
    }

    public static boolean allHaveSameLength(Object... arrays) {
        if (arrays.length < 1) {
            return true;
        }

        int length = Array.getLength(arrays[0]);
        for (Object array : arrays) {
            if (Array.getLength(array) != length) {
                return false;
            }
        }

        return true;
    }

    public static Optional<Attachment> getAttachmentOptional(final String attachmentId, Set<Attachment> attachments) {
        return attachments
                .stream()
                .filter(attachment -> attachmentId.equals(attachment.getAttachmentContentId()))
                .findFirst();
    }

    @NotNull
    public static Attachment getNewAttachment(User user, String attachmentContentId, String fileName) {
        Attachment attachment = new Attachment();
        attachment.setCreatedBy(user.getEmail());
        attachment.setCreatedOn(SW360Utils.getCreatedOn());
        attachment.setCreatedComment("");
        attachment.setCreatedTeam(user.getDepartment());
        attachment.setFilename(fileName);
        attachment.setAttachmentContentId(attachmentContentId);
        attachment.setAttachmentType(AttachmentType.DOCUMENT);
        attachment.setCheckStatus(CheckStatus.NOTCHECKED);
        attachment.setCheckedComment("");
        attachment.setSha1("");
        return attachment;
    }

    @NotNull
    public static Comparator<ModerationRequest> compareByTimeStampDescending() {
        return new Comparator<ModerationRequest>() {
            @Override
            public int compare(ModerationRequest o1, ModerationRequest o2) {
                return Long.compare(o2.getTimestamp(), o1.getTimestamp());
            }
        };
    }

    public static boolean isInProgressOrPending(ModerationRequest moderationRequest) {
        return moderationRequest.getModerationState().equals(ModerationState.INPROGRESS) ||
                moderationRequest.getModerationState().equals(ModerationState.PENDING);
    }

    @NotNull
    public static DocumentState getOriginalDocumentState() {
        DocumentState documentState = new DocumentState().setIsOriginalDocument(true);
        documentState.unsetModerationState();
        return documentState;
    }

    public static Optional<ModerationRequest> getFirstModerationRequestOfUser(List<ModerationRequest> moderationRequestsForDocumentId, final String email) {
        return moderationRequestsForDocumentId
                .stream()
                .filter(moderationRequest -> moderationRequest.getRequestingUser().equals(email))
                .findFirst();
    }

    @NotNull
    public static DocumentState getModeratedDocumentState(ModerationRequest moderationRequest) {
        DocumentState documentState = new DocumentState().setIsOriginalDocument(false);
        documentState.setModerationState(moderationRequest.getModerationState());
        return documentState;
    }

    public static boolean isStillRelevant(ModerationRequest request) {
        return request.getModerationState().equals(ModerationState.PENDING) || request.getModerationState().equals(ModerationState.INPROGRESS);
    }

    public static <T, V> AfterFunction<T, V> afterFunction(Function<V, T> function) {
        return new AfterFunction<>(function);
    }

    public static void closeQuietly(Closeable closeable, Logger logger) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException e) {
            logger.warn("cannot close closeable", e);
        }
    }

    public static boolean isValidUrl(String url) {
        try {
            return !isNullOrEmpty(new URL(url).getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String getTargetNameOfUrl(String url) {
        try {
            String path = new URL(url).getPath();
            String fileName = FilenameUtils.getName(path);
            return !isNullOrEmpty(fileName) ? fileName : path;
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public static Boolean getBoolOrNull(String in) {
        if (!isNullOrEmpty(in)) {
            //elegance in redundancy :)
            if (in.equalsIgnoreCase("true")) return true;
            else if (in.equalsIgnoreCase("false")) return false;
        }
        return null;
    }


    public static Integer getIntegerOrNull(String in) {
        Integer out = null;
        if (!isNullOrEmpty(in)) {
            try {
                out = Integer.parseInt(in);
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public static int getIntOrDefault(String in, int defaultValue) {
        try {
            return Integer.parseInt(in);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static <T extends Enum<T>> String getEnumStringOrNull(T val) {
        if (val != null) return val.name();
        return null;
    }

    public static Map<String, User> getStringUserMap(UserService.Iface userClient) throws TException {
        Map<String, User> userMap;
        userMap = Maps.uniqueIndex(userClient.getAllUsers(), new Function<User, String>() {
            @Override
            public String apply(User input) {
                return input.getEmail();
            }
        });
        return userMap;
    }

    public static void getMessageForRequestSummary(RequestSummary releaseRequestSummary, String typeInfo, StringBuilder stringBuilder) {
        if (releaseRequestSummary.isSetTotalAffectedElements() && releaseRequestSummary.isSetTotalElements()) {
            stringBuilder.append("Affected ").append(typeInfo).append(" elements: ")
                    .append(releaseRequestSummary.getTotalAffectedElements())
                    .append(" of ").append(releaseRequestSummary.getTotalElements())
                    .append(". ");
        }
    }

    public static RequestSummary prepareMessage(RequestSummary input, String info) {
        StringBuilder stringBuilder = new StringBuilder();
        if (input.isSetMessage()) {
            stringBuilder.append(input.message);
        }

        getMessageForRequestSummary(input, info, stringBuilder);
        input.setMessage(stringBuilder.toString());

        return input;
    }


    public static RequestStatus reduceRequestStatus(RequestStatus r1, RequestStatus r2) {
        if (RequestStatus.SUCCESS.equals(r1) && RequestStatus.SUCCESS.equals(r2)) {
            return RequestStatus.SUCCESS;
        }
        return RequestStatus.FAILURE;
    }

    public static RequestSummary addToMessage(RequestSummary left, RequestSummary right, String info) {
        left.setRequestStatus(reduceRequestStatus(left.requestStatus, right.requestStatus));

        StringBuilder stringBuilder = new StringBuilder();
        if (left.isSetMessage()) {
            stringBuilder.append(left.message);
        }

        getMessageForRequestSummary(right, info, stringBuilder);
        left.setMessage(stringBuilder.toString());

        return left;
    }

    @NotNull
    public static RequestSummary addRequestSummaries(RequestSummary left, String typeInfoLeft, RequestSummary right, String typeInfoRight) {
        final RequestSummary requestSummary = prepareMessage(left, typeInfoLeft);
        return addToMessage(requestSummary, right, typeInfoRight);
    }

    public static ImmutableList<String> getAttachmentURLsFromAttachmentContents(List<AttachmentContent> attachmentContents) {
        return FluentIterable.from(attachmentContents).transform(new Function<AttachmentContent, String>() {
            @Override
            public String apply(AttachmentContent input) {
                return input.getRemoteUrl();
            }
        }).toList();
    }

    public static RequestSummary addRequestSummaries(RequestSummary left, RequestSummary right) {
        RequestSummary result = new RequestSummary();
        result.requestStatus = left.isSetRequestStatus() && left.requestStatus.equals(RequestStatus.SUCCESS)
                && right.isSetRequestStatus() && right.requestStatus.equals(RequestStatus.SUCCESS)
                ? RequestStatus.SUCCESS
                : RequestStatus.FAILURE;
        result.setTotalElements(left.getTotalElements() + right.getTotalElements());
        result.setTotalAffectedElements(left.getTotalAffectedElements() + right.getTotalAffectedElements());
        return result;
    }

    @NotNull
    public static Map<String, List<String>> getIdentifierToListOfDuplicates(ListMultimap<String, String> identifierToIds) {
        Map<String, List<String>> output = new HashMap<>();

        for (String identifier : identifierToIds.keySet()) {
            List<String> ids = identifierToIds.get(identifier);
            if (ids.size() > 1) {
                output.put(identifier, ids);
            }
        }

        return output;
    }

    @NotNull
    public static RequestSummary getRequestSummary(List<String> ids, List<DocumentOperationResult> documentOperationResults) {
        final RequestSummary requestSummary = new RequestSummary();
        requestSummary.requestStatus = documentOperationResults.isEmpty() ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
        requestSummary.setTotalElements(ids.size());
        requestSummary.setTotalAffectedElements(ids.size() - documentOperationResults.size());
        return requestSummary;
    }

    public static RequestSummary getRequestSummary(int total, int failures) {
        final RequestSummary requestSummary = new RequestSummary();
        requestSummary.requestStatus = failures == 0 ? RequestStatus.SUCCESS : RequestStatus.FAILURE;
        requestSummary.setTotalElements(total);
        requestSummary.setTotalAffectedElements(total - failures);
        return requestSummary;
    }

    public static Properties loadProperties(Class<?> clazz, String propertiesFilePath) {
        return loadProperties(clazz, propertiesFilePath, true);
    }

    public static Properties loadProperties(Class<?> clazz, String propertiesFilePath, boolean useSystemConfig) {
        Properties props = new Properties();

        try (InputStream resourceAsStream = clazz.getResourceAsStream(propertiesFilePath)) {
            if (resourceAsStream == null)
                throw new IOException("cannot open " + propertiesFilePath);

            props.load(resourceAsStream);
        } catch (IOException e) {
            getLogger(clazz).error("Error opening resources " + propertiesFilePath + ".", e);
        }

        if (useSystemConfig) {
            File systemPropertiesFile = new File(SYSTEM_CONFIGURATION_PATH, propertiesFilePath);
            if (systemPropertiesFile.exists()) {
                try (InputStream resourceAsStream = new FileInputStream(systemPropertiesFile.getPath())) {
                    if (resourceAsStream == null)
                        throw new IOException("cannot open " + systemPropertiesFile.getPath());

                    props.load(resourceAsStream);
                } catch (IOException e) {
                    getLogger(clazz).error("Error opening resources " + systemPropertiesFile.getPath() + ".", e);
                }
            }
        }
        return props;
    }

    public static Optional<byte[]> loadResource(Class<?> clazz, String resourceFilePath) {
        return loadResource(clazz, resourceFilePath, true);
    }

    public static Optional<byte[]> loadResource(Class<?> clazz, String resourceFilePath, boolean useSystemResourses) {
        if (isNullOrEmpty(resourceFilePath)) {
            return Optional.empty();
        }
        if (useSystemResourses) {
            File systemResourceFile = new File(SYSTEM_CONFIGURATION_PATH, resourceFilePath);
            if (systemResourceFile.exists()) {
                try (InputStream resourceAsStream = new FileInputStream(systemResourceFile.getPath())) {
                    if (resourceAsStream == null) {
                        throw new IOException("cannot open " + systemResourceFile.getPath());
                    }
                    return Optional.of(IOUtils.toByteArray(resourceAsStream));
                } catch (IOException e) {
                    getLogger(clazz).error("Error opening resources " + systemResourceFile.getPath() + ".", e);
                }
            }
        }

        try (InputStream resourceAsStream = clazz.getResourceAsStream(resourceFilePath)) {
            if (resourceAsStream == null)
                throw new IOException("cannot open " + resourceFilePath);
            return Optional.of(IOUtils.toByteArray(resourceAsStream));
        } catch (IOException e) {
            getLogger(clazz).error("Error opening resources " + resourceFilePath + ".", e);
        }
        return Optional.empty();
    }

    public static <T> T getFirst(Iterable<T> iterable) {
        final Iterator<T> iterator = iterable.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        } else {
            throw new NoSuchElementException();
        }
    }

    public static Optional<Attachment> getBestClearingReport(Release release) {
        return nullToEmptyCollection(release.getAttachments())
                .stream()
                .filter(att -> att.getAttachmentType() == AttachmentType.CLEARING_REPORT
                        || att.getAttachmentType() == AttachmentType.COMPONENT_LICENSE_INFO_XML)
                .max(Comparator.comparing(Attachment::getCheckStatus, CHECK_STATUS_COMPARATOR));
    }

    public static boolean isTemporaryTodo(Todo todo) {
        return todo.isSetId() && todo.getId().startsWith(TMP_TODO_ID_PREFIX);
    }

    public static class AfterFunction<T, V> {
        private Function<V, T> transformer;

        private AfterFunction(Function<V, T> transformer) {
            this.transformer = transformer;
        }

        public Predicate<V> is(Predicate<T> predicate) {
            return Predicates.compose(predicate, transformer);
        }
    }

    public static <T> Optional<T> wrapThriftOptionalReplacement(List<T> thriftOutput) {
        if (thriftOutput == null || thriftOutput.size() == 0) {
            return Optional.empty();
        }
        if (thriftOutput.size() > 1) {
            getLogger(CommonUtils.class).error("List contained more then one item but was treated as \"Optional\".");
        }
        return Optional.of(thriftOutput.get(0));
    }

    public static String formatTime(int seconds) {
        if (seconds < 0) {
            return "not set";
        }
        int hours = seconds / 3600;
        int remainder = seconds % 3600;
        int minutes = remainder / 60;
        seconds = remainder % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static Map<String, Set<String>> mergeMapIntoMap(Map<String, Set<String>> source, Map<String, Set<String>> destination) {
        if (destination == null) {
            return source;
        }
        if (source == null) {
            return destination;
        }
        source.keySet().stream().forEach(k -> {
                    if (destination.containsKey(k)) {
                        destination.get(k).addAll(source.get(k));
                    } else {
                        destination.put(k, source.get(k));
                    }
                }
        );
        return destination;
    }

    public static boolean isNullEmptyOrWhitespace(String string) {
        return string == null || string.trim().length() == 0;
    }

    public static <T> java.util.function.Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Set<Object> visitedKeys = new HashSet<>();
        return t -> visitedKeys.add(keyExtractor.apply(t));
    }

    public static Set<String> getNullToEmptyKeyset(Map<String, ?> map) {
        return nullToEmptyMap(map).keySet();
    }

    public static Set<String> getNullToEmptyValue(Map<String, Set<String>> map, String key) {
        return nullToEmptySet(nullToEmptyMap(map).get(key));
    }

    public static Set<String> unifiedKeyset(Map<String, ?>... maps) {
        Set<String> keys = new HashSet<>();
        for (Map<String, ?> map : maps) {
            keys.addAll(getNullToEmptyKeyset(map));
        }
        return keys;
    }

    public static <U extends TFieldIdEnum, T extends TBase<T, U>> boolean isMapFieldMapOfStringSets(U field,
                                                                                                    T document,
                                                                                                    T documentAdditions,
                                                                                                    T documentDeletions,
                                                                                                    Logger logger) {
        List<Map<String, Object>> maps = Arrays.asList(
                (Map<String, Object>) document.getFieldValue(field),
                (Map<String, Object>) documentAdditions.getFieldValue(field),
                (Map<String, Object>) documentDeletions.getFieldValue(field));
        List<Map<String, ?>> nonEmptyMaps = maps.stream().filter(m -> m != null && !m.isEmpty()).collect(Collectors.toList());
        if (nonEmptyMaps.isEmpty()) {
            logger.info("Field was empty in document, documentAdditions and documentDeletions: " + field.getFieldName());
            return false;
        }

        Object value = nonEmptyMaps.stream().findAny().get().entrySet().stream()
                .map(e -> ((Map.Entry<String, Object>) e).getValue())
                .findAny()
                .get();
        if (!(value instanceof Set)) {
            return false;
        }
        List<Map<String, ?>> nonEmptyMapsContainingNonEmptySet = nonEmptyMaps.stream()
                .filter(m -> nonEmptyMapOfSetsContainsNonEmptySet((Map<String, Set<Object>>) m))
                .collect(Collectors.toList());

        if (nonEmptyMapsContainingNonEmptySet.isEmpty()) {
            logger.warn("Field contained only maps of only empty sets: " + field.getFieldName());
            return false;
        }

        Map<String, Set<Object>> mapWithNonEmptySet = (Map<String, Set<Object>>) nonEmptyMapsContainingNonEmptySet.stream().findAny().get();
        Object element = getNonEmptySetFromMapOfSets(mapWithNonEmptySet).get().stream()
                .findAny().get();
        return (element instanceof String);
    }

    private static boolean nonEmptyMapOfSetsContainsNonEmptySet(Map<String, Set<Object>> map) {
        Optional<Set<Object>> nonEmptySet = getNonEmptySetFromMapOfSets(map);
        return nonEmptySet.isPresent();
    }

    private static Optional<Set<Object>> getNonEmptySetFromMapOfSets(Map<String, Set<Object>> map) {
        return map.entrySet().stream()
                .map(e -> ((Map.Entry<String, Set<Object>>) e).getValue())
                .filter(s -> !s.isEmpty()).findAny();
    }

    public static String getExtensionFromFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        for (String multipleExtension : MULTIPLE_FILE_EXTENSIONS) {
            if (fileName.toLowerCase().endsWith(multipleExtension)) {
                int lastIndex = fileName.toLowerCase().lastIndexOf(multipleExtension);
                return fileName.substring(lastIndex + 1);
            }
        }
        return FilenameUtils.getExtension(fileName);
    }

}
