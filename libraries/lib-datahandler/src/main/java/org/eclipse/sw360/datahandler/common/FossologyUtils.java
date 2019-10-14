/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;

import java.util.Comparator;
import java.util.stream.Collectors;

public class FossologyUtils {

    public static final String FOSSOLOGY_STEP_NAME_UPLOAD = "01_upload";
    public static final String FOSSOLOGY_STEP_NAME_SCAN = "02_scan";
    public static final String FOSSOLOGY_STEP_NAME_REPORT = "03_report";

    public static void ensureOrderOfProcessSteps(ExternalToolProcess fossologyProcess) {
        fossologyProcess.setProcessSteps(fossologyProcess.getProcessSteps().stream()
                .sorted(Comparator.comparing(s -> s.getStepName())).collect(Collectors.toList()));
    }

}
