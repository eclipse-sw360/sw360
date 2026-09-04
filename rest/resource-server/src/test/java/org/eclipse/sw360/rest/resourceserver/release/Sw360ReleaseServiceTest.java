/*
 * Copyright Siemens AG, 2026.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.rest.resourceserver.release;

import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.core.RestControllerHelper;
import org.eclipse.sw360.rest.resourceserver.project.Sw360ProjectService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class Sw360ReleaseServiceTest {

    private Sw360ReleaseService releaseService;
    private MockedStatic<ThriftClients> thriftClientsMock;

    @BeforeEach
    void setUp() {
        releaseService = new Sw360ReleaseService(mock(RestControllerHelper.class), mock(Sw360ProjectService.class));
        thriftClientsMock = mockStatic(ThriftClients.class);
    }

    @AfterEach
    void tearDown() {
        thriftClientsMock.close();
    }

    @Test
    void shouldFetchOnlyReferencedComponents_whenSettingComponentDependentFieldsForReleaseList() throws Exception {
        User user = new User().setEmail("test@sw360.org");
        Release release1 = new Release().setId("r1").setComponentId("c1");
        Release release2 = new Release().setId("r2").setComponentId("c2");
        Release release3 = new Release().setId("r3").setComponentId("c1");
        AtomicReference<Set<String>> requestedComponentIds = new AtomicReference<>();

        Class<?> componentServiceIface = Class.forName(
                "org.eclipse.sw360.datahandler.thrift.components.ComponentService$Iface");
        Object componentClient = Proxy.newProxyInstance(
                componentServiceIface.getClassLoader(),
                new Class<?>[]{componentServiceIface},
                (proxy, method, args) -> {
                    if ("getComponentsShort".equals(method.getName())) {
                        requestedComponentIds.set((Set<String>) args[0]);
                        return List.of(
                                new Component().setId("c1").setComponentType(ComponentType.OSS),
                                new Component().setId("c2").setComponentType(ComponentType.COTS)
                        );
                    }
                    throw new UnsupportedOperationException("Unexpected call: " + method.getName());
                });

        thriftClientsMock.when(ThriftClients::makeComponentClient).thenAnswer(invocation -> componentClient);

        List<Release> releases = new ArrayList<>(List.of(release1, release2, release3));
        List<Release> result = releaseService.setComponentDependentFieldsInRelease(releases, user);

        assertEquals(ComponentType.OSS, result.get(0).getComponentType());
        assertEquals(ComponentType.COTS, result.get(1).getComponentType());
        assertEquals(ComponentType.OSS, result.get(2).getComponentType());
        assertEquals(Set.of("c1", "c2"), requestedComponentIds.get());
    }
}
