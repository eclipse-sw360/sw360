/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.licensedb.transformer;

import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Transformer class to convert LicenseDB obligation data to SW360 Obligation format.
 * 
 * <p>This transformer handles the conversion of obligation data received from the
 * LicenseDB service into the SW360 internal Obligation data model. It maps all
 * relevant fields and handles null values gracefully.</p>
 * 
 * <p>Field mapping:</p>
 * <ul>
 *   <li>text -> text</li>
 *   <li>title -> title</li>
 *   <li>type -> obligationType</li>
 *   <li>level -> obligationLevel</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * LicenseDBObligation obligationDbData = licenseDBClient.getObligation("obligation-1");
 * ObligationTransformer transformer = new ObligationTransformer();
 * Obligation sw360Obligation = transformer.transform(obligationDbData);
 * </pre>
 */
@Component
public class ObligationTransformer {

    /**
     * Transform LicenseDB obligation data to SW360 Obligation.
     *
     * @param obligationDbData the obligation data from LicenseDB as a Map
     * @return SW360 Obligation object, or null if input is null
     */
    public Obligation transform(Map<String, Object> obligationDbData) {
        if (obligationDbData == null) {
            return null;
        }
        
        Obligation obligation = new Obligation();
        
        // Map basic string fields
        obligation.setText(getStringValue(obligationDbData, "text"));
        obligation.setTitle(getStringValue(obligationDbData, "title"));
        
        // Map obligation type if present
        String type = getStringValue(obligationDbData, "type");
        if (type != null) {
            obligation.setObligationType(type);
        }
        
        // Map obligation level if present
        String level = getStringValue(obligationDbData, "level");
        if (level != null) {
            obligation.setObligationLevel(level);
        }
        
        return obligation;
    }
    
    /**
     * Extract a string value from the data map.
     *
     * @param data the data map
     * @param key the key to look up
     * @return the string value, or null if not found
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }
}