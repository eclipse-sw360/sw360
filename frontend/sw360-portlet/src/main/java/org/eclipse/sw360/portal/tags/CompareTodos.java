/*
 * Copyright Siemens AG, 2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.tags;

import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.apache.thrift.meta_data.FieldMetaData;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptyList;
import static org.eclipse.sw360.portal.tags.TagUtils.*;

/**
 * Display the obligations that are added or added to/removed from department whitelist
 *
 * @author birgit.heydenreich@tngtech.com
 */
public class CompareTodos extends NameSpaceAwareTag {
    public static final List<Obligation._Fields> RELEVANT_FIELDS = FluentIterable
            .from(copyOf(Obligation._Fields.values())).filter(field -> isFieldRelevant(field)).toList();

    private List<Obligation> old;
    private List<Obligation> update;
    private List<Obligation> delete;
    private String department;
    private String tableClasses = "";
    private String idPrefix = "";

    public void setOld(List<Obligation> old) {
        this.old = nullToEmptyList(old);
    }

    public void setUpdate(List<Obligation> update) {
        this.update = nullToEmptyList(update);
    }

    public void setDelete(List<Obligation> delete) {
        this.delete = nullToEmptyList(delete);
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setTableClasses(String tableClasses) {
        this.tableClasses = tableClasses;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

    public int doStartTag() throws JspException {

        JspWriter jspWriter = pageContext.getOut();
        StringBuilder display = new StringBuilder();
        String namespace = getNamespace();

        try {
            renderTodos(display, old, update, delete);

            String renderString = display.toString();

            if (Strings.isNullOrEmpty(renderString)) {
                renderString = "<div class=\"alert alert-danger\">No changes in Obligation</div>";
            }

            jspWriter.print(renderString);
        } catch (IOException e) {
            throw new JspException(e);
        }
        return SKIP_BODY;
    }

    private void renderTodos(StringBuilder display, List<Obligation> current, List<Obligation> update, List<Obligation> delete) {
        List<Obligation> newWhitelistedTodos = update
                .stream()
                .filter(CommonUtils::isTemporaryObligation)
                .filter(oblig -> oblig.isSetWhitelist() && oblig.getWhitelist().contains(department))
                .collect(Collectors.toList());
        Map<String, Obligation> newWhitelistedTodosById = getTodosById(newWhitelistedTodos);
        Set<String> newWhitelistedTodoIds = newWhitelistedTodosById.keySet();
        renderTodoList(display, newWhitelistedTodosById, newWhitelistedTodoIds, "Add to database and to whitelist of " + department);

        List<Obligation> newBlacklistedTodos = update
                .stream()
                .filter(CommonUtils::isTemporaryObligation)
                .filter(oblig -> !oblig.isSetWhitelist() || !oblig.getWhitelist().contains(department))
                .collect(Collectors.toList());
        Map<String, Obligation> newBlacklistedTodosById = getTodosById(newBlacklistedTodos);
        Set<String> newBlacklistedTodoIds = newBlacklistedTodosById.keySet();
        renderTodoList(display, newBlacklistedTodosById, newBlacklistedTodoIds, "Add to database and <em>not</em> to whitelist of " + department);

        Map<String, Obligation> currentTodosById = getTodosById(current);
        Set<String> currentTodoIds = currentTodosById.keySet();
        List<Obligation> whitelistedTodos = update
                .stream()
                .filter(oblig -> !CommonUtils.isTemporaryObligation(oblig))
                .filter(oblig -> oblig.isSetWhitelist() && oblig.getWhitelist().contains(department))
                .filter(oblig -> currentTodoIds.contains(oblig.getId()))
                .filter(oblig -> !(currentTodosById.get(oblig.getId()).isSetWhitelist() &&
                        currentTodosById.get(oblig.getId()).getWhitelist().contains(department)))
                .collect(Collectors.toList());

        Set<String> whitelistedTodoIds = getTodosById(whitelistedTodos).keySet();
        renderTodoList(display, currentTodosById, whitelistedTodoIds, "Add to whitelist of " + department);

        List<Obligation> blacklistedTodos = delete
                .stream()
                .filter(oblig -> oblig.isSetWhitelist() && oblig.getWhitelist().contains(department))
                .filter(oblig -> currentTodoIds.contains(oblig.getId()))
                .filter(oblig -> currentTodosById.get(oblig.getId()).isSetWhitelist() &&
                        currentTodosById.get(oblig.getId()).getWhitelist().contains(department))
                .collect(Collectors.toList());

        Set<String> blacklistedTodoIds = getTodosById(blacklistedTodos).keySet();
        renderTodoList(display, currentTodosById, blacklistedTodoIds, "Remove from whitelist of " + department);

    }

    private void renderTodoList(StringBuilder display, Map<String, Obligation> allTodos, Set<String> obligIds, String msg) {
        if (obligIds.isEmpty()) return;
        display.append(String.format("<table class=\"%s\" id=\"%s%s\" >", tableClasses, idPrefix, msg));

        renderTodoRowHeader(display, msg);
        for (String deletedTodoId : obligIds) {
            renderTodoRow(display, allTodos.get(deletedTodoId));
        }

        display.append("</table>");
    }

    private static void renderTodoRowHeader(StringBuilder display, String msg) {

        display.append(String.format("<thead><tr><th colspan=\"%d\"> %s</th></tr><tr>", RELEVANT_FIELDS.size(), msg));
        for (Obligation._Fields field : RELEVANT_FIELDS) {
            display.append(String.format("<th>%s</th>", field.getFieldName()));
        }
        display.append("</tr></thead>");
    }

    private static void renderTodoRow(StringBuilder display, Obligation oblig) {
        display.append("<tr>");
        for (Obligation._Fields field : RELEVANT_FIELDS) {

            FieldMetaData fieldMetaData = Obligation.metaDataMap.get(field);
            Object fieldValue = oblig.getFieldValue(field);
            display.append(String.format("<td>%s</td>", getDisplayString(fieldMetaData.valueMetaData.type, fieldValue)));

        }
        display.append("</tr>");
    }

    private static Map<String, Obligation> getTodosById(List<Obligation> currentTodos) {
        return Maps.uniqueIndex(currentTodos, input -> input.getId());
    }

    private static boolean isFieldRelevant(Obligation._Fields field) {
        switch (field) {
            //ignored Fields
            case ID:
            case REVISION:
            case TYPE:
            case TITLE:
            case DEVELOPMENT_STRING:
            case DISTRIBUTION_STRING:
            case WHITELIST:
                return false;
            default:
                return true;
        }
    }
}
