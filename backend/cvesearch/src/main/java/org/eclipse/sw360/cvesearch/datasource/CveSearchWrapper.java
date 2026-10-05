/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.cvesearch.datasource;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import org.eclipse.sw360.cvesearch.datasource.heuristics.Heuristic;
import org.eclipse.sw360.cvesearch.datasource.heuristics.SearchLevels;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.packages.Package;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class CveSearchWrapper {

    private static final Logger log = LogManager.getLogger(CveSearchWrapper.class);

    private final Heuristic heuristic;

    public CveSearchWrapper(CveSearchApi cveSearchApi) {
        SearchLevels searchLevels = new SearchLevels(cveSearchApi);
        heuristic = new Heuristic(searchLevels, cveSearchApi);
    }

    public Optional<List<CveSearchData>> searchForRelease(Release release) {
        try {
            return Optional.of(heuristic.run(release));
        } catch (IOException e) {
            log.error("Was not able to search for release with name=" + release.getName() + " and id=" + release.getId(), e);
        }
        return Optional.empty();
    }

    /**
     * Searches for CVEs matching the given package by decomposing its PURL into
     * name/namespace/version and running the existing heuristic-based search.
     * A synthetic Release is constructed so the heuristic can be reused without modification.
     */
    public Optional<List<CveSearchData>> searchForPackage(Package pkg) {
        if (!pkg.isSetPurl() || StringUtils.isBlank(pkg.getPurl())) {
            log.warn("Package {} has no PURL, skipping CVE search", pkg.getId());
            return Optional.empty();
        }
        try {
            PackageURL purl = new PackageURL(pkg.getPurl().trim());
            Release syntheticRelease = buildSyntheticRelease(purl, pkg);
            return Optional.of(heuristic.run(syntheticRelease));
        } catch (MalformedPackageURLException e) {
            log.error("Malformed PURL for package id={}: {}", pkg.getId(), pkg.getPurl(), e);
        } catch (IOException e) {
            log.error("Was not able to search for package id={} purl={}", pkg.getId(), pkg.getPurl(), e);
        }
        return Optional.empty();
    }

    private Release buildSyntheticRelease(PackageURL purl, Package pkg) {
        Release release = new Release();
        release.setId(pkg.getId());
        release.setName(purl.getName());
        if (StringUtils.isNotBlank(purl.getVersion())) {
            release.setVersion(purl.getVersion());
        }
        if (StringUtils.isNotBlank(purl.getNamespace())) {
            org.eclipse.sw360.datahandler.thrift.vendors.Vendor vendor =
                    new org.eclipse.sw360.datahandler.thrift.vendors.Vendor();
            vendor.setShortname(purl.getNamespace());
            vendor.setFullname(purl.getNamespace());
            release.setVendor(vendor);
        }
        return release;
    }
}
