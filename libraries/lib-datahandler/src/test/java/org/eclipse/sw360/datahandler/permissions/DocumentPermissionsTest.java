/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.datahandler.permissions;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.lang.NotImplementedException;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(DataProviderRunner.class)
public class DocumentPermissionsTest {

    @DataProvider
    public static Object[][] areActionsAllowedProvider() {
        // @formatter:off
        return new Object[][] {
                { null, Arrays.asList(false), true},
                { Arrays.asList(), Arrays.asList(false), true},
                { Arrays.asList(RequestedAction.READ), Arrays.asList(true), true},
                { Arrays.asList(RequestedAction.READ), Arrays.asList(false), false},
                { Arrays.asList(RequestedAction.READ, RequestedAction.WRITE, RequestedAction.DELETE), Arrays.asList(true, true, true), true},
                { Arrays.asList(RequestedAction.READ, RequestedAction.WRITE, RequestedAction.DELETE), Arrays.asList(true, true, false), false},
                { Arrays.asList(RequestedAction.READ, RequestedAction.WRITE, RequestedAction.DELETE), Arrays.asList(true, false, true), false},
                { Arrays.asList(RequestedAction.READ, RequestedAction.WRITE, RequestedAction.DELETE), Arrays.asList(false, true, true), false},
        };
        // @formatter:on
    }

    /**
     * test uses knowledge about the implementation but at least I like it better
     * that way than to write the same test for each implementing subclass with
     * complex mock setups...
     */
    @Test
    @UseDataProvider("areActionsAllowedProvider")
    public void testAreActionsAllowed(List<RequestedAction> queriedPermissions, List<Boolean> singlePermissionResults,
            boolean expected) {
        // given
        DocumentPermissions<Object> perm = new TestDocumentPermissions<>(singlePermissionResults);

        // when
        boolean actual = perm.areActionsAllowed(queriedPermissions);

        // then
        assertThat(actual, is(expected));
    }

    /**
     * We use an internal subclass and not a mock so that we do not have to mock the
     * unit under test (which we would have to do when we want to test an abstract
     * class)
     */
    private class TestDocumentPermissions<T> extends DocumentPermissions<T> {

        private List<Boolean> actionAllowedReturnValues;
        private int actionAllowedCallCount;

        protected TestDocumentPermissions(List<Boolean> actionAllowedReturnValues) {
            super(null, null);

            this.actionAllowedReturnValues = actionAllowedReturnValues;
            this.actionAllowedCallCount = 0;
        }

        @Override
        public boolean isActionAllowed(RequestedAction action) {
            if (actionAllowedCallCount < actionAllowedReturnValues.size()) {
                return actionAllowedReturnValues.get(actionAllowedCallCount++);
            } else {
                return actionAllowedReturnValues.get(actionAllowedReturnValues.size() - 1);
            }
        }

        @Override
        public void fillPermissions(T other, Map<RequestedAction, Boolean> permissions) {
            throw new NotImplementedException("method not needed in test right now");
        }

        @Override
        protected Set<String> getContributors() {
            throw new NotImplementedException("method not needed in test right now");
        }

        @Override
        protected Set<String> getModerators() {
            throw new NotImplementedException("method not needed in test right now");
        }
    }
}
