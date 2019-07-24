/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.cvesearch.datasource;

import org.eclipse.sw360.cvesearch.datasource.matcher.ListMatcher;
import org.eclipse.sw360.cvesearch.datasource.matcher.Match;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CveSearchGuesser {

    private final CveSearchApi cveSearchApi;
    private ListMatcher vendorMatcher;
    private Map<String,ListMatcher> productMatchers;

    private int vendorThreshold = 0;
    private int productThreshold = 0;
    private int cutoff = Integer.MAX_VALUE;

    private Logger log = Logger.getLogger(CveSearchGuesser.class);

    public CveSearchGuesser(CveSearchApi cveSearchApi) {
        this.cveSearchApi=cveSearchApi;
        vendorMatcher = null;
        productMatchers = new HashMap<>();
    }

    public void setVendorThreshold(int vendorThreshold) {
        this.vendorThreshold = vendorThreshold;
    }

    public void setProductThreshold(int productThreshold) {
        this.productThreshold = productThreshold;
    }

    public void setCutoff(int cutoff) {
        this.cutoff = cutoff;
    }

    public boolean addVendorGuesserIfNeeded() {
        if(vendorMatcher == null) {
            try {
                vendorMatcher = new ListMatcher(cveSearchApi.allVendorNames());
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    public boolean addProductGuesserIfNeeded(String vendor) {
        if(! productMatchers.containsKey(vendor)) {
            try {
                productMatchers.put(vendor, new ListMatcher(cveSearchApi.allProductsOfVendor(vendor)));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                return false;
            }
        }
        return true;
    }

    public List<Match> getBest(List<Match> matches, int threshold) {
        if(matches.size() == 0){
            return Collections.emptyList();
        }
        List<Match> bestMatches = new ArrayList<>();
        int minDistance = matches.get(0).getDistance();

        Iterator<Match> matchesIterator = matches.iterator();
        Match current;
        do{
            current = matchesIterator.next();
            if(current.getDistance() > minDistance + threshold || current.getDistance() >= cutoff){
                break;
            }
            bestMatches.add(current);
        }while(matchesIterator.hasNext());

        return bestMatches;
    }

    public List<Match> guessVendors(String vendorHaystack) throws IOException {
        if (!addVendorGuesserIfNeeded()){
            throw new IOException("Was not able to instantiate vendor guesser");
        }
        return getBest(vendorMatcher.getMatches(vendorHaystack), vendorThreshold);
    }

    public List<Match> guessProducts(String vendor, String productHaystack) throws IOException {
        if (!addProductGuesserIfNeeded(vendor)) {
            throw new IOException("Was not able to instantiate product guesser for vendor " + vendor);
        }
        return getBest(productMatchers.get(vendor).getMatches(productHaystack), productThreshold);
    }

    public List<Match> guessVendorAndProducts(String haystack) throws IOException {
        return guessVendorAndProducts(haystack, haystack);
    }

    public List<Match> guessVendorAndProducts(String vendorHaystack, String productHaystack) throws IOException {
        List<Match> result = new ArrayList<>();
        List<Match> vendors = guessVendors(vendorHaystack);

        for (Match vendor : vendors) {
            result.addAll(guessProducts(vendor.getNeedle(), productHaystack).stream()
                    .map(product -> vendor.concat(product))
                    .collect(Collectors.toList()));
        }

        return result.stream()
                .sorted((sm1,sm2) -> sm1.compareTo(sm2))
                .filter(sm -> cutoff == 0 || cutoff > sm.getDistance())
                .collect(Collectors.toList());
    }
}
