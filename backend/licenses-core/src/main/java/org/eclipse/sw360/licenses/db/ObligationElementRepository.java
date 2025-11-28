/*
 * Copyright TOSHIBA CORPORATION, 2021. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.licenses.db;

import java.util.HashMap;
import java.util.Map;
import java.util.*;

import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseRepositoryCloudantClient;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * CRUD access for the Obligation Element class
 */
@Component
public class ObligationElementRepository extends DatabaseRepositoryCloudantClient<ObligationElement> {

    private static final String ALL = "function(doc) { if (doc.type == 'obligationElement') emit(null, doc._id) }";
    private static final String BYLANGELEMENT = "function(doc) { if(doc.type == 'obligationElement') { emit(doc.langElement, null) } }";
    private static final String BYACTION = "function(doc) { if(doc.type == 'obligationElement') { emit(doc.action, null) } }";
    private static final String BYOBJECT = "function(doc) { if(doc.type == 'obligationElement') { emit(doc.object, null) } }";

    @Autowired
    public ObligationElementRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, ObligationElement.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byobligationlang", createMapReduce(BYLANGELEMENT, null));
        views.put("byobligationaction", createMapReduce(BYACTION, null));
        views.put("byobligationobject", createMapReduce(BYOBJECT, null));
        initStandardDesignDocument(views, db);
    }


    public List<ObligationElement> searchByObligationLang(String lang) {
        return queryByPrefix("byobligationlang", lang);
    }

    public List<ObligationElement> searchByObligationAction(String action) {
        return queryByPrefix("byobligationaction", action);
    }

    public List<ObligationElement> searchByObligationObject(String object) {
        return queryByPrefix("byobligationobject", object);
    }
}
