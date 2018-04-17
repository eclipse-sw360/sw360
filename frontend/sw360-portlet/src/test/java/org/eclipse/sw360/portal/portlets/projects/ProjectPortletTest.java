/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.portal.portlets.projects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary;
import org.eclipse.sw360.portal.common.ThriftJsonSerializer;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasEntry;

/**
 * @author daniele.fognini@tngtech.com
 */
public class ProjectPortletTest {

    @Test
    public void testJsonOfClearing() throws Exception {
        ReleaseClearingStateSummary releaseClearingStateSummary = new ReleaseClearingStateSummary().setNewRelease(1).setReportAvailable(5).setUnderClearing(6).setUnderClearingByProjectTeam(17).setApproved(4);

        ThriftJsonSerializer thriftJsonSerializer = new ThriftJsonSerializer();
        String json = thriftJsonSerializer.toJson(releaseClearingStateSummary);

        assertThat(json, containsString("{\"newRelease\":1,\"underClearing\":6,\"underClearingByProjectTeam\":17,\"reportAvailable\":5,\"approved\":4}"));

        ObjectMapper objectMapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = objectMapper.readValue(json, Map.class);

        assertThat(map, hasEntry("newRelease", (Object) 1));


    }
}