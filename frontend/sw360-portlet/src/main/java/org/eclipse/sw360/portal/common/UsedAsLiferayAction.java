/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.portal.common;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * The intent of this annotation is to suppress the unused warning for actions and nothing else. Configure your IDE to suppress it.
 * @author daniele.fognini@tngtech.com
 */
@Target({METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface UsedAsLiferayAction {

}
