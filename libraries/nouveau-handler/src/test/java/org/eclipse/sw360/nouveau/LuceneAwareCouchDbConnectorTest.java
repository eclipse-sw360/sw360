/*
 * Copyright Siemens AG, 2024. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.nouveau;

import junit.framework.TestCase;

public class LuceneAwareCouchDbConnectorTest extends TestCase {

    public void testEnsureDesignIdMissing() {
        assert (LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX + "lucene").equals(LuceneAwareCouchDbConnector.ensureDesignId("lucene"));
    }

    public void testEnsureDesignIdContaining() {
        assert (LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX + "lucene").equals(LuceneAwareCouchDbConnector.ensureDesignId(LuceneAwareCouchDbConnector.DEFAULT_DESIGN_PREFIX + "lucene"));
    }
}
