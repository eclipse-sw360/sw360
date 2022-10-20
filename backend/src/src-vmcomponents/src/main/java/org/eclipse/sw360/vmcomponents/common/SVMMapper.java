/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package org.eclipse.sw360.vmcomponents.common;

import com.google.common.base.Joiner;
import org.eclipse.sw360.datahandler.thrift.vmcomponents.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.CVEReference;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.VendorAdvisory;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.Vulnerability;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.apache.log4j.Logger.getLogger;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyCollection;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyMap;

/**
 * maps the updates of a new element to an existing one
 *
 * @author stefan.jaeger@evosoft.com
 */
public class SVMMapper {

    private final static Logger log = getLogger(SVMMapper.class);

    private static final String FORMAT_DATE_TIME = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // 2014-01-28T11:22:20Z
    public static final String NOT_FOUND = "NOT FOUND";

    private SVMMapper() {
    }

    public static VMAction setLastUpdate(VMAction action){
        if (action != null){
            return action.setLastUpdateDate(SW360Utils.getCreatedOnTime());
        }
        return action;
    }

    public static VMAction updateAction(VMAction oldElement, VMAction update){
        if (oldElement != null && update != null){
            return new VMAction(oldElement)
                    .setText(update.getText());
        }
        return oldElement;
    }

    public static VMAction updateActionByJSON(VMAction oldElement, JSONObject json){
        if (oldElement != null && json != null){
            String text = (String) json.get(SVMConstants.ACTION_TEXT);

            return new VMAction(oldElement)
                    .setText(text);
        }
        return oldElement;
    }

    public static VMPriority setLastUpdate(VMPriority priority){
        if (priority != null){
            return priority.setLastUpdateDate(SW360Utils.getCreatedOnTime());
        }
        return priority;
    }

    public static VMPriority updatePriority(VMPriority oldElement, VMPriority update){
        if (oldElement != null && update != null){
            return new VMPriority(oldElement)
                    .setLongText(update.getLongText())
                    .setShortText(update.getShortText());
        }
        return oldElement;
    }

    public static VMPriority updatePriorityByJSON(VMPriority oldElement, JSONObject json){
        if (oldElement != null && json != null){
            String shortText = (String) json.get(SVMConstants.PRIORITY_SHORT);
            String longText = (String) json.get(SVMConstants.PRIORITY_LONG);

            return new VMPriority(oldElement)
                    .setShortText(shortText)
                    .setLongText(longText);
        }
        return oldElement;
    }

    public static VMComponent setLastUpdate(VMComponent component){
        if (component != null){
            return component.setLastUpdateDate(SW360Utils.getCreatedOnTime());
        }
        return component;
    }

    public static VMComponent updateComponent(VMComponent oldElement, VMComponent update){
        if (oldElement != null && update != null){
            return new VMComponent(oldElement)
                    .setName(update.getName())
                    .setCpe(update.getCpe())
                    .setEolReached(update.isEolReached())
                    .setReceivedDate(update.getReceivedDate())
                    .setSecurityUrl(update.getSecurityUrl())
                    .setType(update.getType())
                    .setUrl(update.getUrl())
                    .setVendor(update.getVendor())
                    .setVersion(update.getVersion());
        }
        return oldElement;
    }

    public static VMComponent updateComponentByJSON(VMComponent oldElement, JSONObject json){
        if (oldElement != null && json != null){
            String vendor = (String) json.get(SVMConstants.COMPONENT_VENDOR);
            String name = (String) json.get(SVMConstants.COMPONENT_NAME);
            String version = (String) json.get(SVMConstants.COMPONENT_VERSION);
            String compUrl = (String) json.get(SVMConstants.COMPONENT_URL);
            String secUrl = (String) json.get(SVMConstants.COMPONENT_SECURITY_URL);
            Boolean eol = (Boolean) json.get(SVMConstants.COMPONENT_EOL_REACHED);
            boolean eolReached = eol != null && eol;
            String cpe = (String) json.get(SVMConstants.COMPONENT_CPE);

            return new VMComponent(oldElement)
                    .setVendor(vendor)
                    .setName(name)
                    .setVersion(version)
                    .setUrl(compUrl)
                    .setSecurityUrl(secUrl)
                    .setEolReached(eolReached)
                    .setCpe(cpe);
        }
        return oldElement;
    }

    public static Vulnerability updateVulnerabilityByJSON(Vulnerability oldElement, JSONObject json){
        if (oldElement != null && json != null){
            String title = (String) json.get(SVMConstants.VULNERABILITY_TITLE);
            String description = (String) json.get(SVMConstants.VULNERABILITY_DESCRIPTION);
            String publishDate = mapSVMDate((String) json.get(SVMConstants.VULNERABILITY_PUBLISH_DATE));
            String lastUpdate = mapSVMDate((String) json.get(SVMConstants.VULNERABILITY_LAST_UPDATE));
            Long priority = (Long) json.get(SVMConstants.VULNERABILITY_PRIORITY);
            Long action = (Long) json.get(SVMConstants.VULNERABILITY_ACTION);
            Set<String> compVmids = mapJSONStringArray((JSONArray) json.get(SVMConstants.VULNERABILITY_COMPONENTS));
            Set<VendorAdvisory> vas = mapJSONVendorAdvisories((JSONArray) json.get(SVMConstants.VULNERABILITY_VENDOR_ADVISORIES));
            String legalNotice = (String) json.get(SVMConstants.VULNERABILITY_LEGAL_NOTICE);
            String extendedDescription = (String) json.get(SVMConstants.VULNERABILITY_EXTENDED_DISC);
            Set<CVEReference> cveReferences = mapJSONCVEReferences((JSONArray) json.get(SVMConstants.VULNERABILITY_CVE_REFERENCES));
            Set<String> references = mapJSONStringArray((JSONArray) json.get(SVMConstants.VULNERABILITY_REFERENCES));

            return new Vulnerability(oldElement)
                    .setTitle(title)
                    .setDescription(description)
                    .setPublishDate(publishDate)
                    .setLastExternalUpdate(lastUpdate == null ? publishDate : lastUpdate)
                    .setPriority(priority==null?null:priority.toString())
                    .setAction(action==null?null:action.toString())
                    .setAssignedExtComponentIds(compVmids)
                    .setVendorAdvisories(vas)
                    .setLegalNotice(legalNotice)
                    .setExtendedDescription(extendedDescription)
                    .setCveReferences(cveReferences)
                    .setReferences(references);
        }
        return oldElement;
    }

    private static String mapSVMDate(String date){
        if (date == null)
            return null;
        try {
            // 2014-01-28T11:22:20Z => 2014-01-28 11:22:20
            SimpleDateFormat sdf = new SimpleDateFormat(FORMAT_DATE_TIME);
            Date parsed = sdf.parse(date);
            return SW360Utils.getDateTimeString(parsed);
        } catch (ParseException e) {
            log.error(e);
            return null;
        }
    }

    private static Set<String> mapJSONStringArray(JSONArray stringArray){
        if (stringArray == null || stringArray.size() == 0){
            return Collections.EMPTY_SET;
        }
        Set<String> set = new HashSet<>();
        for (Object string : stringArray) {
            set.add(string.toString());
        }
        return set;
    }

    private static Set<CVEReference> mapJSONCVEReferences(JSONArray cveReferences){
        if (cveReferences == null || cveReferences.size() == 0){
            return Collections.EMPTY_SET;
        }
        Set<CVEReference> set = new HashSet<>();
        for (Object cve : cveReferences) {
            JSONObject cveReference = (JSONObject) cve;
            Long cveYear = (Long) cveReference.get(SVMConstants.VULNERABILITY_CVE_YEAR);
            Long cveNumber = (Long) cveReference.get(SVMConstants.VULNERABILITY_CVE_NUMBER);
            set.add(new CVEReference(cveYear==null?null:cveYear.toString(), cveNumber==null?null:cveNumber.toString()));
        }
        return set;
    }

    private static Set<VendorAdvisory> mapJSONVendorAdvisories(JSONArray vendorAdvisories){
        if (vendorAdvisories == null || vendorAdvisories.size() == 0){
            return Collections.EMPTY_SET;
        }
        Set<VendorAdvisory> set = new HashSet<>();
        for (Object va : vendorAdvisories) {
            JSONObject vendorAdvisory = (JSONObject) va;
            String vendor = (String) vendorAdvisory.get(SVMConstants.VULNERABILITY_VA_VENDOR);
            String name = (String) vendorAdvisory.get(SVMConstants.VULNERABILITY_VA_NAME);
            String url = (String) vendorAdvisory.get(SVMConstants.VULNERABILITY_VA_URL);
            set.add(new VendorAdvisory(vendor, name, url));
        }
        return set;
    }

    public static VMMatch updateMatch(VMMatch match, VMComponent vmComponent, Release release, Supplier<Component> componentSupplier){
        if (match == null){
            return null;
        }
        if (vmComponent == null){
            match.setVmComponentCpe(NOT_FOUND)
                    .setVmComponentName(NOT_FOUND)
                    .setVmComponentVendor(NOT_FOUND)
                    .setVmComponentVersion(NOT_FOUND)
                    .setVmComponentVmid(NOT_FOUND);
        } else {
            match.setVmComponentId(vmComponent.getId())
                    .setVmComponentCpe(vmComponent.getCpe())
                    .setVmComponentName(vmComponent.getName())
                    .setVmComponentVendor(vmComponent.getVendor())
                    .setVmComponentVersion(vmComponent.getVersion())
                    .setVmComponentVmid(vmComponent.getVmid());
        }

        if (release == null){
            match.setReleaseCpe(NOT_FOUND)
                    .setComponentName(NOT_FOUND)
                    .setVendorName(NOT_FOUND)
                    .setReleaseVersion(NOT_FOUND)
                    .setReleaseSvmId(NOT_FOUND);
        } else {
            match.setReleaseId(release.getId())
                    .setReleaseCpe(release.getCpeid())
                    .setReleaseVersion(release.getVersion())
                    .setReleaseSvmId(nullToEmptyMap(release.getExternalIds())
                            .getOrDefault(SW360Constants.SVM_COMPONENT_ID, ""));

            String compName = release.getName();
            if (StringUtils.isEmpty(compName)){
                Component component = componentSupplier.get();
                if (component == null){
                    compName = NOT_FOUND;
                } else {
                    compName = component.getName();
                }
            }
            match.setComponentName(compName);

            if (release.getVendor() == null){
                match.setVendorName(NOT_FOUND);
            } else {
                match.setVendorName(release.getVendor().getFullname());
            }

            List<String> matchTypeNames = nullToEmptyCollection(match.getMatchTypes()).stream().map(VMMatchType::name).collect(Collectors.toList());
            String matchTypesUI = Joiner.on(", ").join(matchTypeNames);
            match.setMatchTypesUI(matchTypesUI);
        }
        return match;
    }

}
