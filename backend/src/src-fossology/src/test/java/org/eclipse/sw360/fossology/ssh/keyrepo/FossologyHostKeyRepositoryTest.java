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
package org.eclipse.sw360.fossology.ssh.keyrepo;

import com.google.common.collect.ImmutableList;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;
import org.eclipse.sw360.fossology.db.FossologyFingerPrintRepository;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FossologyHostKeyRepositoryTest extends TestCase {
    private static final String GOOD_KEY = "\u0000\u0000\u0000\u0007ssh-rsa\u0000\u0000\u0000\u0003\u0001\u0000\u0001\u0000\u0000\u0001\u0001\u0000\u05FEZ/�㒯�>�\u0014>�S�\u001C5�\u0003%�~�\bD�N\u0007��\u0005s\u0012�\u0001���\u0007Θ;��y��w�v\u001C�\b\u0006H \f✃�\"\u0001E�0�V)����QIR��*�gW\u000F�:�\"p�Y X6��ۇ-z?\u007F��\b�\u0019�k��\u0016R�1�gF}I>��g��,ђZ0\u0001=\u001DX�/�;.��g0\u0014�}��b~tN�!I�\u0017�@�[y��i�@S�Q�X��Zxg%��[\u001C�'z8��ջ�hƁ\u0018|\\۵\u007F!\u0017\u0012i��¹�M\t�<�I�S�s�㬧�p��Փ�f~I�\u000E�+��\u0016\n" +
            "pw�\u001F}#�\u007F";
    @Rule
    public ExpectedException exception = ExpectedException.none();
    @Mock
    FossologyFingerPrintRepository connector;
    @Mock
    JSch jSch;
    FossologyHostKeyRepository repo;

    @Before
    public void setUp() throws Exception {
        repo = new FossologyHostKeyRepository(connector);
    }

    @Test
    public void testCheckNullIsNotAValidKey() throws Exception {
        assertThat(repo.check("host", null), is(HostKeyRepository.NOT_INCLUDED));
    }

    @Test
    public void testCheckAnEmptyKeyIsInvalid() throws Exception {
        byte[] key = "".getBytes();
        assertThat(repo.check("host", key), is(HostKeyRepository.NOT_INCLUDED));
    }

    @Test
    public void testCheckAnInvalidKey() throws Exception {
        byte[] key = "cfsdcvdf".getBytes();
        assertThat(repo.check("host", key), is(HostKeyRepository.NOT_INCLUDED));
    }

    @Test
    public void testCheckAValidNewKeyIsSavedAsUntrusted() throws Exception {
        when(connector.getAll()).thenReturn(Collections.<FossologyHostFingerPrint>emptyList());

        String host = "host";
        byte[] key = GOOD_KEY.getBytes();
        final String expectedFingerPrint = new HostKey(host, key).getFingerPrint(jSch);
        assertThat(expectedFingerPrint, not(isEmptyOrNullString()));

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                FossologyHostFingerPrint fingerPrint = (FossologyHostFingerPrint) invocation.getArguments()[0];
                assertThat(fingerPrint.getFingerPrint(), is(expectedFingerPrint));
                assertFalse(fingerPrint.isTrusted());
                return null;
            }
        }).when(connector).add(any(FossologyHostFingerPrint.class));

        assertThat(repo.check(host, key), is(HostKeyRepository.NOT_INCLUDED));

        verify(connector).getAll();
        verify(connector).add(any(FossologyHostFingerPrint.class));
    }

    @Test
    public void testCheckAValidKnownUntrustedKeyIsNotTrusted() throws Exception {
        String host = "host";
        byte[] key = GOOD_KEY.getBytes();
        final String expectedFingerPrint = new HostKey(host, key).getFingerPrint(jSch);
        assertThat(expectedFingerPrint, not(isEmptyOrNullString()));

        when(connector.getAll()).thenReturn(
                ImmutableList.of(new FossologyHostFingerPrint().setFingerPrint(expectedFingerPrint).setTrusted(false))
        );

        assertThat(repo.check(host, key), is(HostKeyRepository.NOT_INCLUDED));

        verify(connector).getAll();
        verify(connector, never()).add(any(FossologyHostFingerPrint.class));
    }

    @Test
    public void testCheckAValidKnownTrustedKeyIsTrusted() throws Exception {
        String host = "host";
        byte[] key = GOOD_KEY.getBytes();
        final String expectedFingerPrint = new HostKey(host, key).getFingerPrint(jSch);
        assertThat(expectedFingerPrint, not(isEmptyOrNullString()));

        when(connector.getAll()).thenReturn(
                ImmutableList.of(new FossologyHostFingerPrint().setFingerPrint(expectedFingerPrint).setTrusted(true))
        );

        assertThat(repo.check(host, key), is(HostKeyRepository.OK));

        verify(connector).getAll();
        verify(connector, never()).add(any(FossologyHostFingerPrint.class));
    }

    @Test
    public void testAdd() {
        UserInfo ui = mock(UserInfo.class);
        HostKey hk = mock(HostKey.class);

        expectUnsupported();
        repo.add(hk, ui);
    }

    @Test
    public void testRemove() {
        expectUnsupported();
        repo.remove("a", "b");
    }

    @Test
    public void testRemove2() {
        expectUnsupported();
        byte[] hk = "".getBytes();
        repo.remove("a", "b", hk);
    }

    @Test
    public void testGetHostKey() {
        assertThat(repo.getHostKey(), notNullValue());
    }

    @Test
    public void testGetHostKey2() {
        assertThat(repo.getHostKey("", ""), notNullValue());
    }

    @Test
    public void testGetId() {
        assertThat(repo.getKnownHostsRepositoryID(), not(isEmptyOrNullString()));
    }

    private void expectUnsupported() {
        exception.expect(UnsupportedOperationException.class);
        exception.expectMessage("this HostKeyRepository supports only queries for existence");
    }

}