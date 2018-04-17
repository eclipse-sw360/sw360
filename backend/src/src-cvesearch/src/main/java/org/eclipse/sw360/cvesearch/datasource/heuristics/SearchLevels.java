/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Copyright Siemens AG, 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.cvesearch.datasource.heuristics;

import org.apache.log4j.Logger;
import org.eclipse.sw360.cvesearch.datasource.CveSearchApi;
import org.eclipse.sw360.cvesearch.datasource.CveSearchGuesser;
import org.eclipse.sw360.cvesearch.datasource.matcher.Match;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyString;

public class SearchLevels {

    private static final Logger log = Logger.getLogger(SearchLevels.class);

    private static final String CPE_PREFIX = "cpe:2.3:";
    private static final String OLD_CPE_PREFIX = "cpe:/";
    private static final String CPE_WILDCARD = ".*";
    private static final String CPE_NEEDLE_PREFIX = CPE_PREFIX + ".:";

    private static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    private static final String VENDOR_THRESHOLD_PROPERTY = "cvesearch.vendor.threshold";
    private static final String PRODUCT_THRESHOLD_PROPERTY = "cvesearch.product.threshold";
    private static final String CUTOFF_PROPERTY = "cvesearch.cutoff";

    private static final int DEFAULT_VENDOR_THRESHOLD = 1;
    private static final int DEFAULT_PRODUCT_THRESHOLD = 0;
    private static final int DEFAULT_CUTOFF = 6;

    private final List<SearchLevel> searchLevels = new ArrayList<>();

    public class NeedleWithMeta {
        public String needle;
        public String description;
        public NeedleWithMeta(String needle, String description){
            this.needle = needle;
            this.description = description;
        }
    }

    @FunctionalInterface
    public interface SearchLevel {
        List<NeedleWithMeta> apply(Release release) throws IOException;
    }


    public SearchLevels(CveSearchApi cveSearchApi) {
        log.info("Preparing Search Levels");
        Properties props = CommonUtils.loadProperties(SearchLevels.class, PROPERTIES_FILE_PATH);
        int vendorThreshold = getIntFromProperties(props, VENDOR_THRESHOLD_PROPERTY, DEFAULT_VENDOR_THRESHOLD);
        int productThreshold = getIntFromProperties(props, PRODUCT_THRESHOLD_PROPERTY, DEFAULT_PRODUCT_THRESHOLD);
        int cutoff = getIntFromProperties(props, CUTOFF_PROPERTY, DEFAULT_CUTOFF);

        setup(cveSearchApi, vendorThreshold, productThreshold, cutoff);
    }

    private static int getIntFromProperties(Properties properties, String key, int defaultValue) {
        int value = CommonUtils.getIntOrDefault(properties.getProperty(key), defaultValue);
        log.info("SearchLevels " + key + ": " + value);
        return value;
    }

    private void setup(CveSearchApi cveSearchApi, int vendorThreshold, int productThreshold, int cutoff) {
        // Level 1. search by full cpe
        addCPESearchLevel();
        // Level 2. and 3.
        addGuessingSearchLevels(cveSearchApi, vendorThreshold, productThreshold, cutoff);
    }

    public Stream<List<NeedleWithMeta>> apply(Release release) throws IOException {
        try {
        return searchLevels.stream()
                .map(searchLevel -> {
                    try {
                        return searchLevel.apply(release);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }) ;
        } catch (UncheckedIOException ue) {
            throw ue.getIOExceptionCause();
        }
    }

    class UncheckedIOException extends RuntimeException{
        UncheckedIOException(IOException e) {
            super(e);
        }

        IOException getIOExceptionCause(){
            return (IOException) getCause();
        }
    }

    //==================================================================================================================
    protected String cleanupCPE(String cpe) {
        if(cpe.startsWith(OLD_CPE_PREFIX)){ // convert cpe2.2 to cpe2.3
            cpe = cpe.replaceAll("^"+OLD_CPE_PREFIX, CPE_PREFIX)
                    .replace("::", ":-:")
                    .replace("~-", "~")
                    .replace("~",  ":-:")
                    .replace("::", ":")
                    .replaceAll("[:-]*$", "");
        }
        return cpe.toLowerCase();
    }

    private void addCPESearchLevel() {
        Predicate<Release> isPossible = r -> r.isSetCpeid() && isCpe(r.getCpeid().toLowerCase());
        searchLevels.add(r -> {
            if(isPossible.test(r)){
                return singletonList(new NeedleWithMeta(cleanupCPE(r.getCpeid()), "CPE"));
            }
            return Collections.emptyList();
        });
    }

    private void addGuessingSearchLevels(CveSearchApi cveSearchApi, int vendorThreshold, int productThreshold, int cutoff) {
        CveSearchGuesser cveSearchGuesser = new CveSearchGuesser(cveSearchApi);
        cveSearchGuesser.setVendorThreshold(vendorThreshold);
        cveSearchGuesser.setProductThreshold(productThreshold);
        cveSearchGuesser.setCutoff(cutoff);

        // Level 2. search by guessed vendors and products with version
        searchLevels.add(release -> guessForRelease(cveSearchGuesser, release, true));

        // Level 3. search by guessed vendors and products without version
        searchLevels.add(release -> guessForRelease(cveSearchGuesser, release, false));
    }


    private List<NeedleWithMeta> guessForRelease(CveSearchGuesser cveSearchGuesser, Release release, boolean useVersionInformation) throws IOException {
        if (useVersionInformation && !release.isSetVersion()) {
            return Collections.emptyList();
        }

        List<Match> vendorProductList;

        String productHaystack = release.getName();
        if (release.isSetVendor() &&
                (release.getVendor().isSetShortname() || release.getVendor().isSetFullname())) {
            String vendorHaystack = nullToEmptyString(release.getVendor().getShortname()) + " " +
                    nullToEmptyString(release.getVendor().getFullname());
            vendorProductList = cveSearchGuesser.guessVendorAndProducts(vendorHaystack, productHaystack);
        } else {
            vendorProductList = cveSearchGuesser.guessVendorAndProducts(productHaystack);
        }

        String cpeNeedlePostfix = ":" + (useVersionInformation ? release.getVersion() : "") + CPE_WILDCARD;
        Function<String,String> cpeBuilder = cpeNeedle -> CPE_NEEDLE_PREFIX + cpeNeedle + cpeNeedlePostfix;

        return vendorProductList.stream()
                .map(match -> new NeedleWithMeta(cpeBuilder.apply(match.getNeedle()),
                        "heuristic (dist. " + (useVersionInformation ? "0" : "1") + match.getDistance() + ")"))
                .collect(Collectors.toList());
    }

    protected boolean isCpe(String potentialCpe){
        return (! (null == potentialCpe))
                && ( potentialCpe.startsWith(CPE_PREFIX) || potentialCpe.startsWith(OLD_CPE_PREFIX) )
                && potentialCpe.length() > 10;
    }
}
