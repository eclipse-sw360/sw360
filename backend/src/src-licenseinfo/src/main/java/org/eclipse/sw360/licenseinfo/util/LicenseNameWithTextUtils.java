/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.licenseinfo.util;

import org.eclipse.sw360.datahandler.thrift.licenseinfo.LicenseNameWithText;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

public class LicenseNameWithTextUtils {

    /**
     * Sanitizes a given {@link LicenseNameWithText} object, which means for every
     * field:
     * <ul>
     * <li>field is trimmed</li>
     * <li>field is unset if empty</li>
     * </ul>
     *
     * @param licenseNameWithText
     *            object to sanitize
     *
     * @return the given object
     */
    public static LicenseNameWithText sanitize(LicenseNameWithText licenseNameWithText) {
        if (licenseNameWithText.isSetLicenseName()) {
            String name = licenseNameWithText.getLicenseName().trim();
            licenseNameWithText.setLicenseName(name.isEmpty() ? null : name);
        }
        if (licenseNameWithText.isSetLicenseText()) {
            String text = licenseNameWithText.getLicenseText().trim();
            licenseNameWithText.setLicenseText(text.isEmpty() ? null : text);
        }
        if (licenseNameWithText.isSetAcknowledgements()) {
            String acknowledgments = licenseNameWithText.getAcknowledgements().trim();
            licenseNameWithText.setAcknowledgements(acknowledgments.isEmpty() ? null : acknowledgments);
        }

        return licenseNameWithText;
    }

    /**
     * Determines if a {@link LicenseNameWithText} object is empty. Such an object
     * is empty if and only if:
     * <ul>
     * <li>name is null or empty (after trimming)</li>
     * <li>text is null or empty (after trimming)</li>
     * <li>acknowledgments are null or empty (after trimming)</li>
     * </ul>
     *
     * @param licenseNameWithText
     *            object to check
     *
     * @return true if the object is empty as by the above definitions
     */
    public static boolean isEmpty(LicenseNameWithText licenseNameWithText) {
        // @formatter:off
        return isNullOrEmpty(nullToEmpty(licenseNameWithText.getLicenseName()).trim())
                && isNullOrEmpty(nullToEmpty(licenseNameWithText.getLicenseText()).trim())
                && isNullOrEmpty(nullToEmpty(licenseNameWithText.getAcknowledgements()).trim());
        // @formatter:on
    }

    /**
     * Checks if two given {@link LicenseNameWithText} objects are equal. They are
     * equal if and only if all fields (name, text, acknowledgement) are equal after
     * <ul>
     * <li>nullToEmpty</li>
     * <li>trim</li>
     * </ul>
     *
     * @param lnwt1
     *            one object
     * @param lnwt2
     *            another object to compare with
     *
     * @return true if both objects are the same as by the above definition
     */
    public static boolean licenseNameWithTextEquals(LicenseNameWithText lnwt1, LicenseNameWithText lnwt2) {
        return nullToEmpty(lnwt1.getLicenseName()).trim().equals(nullToEmpty(lnwt2.getLicenseName()).trim())
                && nullToEmpty(lnwt1.getLicenseText()).trim().equals(nullToEmpty(lnwt2.getLicenseText()).trim())
                && nullToEmpty(lnwt1.getAcknowledgements()).trim().equals(nullToEmpty(lnwt2.getAcknowledgements()).trim());
    }
}
