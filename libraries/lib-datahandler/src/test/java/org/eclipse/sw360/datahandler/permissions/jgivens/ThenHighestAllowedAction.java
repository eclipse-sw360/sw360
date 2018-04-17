/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.datahandler.permissions.jgivens;

import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import com.tngtech.jgiven.Stage;
import com.tngtech.jgiven.annotation.ExpectedScenarioState;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author johannes.najjar@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ThenHighestAllowedAction  extends Stage<ThenHighestAllowedAction> {

    @ExpectedScenarioState
    List<RequestedAction> allowedActions;


    public ThenHighestAllowedAction the_allowed_actions_should_be(List<RequestedAction> i) {
        assertThat(allowedActions, is(i));
        return self();
    }
}
