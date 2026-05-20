/*
 * Copyright Siemens AG, 2026. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.rest.resourceserver.report;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SW360ReportControllerContentDispositionTest {

    @SuppressWarnings("rawtypes")
    @Mock
    private RestControllerHelper restControllerHelper;

    @Mock
    private SW360ReportService sw360ReportService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private SW360ReportController controller;

    private Method setContentDispositionMethod;

    @Before
    public void setUp() throws Exception {
        setContentDispositionMethod = SW360ReportController.class
                .getDeclaredMethod("setContentDisposition", HttpServletResponse.class, String.class);
        setContentDispositionMethod.setAccessible(true);
    }

    @Test
    public void shouldHandleAsciiFilenameWithoutEncoding() throws Exception {
        String fileName = "LicenseInfo-ProjectA-1.0-2026-05-16.docx";

        setContentDispositionMethod.invoke(controller, response, fileName);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), captor.capture());

        String header = captor.getValue();
        assertThat(header).contains("filename=\"" + fileName + "\"");
        assertThat(header).contains("filename*=UTF-8''");
    }

    @Test
    public void shouldReplaceNonAsciiInFallbackFilename() throws Exception {
        String fileName = "LicenseInfo-Gridscale X\u2122 Protection-1.0.docx";

        setContentDispositionMethod.invoke(controller, response, fileName);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), captor.capture());

        String header = captor.getValue();
        assertThat(header).contains("filename=\"LicenseInfo-Gridscale X_ Protection-1.0.docx\"");
        assertThat(header).contains("filename*=UTF-8''");
        assertThat(header).contains("%E2%84%A2");
    }

    @Test
    public void shouldEncodeSpacesAsPercent20() throws Exception {
        String fileName = "License Info Report.docx";

        setContentDispositionMethod.invoke(controller, response, fileName);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), captor.capture());

        String header = captor.getValue();
        String filenameStar = header.substring(header.indexOf("filename*=UTF-8''") + "filename*=UTF-8''".length());
        assertThat(filenameStar).doesNotContain("+");
        assertThat(filenameStar).contains("%20");
    }

    @Test
    public void shouldHandleUmlautsInFilename() throws Exception {
        String fileName = "LicenseInfo-Pr\u00fcfbericht-\u00fcber-1.0.docx";

        setContentDispositionMethod.invoke(controller, response, fileName);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(response).setHeader(eq("Content-Disposition"), captor.capture());

        String header = captor.getValue();
        assertThat(header).contains("filename=\"LicenseInfo-Pr_fbericht-_ber-1.0.docx\"");
        assertThat(header).contains("%C3%BC");
    }
}
