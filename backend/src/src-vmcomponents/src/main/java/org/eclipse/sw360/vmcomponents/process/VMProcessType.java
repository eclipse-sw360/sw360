/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License Version 2.0 as published by the
 * Free Software Foundation with classpath exception.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License version 2.0 for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (please see the COPYING file); if not, write to the Free
 * Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package org.eclipse.sw360.vmcomponents.process;

/**
 * Created by stefan.jaeger on 10.03.16.
 *
 * @author stefan.jaeger@evosoft.com
 */
public enum VMProcessType {

    GET_IDS,
    CLEAN_UP,
    STORE_NEW,
    MASTER_DATA,
    MATCH_SVM,
    MATCH_SW360,
    VULNERABILITIES,
    FINISH;

}
