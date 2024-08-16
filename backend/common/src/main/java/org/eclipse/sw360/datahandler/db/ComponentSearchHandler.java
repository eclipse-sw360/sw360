/*
 * Copyright Siemens AG, 2013-2016. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.db;

import com.ibm.cloud.cloudant.v1.Cloudant;
import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.couchdb.lucene.NouveauLuceneAwareDatabaseConnector;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.nouveau.designdocument.NouveauDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexDesignDocument;
import org.eclipse.sw360.nouveau.designdocument.NouveauIndexFunction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.common.utils.SearchUtils.OBJ_ARRAY_TO_STRING_INDEX;
import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;
import static org.eclipse.sw360.nouveau.LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX;

/**
 * Class for accessing the Lucene connector on the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ComponentSearchHandler {

    private static final Logger log = LogManager.getLogger(ComponentSearchHandler.class);

    private static final String DDOC_NAME = DEFAULT_DESIGN_PREFIX + "/lucene";

    private static final NouveauIndexDesignDocument luceneSearchView
        = new NouveauIndexDesignDocument("components",
            new NouveauIndexFunction(
                "function(doc) {" +
                OBJ_ARRAY_TO_STRING_INDEX +
                "    if(!doc.type || doc.type != 'component') return;" +
                "    arrayToStringIndex(doc.categories, 'categories');" +
                "    arrayToStringIndex(doc.languages, 'languages');" +
                "    arrayToStringIndex(doc.softwarePlatforms, 'softwarePlatforms');" +
                "    arrayToStringIndex(doc.operatingSystems, 'operatingSystems');" +
                "    arrayToStringIndex(doc.vendorNames, 'vendorNames');" +
                "    arrayToStringIndex(doc.mainLicenseIds, 'mainLicenseIds');" +
                "    if(doc.componentType && typeof(doc.componentType) == 'string' && doc.componentType.length > 0) {" +
                "      index('text', 'componentType', doc.componentType, {'store': true});" +
                "    }" +
                "    if(doc.name && typeof(doc.name) == 'string' && doc.name.length > 0) {" +
                "      index('text', 'name', doc.name, {'store': true});"+
                "    }" +
                "    if(doc.createdBy && typeof(doc.createdBy) == 'string' && doc.createdBy.length > 0) {" +
                "      index('text', 'createdBy', doc.createdBy, {'store': true});"+
                "    }" +
                "    if(doc.createdOn && doc.createdOn.length) {"+
                "      var dt = new Date(doc.createdOn);"+
                "      var formattedDt = `${dt.getFullYear()}${(dt.getMonth()+1).toString().padStart(2,'0')}${dt.getDate().toString().padStart(2,'0')}`;" +
                "      index('double', 'createdOn', Number(formattedDt), {'store': true});"+
                "    }" +
                "    if(doc.businessUnit && typeof(doc.businessUnit) == 'string' && doc.businessUnit.length > 0) {" +
                "      index('text', 'businessUnit', doc.businessUnit, {'store': true});"+
                "    }" +
                "}"));


    private final NouveauLuceneAwareDatabaseConnector connector;

    public ComponentSearchHandler(Cloudant cClient, String dbName) throws IOException {
        DatabaseConnectorCloudant db = new DatabaseConnectorCloudant(cClient, dbName);
        connector = new NouveauLuceneAwareDatabaseConnector(db, DDOC_NAME, dbName, db.getInstance().getGson());
        Gson gson = db.getInstance().getGson();
        NouveauDesignDocument searchView = new NouveauDesignDocument();
        searchView.setId(DDOC_NAME);
        searchView.addNouveau(luceneSearchView, gson);
        connector.addDesignDoc(searchView);
    }

    public List<Component> search(String text, final Map<String, Set<String>> subQueryRestrictions ){
        return connector.searchViewWithRestrictions(Component.class, luceneSearchView.getIndexName(),
                text, subQueryRestrictions);
    }

    public List<Component> searchAccessibleComponents(String text, final Map<String,
            Set<String>> subQueryRestrictions, User user ){
        List<Component> resultComponentList = connector.searchViewWithRestrictions(Component.class,
                luceneSearchView.getIndexName(), text, subQueryRestrictions);
        List<Component> componentList = new ArrayList<Component>();
        for (Component component : resultComponentList) {
            if (makePermission(component, user).isActionAllowed(RequestedAction.READ)) {
                componentList.add(component);
            }
        }
        return componentList;
    }

    public List<Component> searchWithAccessibility(String text, final Map<String, Set<String>> subQueryRestrictions,
                                                   User user ){
        List<Component> resultComponentList = connector.searchViewWithRestrictions(Component.class,
                luceneSearchView.getIndexName(), text, subQueryRestrictions);
        for (Component component : resultComponentList) {
            makePermission(component, user).fillPermissionsInOther(component);
        }
        return resultComponentList;
    }
}
