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

import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcess;
import org.eclipse.sw360.datahandler.thrift.components.ExternalToolProcessStep;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertThat;

public class FossologyUtilsTest {

    @Test
    public void testEnsureOrderOfProcessSteps() {
        // given:
        ExternalToolProcessStep stepReport = new ExternalToolProcessStep();
        stepReport.setStepName(FossologyUtils.FOSSOLOGY_STEP_NAME_REPORT);
        ExternalToolProcessStep stepScan = new ExternalToolProcessStep();
        stepScan.setStepName(FossologyUtils.FOSSOLOGY_STEP_NAME_SCAN);
        ExternalToolProcessStep stepUpload = new ExternalToolProcessStep();
        stepUpload.setStepName(FossologyUtils.FOSSOLOGY_STEP_NAME_UPLOAD);

        ExternalToolProcess fossologyProcess = new ExternalToolProcess();
        fossologyProcess.setProcessSteps(Stream.of(stepScan, stepReport, stepUpload).collect(Collectors.toList()));

        // when:
        FossologyUtils.ensureOrderOfProcessSteps(fossologyProcess);

        // then:
        assertThat(fossologyProcess.getProcessSteps(), Matchers.contains(stepUpload, stepScan, stepReport));
    }
}
