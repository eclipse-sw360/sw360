/*
 * Copyright Siemens AG, 2014-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.components.summary;

/**
 * Created by bodet on 10/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author andreas.reichel@tngtech.com
 */
public enum SummaryType {
    SUMMARY, // Create a summary view of a document for use in the frontend summary datatables
    SHORT, // Create a very short copy of a document (typically with only ID and name), in particular for use in the frontend home portlets.
    EXPORT_SUMMARY, // Create a more detailed summary for the purpose of Excel export
    HOME, // Create a more detailed summary with releases array for MyComponents portlet
    DETAILED_EXPORT_SUMMARY, // Create a more detailed summary for the purpose of CSV export
    LINKED_PROJECT_ACCESSIBLE, //for linked projects that are visible to user
    LINKED_PROJECT_NOT_ACCESSIBLE, // for linked projects that are not visible to user
}
