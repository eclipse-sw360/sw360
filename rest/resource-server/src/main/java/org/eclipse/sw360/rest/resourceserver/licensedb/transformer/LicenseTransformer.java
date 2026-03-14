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

import org.eclipse.sw360.datahandler.thrift.Quadratic;
import org.eclipse.sw360.datahandler.thrift.licenses.License;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Transformer class to convert LicenseDB license data to SW360 License format.
 * 
 * <p>This transformer handles the conversion of license data received from the
 * LicenseDB service into the SW360 internal License data model. It maps all
 * relevant fields and handles null values gracefully.</p>
 * 
 * <p>Field mapping:</p>
 * <ul>
 *   <li>shortname -> shortname</li>
 *   <li>fullname -> fullname</li>
 *   <li>text -> text</li>
 *   <li>url -> url</li>
 *   <li>licenseType -> licenseType</li>
 *   <li>osiApproved -> osiApproved</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * LicenseDBLicense licenseDbData = licenseDBClient.getLicense("Apache-2.0");
 * LicenseTransformer transformer = new LicenseTransformer();
 * License sw360License = transformer.transform(licenseDbData);
 * </pre>
 */
@Component
public class LicenseTransformer {

    /**
     * Transform LicenseDB license data to SW360 License.
     *
     * @param licenseDbData the license data from LicenseDB as a Map
     * @return SW360 License object, or null if input is null
     */
    public License transform(Map<String, Object> licenseDbData) {
        if (licenseDbData == null) {
            return null;
        }
        
        License license = new License();
        
        // Map basic string fields
        license.setShortname(getStringValue(licenseDbData, "shortname"));
        license.setFullname(getStringValue(licenseDbData, "fullname"));
        license.setText(getStringValue(licenseDbData, "text"));
        license.setExternalLicenseLink(getStringValue(licenseDbData, "url"));
        
        // Map license type database ID
        String licenseTypeId = getStringValue(licenseDbData, "licenseTypeId");
        if (licenseTypeId != null) {
            license.setLicenseTypeDatabaseId(licenseTypeId);
        }
        
        // Map OSI approved flag to Quadratic enum
        Boolean osiApproved = getBooleanValue(licenseDbData, "osiApproved");
        if (osiApproved != null) {
            license.setOSIApproved(osiApproved ? Quadratic.YES : Quadratic.NA);
        }
        
        return license;
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
    
    /**
     * Extract a boolean value from the data map.
     *
     * @param data the data map
     * @param key the key to look up
     * @return the boolean value, or null if not found
     */
    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
}