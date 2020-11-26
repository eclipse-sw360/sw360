/*
 * Copyright (c) Bosch.IO GmbH 2020.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.antenna.sw360.client.adapter;

import org.eclipse.sw360.antenna.sw360.client.rest.SW360LicenseClient;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360License;
import org.eclipse.sw360.antenna.sw360.client.rest.resource.licenses.SW360SparseLicense;

import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Service interface for an adapter supporting operations on SW360 license
 * entities.
 * </p>
 */
public interface SW360LicenseClientAdapter {
    /**
     * Returns the {@code SW360LicenseClient} used for the interaction with
     * the SW360 server.
     *
     * @return the underlying {@code SW360LicenseClient}
     */
    SW360LicenseClient getLicenseClient();

    /**
     * Returns a list with all licenses known to the system.
     *
     * @return a list with all the licenses known
     * @throws org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException if an error occurs
     */
    List<SW360SparseLicense> getLicenses();

    /**
     * Queries a license from SW360 by its (short) name. If the server
     * responds with a 404 status indicating that the license is unknown,
     * result is an empty {@code Optional}.
     *
     * @param license the ID of the desired license
     * @return an {@code Optional} with the license fetched from the server
     * @throws org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException if an error occurs
     */
    Optional<SW360License> getLicenseByName(String license);

    /**
     * Transforms the given sparse license to an entity with full properties.
     * This method looks up the license on the server by its name. It expects
     * the license to be present. If the lookup fails, an exception is thrown.
     *
     * @param sparseLicense the entity object for the sparse license
     * @return the resolved license
     * @throws org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException if an error occurs
     */
    SW360License enrichSparseLicense(SW360SparseLicense sparseLicense);

    /**
     * Creates a new license in SW360 based on the properties of the data
     * object passed in.
     *
     * @param license the data object for the new license
     * @return the newly created license
     * @throws org.eclipse.sw360.antenna.sw360.client.utils.SW360ClientException if an error occurs
     */
    SW360License createLicense(SW360License license);
}
