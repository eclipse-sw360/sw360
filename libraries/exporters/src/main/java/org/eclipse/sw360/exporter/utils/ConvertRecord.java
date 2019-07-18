/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.eclipse.sw360.commonIO.ConvertUtil;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.licenses.*;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.sw360.datahandler.common.ThriftEnumUtils;
import org.eclipse.sw360.datahandler.thrift.Ternary;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static org.eclipse.sw360.datahandler.common.CommonUtils.isNullEmptyOrWhitespace;

/**
 * Convert CSV Record to license objects
 *
 * @author cedric.bodet@tngtech.com
 * @author manuel.wickmann@tngtech.com
 */
public class ConvertRecord {

    private static Gson gson = new GsonBuilder().create();

    private ConvertRecord() {
        // Utility class with only static functions
    }

    public static List<RiskCategory> convertRiskCategories(List<CSVRecord> records) {
        ArrayList<RiskCategory> list = new ArrayList<>(records.size());

        for (CSVRecord record : records) {
            if (record.size() < 2) break;
            int id = Integer.parseInt(record.get(0));
            String text = record.get(1);

            RiskCategory category = new RiskCategory().setRiskCategoryId(id).setText(text);
            list.add(category);
        }

        return list;
    }

    public static Serializer<RiskCategory> riskCategorySerializer() {
        return new Serializer<RiskCategory>() {
            @Override
            public Function<RiskCategory, List<String>> transformer() {
                return riskCategory -> {
                        final ArrayList<String> out = new ArrayList<>(2);
                        out.add(((Integer)riskCategory.getRiskCategoryId()).toString());
                        out.add(riskCategory.getText());
                        return out;
                };
            }

            @Override
            public List<String> headers() {
                return ImmutableList.of("ID", "Category");
            }
        };
    }

    public static List<Risk> convertRisks(List<CSVRecord> records, Map<Integer, RiskCategory> categories) {
        List<Risk> list = new ArrayList<>(records.size());

        for (CSVRecord record : records) {
            if (record.size() < 3) break;
            int id = Integer.parseInt(record.get(0));
            int catId = Integer.parseInt(record.get(1));
            String text = record.get(2);

            Risk risk = new Risk().setRiskId(id).setText(text);
            risk.setCategory(categories.get(catId));

            list.add(risk);
        }

        return list;
    }

    public static Serializer<Risk> riskSerializer() {
        return new Serializer<Risk>() {
            @Override
            public Function<Risk, List<String>> transformer() {
                return risk -> {
                        final ArrayList<String> out = new ArrayList<>(3);

                        out.add(((Integer) risk.getRiskId()).toString());
                        out.add(((Integer) risk.getCategory().getRiskCategoryId()).toString());
                        out.add(risk.getText());
                        return out;
                };
            }

            @Override
            public List<String> headers() {
                return ImmutableList.of("ID", "Category_ID", "Text");
            }
        };
    }

    public static Map<String, Set<String>> convertCustomProperties(List<CSVRecord> records){
        Map<String, Set<String>> resultProperties = new HashMap<>();
        for (CSVRecord record : records){
            if(! isValidPropertyRecord(record)){
                break;
            }
            String property = record.get(1).trim();
            String value = record.get(2).trim();
            addPropertyAndValueToMap(property, value, resultProperties);
        }
        return resultProperties;
    }

    private static void addPropertyAndValueToMap(String property, String value, Map<String, Set<String>> propertyMap){
        if (propertyMap.containsKey(property)) {
            propertyMap.get(property).add(value);
        } else {
            Set<String> values = new HashSet<>();
            values.add(value);
            propertyMap.put(property, values);
        }
    }

    public static Map<Integer, PropertyWithValue> convertCustomPropertiesById(List<CSVRecord> records){
        Map<Integer, PropertyWithValue> resultPropertiesById = new HashMap<>();
        for (CSVRecord record : records){
            if(! isValidPropertyRecord(record)){
                break;
            }
            Integer id = Integer.parseInt(record.get(0));
            String property = record.get(1).trim();
            String value = record.get(2).trim();
            resultPropertiesById.put(id, new PropertyWithValue(property, value));
        }
        return resultPropertiesById;
    }

    private static boolean isValidPropertyRecord(CSVRecord record){
        if(record.size() < 3 ||
                isNullEmptyOrWhitespace(record.get(1))){
            return  false;
        }
        try {
            Integer.parseInt(record.get(0));
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static void fillTodoCustomPropertyInfo(List<Todo> todos, List<PropertyWithValueAndId> customProperties, SetMultimap<String, Integer> todoCustomPropertyMap) {
        int nextPropertyId = 0;
        for(Todo todo : todos){
            if(todo.isSetCustomPropertyToValue()){
                for(Map.Entry<String, String> entry : todo.getCustomPropertyToValue().entrySet()){
                    customProperties.add(new PropertyWithValueAndId(nextPropertyId, entry.getKey(), entry.getValue()));
                    todoCustomPropertyMap.put(todo.getId(), nextPropertyId);
                    nextPropertyId++;
                }
            }
        }
    }

    public static Serializer<PropertyWithValueAndId> customPropertiesSerializer() {
        return new Serializer<PropertyWithValueAndId>() {
            @Override
            public Function<PropertyWithValueAndId, List<String>> transformer() {
                return propertyWithValueAndId -> {

                    final ArrayList<String> out = new ArrayList<>(3);

                    out.add(propertyWithValueAndId.getId().toString());
                    out.add(propertyWithValueAndId.getProperty());
                    out.add(propertyWithValueAndId.getValue());
                    return out;
                };
            }

            @Override
            public List<String> headers() {
                return ImmutableList.of("ID", "Property", "Value");
            }

        };
    }

    public static List<Todo> convertTodos(List<CSVRecord> records) {
        List<Todo> list = new ArrayList<>(records.size());

        for (CSVRecord record : records) {
            if (record.size() < 2) break;

            String title = record.get(0);
            String text = record.get(1);

            Todo todo = new Todo();
            todo.setTitle(title);
            todo.setText(text);

            // Parse boolean values

            String developmentString = record.get(2);
            if (!"NULL".equals(developmentString)) {
                boolean development = parseBoolean(developmentString);
                todo.setDevelopment(development);
            }

            String distributionString = record.get(3);
            if (!"NULL".equals(distributionString)) {
                boolean distribution = parseBoolean(distributionString);
                todo.setDistribution(distribution);
            }

            if(record.size() >= 5) {
                Optional.ofNullable(record.get(4))
                        .filter(json -> ! "NULL".equals(json))
                        .map(json -> (Map<String, String>) gson.fromJson(json, new TypeToken<Map<String, String>>() { }.getType()))
                        .ifPresent(todo::setExternalIds);
            }

            list.add(todo);
        }
        return list;
    }

    public static Serializer<Todo> todoSerializer() {
        return new Serializer<Todo>() {
            @Override
            public Function<Todo, List<String>> transformer() {
                return todo -> {

                    final ArrayList<String> out = new ArrayList<>(5);

                    out.add(todo.getTitle());
                    out.add(todo.getText());
                    out.add(((Boolean) todo.isDevelopment()).toString());
                    out.add(((Boolean) todo.isDistribution()).toString());
                    out.add(Optional.ofNullable(todo.getExternalIds())
                            .map(gson::toJson)
                            .map(Object::toString)
                            .orElse("{}"));
                    return out;
                };
            }

            @Override
            public List<String> headers() {
                return ImmutableList.of("Title", "Text", "Development", "Distribution", "External IDs");
            }
        };
    }

   public static List<Obligation> convertObligation(List<CSVRecord> records) {
        List<Obligation> list = new ArrayList<>(records.size());

        for (CSVRecord record : records) {
            if (record.size() < 2) break;
            String id = record.get(0);
            String name = record.get(1);

            Obligation obligation = new Obligation().setObligationId(Integer.parseInt(id)).setName(name);
            list.add(obligation);
        }

        return list;
    }

    public static Serializer<Obligation> obligationSerializer() {
        return new Serializer<Obligation>() {
            @Override
            public Function<Obligation, List<String>> transformer() {
                return obligation -> {
                        final ArrayList<String> out = new ArrayList<>(2);

                        out.add(((Integer) obligation.getObligationId()).toString());
                        out.add(obligation.getName());
                        return out;
                };
            }

            @Override
            public List<String> headers() {
                return ImmutableList.of("ID", "Name");
            }
        };
    }


    public static void putToTodos(Map<Integer, Obligation> obligations, Map<Integer, Todo> todos, Map<Integer, Set<Integer>> obligationTodo) {
        Map<Integer, Set<Integer>> todoObligation = invertRelation(obligationTodo);


        for (Map.Entry<Integer, Set<Integer>> entry : todoObligation.entrySet()) {
            Todo todo = todos.get(entry.getKey());

            if (todo != null) {
                fillTodo(obligations, entry.getValue(), todo);
            }

        }
    }

    private static void fillTodo(Map<Integer, Obligation> obligations, Set<Integer> values, Todo todo) {
        for (int obligationId : values) {
            Obligation obligation = obligations.get(obligationId);

            if (obligation != null) {
                todo.addToObligations(obligation);
            }
        }
    }

    public static List<LicenseType> convertLicenseTypes(List<CSVRecord> records) {
        List<LicenseType> list = new ArrayList<>(records.size());

        for (CSVRecord record : records) {
            if (record.size() < 2) break;
            int id = Integer.parseInt(record.get(0));
            String text = record.get(1);

            LicenseType type = new LicenseType().setLicenseTypeId(id).setLicenseType(text);
            list.add(type);
        }

        return list;
    }

    public static Serializer<LicenseType> licenseTypeSerializer() {
        return new Serializer<LicenseType>() {
            @Override
            public Function<LicenseType, List<String>> transformer() {
                return licenseType -> {
                        final ArrayList<String> out = new ArrayList<>(2);
                        out.add(((Integer) licenseType.getLicenseTypeId()).toString());
                        out.add(licenseType.getLicenseType());
                        return out;
                };
            }

            @Override
            public List<String> headers() {
                return ImmutableList.of("ID", "Type");
            }
        };
    }


    public static List<License> fillLicenses(List<CSVRecord> records, Map<Integer, LicenseType> licenseTypeMap, Map<Integer, Todo> todoMap, Map<Integer, Risk> riskMap, Map<String, Set<Integer>> licenseTodo, Map<String, Set<Integer>> licenseRisk) {
        List<License> licenses = new ArrayList<>(records.size());

        for (CSVRecord record : records) {
            if (record.size() < 7) break;
            String identifier = record.get(0);
            String fullname = record.get(1);

            License license = new License().setId(identifier).setShortname(identifier).setFullname(fullname);


            String typeString = record.get(2);
            if (!Strings.isNullOrEmpty(typeString) && !"NULL".equals(typeString)) {
                Integer typeId = Integer.parseInt(typeString);
                LicenseType licenseType = licenseTypeMap.get(typeId);
                license.setLicenseType(licenseType);
            }

            String gplv2CompatString = record.get(3);
            if (!Strings.isNullOrEmpty(gplv2CompatString) && !"NULL".equals(gplv2CompatString)) {
                Ternary gplv2Compat = ThriftEnumUtils.stringToEnum(gplv2CompatString, Ternary.class);
                license.setGPLv2Compat(gplv2Compat);
            }

            String gplv3CompatString = record.get(4);
            if (!Strings.isNullOrEmpty(gplv3CompatString) && !"NULL".equals(gplv3CompatString)) {
                Ternary gplv3Compat = ThriftEnumUtils.stringToEnum(gplv3CompatString, Ternary.class);
                license.setGPLv3Compat(gplv3Compat);
            }

            String reviewdate = record.get(5);
            license.setReviewdate(ConvertUtil.parseDate(reviewdate));

            String text = record.get(6);
            license.setText(text);

            if(record.size() > 7) {
                String externalLink = record.get(7);
                license.setExternalLicenseLink(externalLink);
            }

            if(record.size() > 8) {
                Optional.ofNullable(record.get(8))
                        .map(json -> gson.fromJson(json, new TypeToken<Map<String, String>>() { }.getType()))
                        .map(o -> (Map<String, String>) o)
                        .ifPresent(license::setExternalIds);
            }

            // Add all risks
            Set<Integer> riskIds = licenseRisk.get(identifier);
            if (riskIds != null) {
                for (int riskId : riskIds) {
                    Risk risk = riskMap.get(riskId);
                    if (risk != null) {
                        license.addToRiskDatabaseIds(risk.getId());
                    }
                }
            }

            // Add all todos
            Set<Integer> todoIds = licenseTodo.get(identifier);
            if (todoIds != null) {
                for (int todoId : todoIds) {
                    Todo todo = todoMap.get(todoId);
                    if (todo != null) {
                        license.addToTodoDatabaseIds(todo.getId());
                    }
                }
            }

            licenses.add(license);
        }

        return licenses;
    }

    public static Serializer<License> licenseSerializer() {
        return new Serializer<License>() {
            @Override
            public Function<License, List<String>> transformer() {
                return license -> {
                    final ArrayList<String> out = new ArrayList<>(8);
                    out.add(CommonUtils.nullToEmptyString(license.getId()));
                    out.add(CommonUtils.nullToEmptyString(license.getFullname()));
                    out.add(license.isSetLicenseType() ? ((Integer) license.getLicenseType().getLicenseTypeId()).toString() :
                            CommonUtils.nullToEmptyString(license.getLicenseTypeDatabaseId()));
                    out.add(license.isSetGPLv2Compat() ? (license.getGPLv2Compat()).toString() : "");
                    out.add(license.isSetGPLv3Compat() ? (license.getGPLv3Compat()).toString() : "");
                    out.add(CommonUtils.nullToEmptyString(license.getReviewdate()));
                    out.add(CommonUtils.nullToEmptyString(license.getText()));
                    out.add(CommonUtils.nullToEmptyString(license.getExternalLicenseLink()));
                    out.add(Optional.ofNullable(license.getExternalIds())
                            .filter(json -> ! "NULL".equals(json))
                            .map(gson::toJson)
                            .map(Object::toString)
                            .orElse("{}"));
                    return out;
                };
            }

            @Override
            public List<String> headers() {
                return ImmutableList.of("Identifier", "Fullname", "Type", "Gplv2compat", "Gplv3compat", "reviewdate", "Text", "External Link", "External IDs");
            }
        };
    }

    public static Map<String, Set<Integer>> convertRelationalTable(List<CSVRecord> records) {
        Map<String, Set<Integer>> map = new HashMap<>(records.size());

        for (CSVRecord record : records) {
            if(record.size()<2) break;
            String mainId = record.get(0);
            int otherId = Integer.parseInt(record.get(1));

            if (map.containsKey(mainId)) {
                map.get(mainId).add(otherId);
            } else {
                Set<Integer> ids = new HashSet<>();
                ids.add(otherId);
                map.put(mainId, ids);
            }
        }

        return map;
    }

    public static Map<Integer, Set<Integer>> convertRelationalTableWithIntegerKeys(List<CSVRecord> records) {
        Map<String, Set<Integer>> stringMap = convertRelationalTable(records);
        Map<Integer, Set<Integer>> intMap = new HashMap<>();

        for (Map.Entry<String, Set<Integer>> entry : stringMap.entrySet()) {
            intMap.put(Integer.parseInt(entry.getKey()), entry.getValue());
        }

        return intMap;
    }


    private static Map<Integer, Set<Integer>> invertRelation(Map<Integer, Set<Integer>> obligationTodo) {
        Map<Integer, Set<Integer>> todoObligation = new HashMap<>();

        for (Map.Entry<Integer, Set<Integer>> entry : obligationTodo.entrySet()) {
            int obligationId = entry.getKey();
            for (int todoId : entry.getValue()) {
                if (todoObligation.containsKey(todoId)) {
                    todoObligation.get(todoId).add(obligationId);
                } else {
                    Set<Integer> obligationIds = new HashSet<>();
                    obligationIds.add(obligationId);
                    todoObligation.put(todoId, obligationIds);
                }
            }
        }

        return todoObligation;
    }


    private static boolean parseBoolean(String string) {

        if(string==null) return false;

        try {
            int value = Integer.parseInt(string);

            if (value != 0 && value != 1) {
                throw new IllegalArgumentException("Invalid string for boolean: " + string);
            }

            return value == 1;
        } catch (NumberFormatException e) {

         return Boolean.parseBoolean(string);
        }
    }

    @NotNull
    public static SetMultimap<Integer, String> getTodoToObligationMap(List<Todo> todos) {
        SetMultimap<Integer, String> obligationTodo = HashMultimap.create();

        for (Todo todo : todos) {
            if (todo.isSetObligations()) {
                for (Obligation obligation : todo.getObligations()) {
                    obligationTodo.put(obligation.getObligationId(), todo.getId());
                }
            }
        }
        return obligationTodo;
    }

    @NotNull
    public static SetMultimap<String, String> getLicenseToTodoMap(List<License> licenses) {
        SetMultimap<String, String> licenseToTodo = HashMultimap.create();

        for (License license : licenses) {
            if (license.isSetTodos()) {
                for (Todo todo : license.getTodos()) {
                    licenseToTodo.put(license.getId(), todo.getId());
                }
            }
        }
        return licenseToTodo;
    }

    @NotNull
    public static SetMultimap<String, Integer> getLicenseToRiskMap(List<License> licenses) {
        SetMultimap<String, Integer> licenseToRisk = HashMultimap.create();

        for (License license : licenses) {
            if (license.isSetRisks()) {
                for (Risk risk : license.getRisks()) {
                    licenseToRisk.put(license.getId(), risk.getRiskId());
                }
            }
        }
        return licenseToRisk;
    }

    public interface Serializer<T> {
        Function<T, List<String>> transformer();

        List<String> headers();
    }

    public static <T> List<List<String>> serialize(List<T> in, Serializer<T> serializer) {
        return serialize(in, serializer.headers(), serializer.transformer());
    }

    public static <T> List<List<String>> serialize(List<T> in, List<String> headers, Function<T, List<String>> function) {
        final ArrayList<List<String>> out = new ArrayList<>(in.size() + 1);
        out.add(headers);
        for (T t : in) {
            out.add(function.apply(t));
        }
        return out;
    }

    @NotNull
    public static <T, U> List<List<String>> serialize(SetMultimap<T, U> aToB, List<String> headers) {
        final List<List<String>> mapEntryList = new ArrayList<>(aToB.size() + 1);

        mapEntryList.add(headers);

        for (Map.Entry<T, U> mapEntry : aToB.entries()) {
            final ArrayList<String> entry = new ArrayList<>(2);
            entry.add(mapEntry.getKey().toString());
            entry.add(mapEntry.getValue().toString());
            mapEntryList.add(entry);
        }
        return mapEntryList;
    }

    public static class PropertyWithValue{
        private String property;
        private String value;

        public PropertyWithValue(String property, String value){
            this.property = property;
            this.value = value;
        }

        public String getProperty() {
            return property;
        }

        public String getValue() {
            return value;
        }
    }

    public static class PropertyWithValueAndId {
        private PropertyWithValue propertyWithValue;
        private Integer id;

        public PropertyWithValueAndId(Integer id, PropertyWithValue propertyWithValue){
            this.propertyWithValue = propertyWithValue;
            this.id = id;
        }

        public PropertyWithValueAndId(Integer id, String property, String value){
            this.propertyWithValue = new PropertyWithValue(property, value);
            this.id = id;
        }

        public Integer getId() {
            return id;
        }

        public String getProperty() {
            return propertyWithValue.getProperty();
        }

        public String getValue() {
            return propertyWithValue.getValue();
        }

    }

}
