/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * SPDX-License-Identifier: EPL-1.0
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.sw360.fossology.ssh.keyrepo;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.UserInfo;
import org.eclipse.sw360.datahandler.thrift.fossology.FossologyHostFingerPrint;
import org.eclipse.sw360.fossology.db.FossologyFingerPrintRepository;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static java.lang.String.format;
import static org.apache.log4j.LogManager.getLogger;

/**
 * @author daniele.fognini@tngtech.com
 */
@Component
public class FossologyHostKeyRepository implements HostKeyRepository {
    private static final Logger log = getLogger(FossologyHostKeyRepository.class);

    // dummy instance needed only for calculating fingerprints
    // (HASH class comes from instance config, and we are not configuring it on the instances)
    private static final JSch J_SHC = new JSch();

    private final FossologyFingerPrintRepository hostKeyDb;

    @Autowired
    public FossologyHostKeyRepository(FossologyFingerPrintRepository fossologyFingerPrintRepository) {
        this.hostKeyDb = fossologyFingerPrintRepository;
    }

    private UnsupportedOperationException throwUnsupportedOperationException() {
        return new UnsupportedOperationException("this HostKeyRepository supports only queries for existence");
    }

    @Override
    public int check(String host, byte[] key) {
        String fingerPrint;
        try {
            fingerPrint = new HostKey(host, key).getFingerPrint(J_SHC);

            for (FossologyHostFingerPrint savedFingerPrint : hostKeyDb.getAll()) {
                if (fingerPrint.equals(savedFingerPrint.getFingerPrint())) {
                    if (savedFingerPrint.isTrusted()) {
                        return OK;
                    } else {
                        log.error("attempting connection to untrusted Host");
                        return NOT_INCLUDED;
                    }
                }
            }
        } catch (Exception e) {
            log.error(format("exception while verifying host '%s'", host), e);
            return NOT_INCLUDED;
        }
        log.error(format("cannot verify host '%s', fingerprint = '%s'", host, fingerPrint));

        final FossologyHostFingerPrint newFossologyHostFingerPrint = new FossologyHostFingerPrint().setFingerPrint(fingerPrint).setTrusted(false);
        hostKeyDb.add(newFossologyHostFingerPrint);
        return NOT_INCLUDED;
    }

    @Override
    public void add(HostKey hostkey, UserInfo ui) {
        throw throwUnsupportedOperationException();
    }

    @Override
    public void remove(String host, String type) {
        throw throwUnsupportedOperationException();
    }

    @Override
    public void remove(String host, String type, byte[] key) {
        throw throwUnsupportedOperationException();
    }

    @Override
    public String getKnownHostsRepositoryID() {
        return "fossologyHKR";
    }

    @Override
    public HostKey[] getHostKey() {
        return new HostKey[0];
    }

    @Override
    public HostKey[] getHostKey(String host, String type) {
        return new HostKey[0];
    }
}
