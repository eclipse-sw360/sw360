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
package org.eclipse.sw360.commonIO;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.attachments.AttachmentType;
import org.eclipse.sw360.datahandler.thrift.components.*;

import java.util.Arrays;

/**
 * @author johannes.najjar@tngtech.com
 */
public class SampleOptions {
    //Helpers for static methods
    public static final Joiner optionJoiner = Joiner.on(" | ");
    public static <T extends Enum<T>> String getJoinedOptions(Class<T> clazz) {
        return optionJoiner.join(FluentIterable.from(Arrays.asList(clazz.getEnumConstants())).transform(new Function<T, String>() {
            @Override
            public String apply(T input) {
                return input.name();
            }
        }));
    }

    //Options for sample inputs
    //generated
    public static final String ATTACHMENT_TYPE_OPTIONS = getJoinedOptions(AttachmentType.class);
    public static final String COMPONENT_TYPE_OPTIONS = getJoinedOptions(ComponentType.class);
    public static final String REPOSITORY_TYPE_OPTIONS = getJoinedOptions(RepositoryType.class);
    public static final String CLEARING_STATE_OPTIONS = getJoinedOptions(ClearingState.class);
    public static final String MAINLINE_STATE_OPTIONS = getJoinedOptions(MainlineState.class);
    public static final String RELEASE_RELEATIONSHIP_OPTIONS = getJoinedOptions(ReleaseRelationship.class);

    //static
    public static final String BOOL_OPTION = "TRUE | FALSE";
    public static final String URL_OPTION = "http://www.siemens.com";
    public static final String DATE_OPTION = "YYYY-MM-dd";
    public static final String FILE_OPTION = "sample.tar.gz";
    public static final String ID_OPTION = "41dc44194f705a36f60246b69a1cc6b5";
    public static final String NUMBER_OPTION = "8";
    public static final String USER_LIST_OPTION = "user@mail.com, user2@mail.com,..";
    public static final String USER_OPTION = "user@mail.com";
    public static final String PROGRAMMING_LANGUAGES_OPTION = "C, Java, Python,...";
    public static final String OPERATING_SYSTEMS_OPTION = "Ubuntu, Debian, Mint,...";
    public static final String SOFTWARE_PLATFORMS_OPTION = "Firefox, Java,...";
    public static final String CATEGORIES_OPTION = "OS, Library,...";
    public static final String LICENSE_LIST_OPTION = "GPL, LGPL,...";
    public static final String CPE_OPTION = "cpe:2.3:a:microsoft:internet_explorer:8.0.6001:beta:*:*:*:*:*:*";
    public static final String VERSION_OPTION = "1.82a";
}
