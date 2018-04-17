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

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

public class CveSearchData {

    /**
     * wrapper around entries of the vulnerable configuration list, since
     * - sometimes they are plain strings containing the cpe
     * - sometimes they are objects containing an "id" which is the cpe and an "title" which is the human readeable name of an release
     */
    static public class VulnerableConfigurationEntry {
        private String title;
        private String id;

        public VulnerableConfigurationEntry(String title, String id) {
            this.title = title;
            this.id = id;
        }

        public VulnerableConfigurationEntry(String id) {
            this.title = id;
            this.id = id;
        }

        public Map.Entry<String,String> getAsMapEntry() {
            return new HashMap.SimpleImmutableEntry<>(id,title);
        }
    }

    /**
     * wrapper for date fields in Object, since
     * - the api of version <=2.1 returns a date as formatted string
     * - the api of version >=2.2 returns a object which contains the field "$date" which contains a long
     */
    public static class DateTimeObject {
        private String formattedDate;

        public DateTimeObject(String formattedDate) {
            this.formattedDate = formattedDate;
        }

        public DateTimeObject(long dateAsLong) {
            this.formattedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
                          .format(new Date(dateAsLong));
        }

        public String getFormattedDate() {
            return formattedDate;
        }
    }

    //==================================================================================================================
    // Data extracted from raw cve-search result
    private String id;
    private Set<String> references;
    @SerializedName("Modified") private DateTimeObject modified;
    @SerializedName("Published") private DateTimeObject published;
    private Set<VulnerableConfigurationEntry> vulnerable_configuration;
    /**
     * Common Vulnerability Scoring System
     */
    private Double cvss;
    @SerializedName("cvss-time") private DateTimeObject cvss_time;
    /**
     * CWE™ International in scope and free for public use, CWE provides a unified, measurable set of software
     * weaknesses that is enabling more effective discussion, description, selection, and use of software security tools
     * and services that can find these weaknesses in source code and operational systems as well as better
     * understanding and management of software weaknesses related to architecture and design.
     *
     * see: https://cwe.mitre.org/
     */
    private String cwe;
    private Map<String,String> impact;
    private Map<String,String> access;
    private String summary;
    private Map<String,String> map_cve_bid;
    private Map<String,String> map_cve_debian;
    private Map<String,String> map_cve_exploitdb;
    private Map<String,String> map_cve_gedora;
    private Map<String,String> map_cve_hp;
    private Map<String,String> map_cve_iavm;
    private Map<String,String> map_cve_msf;
    private Map<String,String> map_cve_nessus;
    private Map<String,String> map_cve_openvas;
    private Map<String,String> map_cve_osvdb;
    private Map<String,String> map_cve_oval;
    private Map<String,String> map_cve_saint;
    private Map<String,String> map_cve_scip;
    private Map<String,String> map_cve_suse;
    private Map<String,String> map_cve_vmware;
    private Map<String,String> map_redhat_bugzilla;
    private Set<Set<Map<String,Integer>>> ranking; // only filled, when `cve`-api is used, not `cvefor`
    /**
     * CAPEC™ is a comprehensive dictionary and classification taxonomy of known attacks that can be used by analysts,
     * developers, testers, and educators to advance community understanding and enhance defenses.
     *
     * see: https://capec.mitre.org/
     */
    private Set<Map<String,List<String>>> capec;

    //==================================================================================================================
    // Other metadata
    private String matchedBy;
    private String usedNeedle;

    //==================================================================================================================
    // getter and setter
    public Map<String, String> getAccess() {
        return access;
    }

    public String getId() {
        return id;
    }

    public String getModified() {
        if(modified == null){
            return null;
        }
        return modified.getFormattedDate();
    }

    public Set<String> getReferences() {
        return references;
    }

    public String getPublished() {
        if(published == null){
            return null;
        }
        return published.getFormattedDate();
    }

    public String getCvss_time() {
        if(cvss_time == null){
            return null;
        }
        return cvss_time.getFormattedDate();
    }

    public Map<String,String> getVulnerable_configuration() {
        Map<String,String> toReturn = new HashMap<>();
        nullToEmptySet(vulnerable_configuration).stream()
                .map(VulnerableConfigurationEntry::getAsMapEntry)
                .forEach(entry -> toReturn.put(entry.getKey(), entry.getValue()));
        return toReturn;
    }

    public Double getCvss() {
        return cvss;
    }

    public String getCwe() {
        return cwe;
    }

    public Map<String, String> getImpact() {
        return impact;
    }

    public String getSummary() {
        return summary;
    }

    public Map<String,Map<String,String>> getMap_cve_all(){
        Map<String,Map<String,String>> mapOfAll = new HashMap<>();

        BiConsumer<String, Map<String,String>> f = (title, map) -> {if(map != null) mapOfAll.put(title, map);};

        f.accept("bid"            , map_cve_bid);
        f.accept("debian"         , map_cve_debian);
        f.accept("exploitdb"      , map_cve_exploitdb);
        f.accept("gedora"         , map_cve_gedora);
        f.accept("hp"             , map_cve_hp);
        f.accept("iavm"           , map_cve_iavm);
        f.accept("msf"            , map_cve_msf);
        f.accept("nessus"         , map_cve_nessus);
        f.accept("openvas"        , map_cve_openvas);
        f.accept("osvdb"          , map_cve_osvdb);
        f.accept("oval"           , map_cve_oval);
        f.accept("saint"          , map_cve_saint);
        f.accept("scip"           , map_cve_scip);
        f.accept("suse"           , map_cve_suse);
        f.accept("vmware"         , map_cve_vmware);
        f.accept("redhat_bugzilla", map_redhat_bugzilla);

        return mapOfAll;
    }

    public Set<Map<String,String>> getCapec() {
        Set<Map<String,String>> capecToReturn = new HashSet<>();

        for(Map<String,List<String>> rawCapecData : capec){
            Map<String,String> capecData = new HashMap<>();
            for(Map.Entry<String,List<String>> rawCapecDataEntry : rawCapecData.entrySet()){
                StringBuilder sb = new StringBuilder();
                for(String line : rawCapecDataEntry.getValue()){
                    sb.append(line);
                    sb.append("\\n");
                }
                capecData.put(rawCapecDataEntry.getKey(), sb.toString());
            }
        }

        return capecToReturn;
    }

    public Set<Set<Map<String, Integer>>> getRanking() {
        return ranking;
    }

    public CveSearchData setMatchedBy(String matchedBy) {
        this.matchedBy = matchedBy;
        return this;
    }

    public String getMatchedBy() {
        return matchedBy;
    }

    public CveSearchData setUsedNeedle(String usedNeedle) {
        this.usedNeedle = usedNeedle;
        return this;
    }

    public String getUsedNeedle() {
        return usedNeedle;
    }
}
