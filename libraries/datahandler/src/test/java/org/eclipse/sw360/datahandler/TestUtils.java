/*
 * Copyright Siemens AG, 2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import com.ibm.cloud.cloudant.v1.Cloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceCloudant;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseInstanceTrackerCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettingsTest;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import org.apache.thrift.TBase;
import org.apache.thrift.TFieldIdEnum;
import org.hamcrest.*;
import org.hamcrest.collection.IsEmptyCollection;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Iterables.tryFind;
import static com.google.common.collect.Ordering.usingToString;
import static java.lang.String.format;
import static java.util.Collections.sort;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeNoException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author daniele.fognini@tngtech.com
 */
public class TestUtils {
    public static final String BLACK_HOLE_ADDRESS = "100::/64";

    private static final List<String> dbNames = ImmutableList.of(
            DatabaseSettingsTest.COUCH_DB_DATABASE,
            DatabaseSettingsTest.COUCH_DB_ATTACHMENTS,
            DatabaseSettingsTest.COUCH_DB_USERS);

    static {
        assertTestDbNames();
    }

    public static void assertTestDbNames() {
        for (String dbName : dbNames) {
            assertTestString(dbName);

        }
    }

    public static void deleteAllDatabases() throws MalformedURLException {
        for (String dbName : dbNames) {
            deleteDatabase(DatabaseSettingsTest.getConfiguredClient(), dbName);
        }
    }

    public static void assertTestString(String testString) {
        assertThat(testString, containsString("test"));
    }

    public static Answer failOnUnexpectedMethod() {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                fail(format("unexpected call on this mock to <%s>", invocation.getMethod()));
                return null;
            }
        };
    }

    public static <T> T failingMock(Class<T> clazz) {
        final T mock = mock(clazz, failOnUnexpectedMethod());
        @SuppressWarnings("unused")
        final String toString = doReturn("failing mock for " + clazz).when(mock).toString();
        return mock;
    }

    public static User getAdminUser(Class caller) {
        User user = failingMock(User.class);

        doReturn(true).when(user).isSetUserGroup();
        doReturn(UserGroup.ADMIN).when(user).getUserGroup();

        doReturn(true).when(user).isSetEmail();
        doReturn(caller.getSimpleName() + "@tngtech.com").when(user).getEmail();

        doReturn(true).when(user).isSetDepartment();
        doReturn(caller.getPackage().getName()).when(user).getDepartment();

        doReturn(false).when(user).isSetSecondaryDepartmentsAndRoles();
        doReturn(null).when(user).getSecondaryDepartmentsAndRoles();

        return user;
    }

    public static void deleteDatabase(Cloudant httpClient, String dbName) throws MalformedURLException {
        assertTestString(dbName);

        DatabaseInstanceCloudant instance = new DatabaseInstanceCloudant(httpClient);
        if (instance.checkIfDbExists(dbName))
            instance.deleteDatabase(dbName);

        // Giving 500ms Delay between Deleting and Creating test Db
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    public static void createDatabase(Cloudant httpClient, String dbName) throws MalformedURLException {
        assertTestString(dbName);

        DatabaseInstanceCloudant instance = new DatabaseInstanceCloudant(httpClient);

        if (instance.checkIfDbExists(dbName)) {
            instance.deleteDatabase(dbName);
        }
        instance.createDB(dbName);

        DatabaseInstanceTrackerCloudant.destroy();
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum>
    Comparator<T> orderByField(final F field) {
        return orderByField(field, usingToString());
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum>
    Ordering<T> orderByField(final F field, final Comparator<Object> fieldComparator) {
        return Ordering.from(new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                Object fieldValue1 = o1.getFieldValue(field);
                Object fieldValue2 = o2.getFieldValue(field);
                return fieldComparator.compare(fieldValue1, fieldValue2);
            }
        });
    }

    public static <T extends TBase<T, F>, F extends TFieldIdEnum> void sortByField(List<T> list, F field) {
        Comparator<T> comparator = orderByField(field);
        sort(list, comparator);
    }

    public static Matcher<String> nullOrEmpty() {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return isNullOrEmpty(item);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("not an empty string");
            }
        };
    }

    public static <T> Matcher<Collection<? extends T>> emptyOrNullCollectionOf(final Class<T> clazz) {
        return new EmptyOrNullCollectionMatcher<>(clazz);
    }

    public static NetworkInterface getAvailableNetworkInterface() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isLoopback())
                    continue;

                if (networkInterface.isUp())
                    return networkInterface;
            }
            return null;
        } catch (SocketException e) {
            return null;
        }
    }


    public static Matcher<NetworkInterface> isAvailable() {
        return new TypeSafeMatcher<NetworkInterface>() {
            @Override
            protected boolean matchesSafely(NetworkInterface item) {
                try {
                    if (item != null && item.isUp())
                        return true;
                } catch (SocketException ignored) {
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("an available network interface");
            }
        };
    }

    @SafeVarargs
    public static <F> F[] restrictedToFields(F... fields) {
        return fields;
    }

    @SafeVarargs
    public static <T extends TBase<T, F>, F extends TFieldIdEnum> Matcher<? super T> equalTo(final T expected, final F... fields) {
        if (fields.length == 0) {
            return CoreMatchers.equalTo(expected);
        }

        return new TypeSafeDiagnosingMatcher<T>() {
            @Override
            protected boolean matchesSafely(T item, Description mismatchDescription) {
                for (F field : fields) {
                    Object fieldValue = item.getFieldValue(field);
                    Object expectedFieldValue = expected.getFieldValue(field);

                    if (!Objects.equals(fieldValue, expectedFieldValue)) {
                        mismatchDescription.appendText("field " + field + " does not match, it was :");
                        mismatchDescription.appendValue(fieldValue);
                        return false;
                    }
                }

                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("object with fields: ");
                description.appendValueList("{", ",", "}", Arrays.asList(fields));
                description.appendText(" same as object: ");
                description.appendValue(expected);
            }
        };
    }

    public static void assumeCanConnectTo(String url) {
        try {
            new URL(url).openConnection().getInputStream().close();
        } catch (IOException e) {
            assumeNoException(e);
        }
    }

    private static class EmptyOrNullCollectionMatcher<T> extends TypeSafeCollectionMatcher<T> {
        public EmptyOrNullCollectionMatcher(Class<T> expectedType) {
            super(expectedType);
        }

        protected boolean matchesSafely(Collection<? extends T> item) {
            return IsEmptyCollection.<T>empty().matches(item);
        }

        @Override
        protected boolean nullMatches() {
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("an empty collection or null");
        }
    }

    private static abstract class TypeSafeCollectionMatcher<T> extends DiagnosingMatcher<Collection<? extends T>> {
        private final Class<T> expectedType;

        public TypeSafeCollectionMatcher(Class<T> expectedType) {
            this.expectedType = expectedType;
        }

        protected boolean nullMatches() {
            return false;
        }

        @Override
        public boolean matches(Object item, Description mismatchDescription) {
            if (item == null) {
                return nullMatches();
            }

            if (item instanceof Collection) {
                Collection<?> collection = (Collection<?>) item;

                Optional<?> find = tryFind(collection, not(instanceOf(expectedType)));
                if (!find.isPresent()) {
                    @SuppressWarnings("unchecked")
                    Collection<? extends T> safeItem = (Collection<? extends T>) collection;
                    return matchesSafely(safeItem);
                } else {
                    mismatchDescription.appendText("element '");
                    mismatchDescription.appendValue(find.get());
                    mismatchDescription.appendText("' is not an instance of " + expectedType.getCanonicalName());
                    return false;
                }
            } else {
                return false;
            }
        }

        protected abstract boolean matchesSafely(Collection<? extends T> safeItem);
    }
}
