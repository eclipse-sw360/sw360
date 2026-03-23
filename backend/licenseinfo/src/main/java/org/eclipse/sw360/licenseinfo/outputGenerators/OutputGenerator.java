/*
 * Copyright Bosch Software Innovations GmbH, 2016.
 * With modifications by Siemens AG, 2017-2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.licenseinfo.outputGenerators;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.apache.velocity.tools.ToolManager;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.projects.ObligationStatusInfo;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenseinfo.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.licenseinfo.util.LicenseNameWithTextUtils;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class OutputGenerator<T> {
    private static final Logger log = LogManager.getLogger(OutputGenerator.class);
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

    public abstract T generateOutputFile(Collection<LicenseInfoParsingResult> projectLicenseInfoResults,
            Project project, Collection<ObligationParsingResult> obligationResults, User user,
            Map<String, String> externalIds, Map<String, ObligationStatusInfo> obligationsStatus, String fileName,
            boolean excludeReleaseVersion)
            throws SW360Exception;

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

    public String getComponentLongNameWithoutVersion(LicenseInfoParsingResult li) {
        return SW360Utils.getReleaseFullname(li.getVendor(), li.getName(), "");
    }

    public String getComponentShortName(LicenseInfoParsingResult li) {
        return SW360Utils.getReleaseFullname("", li.getName(), li.getVersion());
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
	protected SortedMap<String, LicenseInfoParsingResult> getSortedLicenseInfos(
			Collection<LicenseInfoParsingResult> projectLicenseInfoResults, boolean excludeReleaseVersion) {
		Map<String, LicenseInfoParsingResult> licenseInfos = excludeReleaseVersion
				? projectLicenseInfoResults.stream()
						.collect(Collectors.toMap(this::getComponentLongNameWithoutVersion, li -> li,
								(l1, l2) -> mergeLicenseInfoParsingResults(l1, l2, true)))
				: projectLicenseInfoResults.stream().collect(Collectors.toMap(this::getComponentLongName, li -> li,
						(l1, l2) -> mergeLicenseInfoParsingResults(l1, l2, false)));
		return sortStringKeyedMap(licenseInfos);
	}

    @NotNull
    protected LicenseInfoParsingResult mergeLicenseInfoParsingResults(LicenseInfoParsingResult r1, LicenseInfoParsingResult r2, boolean excludeReleaseVersion){
        String r1_name = excludeReleaseVersion ? getComponentLongNameWithoutVersion(r1) : getComponentLongName(r1);
        String r2_name = excludeReleaseVersion ? getComponentLongNameWithoutVersion(r2) : getComponentLongName(r2);

        if (r1.getStatus() != LicenseInfoRequestStatus.SUCCESS || r2.getStatus() != LicenseInfoRequestStatus.SUCCESS ||
                !r1_name.equals(r2_name)){
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


    protected Map<String, Set<String>> buildAcknowledgements(
            SortedMap<String, LicenseInfoParsingResult> sortedLicenseInfos) {
        Map<String, Set<String>> acknowledgements = new LinkedHashMap<>();

        for (Map.Entry<String, LicenseInfoParsingResult> e : sortedLicenseInfos.entrySet()) {
            LicenseInfoParsingResult pr = e.getValue();
            if (pr == null) continue;

            LicenseInfo li = pr.getLicenseInfo();
            if (li == null) continue;

            Set<LicenseNameWithText> licenses = li.getLicenseNamesWithTexts();
            if (licenses == null || licenses.isEmpty()) continue;

            Set<String> ackSet = new HashSet<>();

            for (LicenseNameWithText l : licenses) {
                String acks = l.getAcknowledgements();
                if (acks != null && !acks.isBlank()) {
                    ackSet.add(acks);
                }
            }

            if (!ackSet.isEmpty()) {
                acknowledgements.put(e.getKey(), ackSet);
            }
        }

        return acknowledgements;
    }


    public static String normaliseLicenseText(LicenseNameWithText lnt) {
        String name = lnt.getLicenseName();
        String text = lnt.getLicenseText();

        if (text == null || text.isEmpty()) {
            return "";
        }

        int len = text.length();

        StringBuilder sb = new StringBuilder(name.length() + 1 + len);

        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);

            if ((c >= '0' && c <= '9') ||
                    (c >= 'a' && c <= 'z')) {

                sb.append(c);

            } else if (c >= 'A' && c <= 'Z') {
                sb.append((char) (c + 32));
            }
        }

        //if the text is not in English or contains only special characters, retain the original text
        if(sb.length() == 0) return name + '\0' + text;
        return  name + '\0' + sb;
    }

    @NotNull
    protected SortedMap<String, Map<String, Set<String>>> getSortedAcknowledgements(Map<String, LicenseInfoParsingResult> sortedLicenseInfos) {
        Map<String, Map<String, Set<String>>> acknowledgements = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map.Entry<String, LicenseInfoParsingResult> entry : sortedLicenseInfos.entrySet()) {
            String releaseName = entry.getKey();
            LicenseInfoParsingResult parsingResult = entry.getValue();
            LicenseInfo licenseInfo = parsingResult.getLicenseInfo();

            if (licenseInfo != null && licenseInfo.getLicenseNamesWithTexts() != null) {
                Map<String, Set<String>> licenseAckMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                for (LicenseNameWithText lnt : licenseInfo.getLicenseNamesWithTexts()) {
                    String licenseName = lnt.getLicenseName();
                    Set<String> acknowledgementsSet = new HashSet<>();
                    if(lnt.getAcknowledgements()!=null && !lnt.getAcknowledgements().isEmpty()){
                        acknowledgementsSet.add(lnt.getAcknowledgements());
                    }
                    if (!acknowledgementsSet.isEmpty()) {
                        licenseAckMap.put(licenseName, new TreeSet<>(acknowledgementsSet));
                    }
                }
                if (!licenseAckMap.isEmpty()) {
                    acknowledgements.put(releaseName, licenseAckMap);
                }
            }
        }

        return new TreeMap<>(acknowledgements);
    }

    @NotNull
    protected static List<LicenseNameWithText> getSortedLicenseNameWithTexts(Collection<LicenseInfoParsingResult> projectLicenseInfoResults) {
        Set<LicenseNameWithText> licenseNamesWithText = projectLicenseInfoResults.stream()
                .map(LicenseInfoParsingResult::getLicenseInfo)
                .filter(Objects::nonNull)
                .map(LicenseInfo::getLicenseNamesWithTexts)
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

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
    protected String renderTemplateWithDefaultValues(Collection<LicenseInfoParsingResult> projectLicenseInfoResults,
                                                     String file, String projectTitle, String licenseInfoHeaderText, String obligationsText,
                                                     Map<String, String> externalIds, boolean excludeReleaseVersion) {
        VelocityContext vc = getConfiguredVelocityContext();
        // set header
        vc.put(LICENSE_INFO_PROJECT_TITLE, projectTitle);
        vc.put(LICENSE_INFO_HEADER_TEXT, licenseInfoHeaderText);
        vc.put(OBLIGATIONS_TEXT, obligationsText);

        SortedMap<String, LicenseNameWithText> uniqueLicenses = new TreeMap<>();
        Map<LicenseNameWithText, String> objToString= new LinkedHashMap<>();

        for (LicenseInfoParsingResult r : projectLicenseInfoResults) {
            if (r == null || r.getLicenseInfo() == null) continue;

            Set<LicenseNameWithText> set = r.getLicenseInfo().getLicenseNamesWithTexts();
            if (set == null || set.isEmpty()) continue;

            for (LicenseNameWithText lnt : set) {
                if (lnt == null || LicenseNameWithTextUtils.isEmpty(lnt)) continue;

                String key = normaliseLicenseText(lnt);
                uniqueLicenses.putIfAbsent(key, lnt);
                objToString.put(lnt,key);
            }
        }

        Map<String, Integer> licenseToReferenceId = new LinkedHashMap<>();
        Map<Integer, LicenseNameWithText> allLicensesForVm = new LinkedHashMap<>();

        int refId = 1;

        for (Map.Entry<String, LicenseNameWithText> entry : uniqueLicenses.entrySet()) {
            licenseToReferenceId.put(entry.getKey(),refId);
            allLicensesForVm.put(refId, entry.getValue());
            refId++;
        }

        vc.put(ALL_LICENSE_NAMES_WITH_TEXTS, allLicensesForVm);

        Map<String, LicenseInfoParsingResult> successResults = new HashMap<>();
        Map<String, List<LicenseInfoParsingResult>> failedResults= new HashMap<>();
        Map<LicenseNameWithText,Integer> licenseNameWithTextToRefId = new HashMap<>();

        parseAllReleases(projectLicenseInfoResults, excludeReleaseVersion, failedResults, successResults);

        //sort licenseNameWithText object within each Release
        for (LicenseInfoParsingResult r : successResults.values()) {
            LicenseInfo li = r.getLicenseInfo();
            if (li == null) continue;
            Set<LicenseNameWithText> set = li.getLicenseNamesWithTexts();
            if (set == null) continue;
            for(LicenseNameWithText lnt : set) {
                String key = objToString.get(lnt);
                licenseNameWithTextToRefId.put(lnt,licenseToReferenceId.get(key));
            }
            if (set.size() > 1) {
                SortedSet<LicenseNameWithText> sortedSet = new TreeSet<>(Comparator.comparing(a->a.getLicenseName()));
                sortedSet.addAll(set);
                li.setLicenseNamesWithTexts(sortedSet);
            }
        }

        SortedMap<String, LicenseInfoParsingResult> sortedReleases =
                sortStringKeyedMap(successResults);
        vc.put(LICENSE_REFERENCE_ID_MAP_CONTEXT_PROPERTY, licenseNameWithTextToRefId);
        vc.put(LICENSE_INFO_RESULTS_CONTEXT_PROPERTY, sortedReleases);
        vc.put(LICENSE_INFO_ERROR_RESULTS_CONTEXT_PROPERTY, failedResults);

        // also display acknowledgments
        Map<String, Set<String>> acknowledgements = buildAcknowledgements(sortedReleases);
        vc.put(ACKNOWLEDGEMENTS_CONTEXT_PROPERTY, acknowledgements);

        vc.put(EXTERNAL_IDS, externalIds);

        StringWriter sw = new StringWriter();
        Velocity.mergeTemplate(file, "utf-8", vc, sw);
        IOUtils.closeQuietly(sw);
        return sw.toString();
    }

    private void parseAllReleases(Collection<LicenseInfoParsingResult> projectLicenseInfoResults, boolean excludeReleaseVersion,
                                  Map<String, List<LicenseInfoParsingResult>> failedResults, Map<String, LicenseInfoParsingResult> successResults) {
        for (LicenseInfoParsingResult r : projectLicenseInfoResults) {
            if (r == null) continue;
            String key = excludeReleaseVersion
                    ? getComponentLongNameWithoutVersion(r)
                    : getComponentLongName(r);

            if (r.getStatus() != LicenseInfoRequestStatus.SUCCESS) {
                failedResults
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(r);
                continue;
            }
            LicenseInfoParsingResult existing = successResults.get(key);
            if (existing == null) {
                successResults.put(key, r);
                continue;
            }

            LicenseInfo target = existing.getLicenseInfo();
            LicenseInfo incoming = r.getLicenseInfo();

            if (target == null) {
                existing.setLicenseInfo(incoming);
                continue;
            }

            if (incoming == null) {
                continue;
            }

            // merge license names
            if (incoming.isSetLicenseNamesWithTexts()) {
                if (!target.isSetLicenseNamesWithTexts()) {
                    target.setLicenseNamesWithTexts(new HashSet<>());
                }
                target.getLicenseNamesWithTexts()
                        .addAll(incoming.getLicenseNamesWithTexts());
            }

            // merge copyrights
            if (incoming.isSetCopyrights()) {
                if (!target.isSetCopyrights()) {
                    target.setCopyrights(new HashSet<>());
                }
                target.getCopyrights()
                        .addAll(incoming.getCopyrights());
            }

            // merge filenames
            if (incoming.isSetFilenames()) {
                if (!target.isSetFilenames()) {
                    target.setFilenames(new ArrayList<>());
                }
                target.getFilenames()
                        .addAll(incoming.getFilenames());
            }
        }
    }


    /**
     * Uses the given map to sort the licenses inside a license of by id. The given
     * map must contain an id for each license name present in the license info objects.
     * @param licenseInfoResults
     *          parsing results with license infos and licenses to be sorted
     * @param licenseToReferenceId
     *          mapping of license name to id to be able to sort the licenses
     */
    private void sortLicenseNamesWithinEachLicenseInfoById(Collection<LicenseInfoParsingResult> licenseInfoResults,
            Map<LicenseNameWithText, Integer> licenseToReferenceId) {
        licenseInfoResults.stream().map(LicenseInfoParsingResult::getLicenseInfo).filter(Objects::nonNull)
                .forEach((LicenseInfo li) -> li.setLicenseNamesWithTexts(
                        sortSet(li.getLicenseNamesWithTexts(), licenseNameWithText -> Optional.ofNullable(licenseToReferenceId.get(licenseNameWithText)).orElse(Integer.MAX_VALUE))));
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
