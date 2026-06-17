/*
 * Copyright Siemens AG, 2013-2017.
 * Copyright Bosch Software Innovations GmbH, 2018.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.importer;

import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.*;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.utils.ConvertRecord;
import org.eclipse.sw360.exporter.utils.LicsArchive;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.ImportCSV.readAsCSVRecords;
import static org.eclipse.sw360.exporter.utils.LicsArchive.*;
import static org.eclipse.sw360.exporter.utils.TypeMappings.*;

public class LicsImporter {
    private static final Logger log = LogManager.getLogger(LicsImporter.class);

    private final boolean overwriteIfExternalIdMatches;
    private final boolean overwriteIfIdMatchesEvenWithoutExternalIdMatch;

    private final LicenseService.Iface licenseClient;

    public LicsImporter(LicenseService.Iface licenseClient, boolean overwriteIfExternalIdMatches, boolean overwriteIfIdMatchesEvenWithoutExternalIdMatch) {
        this.licenseClient = licenseClient;
        this.overwriteIfExternalIdMatches = overwriteIfExternalIdMatches;
        this.overwriteIfIdMatchesEvenWithoutExternalIdMatch = overwriteIfIdMatchesEvenWithoutExternalIdMatch;
    }

    public void importLics(User user, Map<String, InputStream> inputMap) throws TException {
        final List<License> licensesToAdd = parseLics(user, inputMap);

        final List<License> knownLicenses = licenseClient.getLicenses();
        final List<License> filteredLicenses = reworkAndFilterLicenses(licensesToAdd, knownLicenses);
        log.info("Sending " + filteredLicenses.size() + " of " + licensesToAdd.size() + " Licenses to the database!");
        addLicenses(filteredLicenses, user);
    }

    private Map<Integer, Obligation> parseLicsTodoMap(User user, Map<String, InputStream> inputMap) throws TException {

        log.debug("Parsing obligations ...");
        return getTodoMapAndWriteMissingToDatabase(licenseClient, inputMap.get(TODO_FILE), user);
    }

    private Map<Integer, LicenseType> parseLicsLicenseTypeMap(User user, Map<String, InputStream> inputMap) throws TException {
        log.debug("Parsing license types ...");
        return getIdentifierToTypeMapAndWriteMissingToDatabase(licenseClient,
                inputMap.get(LICENSETYPE_FILE), LicenseType.class, Integer.class, user);
    }

    private Map<String, Set<Integer>> parseLicsTodoToLicenseMap(Map<String, InputStream> inputMap) {
        log.debug("Parsing license obligations ...");
        List<CSVRecord> licenseTodoRecord = readAsCSVRecords(inputMap.get(LICENSE_TODO_FILE));
        return ConvertRecord.convertRelationalTable(licenseTodoRecord);
    }

    private Map<Integer, Obligation> enhanceLicsTodosWithCustomProperties(User user, Map<String, InputStream> inputMap, Map<Integer, Obligation> obligMap) throws TException {
        if(inputMap.containsKey(CUSTOM_PROPERTIES_FILE)) {
            log.debug("Parsing custom properties ...");
            Map<Integer, ConvertRecord.PropertyWithValue> customPropertiesMap =
                    getCustomPropertiesWithValuesByIdAndWriteMissingToDatabase(licenseClient, inputMap.get(CUSTOM_PROPERTIES_FILE), user);

            log.debug("Parsing oblig custom properties relation ...");
            List<CSVRecord> obligPropertiesRecord = readAsCSVRecords(inputMap.get(TODO_CUSTOM_PROPERTIES_FILE));
            Map<Integer, Set<Integer>> obligPropertiesMap = ConvertRecord.convertRelationalTableWithIntegerKeys(obligPropertiesRecord);

            return updateTodoMapWithCustomPropertiesAndWriteToDatabase(licenseClient, obligMap, customPropertiesMap, obligPropertiesMap, user);
        }
        return obligMap;
    }

    private List<License> parseLics(User user, Map<String, InputStream> inputMap) throws TException {
        if (! LicsArchive.isValidLicenseArchive(inputMap)) {
            throw new SW360Exception("Invalid file format");
        }

        Map<Integer, LicenseType> licenseTypeMap = parseLicsLicenseTypeMap(user, inputMap);
        Map<Integer, Obligation> obligMap = parseLicsTodoMap(user, inputMap);
        obligMap = enhanceLicsTodosWithCustomProperties(user, inputMap, obligMap);
        Map<String, Set<Integer>> licenseTodoMap = parseLicsTodoToLicenseMap(inputMap);

        log.debug("Parsing licenses ...");
        List<CSVRecord> licenseRecord = readAsCSVRecords(inputMap.get(LICENSE_FILE));

        return ConvertRecord.fillLicenses(licenseRecord, licenseTypeMap, obligMap, licenseTodoMap);
    }

    private Map<String,Map<String,Set<String>>> genExternalIdToIdLookupMap(List<License> licenses) {
        Map<String,Map<String,Set<String>>> result = new HashMap<>();

        licenses.stream()
                .filter(license -> license.getExternalIds() != null)
                .forEach(license -> license.getExternalIds().forEach((key, value) -> {
            if (!result.containsKey(key)) {
                result.put(key, new HashMap<>());
            }
            if (!result.get(key).containsKey(value)) {
                result.get(key).put(value, new HashSet<>());
            }
            result.get(key).get(value).add(license.getId());
        }));

        return result;
    }

    private Optional<String> lookupIdInExtarnalIdToIdLookupMap(Map<String,Map<String,Set<String>>> map, String externalIdName, String externalId) {
        if(map.containsKey(externalIdName)) {
            if (map.get(externalIdName).containsKey(externalId)) {
                final Set<String> matchingIds = map.get(externalIdName).get(externalId);
                if (matchingIds.size() <= 1) {
                    return matchingIds.stream().findFirst();
                } else {
                    log.warn("The external id=[" + externalIdName + ":" + externalId + "] was not uniq, it is found at licenses with the ids: " +
                            matchingIds.stream().collect(Collectors.joining(", ")));
                }
            }

        }
        return Optional.empty();
    }

    private Optional<String> lookupExternalIdsInExtarnalIdToIdLookupMap(Map<String,Map<String,Set<String>>> map, Map<String,String> externalIds) {
        final Set<String> collectedIds = externalIds.entrySet().stream()
                .map(e -> lookupIdInExtarnalIdToIdLookupMap(map, e.getKey(), e.getValue()))
                .filter(Optional::isPresent).map(Optional::get)
                .collect(Collectors.toSet());
        if (collectedIds.size() == 1) {
            return collectedIds.stream().findAny();
        }

        log.warn("The external ids matched multiple licenses: " +
                collectedIds.stream().collect(Collectors.joining(", ")));
        return Optional.empty();
    }

    private List<License> reworkAndFilterLicenses(List<License> licensesToAdd, List<License> knownLicenses) {
        final Set<String> knownLicenseNames = knownLicenses.stream().map(License::getId).collect(Collectors.toSet());
        final Map<String,Map<String,Set<String>>> externalIdToIdLookupMap = genExternalIdToIdLookupMap(knownLicenses);

        final List<License> result = new ArrayList<>();

        for(License licenseToAdd : licensesToAdd) {
            Optional<String> idFromExternalIdLookup = lookupExternalIdsInExtarnalIdToIdLookupMap(externalIdToIdLookupMap, licenseToAdd.getExternalIds());
            if(idFromExternalIdLookup.isPresent() && overwriteIfExternalIdMatches) {
                if (! idFromExternalIdLookup.get().equals(licenseToAdd.getId())){
                    log.warn("The id=["+ licenseToAdd.getId() + "] did not match [" + idFromExternalIdLookup.get() + "], which was determined via external IDs. It gets overwritten.");
                    licenseToAdd.setId(idFromExternalIdLookup.get());
                }
                result.add(licenseToAdd);
            } else if (! knownLicenseNames.contains(licenseToAdd.getId()) ||
                    overwriteIfIdMatchesEvenWithoutExternalIdMatch) {
                result.add(licenseToAdd);
            }
        }

        return result;
    }

    private void addLicenses(List<License> filteredLicenses, User user) throws TException {
        final List<License> addedLicenses = licenseClient.addOrOverwriteLicenses(filteredLicenses, user);

        if (addedLicenses == null) {
            log.error("There were errors while adding licenses from Import.");
        } else {
            log.info("Everything went fine.");
        }
    }
}
