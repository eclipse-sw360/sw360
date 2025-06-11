/*
 * Copyright Siemens AG, 2025. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.release;

import java.util.Arrays;

import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.jetbrains.annotations.NotNull;

public class ReleaseMergeSelector extends Release {

    public static @NotNull ReleaseMergeSelector from(Release release) {
        ReleaseMergeSelector selector = new ReleaseMergeSelector();
        ThriftUtils.copyFields(release, selector, Arrays.asList(Release._Fields.values()));
        return selector;
    }
}
