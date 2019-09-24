/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * With modifications by Siemens AG, 2017-2018.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.licenseinfo.outputGenerators;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.apache.velocity.tools.ToolManager;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.licenseinfo.util.LicenseNameWithTextUtils;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class OutputGenerator<T> {
    protected static final String VELOCITY_TOOLS_FILE = "velocity-tools.xml";
    protected static final String LICENSE_REFERENCE_ID_MAP_CONTEXT_PROPERTY = "licenseNameWithTextToReferenceId";
    protected static final String ACKNOWLEDGEMENTS_CONTEXT_PROPERTY = "acknowledgements";
    protected static final String ALL_LICENSE_NAMES_WITH_TEXTS = "allLicenseNamesWithTexts";
    protected static final String LICENSE_INFO_RESULTS_CONTEXT_PROPERTY = "licenseInfoResults";
    protected static final String LICENSE_INFO_ERROR_RESULTS_CONTEXT_PROPERTY = "licenseInfoErrorResults";
    protected static final String LICENSE_INFO_HEADER_TEXT = "licenseInfoHeader";
    protected static final String OBLIGATIONS_TEXT = "obligations";
    protected static final String LICENSE_INFO_PROJECT_TITLE = "projectTitle";
    protected static final String EXTERNAL_IDS = "externalIds";

    private final String outputType;
    private final String outputDescription;
    private final boolean isOutputBinary;
    private final String outputMimeType;
    private final OutputFormatVariant outputVariant;

    OutputGenerator(String outputType, String outputDescription, boolean isOutputBinary, String mimeType, OutputFormatVariant variant) {
        this.outputType = outputType;
        this.outputDescription = outputDescription;
        this.isOutputBinary = isOutputBinary;
        this.outputMimeType = mimeType;
        this.outputVariant = variant;
    }

    public abstract T generateOutputFile(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, Project project, Collection<ObligationParsingResult> obligationResults, User user, Map<String,String> externalIds) throws SW360Exception;

    public String getOutputType() {
        return outputType;
    }

    public String getOutputDescription() {
        return outputDescription;
    }

    public boolean isOutputBinary() {
        return isOutputBinary;
    }

    public String getOutputMimeType() {
        return outputMimeType;
    }

    public OutputFormatVariant getOutputVariant() {
        return outputVariant;
    }

    public OutputFormatInfo getOutputFormatInfo() {
        return new OutputFormatInfo()
                .setFileExtension(getOutputType())
                .setDescription(getOutputDescription())
                .setIsOutputBinary(isOutputBinary())
                .setGeneratorClassName(this.getClass().getSimpleName())
                .setMimeType(getOutputMimeType())
                .setVariant(getOutputVariant());
    }

    public String getComponentLongName(LicenseInfoParsingResult li) {
        return SW360Utils.getReleaseFullname(li.getVendor(), li.getName(), li.getVersion());
    }

    public VelocityContext getConfiguredVelocityContext() {
        Properties p = new Properties();
        p.setProperty(RuntimeConstants.RESOURCE_LOADER, "file, class");
        p.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH, CommonUtils.SYSTEM_CONFIGURATION_PATH);
        p.setProperty("file.resource.loader.class", FileResourceLoader.class.getName());
        p.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        Velocity.init(p);
        ToolManager velocityToolManager = new ToolManager();
        velocityToolManager.configure(VELOCITY_TOOLS_FILE);
        return new VelocityContext(velocityToolManager.createContext());
    }

    @NotNull
    protected SortedMap<String, LicenseInfoParsingResult> getSortedLicenseInfos(Collection<LicenseInfoParsingResult> projectLicenseInfoResults) {
        Map<String, LicenseInfoParsingResult> licenseInfos = projectLicenseInfoResults.stream()
                .collect(Collectors.toMap(this::getComponentLongName, li -> li, this::mergeLicenseInfoParsingResults));
        return sortStringKeyedMap(licenseInfos);
    }

    @NotNull
    protected LicenseInfoParsingResult mergeLicenseInfoParsingResults(LicenseInfoParsingResult r1, LicenseInfoParsingResult r2){
        if (r1.getStatus() != LicenseInfoRequestStatus.SUCCESS || r2.getStatus() != LicenseInfoRequestStatus.SUCCESS ||
                !getComponentLongName(r1).equals(getComponentLongName(r2))){
            throw new IllegalArgumentException("Only successful parsing results for the same release can be merged");
        }

        LicenseInfoParsingResult r = new LicenseInfoParsingResult(r1);

        if (r.isSetLicenseInfo()) {
            r.getLicenseInfo().setLicenseNamesWithTexts(Stream.concat(
                    getCollectionStream(r, LicenseInfo::getLicenseNamesWithTexts, Collections::emptySet),
                    getCollectionStream(r2, LicenseInfo::getLicenseNamesWithTexts, Collections::emptySet))
                    .collect(Collectors.toSet()));

            r.getLicenseInfo().setCopyrights(Stream.concat(
                    getCollectionStream(r, LicenseInfo::getCopyrights, Collections::emptySet),
                    getCollectionStream(r2, LicenseInfo::getCopyrights, Collections::emptySet))
                    .collect(Collectors.toSet()));

            r.getLicenseInfo().setFilenames(Stream.concat(
                    getCollectionStream(r, LicenseInfo::getFilenames, Collections::emptyList),
                    getCollectionStream(r2, LicenseInfo::getFilenames, Collections::emptyList))
                    .collect(Collectors.toList()));
        } else {
            r.setLicenseInfo(r2.getLicenseInfo());
        }

        return r;
    }

    private <T extends Collection<U>, U> Stream<U> getCollectionStream(LicenseInfoParsingResult r, Function<LicenseInfo, T> collectionExtractor, Supplier<T> defaultEmpty) {
        return Optional.of(r)
                .map(LicenseInfoParsingResult::getLicenseInfo)
                .map(collectionExtractor)
                .orElse(defaultEmpty.get()).stream();
    }

    @NotNull
    protected SortedMap<String, Set<String>> getSortedAcknowledgements(Map<String, LicenseInfoParsingResult> sortedLicenseInfos) {
        Map<String, Set<String>> acknowledgements = Maps.filterValues(Maps.transformValues(sortedLicenseInfos, pr -> Optional
                .ofNullable(pr.getLicenseInfo())
                .map(LicenseInfo::getLicenseNamesWithTexts)
                .filter(Objects::nonNull)
                .map(s -> s
                        .stream()
                        .map(LicenseNameWithText::getAcknowledgements)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()))
                .orElse(Collections.emptySet())), set -> !set.isEmpty());
        return sortStringKeyedMap(acknowledgements);
    }

    @NotNull
    protected static List<LicenseNameWithText> getSortedLicenseNameWithTexts(Collection<LicenseInfoParsingResult> projectLicenseInfoResults) {
        Set<LicenseNameWithText> licenseNamesWithText = projectLicenseInfoResults.stream()
                .map(LicenseInfoParsingResult::getLicenseInfo)
                .filter(Objects::nonNull)
                .map(LicenseInfo::getLicenseNamesWithTexts)
                .filter(Objects::nonNull)
                .reduce(Sets::union)
                .orElse(Collections.emptySet());

        return licenseNamesWithText.stream()
                .filter(licenseNameWithText -> !LicenseNameWithTextUtils.isEmpty(licenseNameWithText))
                .sorted(Comparator.comparing(LicenseNameWithText::getLicenseName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private static <U> SortedMap<String, U> sortStringKeyedMap(Map<String, U> unsorted){
        SortedMap<String, U> sorted = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        sorted.putAll(unsorted);
        if (sorted.size() != unsorted.size()){
            // there were key collisions and some data was lost -> throw away the sorted map and sort by case sensitive order
            sorted = new TreeMap<>();
            sorted.putAll(unsorted);
        }
        return sorted;
    }

    /**
     * Creates a velocity context and fills it with the default, commonly used
     * values:
     * <ul>
     * <li>allLicenseNamesWithTexts: list of {@link LicenseNameWithText} objects,
     * sorted by license name</li>
     * <li>licenseNameWithTextToReferenceId: map from the license name to a unique
     * id inside the file. May be used to reference a license.</li>
     * <li>licenseInfoResults: map of {@link LicenseInfoParsingResult} objects,
     * where the key is the name of the release. The licenses within the objects are
     * sorted by name. Contains only the results with status {@link LicenseInfoRequestStatus#SUCCESS}</li>
     * <li>licenseInfoErrorResults: map {@link List}of {@link LicenseInfoParsingResult} objects,
     * where the key is the name of the release. Contains only the results with status other than
     * {@link LicenseInfoRequestStatus#SUCCESS}. These results are not merged, that's why the map values are lists.</li>
     * <li>acknowledgments: map of acknowledgments for a release where the key is
     * the release and the value is a set of strings (acknowledgments)</li>
     * </ul>
     * The given file will be used as velocity template and will be rendered with
     * the described data.
     *
     * @param projectLicenseInfoResults
     *            parsing results to be rendered
     * @param file
     *            name of template file
     * @param externalIds
     * @return rendered template
     */
    protected String renderTemplateWithDefaultValues(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, String file,
                                                     String projectTitle, String licenseInfoHeaderText, String obligationsText, Map<String, String> externalIds) {
        VelocityContext vc = getConfiguredVelocityContext();
        // set header
        vc.put(LICENSE_INFO_PROJECT_TITLE, projectTitle);
        vc.put(LICENSE_INFO_HEADER_TEXT, licenseInfoHeaderText);
        vc.put(OBLIGATIONS_TEXT, obligationsText);

        // sorted lists of all license to be displayed at the end of the file at once
        List<LicenseNameWithText> licenseNamesWithTexts = getSortedLicenseNameWithTexts(projectLicenseInfoResults);
        vc.put(ALL_LICENSE_NAMES_WITH_TEXTS, licenseNamesWithTexts);
        // assign a reference id to each license in order to only display references for
        // each release. The references will point to
        // the list with all details at the and of the file (see above)
        int referenceId = 1;
        Map<LicenseNameWithText, Integer> licenseToReferenceId = Maps.newHashMap();
        for (LicenseNameWithText licenseNamesWithText : licenseNamesWithTexts) {
            licenseToReferenceId.put(licenseNamesWithText, referenceId++);
        }
        vc.put(LICENSE_REFERENCE_ID_MAP_CONTEXT_PROPERTY, licenseToReferenceId);

        Map<Boolean, List<LicenseInfoParsingResult>> partitionedResults =
                projectLicenseInfoResults.stream().collect(Collectors.partitioningBy(r -> r.getStatus() == LicenseInfoRequestStatus.SUCCESS));
        List<LicenseInfoParsingResult> goodResults = partitionedResults.get(true);
        Map<String, List<LicenseInfoParsingResult>> badResultsPerRelease =
                partitionedResults.get(false).stream().collect(Collectors.groupingBy(this::getComponentLongName));
        vc.put(LICENSE_INFO_ERROR_RESULTS_CONTEXT_PROPERTY, badResultsPerRelease);

        // be sure that the licenses inside a release are sorted by id. This looks nicer
        SortedMap<String, LicenseInfoParsingResult> sortedLicenseInfos = getSortedLicenseInfos(goodResults);
        // this will effectively change the objects in the collection and therefore the
        // objects in the sorted map above
        sortLicenseNamesWithinEachLicenseInfoById(sortedLicenseInfos.values(), licenseToReferenceId);
        vc.put(LICENSE_INFO_RESULTS_CONTEXT_PROPERTY, sortedLicenseInfos);

        // also display acknowledgments
        SortedMap<String, Set<String>> acknowledgements = getSortedAcknowledgements(sortedLicenseInfos);
        vc.put(ACKNOWLEDGEMENTS_CONTEXT_PROPERTY, acknowledgements);

        vc.put(EXTERNAL_IDS, externalIds);

        StringWriter sw = new StringWriter();
        Velocity.mergeTemplate(file, "utf-8", vc, sw);
        IOUtils.closeQuietly(sw);
        return sw.toString();
    }

    /**
     * Uses the given map to sort the licenses inside a license of by id. The given
     * map must contain an id for each license name present in the license info
     * objects.
     *
     * @param licenseInfoResults
     *            parsing results with license infos and licenses to be sorted
     * @param licenseToReferenceId
     *            mapping of license name to id to be able to sort the licenses
     */
    private void sortLicenseNamesWithinEachLicenseInfoById(Collection<LicenseInfoParsingResult> licenseInfoResults,
            Map<LicenseNameWithText, Integer> licenseToReferenceId) {
        licenseInfoResults.stream().map(LicenseInfoParsingResult::getLicenseInfo).filter(Objects::nonNull)
                .forEach((LicenseInfo li) -> li.setLicenseNamesWithTexts(
                        sortSet(li.getLicenseNamesWithTexts(), licenseNameWithText -> licenseToReferenceId.get(licenseNameWithText))));
    }

    /**
     * Helper function to sort a set by the given key extractor. Falls back to the
     * unsorted set if sorting the set would squash values.
     *
     * @param unsorted
     *            set to be sorted
     * @param keyExtractor
     *            function to extract the key to use for sorting
     *
     * @return the sorted set
     */
    private static <U, K extends Comparable<K>> SortedSet<U> sortSet(Set<U> unsorted, Function<U, K> keyExtractor) {
        if (unsorted == null || unsorted.isEmpty()) {
            return Collections.emptySortedSet();
        }
        SortedSet<U> sorted = new TreeSet<>(Comparator.comparing(keyExtractor));
        sorted.addAll(unsorted);
        if (sorted.size() != unsorted.size()) {
            // there were key collisions and some data was lost -> throw away the sorted set
            // and sort by U's natural order
            sorted = new TreeSet<>();
            sorted.addAll(unsorted);
        }
        return sorted;
    }
}
