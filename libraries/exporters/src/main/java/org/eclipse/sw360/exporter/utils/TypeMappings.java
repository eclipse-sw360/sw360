/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 * With contributions by Bosch Software Innovations GmbH, 2016.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.exporter.utils;

import org.apache.commons.csv.CSVRecord;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.ImportCSV;
import org.eclipse.sw360.datahandler.common.SW360Constants;
import org.eclipse.sw360.datahandler.thrift.CustomProperties;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.sw360.exporter.utils.ConvertRecord.putToTodos;

/**
 * @author johannes.najjar@tngtech.com
 * @author birgit.heydenreich@tngtech.com
 */
public class TypeMappings {

    @NotNull
    private static <T> Predicate<T> containedWithAddIn(final Set<T> knownIds) {
        return knownIds::add;
    }

    private static <T, U> List<T> getElementsWithIdentifiersNotInMap(Function<T, U> getIdentifier, Map<U, T> theMap, List<T> candidates) {
        Set<U> keysFromTheMap = theMap.keySet();
        return candidates.stream()
                .filter(a -> (containedWithAddIn(keysFromTheMap)).test(getIdentifier.apply(a)))
                .collect(Collectors.toList());
    }

    @NotNull
    private static Function<Todo, Integer> getTodoIdentifier() {
        return todo -> -1;
    }

    @NotNull
    private static Function<Obligation, Integer> getObligationIdentifier() {
        return o -> -1;
    }

    @NotNull
    private static Function<LicenseType, Integer> getLicenseTypeIdentifier() {
        return LicenseType::getLicenseTypeId;
    }

    @NotNull
    private static Function<RiskCategory, Integer> getRiskCategoryIdentifier() {
        return RiskCategory::getRiskCategoryId;
    }

    @NotNull
    private static Function<Risk, Integer> getRiskIdentifier() {
        return Risk::getRiskId;
    }

    @SuppressWarnings("unchecked")
    private static <T, U> Optional<Function<T, U>> getIdentifier(Class<T> clazz, @SuppressWarnings("unused") Class<U> uClass /*used to infer type*/) {
        if (clazz.equals(LicenseType.class)) {
            return Optional.of((Function<T, U>) getLicenseTypeIdentifier());
        } else if (clazz.equals(Obligation.class)) {
            return Optional.of((Function<T, U>) getObligationIdentifier());
        } else if (clazz.equals(RiskCategory.class)) {
            return Optional.of((Function<T, U>) getRiskCategoryIdentifier());
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> getAllFromDB(LicenseService.Iface licenseClient, Class<T> clazz) throws TException {
        if (clazz.equals(LicenseType.class)) {
            return (List<T>) licenseClient.getLicenseTypes();
        } else if (clazz.equals(Obligation.class)) {
            return (List<T>) licenseClient.getObligations();
        } else if (clazz.equals(RiskCategory.class)) {
            return (List<T>) licenseClient.getRiskCategories();
        }
        throw new SW360Exception("Unknown Type requested");
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> simpleConvert(List<CSVRecord> records, Class<T> clazz) throws SW360Exception {
        if (clazz.equals(LicenseType.class)) {
            return (List<T>) ConvertRecord.convertLicenseTypes(records);
        } else if (clazz.equals(Obligation.class)) {
            return (List<T>) ConvertRecord.convertObligation(records);
        } else if (clazz.equals(RiskCategory.class)) {
            return (List<T>) ConvertRecord.convertRiskCategories(records);
        }
        throw new SW360Exception("Unknown Type requested");
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> addAlltoDB(LicenseService.Iface licenseClient, Class<T> clazz, List<T> candidates, User user) throws TException {
        if (candidates != null && !candidates.isEmpty()) {
            if (clazz.equals(LicenseType.class)) {
                return (List<T>) licenseClient.addLicenseTypes((List<LicenseType>) candidates, user);
            } else if (clazz.equals(Obligation.class)) {
                return (List<T>) licenseClient.addObligations((List<Obligation>) candidates, user);
            } else if (clazz.equals(RiskCategory.class)) {
                return (List<T>) licenseClient.addRiskCategories((List<RiskCategory>) candidates, user);
            }
        }
        throw new SW360Exception("Unknown Type requested");
    }

    @NotNull
    public static <T, U> Map<U, T> getIdentifierToTypeMapAndWriteMissingToDatabase(LicenseService.Iface licenseClient, InputStream in, Class<T> clazz, Class<U> uClass, User user) throws TException {
        List<CSVRecord> records = ImportCSV.readAsCSVRecords(in);
        final List<T> recordsToAdd = simpleConvert(records, clazz);
        final List<T> fullList = CommonUtils.nullToEmptyList(getAllFromDB(licenseClient, clazz));

        final Optional<Function<T, U>> getIdentifier = getIdentifier(clazz, uClass);
        if (! getIdentifier.isPresent()) {
            return Collections.emptyMap();
        }

        Map<U, T> typeMap = fullList.stream()
                .collect(Collectors.toMap(f -> getIdentifier.get().apply(f), Function.identity()));
        final List<T> filteredList = getElementsWithIdentifiersNotInMap(getIdentifier.get(), typeMap, recordsToAdd);
        List<T> added = null;
        if (filteredList.size() > 0) {
            added = addAlltoDB(licenseClient, clazz, filteredList, user);
        }
        if (added != null) {
            final Map<U, T> mapFromAdded = added.stream()
                    .collect(Collectors.toMap(f -> getIdentifier.get().apply(f), Function.identity()));
            return Stream.concat(typeMap.entrySet().stream(), mapFromAdded.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return typeMap;
    }

    @NotNull
    public static Map<Integer, Risk> getIntegerRiskMap(LicenseService.Iface licenseClient, Map<Integer, RiskCategory> riskCategoryMap, InputStream in, User user) throws TException {
        List<CSVRecord> riskRecords = ImportCSV.readAsCSVRecords(in);
        final List<Risk> risksToAdd = ConvertRecord.convertRisks(riskRecords, riskCategoryMap);
        final List<Risk> risks = CommonUtils.nullToEmptyList(licenseClient.getRisks());
        Map<Integer, Risk> riskMap = risks.stream()
                .collect(Collectors.toMap(getRiskIdentifier(), Function.identity()));
        final List<Risk> filteredList = getElementsWithIdentifiersNotInMap(getRiskIdentifier(), riskMap, risksToAdd);
        List<Risk> addedRisks = null;
        if (filteredList.size() > 0) {
            addedRisks = licenseClient.addRisks(filteredList, user);
        }
        if (addedRisks != null){
            final Map<Integer, Risk> mapFromAdded = addedRisks.stream()
                    .collect(Collectors.toMap(getRiskIdentifier(), Function.identity()));
            return Stream.concat(riskMap.entrySet().stream(), mapFromAdded.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return riskMap;
    }

    @NotNull
    public static Map<Integer, Todo> getTodoMapAndWriteMissingToDatabase(LicenseService.Iface licenseClient, Map<Integer, Obligation> obligationMap, Map<Integer, Set<Integer>> obligationTodoMapping, InputStream in, User user) throws TException {
        List<CSVRecord> todoRecords = ImportCSV.readAsCSVRecords(in);
        final List<Todo> todos = CommonUtils.nullToEmptyList(licenseClient.getTodos());
        Map<Integer, Todo> todoMap = todos.stream()
                .collect(Collectors.toMap(getTodoIdentifier(), Function.identity()));
        final List<Todo> todosToAdd = ConvertRecord.convertTodos(todoRecords);
        final List<Todo> filteredTodos = getElementsWithIdentifiersNotInMap(getTodoIdentifier(), todoMap, todosToAdd);
        final Map<Integer, Todo> filteredMap = filteredTodos.stream()
                .collect(Collectors.toMap(getTodoIdentifier(), Function.identity()));
        putToTodos(obligationMap, filteredMap, obligationTodoMapping);
        //insertCustomProperties

        if (filteredTodos.size() > 0) {
            final List<Todo> addedTodos = licenseClient.addTodos(filteredTodos, user);
            if (addedTodos != null) {
                final Map<Integer, Todo> addedTodoMap = addedTodos.stream()
                        .collect(Collectors.toMap(f -> getTodoIdentifier().apply(f), Function.identity()));
                todoMap.putAll(addedTodoMap);
            }
        }
        return todoMap;
    }

    @NotNull
    public static Map<Integer, Todo> updateTodoMapWithCustomPropertiesAndWriteToDatabase(LicenseService.Iface licenseClient, Map<Integer, Todo> todoMap, Map<Integer, ConvertRecord.PropertyWithValue> customPropertiesMap, Map<Integer, Set<Integer>> todoPropertiesMap, User user) throws TException {
        for(Integer todoId : todoPropertiesMap.keySet()){
            Todo todo = todoMap.get(todoId);
            if(! todo.isSetCustomPropertyToValue()){
                todo.setCustomPropertyToValue(new HashMap<>());
            }
            for(Integer propertyWithValueId : todoPropertiesMap.get(todoId)){
                ConvertRecord.PropertyWithValue propertyWithValue = customPropertiesMap.get(propertyWithValueId);
                todo.getCustomPropertyToValue().put(propertyWithValue.getProperty(), propertyWithValue.getValue());
            }
        }

        if (todoMap.values().size() > 0) {
            final List<Todo> addedTodos = licenseClient.addTodos(new ArrayList<>(todoMap.values()), user);
            if (addedTodos != null) {
                final Map<Integer, Todo> addedTodoMap = addedTodos.stream()
                        .collect(Collectors.toMap(f -> getTodoIdentifier().apply(f), Function.identity()));
                todoMap.putAll(addedTodoMap);
            }
        }
        return todoMap;
    }

    public static  Map<Integer, ConvertRecord.PropertyWithValue> getCustomPropertiesWithValuesByIdAndWriteMissingToDatabase(LicenseService.Iface licenseClient, InputStream inputStream, User user) throws TException {
        List<CSVRecord> records = ImportCSV.readAsCSVRecords(inputStream);
        Optional<CustomProperties> dbCustomProperties = CommonUtils.wrapThriftOptionalReplacement(licenseClient.getCustomProperties(SW360Constants.TYPE_TODO));
        CustomProperties customProperties;
        customProperties = dbCustomProperties.orElseGet(() -> new CustomProperties().setDocumentType(SW360Constants.TYPE_TODO));
        Map<String, Set<String>> propertyToValuesToAdd = ConvertRecord.convertCustomProperties(records);
        customProperties.setPropertyToValues(CommonUtils.mergeMapIntoMap(propertyToValuesToAdd, customProperties.getPropertyToValues()));
        licenseClient.updateCustomProperties(customProperties, user);
        return ConvertRecord.convertCustomPropertiesById(records);
    }
}
