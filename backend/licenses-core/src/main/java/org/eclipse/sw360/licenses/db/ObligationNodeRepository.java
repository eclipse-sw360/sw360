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
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode;

import com.ibm.cloud.cloudant.v1.model.DesignDocumentViewsMapReduce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * CRUD access for the Obligation Node class
 */
@Component
public class ObligationNodeRepository extends DatabaseRepositoryCloudantClient<ObligationNode> {

    private static final String ALL = "function(doc) { if (doc.type == 'obligationNode') emit(null, doc._id) }";
    private static final String BYNODETYPE = "function(doc) { if(doc.type == 'obligationNode') { emit(doc.nodeType, null) } }";
    private static final String BYNODETEXT = "function(doc) { if(doc.type == 'obligationNode') { emit(doc.nodeText, null) } }";
    private static final String BYOBLIGATIONID = "function(doc) { if(doc.type == 'obligationNode') { emit(doc.oblElementId, null) } }";

    @Autowired
    public ObligationNodeRepository(
            @Qualifier("CLOUDANT_DB_CONNECTOR_DATABASE") DatabaseConnectorCloudant db
    ) {
        super(db, ObligationNode.class);
        Map<String, DesignDocumentViewsMapReduce> views = new HashMap<>();
        views.put("all", createMapReduce(ALL, null));
        views.put("byobligationnodetype", createMapReduce(BYNODETYPE, null));
        views.put("byobligationnodetext", createMapReduce(BYNODETEXT, null));
        views.put("byobligationid", createMapReduce(BYOBLIGATIONID, null));
        initStandardDesignDocument(views, db);
    }

    public List<ObligationNode> searchByObligationNodeType(String type) {
        return queryByPrefix("byobligationnodetype", type);
    }

    public List<ObligationNode> searchByObligationNodeText(String text) {
        return queryByPrefix("byobligationnodetext", text);
    }

    public List<ObligationNode> searchByObligationNodeOblElementId(String oblElementId) {
        return queryByPrefix("byobligationid", oblElementId);
    }

}
