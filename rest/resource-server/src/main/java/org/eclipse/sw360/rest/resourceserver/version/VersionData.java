/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.version;

import lombok.Data;

@Data
public class VersionData {
    String apiVersion;
    String buildTime;
    String buildNumber;
    String sw360Version;
    String gitBranch;
}
