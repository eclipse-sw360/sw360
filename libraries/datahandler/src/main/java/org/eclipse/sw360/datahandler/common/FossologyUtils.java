/*
 * Copyright Siemens AG, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.common;

import org.eclipse.sw360.datahandler.services.components.ExternalTool;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcessStatus;
import org.eclipse.sw360.datahandler.services.components.ExternalToolProcessStep;
import org.eclipse.sw360.datahandler.services.components.Release;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;

public class FossologyUtils {

    public static final String FOSSOLOGY_STEP_NAME_UPLOAD = "01_upload";
    public static final String FOSSOLOGY_STEP_NAME_SCAN = "02_scan";
    public static final String FOSSOLOGY_STEP_NAME_REPORT = "03_report";

    public static void ensureOrderOfProcessSteps(ExternalToolProcess fossologyProcess) {
        fossologyProcess.setProcessSteps(fossologyProcess.getProcessSteps().stream()
                .sorted(Comparator.comparing(s -> s.getStepName())).collect(Collectors.toList()));
    }

    public static Set<ExternalToolProcess> getExternalToolProcessesForTool(Release release, ExternalTool et) {
        if (release == null || et == null) {
            return new HashSet<>();
        }

        return nullToEmptySet(release.getExternalToolProcesses())
                .stream()
                .filter(etp -> et.equals(etp.getExternalTool()))
                .collect(Collectors.toSet());
    }

    public static Set<ExternalToolProcess> getNotOutdatedExternalToolProcessesForTool(Release release, ExternalTool et) {
        return getExternalToolProcessesForTool(release, et)
                .stream()
                .filter(etp -> !ExternalToolProcessStatus.OUTDATED.equals(etp.getProcessStatus()))
                .collect(Collectors.toSet());
    }

    public static ExternalToolProcessStep getExternalToolProcessStepOfFirstProcessForTool(
            Release release, ExternalTool et, String stepName) {
        if (release == null || et == null || CommonUtils.isNullEmptyOrWhitespace(stepName)) {
            return null;
        }

        return getNotOutdatedExternalToolProcessesForTool(release, et)
                .stream()
                .findFirst()
                .map(ExternalToolProcess::getProcessSteps)
                .orElseGet(ArrayList::new)
                .stream()
                .filter(etps -> stepName.equals(etps.getStepName()))
                .findFirst()
                .orElse(null);
    }

}
